package com.xiyu.bid.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage OAuth2 state tokens for CSRF protection.
 * Uses Redis if available, otherwise falls back to in-memory map.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthStateService {

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, Boolean> localMap = new ConcurrentHashMap<>();
    private static final String REDIS_PREFIX = "oauth_state:";
    private static final Duration TTL = Duration.ofMinutes(10);

    public void storeState(String state) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(REDIS_PREFIX + state, "true", TTL);
                return;
            } catch (Exception e) {
                log.warn("Failed to store OAuth state in Redis, falling back to local map", e);
            }
        }
        localMap.put(state, true);
        // Periodically cleaning local map is omitted for simplicity in this POC stage
    }

    public boolean validateAndRemoveState(String state) {
        if (state == null || state.isBlank()) {
            return false;
        }

        if (redisTemplate != null) {
            try {
                Boolean deleted = redisTemplate.delete(REDIS_PREFIX + state);
                return Boolean.TRUE.equals(deleted);
            } catch (Exception e) {
                log.warn("Failed to validate OAuth state from Redis, checking local map", e);
            }
        }
        return localMap.remove(state) != null;
    }
}
