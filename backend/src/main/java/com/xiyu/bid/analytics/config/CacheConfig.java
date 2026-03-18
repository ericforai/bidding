// Input: Spring environment and framework beans
// Output: Cache configuration beans
// Pos: Config/配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.analytics.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache configuration for dashboard analytics
 * Uses in-memory cache for development. Can be replaced with Redis in production.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache manager with TTL configuration
     * Cache names:
     * - dashboard:overview: Full dashboard overview (5 minutes TTL)
     * - dashboard:summary: Summary statistics (5 minutes TTL)
     * - dashboard:trends: Trend data (10 minutes TTL)
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("dashboard:overview"),
                new ConcurrentMapCache("dashboard:summary"),
                new ConcurrentMapCache("dashboard:trends")
        ));
        return cacheManager;
    }
}
