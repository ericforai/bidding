package com.xiyu.bid.crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.crm")
public class CrmProperties {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int DEFAULT_RETRIES = 3;
    private static final int BASE_DELAY_MS = 1000;
    private static final int MAX_DELAY_MS = 30000;
    private static final int TOKEN_RENEW_RATIO = 10;
    private static final int COOL_DOWN_RETRIES = 3;
    private static final long COOL_DOWN_MS = 60000;
    private static final int MENU_CACHE_TTL_SEC = 1800;
    private static final int MSG_BATCH_MAX = 100;
    private static final int MAX_CONNECTIONS = 50;

    /** CRM application client ID. */
    private String clientId;

    /** CRM application client secret. */
    private String clientSecret;

    /** Base URL for CRM API endpoints. */
    private String baseUrl;

    /** HTTP connect timeout in milliseconds. */
    private int connectTimeoutMs = CONNECT_TIMEOUT_MS;

    /** HTTP read timeout in milliseconds. */
    private int readTimeoutMs = READ_TIMEOUT_MS;

    /** Maximum retry attempts for 5xx errors. */
    private int maxRetries = DEFAULT_RETRIES;

    /** Base retry delay for exponential backoff. */
    private int retryBaseDelayMs = BASE_DELAY_MS;

    /** Maximum retry delay in milliseconds. */
    private int retryMaxDelayMs = MAX_DELAY_MS;

    /** Ratio (percent) of TTL remaining before triggering auto-renewal. */
    private int tokenRenewBeforeExpiryRatio = TOKEN_RENEW_RATIO;

    /** Number of consecutive failures before entering cooldown. */
    private int tokenCoolDownRetries = COOL_DOWN_RETRIES;

    /** Cooldown duration in milliseconds. */
    private long tokenCoolDownMs = COOL_DOWN_MS;

    /** Menu tree cache TTL in seconds. */
    private int menuCacheTtlSeconds = MENU_CACHE_TTL_SEC;

    /** Maximum messages per batch request. */
    private int messageBatchMaxSize = MSG_BATCH_MAX;

    /** Maximum HTTP connections in the pool. */
    private int maxConnections = MAX_CONNECTIONS;
}
