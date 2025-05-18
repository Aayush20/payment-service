package org.example.paymentservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisIdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(RedisIdempotencyService.class);
    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdempotencyService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean isAlreadyProcessed(String key) {
        Boolean exists = stringRedisTemplate.hasKey(key);
        logger.debug("Checking idempotency for key '{}': {}", key, exists != null && exists);
        return exists != null && exists;
    }

    public void markAsProcessed(String key, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, "processed", ttl);
        logger.info("Idempotency key '{}' marked as processed for TTL {}", key, ttl);
    }
}
