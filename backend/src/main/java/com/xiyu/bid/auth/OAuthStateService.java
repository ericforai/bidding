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
    private final ConcurrentHashMap<String, java.time.Instant> localMap = new ConcurrentHashMap<>();
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
        localMap.put(state, java.time.Instant.now());
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
        
        java.time.Instant timestamp = localMap.remove(state);
        if (timestamp == null) {
            return false;
        }
        
        return java.time.Duration.between(timestamp, java.time.Instant.now()).compareTo(TTL) <= 0;
    }

    /**
     * Periodically clean expired states from local map.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000) // Every minute
    public void cleanExpiredLocalStates() {
        if (localMap.isEmpty()) {
            return;
        }
        
        java.time.Instant now = java.time.Instant.now();
        int removedCount = 0;
        
        var iterator = localMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (java.time.Duration.between(entry.getValue(), now).compareTo(TTL) > 0) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.debug("Cleaned up {} expired local OAuth states", removedCount);
        }
    }
}
