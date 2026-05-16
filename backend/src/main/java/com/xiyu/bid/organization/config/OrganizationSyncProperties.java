package com.xiyu.bid.organization.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.organization-sync")
public class OrganizationSyncProperties {

    private static final int SECONDS_PER_DAY = 86400;
    private static final int DEFAULT_RETRIES = 3;
    private static final int BASE_DELAY_MS = 1000;
    private static final int MAX_DELAY_MS = 60000;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int RECONCILE_WINDOW_DAYS = 7;
    private static final int MAX_DIFF_THRESHOLD = 1000;
    private static final int INIT_PAGE_SIZE = 500;

    /** Xiyu ClientSDK application ID. */
    private String sdkAppId;

    /** Xiyu ClientSDK application secret. */
    private String sdkAppSecret;

    /** Consumer group name for SDK subscription. */
    private String sdkConsumerGroup = "xiyu-bid";

    /** Base URL for Xiyu organization REST API (lookback queries). */
    private String xiuyApiBaseUrl;

    /** Redis TTL in seconds for event dedup cache. */
    private int dedupRedisTtlSeconds = SECONDS_PER_DAY;

    /** Maximum retry attempts for failed event processing. */
    private int maxRetries = DEFAULT_RETRIES;

    /** Base retry delay in milliseconds. */
    private int retryBaseDelayMs = BASE_DELAY_MS;

    /** Maximum retry delay in milliseconds. */
    private int retryMaxDelayMs = MAX_DELAY_MS;

    /** HTTP connect timeout in milliseconds. */
    private int connectTimeoutMs = CONNECT_TIMEOUT_MS;

    /** HTTP read timeout in milliseconds. */
    private int readTimeoutMs = READ_TIMEOUT_MS;

    /** Number of days for daily reconciliation time window. */
    private int reconciliationWindowDays = RECONCILE_WINDOW_DAYS;

    /** Max differences before alert instead of auto-fix. */
    private int reconciliationMaxDiffThreshold = MAX_DIFF_THRESHOLD;

    /** Page size for full initialization API calls. */
    private int fullInitPageSize = INIT_PAGE_SIZE;
}
