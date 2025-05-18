package org.example.paymentservice.services;

import org.example.paymentservice.clients.AuthClient;
import org.example.paymentservice.dtos.TokenIntrospectionResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private static final String CACHE_PREFIX = "token:introspect:";
    private static final long TTL_SECONDS = 300; // 5 minutes

    @Autowired
    private AuthClient authClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public TokenIntrospectionResponseDTO introspect(String tokenHeader) {
        String token = tokenHeader.replace("Bearer ", "").trim();
        String cacheKey = CACHE_PREFIX + token;

        // 1. Check Redis cache
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof TokenIntrospectionResponseDTO dto) {
            logger.info("✅ Local cache hit for token introspection");
            return dto;
        }

        // 2. Fetch from auth-service
        TokenIntrospectionResponseDTO response = authClient.introspectToken(tokenHeader);

        // 3. Store in Redis
        redisTemplate.opsForValue().set(cacheKey, response, TTL_SECONDS, TimeUnit.SECONDS);
        logger.info("📦 Stored introspection result in Redis for 5 minutes");

        return response;
    }
}
