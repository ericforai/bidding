package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookRequest;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationEventAppService {

    private final OrganizationDirectorySyncAppService directorySyncAppService;

    public OrganizationEventWebhookResponse receiveWebhook(
            OrganizationEventWebhookRequest request,
            String traceId,
            String sourceApp
    ) {
        return directorySyncAppService.receiveWebhook(request);
    }
}
