// Input: Spring environment and framework beans
// Output: Async configuration beans
// Pos: Config/配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
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
