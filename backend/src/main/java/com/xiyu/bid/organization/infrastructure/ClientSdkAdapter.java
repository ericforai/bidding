package com.xiyu.bid.organization.infrastructure;

import com.xiyu.bid.organization.application.EventSyncService;
import com.xiyu.bid.organization.config.OrganizationSyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for the Xiyu ClientSDK event subscription.
 * <p>
 * This bean is conditionally created only when the SDK jar
 * (com.ehsy.eventlibrary:ClientSDK) is on the classpath.
 * When the jar is absent, the system operates in HTTP-fallback-only mode.
 */
// @Component
// @ConditionalOnClass(name = "com.ehsy.eventlibrary.ClientSDK")
public class ClientSdkAdapter {

    private static final Logger log = LoggerFactory.getLogger(ClientSdkAdapter.class);

    private final EventSyncService eventSyncService;
    private final OrganizationSyncProperties properties;

    public ClientSdkAdapter(EventSyncService eventSyncService, OrganizationSyncProperties properties) {
        this.eventSyncService = eventSyncService;
        this.properties = properties;
        log.info("ClientSdkAdapter not yet active: SDK jar pending delivery. HTTP fallback mode active.");
    }

    // Placeholder: when SDK jar arrives, uncomment annotations and implement:
    // - @PostConstruct to register @AcceptEvent handlers for BaseOssDept / BaseOssUser
    // - Forward each event through eventSyncService.receiveViaSdk(...)
}
