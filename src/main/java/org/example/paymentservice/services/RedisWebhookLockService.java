package org.example.paymentservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisWebhookLockService {
    private static final Logger logger = LoggerFactory.getLogger(RedisWebhookLockService.class);

    private final StringRedisTemplate redisTemplate;

    public RedisWebhookLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String key, Duration ttl) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "locked", ttl);
        if (Boolean.TRUE.equals(success)) {
            logger.debug("Acquired webhook lock: {}", key);
            return true;
        }
        logger.warn("Webhook lock already exists: {}", key);
        return false;
    }
}
