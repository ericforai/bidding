package com.xiyu.bid.approval.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.approval.dto.ApprovalDecisionRequest;
import com.xiyu.bid.approval.dto.ApprovalSubmitRequest;
import com.xiyu.bid.approval.service.ApprovalWorkflowService;
import com.xiyu.bid.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ApprovalController测试
 */
@WebMvcTest(ApprovalController.class)
@DisplayName("ApprovalController测试")
class ApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApprovalWorkflowService approvalWorkflowService;

    private ApprovalSubmitRequest submitRequest;
    private ApprovalDecisionRequest decisionRequest;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        submitRequest = ApprovalSubmitRequest.builder()
                .projectId(1L)
                .projectName("测试项目")
                .approvalType("BID_DOCUMENT")
                .title("投标文档审批")
                .description("请审批")
                .priority(1)
                .dueDate(LocalDateTime.now().plusDays(3))
                .build();

        decisionRequest = ApprovalDecisionRequest.builder()
                .comment("同意")
                .requireResubmit(false)
                .build();
    }

    @Test
    @DisplayName("应该成功提交审批")
    void submitApproval_ShouldSucceed() throws Exception {
        when(approvalWorkflowService.submitForApproval(any(), anyLong(), anyString()))
                .thenReturn(Map.of("id", testId.toString()));

        mockMvc.perform(post("/api/approvals/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approvalWorkflowService, times(1)).submitForApproval(any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("应该成功审批通过")
    void approve_ShouldSucceed() throws Exception {
        when(approvalWorkflowService.approve(eq(testId), anyLong(), anyString(), anyString()))
                .thenReturn(Map.of("status", "APPROVED"));

        mockMvc.perform(post("/api/approvals/{id}/approve", testId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approvalWorkflowService, times(1)).approve(eq(testId), anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("应该成功审批驳回")
    void reject_ShouldSucceed() throws Exception {
        when(approvalWorkflowService.reject(eq(testId), anyLong(), anyString(), anyString()))
                .thenReturn(Map.of("status", "REJECTED"));

        mockMvc.perform(post("/api/approvals/{id}/reject", testId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approvalWorkflowService, times(1)).reject(eq(testId), anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("应该成功取消审批")
    void cancel_ShouldSucceed() throws Exception {
        doNothing().when(approvalWorkflowService).cancel(eq(testId), anyLong(), anyString());

        mockMvc.perform(delete("/api/approvals/{id}", testId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approvalWorkflowService, times(1)).cancel(eq(testId), anyLong(), anyString());
    }

    @Test
    @DisplayName("应该获取待审批列表")
    void getPendingApprovals_ShouldSucceed() throws Exception {
        when(approvalWorkflowService.getPendingApprovals(isNull(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/approvals/pending")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approvalWorkflowService, times(1)).getPendingApprovals(isNull(), any());
    }

    @Test
    @DisplayName("应该获取统计数据")
    void getStatistics_ShouldSucceed() throws Exception {
        when(approvalWorkflowService.getStatistics())
                .thenReturn(Map.of(
                        "totalCount", 100L,
                        "pendingCount", 10L,
                        "approvedCount", 70L
                ));

        mockMvc.perform(get("/api/approvals/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(100));

        verify(approvalWorkflowService, times(1)).getStatistics();
    }

    @Test
    @DisplayName("应该获取审批详情")
    void getApprovalDetail_ShouldSucceed() throws Exception {
        when(approvalWorkflowService.getApprovalDetail(testId))
                .thenReturn(Map.of("id", testId.toString()));

        mockMvc.perform(get("/api/approvals/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testId.toString()));

        verify(approvalWorkflowService, times(1)).getApprovalDetail(testId);
    }

    @Test
    @DisplayName("应该标记为已读")
    void markAsRead_ShouldSucceed() throws Exception {
        doNothing().when(approvalWorkflowService).markAsRead(eq(testId), anyLong());

        mockMvc.perform(put("/api/approvals/{id}/read", testId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approvalWorkflowService, times(1)).markAsRead(eq(testId), anyLong());
    }

    @Test
    @DisplayName("应该获取我的审批列表")
    void getMyApprovals_ShouldSucceed() throws Exception {
        when(approvalWorkflowService.getMyApprovals(anyLong(), isNull(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/approvals/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approvalWorkflowService, times(1)).getMyApprovals(anyLong(), isNull(), any());
    }

    @Test
    @DisplayName("应该批量审批")
    void batchApprove_ShouldSucceed() throws Exception {
        List<UUID> ids = Arrays.asList(testId, UUID.randomUUID());

        when(approvalWorkflowService.batchApprove(anyList(), anyLong(), anyString(), anyString()))
                .thenReturn(Map.of(testId, "审批成功"));

        mockMvc.perform(post("/api/approvals/batch/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ids", ids,
                                "comment", "批量通过"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approvalWorkflowService, times(1)).batchApprove(anyList(), anyLong(), anyString(), anyString());
    }
}
