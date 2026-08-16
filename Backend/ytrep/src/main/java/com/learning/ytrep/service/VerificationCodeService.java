package com.learning.ytrep.service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.learning.ytrep.exception.APIException;

@Service
public class VerificationCodeService {

    private static final String CODE_PREFIX = "verify:code:";
    private static final String ATTEMPTS_PREFIX = "verify:attempts:";
    private static final String COOLDOWN_PREFIX = "verify:cooldown:";

    private static final long CODE_TTL_MINUTES = 10;
    private static final long COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationCodeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateAndStoreCode(String email) {
        String normalized = normalizeEmail(email);

        String cooldown = redisTemplate.opsForValue().get(COOLDOWN_PREFIX + normalized);
        if (cooldown != null) {
            long remaining = redisTemplate.getExpire(COOLDOWN_PREFIX + normalized, TimeUnit.SECONDS);
            throw new APIException("Please wait " + Math.max(1, remaining) + " seconds before requesting a new code");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        redisTemplate.opsForValue().set(CODE_PREFIX + normalized, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(COOLDOWN_PREFIX + normalized, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);
        redisTemplate.delete(ATTEMPTS_PREFIX + normalized);

        return code;
    }

    public boolean verify(String email, String code) {
        String normalized = normalizeEmail(email);
        String storedCode = redisTemplate.opsForValue().get(CODE_PREFIX + normalized);

        if (storedCode == null) {
            throw new APIException("Verification code has expired. Please request a new one.");
        }

        if (!storedCode.equals(code)) {
            Long attempts = redisTemplate.opsForValue().increment(ATTEMPTS_PREFIX + normalized);
            if (attempts == null) {
                attempts = 1L;
            }
            redisTemplate.expire(ATTEMPTS_PREFIX + normalized, CODE_TTL_MINUTES, TimeUnit.MINUTES);

            if (attempts >= MAX_ATTEMPTS) {
                redisTemplate.delete(CODE_PREFIX + normalized);
                throw new APIException("Too many incorrect attempts. Please request a new code.");
            }
            throw new APIException("Invalid verification code. " + (MAX_ATTEMPTS - attempts) + " attempts remaining.");
        }

        redisTemplate.delete(CODE_PREFIX + normalized);
        redisTemplate.delete(ATTEMPTS_PREFIX + normalized);
        redisTemplate.delete(COOLDOWN_PREFIX + normalized);
        return true;
    }

    public void invalidate(String email) {
        String normalized = normalizeEmail(email);
        redisTemplate.delete(CODE_PREFIX + normalized);
        redisTemplate.delete(ATTEMPTS_PREFIX + normalized);
        redisTemplate.delete(COOLDOWN_PREFIX + normalized);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
