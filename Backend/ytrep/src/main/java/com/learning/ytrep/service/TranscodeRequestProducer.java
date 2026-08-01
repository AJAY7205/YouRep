package com.learning.ytrep.service;

import com.learning.ytrep.config.KafkaConfig;
import com.learning.ytrep.payload.TranscodeRequestDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TranscodeRequestProducer {

    private static final Logger log = LoggerFactory.getLogger(TranscodeRequestProducer.class);

    private final KafkaTemplate<String, TranscodeRequestDTO> kafkaTemplate;

    public TranscodeRequestProducer(KafkaTemplate<String, TranscodeRequestDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTranscodeRequest(TranscodeRequestDTO request) {
        CompletableFuture<SendResult<String, TranscodeRequestDTO>> future =
                kafkaTemplate.send(KafkaConfig.TRANSCODE_REQUESTS_TOPIC, request.getVideoId().toString(), request);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent transcode request for video {} to topic {}",
                        request.getVideoId(), KafkaConfig.TRANSCODE_REQUESTS_TOPIC);
            } else {
                log.error("Failed to send transcode request for video {}",
                        request.getVideoId(), ex);
            }
        });
    }
}
