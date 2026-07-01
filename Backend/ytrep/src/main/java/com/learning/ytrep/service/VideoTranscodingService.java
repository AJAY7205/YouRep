package com.learning.ytrep.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.bytedeco.ffmpeg.ffmpeg;
import org.bytedeco.javacpp.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class VideoTranscodingService {

    private static final Logger log = LoggerFactory.getLogger(VideoTranscodingService.class);
    private static final long TIMEOUT_SECONDS = 300;

    private boolean available;
    private String ffmpegPath;

    @PostConstruct
    void init() {
        try {
            String loadedPath = Loader.load(ffmpeg.class);
            Path dir = Path.of(loadedPath).getParent();
            String exeName = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? "ffmpeg.exe" : "ffmpeg";
            Path exePath = dir.resolve(exeName);
            if (!Files.exists(exePath)) {
                throw new RuntimeException("FFmpeg executable not found in cache dir: " + dir);
            }
            ffmpegPath = exePath.toAbsolutePath().toString();
            available = true;
            log.info("FFmpeg found at: {}", ffmpegPath);
        } catch (Throwable e) {
            available = false;
            log.warn("FFmpeg not available via JavaCPP: {}", e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public Path transcodeToMp4(Path inputFile) throws Exception {
        Path outputFile = Files.createTempFile("transcoded_", ".mp4");

        int exitCode = runProcess(true, inputFile, outputFile);

        if (exitCode != 0) {
            log.info("Remux failed (exit {}), falling back to transcode for {}", exitCode, inputFile.getFileName());
            Files.deleteIfExists(outputFile);
            outputFile = Files.createTempFile("transcoded_", ".mp4");
            exitCode = runProcess(false, inputFile, outputFile);
        }

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg processing failed with exit code " + exitCode);
        }

        if (Files.size(outputFile) == 0) {
            throw new RuntimeException("FFmpeg produced empty output file");
        }

        return outputFile;
    }

    private int runProcess(boolean remux, Path inputFile, Path outputFile) throws Exception {
        String[] args;
        if (remux) {
            log.info("Remuxing {} to MP4", inputFile.getFileName());
            args = new String[]{
                    ffmpegPath,
                    "-y",
                    "-i", inputFile.toAbsolutePath().toString(),
                    "-c:v", "copy",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-movflags", "+faststart",
                    outputFile.toAbsolutePath().toString()
            };
        } else {
            log.info("Transcoding {} to H.264 MP4", inputFile.getFileName());
            args = new String[]{
                    ffmpegPath,
                    "-y",
                    "-i", inputFile.toAbsolutePath().toString(),
                    "-c:v", "libx264",
                    "-crf", "23",
                    "-preset", "medium",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-movflags", "+faststart",
                    outputFile.toAbsolutePath().toString()
            };
        }

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg timed out after " + TIMEOUT_SECONDS + " seconds");
        }

        return process.exitValue();
    }
}
