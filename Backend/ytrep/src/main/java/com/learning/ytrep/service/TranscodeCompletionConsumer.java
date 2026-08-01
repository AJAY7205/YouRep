package com.learning.ytrep.service;

import com.learning.ytrep.config.KafkaConfig;
import com.learning.ytrep.model.Video;
import com.learning.ytrep.model.VideoStatus;
import com.learning.ytrep.payload.TranscodeCompletionDTO;
import com.learning.ytrep.repository.VideoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TranscodeCompletionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TranscodeCompletionConsumer.class);

    private final VideoRepository videoRepository;

    public TranscodeCompletionConsumer(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @KafkaListener(topics = KafkaConfig.TRANSCODE_COMPLETION_TOPIC, groupId = "video-processing-group")
    @Transactional
    public void onTranscodeCompletion(TranscodeCompletionDTO completion) {
        Video video = videoRepository.findByVideoId(completion.getVideoId());
        if (video == null) {
            log.warn("Transcode completion received for unknown video id {}", completion.getVideoId());
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(completion.getStatus())) {
            video.setStatus(VideoStatus.PUBLISHED);
            if (completion.getOutputKey() != null) {
                video.setTranscodedKey(completion.getOutputKey());
            }
            videoRepository.save(video);
            log.info("Video {} transcoding completed, status -> PUBLISHED (transcodedKey={})",
                    completion.getVideoId(), completion.getOutputKey());
        } else {
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            log.error("Video {} transcoding failed: {}", completion.getVideoId(), completion.getError());
        }
    }
}
