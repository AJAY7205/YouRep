"""Kafka-driven video transcoding microservice.

Consumes transcode requests from Kafka, downloads the original from MinIO,
converts it to an H.264/AAC MP4 via FFmpeg, uploads the result back to MinIO,
and produces a completion event on Kafka.

Runs a bounded pool of FFmpeg workers (default: CPU cores - 1) so multiple
videos transcode concurrently without thrashing the machine.
"""

import json
import logging
import os
import signal
import subprocess
import tempfile
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING

from dotenv import load_dotenv
from kafka import KafkaConsumer, KafkaProducer, TopicPartition, OffsetAndMetadata
from minio import Minio
from minio.error import S3Error

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("transcoder.log", encoding="utf-8"),
    ],
)
log = logging.getLogger("transcoder")

load_dotenv()

# --------------------------------------------------------------------------
# Configuration (from environment / .env)
# --------------------------------------------------------------------------

KAFKA_BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_GROUP = os.getenv("KAFKA_GROUP_ID", "python-transcoder-group")
REQUESTS_TOPIC = os.getenv("TRANSCODE_REQUESTS_TOPIC", "transcode-requests")
COMPLETION_TOPIC = os.getenv("TRANSCODE_COMPLETION_TOPIC", "transcode-completion")
PROGRESS_TOPIC = os.getenv("TRANSCODE_PROGRESS_TOPIC", "transcode-progress")

MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "localhost:9000")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "minio")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "minio123")
MINIO_BUCKET = os.getenv("MINIO_VIDEO_BUCKET", "videos")
MINIO_SECURE = os.getenv("MINIO_SECURE", "false").lower() == "true"

FFMPEG_PRESET = os.getenv("FFMPEG_PRESET", "veryfast")
FFMPEG_CRF = os.getenv("FFMPEG_CRF", "23")
CORES = os.cpu_count() or 2
try:
    _max_workers = int(os.getenv("MAX_WORKERS", "0"))
except ValueError:
    _max_workers = 0
MAX_WORKERS = _max_workers if _max_workers > 0 else max(1, CORES - 1)
POLL_TIMEOUT_MS = 1000

STATUS_SUCCESS = "SUCCESS"
STATUS_FAILED = "FAILED"


# --------------------------------------------------------------------------
# MinIO helpers
# --------------------------------------------------------------------------

def make_minio_client() -> Minio:
    return Minio(
        MINIO_ENDPOINT,
        access_key=MINIO_ACCESS_KEY,
        secret_key=MINIO_SECRET_KEY,
        secure=MINIO_SECURE,
    )


# --------------------------------------------------------------------------
# FFmpeg transcoding (bounded worker pool)
# --------------------------------------------------------------------------

@dataclass
class TranscodeJob:
    video_id: int
    input_path: Path
    output_path: Path


@dataclass
class TranscodeResult:
    video_id: int
    success: bool
    error: str = ""


class VideoTranscoder:
    """Runs one or more FFmpeg jobs with a bounded, CPU-aware thread pool."""

    def __init__(self, preset: str = "veryfast", crf: str = "23", max_workers: int | None = None):
        self.preset = preset
        self.crf = crf
        self.max_workers = max_workers or max(1, CORES - 1)
        self._pool = ThreadPoolExecutor(
            max_workers=self.max_workers,
            thread_name_prefix="ffmpeg",
        )
        log.info("Transcoder pool ready: %s workers (preset=%s, crf=%s)", self.max_workers, preset, crf)

    def _build_command(self, job: TranscodeJob) -> list[str]:
        return [
            "ffmpeg",
            "-hide_banner", "-loglevel", "error",
            "-y",
            "-i", str(job.input_path),
            "-c:v", "libx264",
            "-preset", self.preset,
            "-crf", self.crf,
            "-c:a", "aac",
            "-b:a", "128k",
            "-movflags", "+faststart",
            "-progress", "pipe:1",
            "-nostats",
            str(job.output_path),
        ]

    def _probe_duration(self, path: Path) -> float | None:
        """Return the input video duration in seconds via ffprobe, or None."""
        try:
            out = subprocess.run(
                ["ffprobe", "-v", "error", "-show_entries", "format=duration",
                 "-of", "json", str(path)],
                capture_output=True, text=True, timeout=60,
            )
            data = json.loads(out.stdout)
            return float(data["format"]["duration"])
        except (Exception, ValueError):  # noqa: BLE001
            return None

    def run(self, job: TranscodeJob, progress_cb=None) -> TranscodeResult:
        """Run one FFmpeg job synchronously. Called from within a pool worker.

        When ``progress_cb(percent: float, eta_seconds: float | None)`` is given
        it is invoked every ~2 seconds with real encode progress.
        """
        command = self._build_command(job)
        duration = self._probe_duration(job.input_path)
        if progress_cb:
            progress_cb(0.0, None)
        try:
            proc = subprocess.Popen(
                command,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                bufsize=1,
            )
            deadline = time.monotonic() + 3600
            start = time.monotonic()
            last_emit = 0.0
            out_time = 0.0

            for line in proc.stdout:
                if time.monotonic() > deadline:
                    proc.kill()
                    raise subprocess.TimeoutExpired(cmd=command, timeout=3600)
                line = line.strip()
                if not line or "=" not in line:
                    continue
                key, _, value = line.partition("=")
                if key.strip() == "out_time_us":
                    try:
                        out_time = float(value) / 1_000_000.0
                    except ValueError:
                        pass
                    if progress_cb and duration and out_time > 0:
                        elapsed = time.monotonic() - start
                        percent = min(100.0, out_time / duration * 100.0)
                        if elapsed >= 2.0 and elapsed - last_emit >= 2.0:
                            speed = out_time / elapsed
                            eta = (duration - out_time) / speed if speed > 0 else None
                            progress_cb(percent, max(0.0, eta))
                            last_emit = elapsed

            stderr = proc.stderr.read()
            proc.wait(timeout=10)

            if progress_cb and duration:
                progress_cb(100.0, 0.0)

            if proc.returncode == 0 and job.output_path.exists():
                return TranscodeResult(job.video_id, True)

            log.error("FFmpeg failed (rc=%s) for video %s:\n%s",
                      proc.returncode, job.video_id, stderr.strip()[-800:])
            return TranscodeResult(job.video_id, False, error=stderr.strip()[-800:])
        except subprocess.TimeoutExpired:
            proc.kill()
            return TranscodeResult(job.video_id, False, error="FFmpeg timed out after 1h")
        except FileNotFoundError:
            return TranscodeResult(job.video_id, False, error="FFmpeg not found on PATH")

    def submit(self, fn, *args, **kwargs):
        """Submit an arbitrary callable to the worker pool."""
        return self._pool.submit(fn, *args, **kwargs)

    def shutdown(self):
        self._pool.shutdown(wait=True)


# --------------------------------------------------------------------------
# Offset tracker: commits only contiguous, completed offsets (at-least-once)
# --------------------------------------------------------------------------

class OffsetTracker:
    """Tracks in-flight message offsets per partition so Kafka offsets are only
    committed after a job (and its completion event) actually succeeded."""

    def __init__(self, consumer: KafkaConsumer):
        self.consumer = consumer
        self.partitions: dict[TopicPartition, dict[int, "FutureRecordMetadata"]] = {}
        self._lock = threading.Lock()

    def _partition_state(self, tp: TopicPartition) -> dict[int, "FutureRecordMetadata"]:
        if tp not in self.partitions:
            self.partitions[tp] = {}
        return self.partitions[tp]

    def add(self, tp: TopicPartition, offset: int, future):
        with self._lock:
            self._partition_state(tp)[offset] = future

    def poll(self):
        """Commit the longest contiguous prefix of completed offsets per partition.
        Never commits an offset whose job is still in flight."""
        with self._lock:
            commits = {}
            for tp, inflight in list(self.partitions.items()):
                if not inflight:
                    continue
                next_expected = min(inflight)
                made_progress = False
                while next_expected in inflight:
                    future = inflight[next_expected]
                    if not future.done():
                        break
                    try:
                        future.result()  # raise if the job failed unexpectedly
                    except BaseException:  # noqa: BLE001 - keep the partition draining
                        log.exception("Job for offset %s failed unexpectedly; discarding message", next_expected)
                    del inflight[next_expected]
                    next_expected += 1
                    made_progress = True
                if made_progress:
                    commits[tp] = OffsetAndMetadata(next_expected, "")
            if commits:
                self.consumer.commit(commits)
                for tp in commits:
                    log.debug("Committed offset %s for %s", commits[tp].offset, tp)


# --------------------------------------------------------------------------
# Job pipeline: download -> transcode -> upload -> notify
# --------------------------------------------------------------------------

class JobProcessor:
    def __init__(self, minio_client: Minio, transcoder: VideoTranscoder, producer: KafkaProducer):
        self.minio = minio_client
        self.transcoder = transcoder
        self.producer = producer

    def _download(self, object_key: str, dest: Path) -> bool:
        try:
            self.minio.fget_object(MINIO_BUCKET, object_key, str(dest))
            return True
        except S3Error as e:
            log.error("MinIO download failed for %s: %s", object_key, e)
            return False

    def _upload(self, output_key: str, src: Path) -> bool:
        try:
            self.minio.fput_object(
                MINIO_BUCKET, output_key, str(src),
                content_type="video/mp4",
            )
            return True
        except S3Error as e:
            log.error("MinIO upload failed for %s: %s", output_key, e)
            return False

    def _notify(self, video_id: int, status: str, output_key: str | None = None, error: str = "") -> None:
        payload = {
            "videoId": video_id,
            "status": status,
            "outputKey": output_key,
            "error": error,
        }
        sent = self.producer.send(
            COMPLETION_TOPIC,
            key=str(video_id).encode(),
            value=payload,
        ).add_callback(
            lambda metadata: log.info(
                "Completion %s for video %s delivered to %s[%s]@%s",
                status, video_id, metadata.topic, metadata.partition, metadata.offset,
            )
        ).add_errback(
            lambda exc: log.error("Completion delivery failed for video %s: %s", video_id, exc)
        )
        # Block until the record is acknowledged so an undelivered completion
        # surfaces here instead of being silently dropped by flush().
        if not sent.get(timeout=10):
            raise RuntimeError(f"Completion event for video {video_id} was not acknowledged")

    def _publish_progress(self, video_id: int, percent: float, eta_seconds: float | None):
        payload = {
            "videoId": video_id,
            "percent": round(percent, 1),
            "etaSeconds": int(eta_seconds) if eta_seconds is not None else None,
        }
        try:
            self.producer.send(
                PROGRESS_TOPIC,
                key=str(video_id).encode(),
                value=payload,
            )
            self.producer.flush(timeout=5)
        except Exception as e:  # noqa: BLE001 - progress must never break the job
            log.warning("Failed to publish progress for video %s: %s", video_id, e)

    def process(self, message) -> None:
        data = message.value
        if isinstance(data, (str, bytes)):
            try:
                data = json.loads(data)
            except (TypeError, json.JSONDecodeError) as e:
                log.error("Skipping malformed message at offset %s: %s", message.offset, e)
                return

        video_id = data.get("videoId")
        object_key = data.get("objectKey")
        if video_id is None or not object_key:
            log.error("Skipping message missing videoId/objectKey: %s", data)
            return
        video_id = int(video_id)

        log.info("Processing video %s (objectKey=%s)", video_id, object_key)

        with tempfile.TemporaryDirectory(prefix=f"transcode-{video_id}-") as tmp:
            tmp_dir = Path(tmp)
            input_path = tmp_dir / "input.bin"
            output_path = tmp_dir / "output.mp4"

            if not self._download(object_key, input_path):
                self._notify(video_id, STATUS_FAILED, error="MinIO download failed")
                return

            result = self.transcoder.run(
                TranscodeJob(video_id, input_path, output_path),
                progress_cb=lambda p, e: self._publish_progress(video_id, p, e),
            )

            if not result.success:
                self._notify(video_id, STATUS_FAILED, error=result.error)
                return

            output_key = f"transcoded/{video_id}.mp4"
            if not self._upload(output_key, output_path):
                self._notify(video_id, STATUS_FAILED, error="MinIO upload failed")
                return

            self._notify(video_id, STATUS_SUCCESS, output_key=output_key)
            log.info("Video %s transcoded successfully -> %s", video_id, output_key)


# --------------------------------------------------------------------------
# Main loop
# --------------------------------------------------------------------------

def main():
    minio_client = make_minio_client()
    transcoder = VideoTranscoder(preset=FFMPEG_PRESET, crf=FFMPEG_CRF, max_workers=MAX_WORKERS)

    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        key_serializer=lambda k: k,
    )

    consumer = KafkaConsumer(
        REQUESTS_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP,
        group_id=KAFKA_GROUP,
        enable_auto_commit=False,
        auto_offset_reset="earliest",
        max_poll_records=32,
        key_deserializer=lambda k: k.decode() if k else None,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")) if v else None,
    )

    processor = JobProcessor(minio_client, transcoder, producer)
    tracker = OffsetTracker(consumer)

    shutdown = threading.Event()

    def handle_signal(_signum, _frame):
        log.info("Shutdown requested, draining workers...")
        shutdown.set()

    signal.signal(signal.SIGINT, handle_signal)
    try:
        signal.signal(signal.SIGTERM, handle_signal)
    except (AttributeError, ValueError):
        pass  # SIGTERM not available on all platforms (e.g. Windows)

    log.info("Transcoder listening on '%s' (bootstrap=%s)", REQUESTS_TOPIC, KAFKA_BOOTSTRAP)

    try:
        while not shutdown.is_set():
            records = consumer.poll(timeout_ms=POLL_TIMEOUT_MS, max_records=16)
            if not records:
                tracker.poll()
                continue

            for tp, messages in records.items():
                for message in messages:
                    if message.offset is None:
                        log.warning("Skipping message without an offset from %s", tp)
                        continue
                    future = transcoder.submit(_safe_process, processor, message)
                    tracker.add(tp, message.offset, future)

            tracker.poll()
    finally:
        log.info("Waiting for in-flight transcodes to finish...")
        transcoder.shutdown()
        tracker.poll()
        consumer.close()
        producer.close()
        log.info("Transcoder stopped.")


def _safe_process(processor: JobProcessor, message) -> None:
    try:
        processor.process(message)
    except Exception as e:  # noqa: BLE001 - never let a single job kill the worker
        log.exception("Unexpected error processing message at offset %s: %s", message.offset, e)


if TYPE_CHECKING:
    from kafka import FutureRecordMetadata  # noqa: F401 (type hint for OffsetTracker)


if __name__ == "__main__":
    main()
