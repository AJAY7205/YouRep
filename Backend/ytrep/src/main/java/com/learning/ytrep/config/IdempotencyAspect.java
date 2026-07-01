package com.learning.ytrep.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.ytrep.service.IdempotencyService;
import com.learning.ytrep.service.IdempotencyService.CachedResponse;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(com.learning.ytrep.config.Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String idempotencyKey = request.getHeader("Idempotency-Key");

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return pjp.proceed();
        }

        CachedResponse cached = idempotencyService.getCachedResponse(idempotencyKey);
        if (cached != null) {
            return ResponseEntity.status(cached.status())
                    .contentType(MediaType.parseMediaType(cached.contentType()))
                    .body(cached.body());
        }

        Object result = pjp.proceed();

        if (result instanceof ResponseEntity<?> responseEntity) {
            try {
                Object body = responseEntity.getBody();
                String bodyJson = (body instanceof String) ? (String) body : objectMapper.writeValueAsString(body);
                String contentType = responseEntity.getHeaders().getContentType() != null
                        ? responseEntity.getHeaders().getContentType().toString()
                        : MediaType.APPLICATION_JSON_VALUE;
                idempotencyService.cacheResponse(idempotencyKey,
                        responseEntity.getStatusCode().value(), bodyJson, contentType);
            } catch (Exception e) {
                System.err.println("Failed to cache idempotency response: " + e.getMessage());
            }
        }

        return result;
    }
}
