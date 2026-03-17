package com.xiyu.bid.service;

import com.xiyu.bid.config.ExportConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate limiting service for export operations.
 * Uses Redis to track export counts per user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ExportConfig exportConfig;

    private static final String EXPORT_RATE_LIMIT_KEY_PREFIX = "export:rateLimit:user:";

    /**
     * Check if the user has exceeded their export rate limit.
     *
     * @param userId The ID of the user to check
     * @return true if the user is within their limit, false if they have exceeded it
     */
    public boolean checkExportRateLimit(Long userId) {
        if (userId == null) {
            // If we can't identify the user, allow the request but log it
            log.warn("Rate limit check: userId is null, allowing request");
            return true;
        }

        try {
            String key = EXPORT_RATE_LIMIT_KEY_PREFIX + userId;

            // Get current count
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount == null) {
                // If increment failed, assume Redis is down and allow
                log.warn("Rate limit check: Redis increment failed, allowing request");
                return true;
            }

            // Set expiration on first increment (1 hour)
            if (currentCount == 1) {
                redisTemplate.expire(key, Duration.ofHours(1));
            }

            // Check if limit exceeded
            if (currentCount > exportConfig.getMaxExportsPerHour()) {
                log.info("Rate limit exceeded: userId={}, count={}, limit={}",
                    userId, currentCount, exportConfig.getMaxExportsPerHour());
                return false;
            }

            return true;

        } catch (Exception e) {
            // If Redis is unavailable, fail open (allow the request)
            log.error("Rate limit check failed for user {}: {}", userId, e.getMessage());
            return true;
        }
    }

    /**
     * Reset the rate limit for a specific user.
     * For admin use only.
     *
     * @param userId The ID of the user whose limit should be reset
     */
    public void resetRateLimit(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            String key = EXPORT_RATE_LIMIT_KEY_PREFIX + userId;
            redisTemplate.delete(key);
            log.info("Rate limit reset for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to reset rate limit for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Get the current export count for a user.
     *
     * @param userId The ID of the user
     * @return The number of exports in the current time window
     */
    public long getCurrentExportCount(Long userId) {
        if (userId == null) {
            return 0;
        }

        try {
            String key = EXPORT_RATE_LIMIT_KEY_PREFIX + userId;
            String count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count.toString()) : 0;
        } catch (Exception e) {
            log.error("Failed to get export count for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }
}
