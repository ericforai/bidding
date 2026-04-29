package com.xiyu.bid.security;

import com.xiyu.bid.security.service.CrmPermissionSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrmWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CrmPermissionSyncService syncService;

    @Test
    void syncPermissions_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/webhooks/crm/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "CRM_001",
                                  "permissions": [
                                    { "userId": 101, "permissionType": "OWNER" },
                                    { "userId": 102, "permissionType": "SHARING" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
