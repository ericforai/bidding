package com.xiyu.bid.integration.organization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.auth.JwtAuthenticationFilter;
import com.xiyu.bid.config.RateLimitFilter;
import com.xiyu.bid.config.SecurityConfig;
import com.xiyu.bid.integration.organization.application.OrganizationEventAppService;
import com.xiyu.bid.integration.organization.application.OrganizationWebhookSignatureVerifier;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookData;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookRequest;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrganizationEventWebhookController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class, RateLimitFilter.class}
        ))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrganizationEventWebhookController — customer event contract")
class OrganizationEventWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationEventAppService appService;

    @MockBean
    private OrganizationWebhookSignatureVerifier signatureVerifier;

    @Test
    @DisplayName("POST returns customer code/msg/timestamp/data envelope")
    void post_returnsCustomerEnvelope() throws Exception {
        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest(
                "org.user.upsert",
                "{\"userCode\":\"u001\"}"
        );
        OrganizationEventWebhookResponse response = new OrganizationEventWebhookResponse(
                "200",
                "success",
                1777359048000L,
                new OrganizationEventWebhookData("event-1", true, false, "PROCESSED")
        );
        when(signatureVerifier.valid("trace-1", "customer-org", request.eventMessage(), "sig-1")).thenReturn(true);
        when(appService.receiveWebhook(eq(request), eq("trace-1"), eq("customer-org"))).thenReturn(response);

        mockMvc.perform(post("/api/integrations/organization/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("EHSY-TraceID", "trace-1")
                        .header("EHSY-SRCAPP", "customer-org")
                        .header("EHSY-Signature", "sig-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.data.eventId").value("event-1"))
                .andExpect(jsonPath("$.data.accepted").value(true));
    }

    @Test
    @DisplayName("POST rejects requests with missing or invalid signature")
    void post_invalidSignature_returns401() throws Exception {
        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest(
                "org.user.upsert",
                "{\"userCode\":\"u001\"}"
        );
        when(signatureVerifier.valid("trace-1", "customer-org", request.eventMessage(), null)).thenReturn(false);

        mockMvc.perform(post("/api/integrations/organization/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("EHSY-TraceID", "trace-1")
                        .header("EHSY-SRCAPP", "customer-org")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.msg").value("invalid signature"));
    }
}
