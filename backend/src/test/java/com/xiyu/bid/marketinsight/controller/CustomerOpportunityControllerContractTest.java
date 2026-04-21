package com.xiyu.bid.marketinsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.marketinsight.dto.CustomerInsightDTO;
import com.xiyu.bid.marketinsight.dto.CustomerPredictionDTO;
import com.xiyu.bid.marketinsight.service.CustomerOpportunityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerOpportunityControllerContractTest {

    @Mock
    private CustomerOpportunityService customerOpportunityService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerOpportunityController(customerOpportunityService))
                .setControllerAdvice(new com.xiyu.bid.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void getCustomerInsights_ShouldPreserveRouteAndResponseEnvelope() throws Exception {
        when(customerOpportunityService.getCustomerInsights()).thenReturn(List.of(
                CustomerInsightDTO.builder()
                        .customerId("hash-1")
                        .customerName("国网江苏省电力")
                        .status("watch")
                        .build()));

        mockMvc.perform(get("/api/customer-opportunities/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].customerId").value("hash-1"))
                .andExpect(jsonPath("$.data[0].customerName").value("国网江苏省电力"));
    }

    @Test
    void transitionPrediction_ShouldPreserveCommandRoute() throws Exception {
        when(customerOpportunityService.transitionPrediction(1L, "RECOMMEND")).thenReturn(
                CustomerPredictionDTO.builder()
                        .opportunityId(1L)
                        .customerId("hash-1")
                        .predictedCategory("能源电力")
                        .build());

        mockMvc.perform(put("/api/customer-opportunities/predictions/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RECOMMEND"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.opportunityId").value(1))
                .andExpect(jsonPath("$.data.customerId").value("hash-1"));
    }
}
