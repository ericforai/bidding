package com.xiyu.bid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Redis for distributed rate limiting
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RateLimiter rateLimiter(RedisTemplate<String, String> redisTemplate) {
        return new RateLimiter(redisTemplate);
    }

    /**
     * Rate limiter using Redis for distributed rate limiting
     * Falls back to in-memory rate limiting if Redis is unavailable
     */
    public static class RateLimiter {
        private final RedisTemplate<String, String> redisTemplate;
        private final ConcurrentHashMap<String, RateLimitInfo> localCache = new ConcurrentHashMap<>();

        public RateLimiter(RedisTemplate<String, String> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        /**
         * Check if the request should be rate limited
         * @param key Unique identifier for the rate limit (e.g., IP address, username)
         * @param maxRequests Maximum number of requests allowed
         * @param duration Time window for the rate limit
         * @return true if the request should be allowed, false if rate limited
         */
        public boolean allowRequest(String key, int maxRequests, Duration duration) {
            try {
                String redisKey = "rate_limit:" + key;
                Long currentCount = redisTemplate.opsForValue().increment(redisKey);

                if (currentCount == null) {
                    // Fallback to local cache if Redis fails
                    return allowRequestLocal(key, maxRequests, duration);
                }

                if (currentCount == 1) {
                    // First request, set expiration
                    redisTemplate.expire(redisKey, duration);
                }

                return currentCount <= maxRequests;
            } catch (Exception e) {
                // Redis unavailable, fallback to local cache
                return allowRequestLocal(key, maxRequests, duration);
            }
        }

        private boolean allowRequestLocal(String key, int maxRequests, Duration duration) {
            RateLimitInfo info = localCache.compute(key, (k, existing) -> {
                long now = System.currentTimeMillis();
                if (existing == null || now > existing.expiryTime) {
                    return new RateLimitInfo(1, now + duration.toMillis());
                }
                return new RateLimitInfo(existing.count + 1, existing.expiryTime);
            });
            return info.count <= maxRequests;
        }

        private static class RateLimitInfo {
            final long count;
            final long expiryTime;

            RateLimitInfo(long count, long expiryTime) {
                this.count = count;
                this.expiryTime = expiryTime;
            }
        }
    }
}
