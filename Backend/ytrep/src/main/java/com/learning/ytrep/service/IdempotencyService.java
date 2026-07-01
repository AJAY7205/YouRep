package com.learning.ytrep.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final long TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheResponse(String key, int statusCode, String bodyJson, String contentType) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("status", statusCode);
            data.put("body", bodyJson);
            data.put("contentType", contentType);
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + key, json, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            // Log and swallow — never fail the original request due to caching
            System.err.println("Failed to cache idempotency response: " + e.getMessage());
        }
    }

    public CachedResponse getCachedResponse(String key) {
        String json = redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + key);
        if (json == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            int status = node.get("status").asInt();
            String body = node.get("body").asText();
            String contentType = node.get("contentType").asText();
            return new CachedResponse(status, body, contentType);
        } catch (Exception e) {
            redisTemplate.delete(IDEMPOTENCY_PREFIX + key);
            return null;
        }
    }

    public record CachedResponse(int status, String body, String contentType) {}
}
