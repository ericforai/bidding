package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationEventLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrganizationEventRetryAppService {
    private static final long PROCESSING_LEASE_MINUTES = 15;

    private final OrganizationEventInboxService inboxService;
    private final OrganizationDirectorySyncAppService syncAppService;
    private final OrganizationIntegrationProperties properties;
    private final OrganizationIntegrationSettingsResolver settingsResolver;

    public OrganizationEventRetrySummary retryDueEvents(LocalDateTime now) {
        if (!settingsResolver.resolve().enabled() || !properties.getRetry().isEnabled()) {
            return OrganizationEventRetrySummary.empty();
        }
        inboxService.recoverStaleProcessing(now.minusMinutes(PROCESSING_LEASE_MINUTES), now);
        OrganizationEventRetrySummary summary = OrganizationEventRetrySummary.empty();
        for (OrganizationEventLogEntity event : inboxService.findDueRetries(now, properties.getRetry().getBatchSize())) {
            summary = summary.add(retryOne(event, now));
        }
        return summary;
    }

    private OrganizationEventRetrySummary retryOne(OrganizationEventLogEntity event, LocalDateTime now) {
        if (!inboxService.claimDueRetry(event.getEventKey(), now)) {
            return OrganizationEventRetrySummary.empty();
        }
        OrganizationEventWebhookResponse response =
                syncAppService.reprocessReservedEvent(event.getEventKey(), event.getRawPayload());
        boolean success = "200".equals(response.code());
        return new OrganizationEventRetrySummary(1, success ? 1 : 0, success ? 0 : 1);
    }
}
