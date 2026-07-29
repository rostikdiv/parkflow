package com.parkflow.reservation.application;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for idempotency control using Redis.
 * See plan §5: Redis SETNX acts as a fast fail-fast layer for concurrent duplicate requests,
 * while the unique constraint in the DB acts as a durable fallback layer.
 */
@Service
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofSeconds(300);

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String idempotencyKey) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(IDEMPOTENCY_PREFIX + idempotencyKey, "processing", TTL);
        return Boolean.TRUE.equals(acquired);
    }
}
