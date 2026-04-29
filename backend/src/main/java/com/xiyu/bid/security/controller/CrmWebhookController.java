package com.xiyu.bid.security.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.webhook.CrmPermissionWebhookPayload;
import com.xiyu.bid.security.service.CrmPermissionSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/crm")
@RequiredArgsConstructor
@Slf4j
public class CrmWebhookController {

    private final CrmPermissionSyncService syncService;

    @PostMapping("/permissions")
    public ResponseEntity<ApiResponse<Void>> syncPermissions(@RequestBody CrmPermissionWebhookPayload payload) {
        log.info("Received CRM permission sync request for customer: {}", payload.getCustomerId());
        syncService.syncCustomerPermissions(payload);
        return ResponseEntity.ok(ApiResponse.success("CRM permissions synced successfully", null));
    }
}
