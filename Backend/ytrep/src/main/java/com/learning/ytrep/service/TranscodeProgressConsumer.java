package com.learning.ytrep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.learning.ytrep.config.KafkaConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TranscodeProgressConsumer {

    private static final Logger log = LoggerFactory.getLogger(TranscodeProgressConsumer.class);

    private final TranscodeProgressService progressService;
    private final ObjectMapper objectMapper;

    public TranscodeProgressConsumer(TranscodeProgressService progressService, ObjectMapper objectMapper) {
        this.progressService = progressService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = KafkaConfig.TRANSCODE_PROGRESS_TOPIC,
            groupId = "video-processing-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onTranscodeProgress(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String videoId = node.get("videoId").asText();
            progressService.saveProgress(videoId, payload);
        } catch (Exception e) {
            log.warn("Ignoring malformed transcode progress: {}", payload);
        }
    }
}
