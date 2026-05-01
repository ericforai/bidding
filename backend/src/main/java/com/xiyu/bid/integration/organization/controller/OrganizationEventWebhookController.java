package com.xiyu.bid.integration.organization.controller;

import com.xiyu.bid.integration.organization.application.OrganizationEventAppService;
import com.xiyu.bid.integration.organization.application.OrganizationWebhookSignatureVerifier;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookData;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookRequest;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/integrations/organization/events")
@RequiredArgsConstructor
public class OrganizationEventWebhookController {

    private final OrganizationEventAppService appService;
    private final OrganizationWebhookSignatureVerifier signatureVerifier;

    @PostMapping
    public ResponseEntity<OrganizationEventWebhookResponse> receiveEvent(
            @RequestBody OrganizationEventWebhookRequest request,
            @RequestHeader(value = "EHSY-TraceID", required = false) String traceId,
            @RequestHeader(value = "EHSY-SRCAPP", required = false) String sourceApp,
            @RequestHeader(value = "EHSY-Signature", required = false) String signature,
            HttpServletRequest servletRequest
    ) {
        String payload = request == null ? "" : request.eventMessage();
        if (!signatureVerifier.ipAllowed(servletRequest.getRemoteAddr())) {
            return ResponseEntity.status(403).body(new OrganizationEventWebhookResponse(
                    "500",
                    "ip not allowed",
                    System.currentTimeMillis(),
                    new OrganizationEventWebhookData("", false, false, "REJECTED")
            ));
        }
        if (!signatureVerifier.valid(traceId, sourceApp, payload, signature)) {
            return ResponseEntity.status(401).body(new OrganizationEventWebhookResponse(
                    "500",
                    "invalid signature",
                    System.currentTimeMillis(),
                    new OrganizationEventWebhookData("", false, false, "REJECTED")
            ));
        }
        return ResponseEntity.ok(appService.receiveWebhook(request, traceId, sourceApp));
    }
}
