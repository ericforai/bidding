package com.xiyu.bid.security.service;

import com.xiyu.bid.dto.webhook.CrmPermissionWebhookPayload;
import com.xiyu.bid.entity.CrmCustomerPermission;
import com.xiyu.bid.repository.CrmCustomerPermissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CrmPermissionSyncServiceTest {

    @Autowired
    private CrmPermissionSyncService syncService;

    @Autowired
    private CrmCustomerPermissionRepository repository;

    @Test
    void syncCustomerPermissions_ShouldBeIdempotent() {
        String customerId = "TEST_CUST_123";
        
        // Initial sync
        CrmPermissionWebhookPayload payload1 = CrmPermissionWebhookPayload.builder()
                .customerId(customerId)
                .permissions(List.of(
                        new CrmPermissionWebhookPayload.UserPermission(1L, "OWNER"),
                        new CrmPermissionWebhookPayload.UserPermission(2L, "SHARING")
                ))
                .build();
        
        syncService.syncCustomerPermissions(payload1);
        
        List<CrmCustomerPermission> result1 = repository.findByCustomerId(customerId);
        assertEquals(2, result1.size());
        
        // Secondary sync (update)
        CrmPermissionWebhookPayload payload2 = CrmPermissionWebhookPayload.builder()
                .customerId(customerId)
                .permissions(List.of(
                        new CrmPermissionWebhookPayload.UserPermission(1L, "OWNER"),
                        new CrmPermissionWebhookPayload.UserPermission(3L, "TEAM")
                ))
                .build();
        
        syncService.syncCustomerPermissions(payload2);
        
        List<CrmCustomerPermission> result2 = repository.findByCustomerId(customerId);
        assertEquals(2, result2.size());
        assertEquals("TEAM", result2.stream()
                .filter(p -> p.getUserId().equals(3L))
                .findFirst()
                .orElseThrow()
                .getPermissionType());
    }
}
