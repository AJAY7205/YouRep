package com.learning.ytrep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TranscodeProgressService {

    private static final String KEY_PREFIX = "transcode-progress:";
    private static final long TTL_HOURS = 1;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TranscodeProgressService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveProgress(String videoId, String payload) {
        redisTemplate.opsForValue().set(KEY_PREFIX + videoId, payload, TTL_HOURS, TimeUnit.HOURS);
    }

    public Map<String, Object> getProgress(Long videoId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + videoId);
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("videoId", videoId);
        empty.put("percent", 0);
        empty.put("etaSeconds", null);
        if (json == null) {
            return empty;
        }
        try {
            Map<String, Object> progress = objectMapper.readValue(json, new TypeReference<>() {});
            progress.putIfAbsent("videoId", videoId);
            progress.putIfAbsent("percent", 0);
            progress.putIfAbsent("etaSeconds", null);
            return progress;
        } catch (Exception e) {
            return empty;
        }
    }
}
