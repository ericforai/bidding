package com.xiyu.bid.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Async Configuration
 * Enables asynchronous method execution for AI service operations
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
    // Async thread pool configuration can be customized here if needed
}
