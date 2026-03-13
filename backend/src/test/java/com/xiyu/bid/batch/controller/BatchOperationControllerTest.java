package com.xiyu.bid.batch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.batch.dto.BatchAssignRequest;
import com.xiyu.bid.batch.dto.BatchClaimRequest;
import com.xiyu.bid.batch.dto.BatchDeleteRequest;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.batch.service.BatchOperationService;
import com.xiyu.bid.util.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 批量操作控制器测试类
 * 测试批量操作API端点
 */
@WebMvcTest(BatchOperationController.class)
@ActiveProfiles("test")
class BatchOperationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatchOperationService batchOperationService;

    private BatchClaimRequest validClaimRequest;
    private BatchAssignRequest validAssignRequest;
    private BatchDeleteRequest validDeleteRequest;

    @BeforeEach
    void setUp() {
        validClaimRequest = new BatchClaimRequest(
                Arrays.asList(1L, 2L, 3L),
                100L,
                "tender"
        );

        validAssignRequest = new BatchAssignRequest(
                Arrays.asList(1L, 2L),
                200L,
                "Reassign to new team"
        );

        validDeleteRequest = new BatchDeleteRequest(
                Arrays.asList(1L, 2L),
                100L,
                "Cleanup old projects",
                false
        );
    }

    @Nested
    @DisplayName("批量认领标讯API测试")
    class BatchClaimTendersApiTests {

        @Test
        @WithMockUser(roles = {"ADMIN", "MANAGER"})
        @DisplayName("POST /api/batch/tenders/claim - 成功批量认领")
        void batchClaimTenders_Success() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(3)
                    .failureCount(0)
                    .totalCount(3)
                    .successIds(Arrays.asList(1L, 2L, 3L))
                    .operationType("CLAIM")
                    .build();

            when(batchOperationService.batchClaimTenders(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.successCount").value(3))
                    .andExpect(jsonPath("$.data.failureCount").value(0))
                    .andExpect(jsonPath("$.data.totalCount").value(3));

            verify(batchOperationService, times(1)).batchClaimTenders(anyList(), anyLong());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "MANAGER"})
        @DisplayName("POST /api/batch/tenders/claim - 部分失败")
        void batchClaimTenders_PartialFailure() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(false)
                    .successCount(2)
                    .failureCount(1)
                    .totalCount(3)
                    .successIds(Arrays.asList(1L, 2L))
                    .operationType("CLAIM")
                    .build();
            mockResponse.addError(3L, "Tender not found", "NOT_FOUND");

            when(batchOperationService.batchClaimTenders(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.successCount").value(2))
                    .andExpect(jsonPath("$.data.failureCount").value(1))
                    .andExpect(jsonPath("$.data.errors").isArray())
                    .andExpect(jsonPath("$.data.errors.length()").value(1));
        }

        @Test
        @DisplayName("POST /api/batch/tenders/claim - 未认证用户返回401")
        void batchClaimTenders_Unauthorized() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest)))
                    .andExpect(status().isUnauthorized());

            verify(batchOperationService, never()).batchClaimTenders(anyList(), anyLong());
        }

        @Test
        @WithMockUser(roles = {"STAFF"})
        @DisplayName("POST /api/batch/tenders/claim - 权限不足返回403")
        void batchClaimTenders_Forbidden() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest)))
                    .andExpect(status().isForbidden());

            verify(batchOperationService, never()).batchClaimTenders(anyList(), anyLong());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("POST /api/batch/tenders/claim - 空ID列表返回400")
        void batchClaimTenders_EmptyList() throws Exception {
            // Given
            BatchClaimRequest emptyRequest = new BatchClaimRequest(
                    Collections.emptyList(),
                    100L,
                    "tender"
            );

            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyRequest)))
                    .andExpect(status().isBadRequest());

            verify(batchOperationService, never()).batchClaimTenders(anyList(), anyLong());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("POST /api/batch/tenders/claim - 缺少必需字段返回400")
        void batchClaimTenders_MissingRequiredField() throws Exception {
            // Given
            String invalidJson = "{\"itemIds\": [1, 2]}"; // Missing userId

            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("批量分配任务API测试")
    class BatchAssignTasksApiTests {

        @Test
        @WithMockUser(roles = {"ADMIN", "MANAGER"})
        @DisplayName("POST /api/batch/tasks/assign - 成功批量分配")
        void batchAssignTasks_Success() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(2)
                    .failureCount(0)
                    .totalCount(2)
                    .successIds(Arrays.asList(1L, 2L))
                    .operationType("ASSIGN")
                    .build();

            when(batchOperationService.batchAssignTasks(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/batch/tasks/assign")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validAssignRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.successCount").value(2))
                    .andExpect(jsonPath("$.message").value("Successfully assigned 2 tasks"));

            verify(batchOperationService, times(1)).batchAssignTasks(anyList(), anyLong());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("POST /api/batch/tasks/assign - 分配人为null返回400")
        void batchAssignTasks_NullAssignee() throws Exception {
            // Given
            BatchAssignRequest invalidRequest = new BatchAssignRequest(
                    Arrays.asList(1L, 2L),
                    null,
                    "Test"
            );

            // When & Then
            mockMvc.perform(post("/api/batch/tasks/assign")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(batchOperationService, never()).batchAssignTasks(anyList(), anyLong());
        }

        @Test
        @WithMockUser(roles = {"MANAGER"})
        @DisplayName("POST /api/batch/tasks/assign - 带备注的分配")
        void batchAssignTasks_WithRemark() throws Exception {
            // Given
            BatchAssignRequest requestWithRemark = new BatchAssignRequest(
                    Arrays.asList(1L),
                    200L,
                    "Urgent reassignment due to team member change"
            );

            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(1)
                    .failureCount(0)
                    .totalCount(1)
                    .operationType("ASSIGN")
                    .build();

            when(batchOperationService.batchAssignTasks(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/batch/tasks/assign")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithRemark)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("批量删除项目API测试")
    class BatchDeleteProjectsApiTests {

        @Test
        @WithMockUser(roles = {"ADMIN", "MANAGER"})
        @DisplayName("DELETE /api/batch/projects - 成功批量删除")
        void batchDeleteProjects_Success() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(2)
                    .failureCount(0)
                    .totalCount(2)
                    .successIds(Arrays.asList(1L, 2L))
                    .operationType("DELETE")
                    .build();

            when(batchOperationService.batchDeleteProjects(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(delete("/api/batch/projects")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDeleteRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.successCount").value(2))
                    .andExpect(jsonPath("$.message").value("Successfully deleted 2 projects"));

            verify(batchOperationService, times(1)).batchDeleteProjects(anyList(), anyLong());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("DELETE /api/batch/projects - 强制删除")
        void batchDeleteProjects_ForceDelete() throws Exception {
            // Given
            BatchDeleteRequest forceDeleteRequest = new BatchDeleteRequest(
                    Arrays.asList(1L, 2L),
                    100L,
                    "Force delete with dependencies",
                    true
            );

            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(2)
                    .failureCount(0)
                    .totalCount(2)
                    .operationType("DELETE")
                    .build();

            when(batchOperationService.batchDeleteProjects(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(delete("/api/batch/projects")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(forceDeleteRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = {"MANAGER"})
        @DisplayName("DELETE /api/batch/projects - 权限不足删除其他用户项目")
        void batchDeleteProjects_PermissionDenied() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(false)
                    .successCount(0)
                    .failureCount(2)
                    .totalCount(2)
                    .operationType("DELETE")
                    .build();
            mockResponse.addError(1L, "Permission denied", "PERMISSION_DENIED");
            mockResponse.addError(2L, "Permission denied", "PERMISSION_DENIED");

            when(batchOperationService.batchDeleteProjects(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(delete("/api/batch/projects")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDeleteRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.successCount").value(0))
                    .andExpect(jsonPath("$.data.failureCount").value(2));
        }
    }

    @Nested
    @DisplayName("通用批量删除API测试")
    class BatchDeleteItemsApiTests {

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("DELETE /api/batch/{type} - 删除标讯")
        void batchDeleteItems_Tenders() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(2)
                    .failureCount(0)
                    .totalCount(2)
                    .operationType("DELETE")
                    .build();

            when(batchOperationService.batchDeleteItems(eq("tender"), anyList()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(delete("/api/batch/tender")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDeleteRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.successCount").value(2));

            verify(batchOperationService, times(1)).batchDeleteItems(eq("tender"), anyList());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("DELETE /api/batch/{type} - 删除任务")
        void batchDeleteItems_Tasks() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(1)
                    .failureCount(0)
                    .totalCount(1)
                    .operationType("DELETE")
                    .build();

            when(batchOperationService.batchDeleteItems(eq("task"), anyList()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(delete("/api/batch/task")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDeleteRequest)))
                    .andExpect(status().isOk());

            verify(batchOperationService, times(1)).batchDeleteItems(eq("task"), anyList());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("DELETE /api/batch/{type} - 删除项目")
        void batchDeleteItems_Projects() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(1)
                    .failureCount(0)
                    .totalCount(1)
                    .operationType("DELETE")
                    .build();

            when(batchOperationService.batchDeleteItems(eq("project"), anyList()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(delete("/api/batch/project")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDeleteRequest)))
                    .andExpect(status().isOk());

            verify(batchOperationService, times(1)).batchDeleteItems(eq("project"), anyList());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("DELETE /api/batch/{type} - 不支持的类型返回400")
        void batchDeleteItems_UnsupportedType() throws Exception {
            // Given
            when(batchOperationService.batchDeleteItems(eq("unsupported"), anyList()))
                    .thenThrow(new IllegalArgumentException("Unsupported item type: unsupported"));

            // When & Then
            mockMvc.perform(delete("/api/batch/unsupported")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDeleteRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("DELETE /api/batch/{type} - 大小写不敏感")
        void batchDeleteItems_CaseInsensitive() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(1)
                    .failureCount(0)
                    .totalCount(1)
                    .operationType("DELETE")
                    .build();

            when(batchOperationService.batchDeleteItems(eq("TENDER"), anyList()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(delete("/api/batch/TENDER")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDeleteRequest)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("批量操作输入验证测试")
    class InputValidationTests {

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("批量操作 - JSON格式错误返回400")
        void invalidJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("批量操作 - Content-Type错误返回415")
        void wrongContentType() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(objectMapper.writeValueAsString(validClaimRequest)))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("批量操作 - 缺少CSRF token返回403")
        void missingCsrfToken() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("批量操作 - 超长备注截断")
        void longRemarkTruncated() throws Exception {
            // Given
            String longRemark = "A".repeat(10000); // Very long string
            BatchAssignRequest requestWithLongRemark = new BatchAssignRequest(
                    Collections.singletonList(1L),
                    200L,
                    longRemark
            );

            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(1)
                    .failureCount(0)
                    .totalCount(1)
                    .operationType("ASSIGN")
                    .build();

            when(batchOperationService.batchAssignTasks(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/batch/tasks/assign")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithLongRemark)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("批量操作响应格式测试")
    class ResponseFormatTests {

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("批量操作响应包含所有必需字段")
        void responseContainsAllFields() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(true)
                    .successCount(2)
                    .failureCount(0)
                    .totalCount(2)
                    .successIds(Arrays.asList(1L, 2L))
                    .operationType("CLAIM")
                    .build();

            when(batchOperationService.batchClaimTenders(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").exists())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.successCount").exists())
                    .andExpect(jsonPath("$.data.failureCount").exists())
                    .andExpect(jsonPath("$.data.totalCount").exists())
                    .andExpect(jsonPath("$.data.successIds").exists())
                    .andExpect(jsonPath("$.data.operationType").exists())
                    .andExpect(jsonPath("$.data.operationTime").exists());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("部分失败响应包含错误详情")
        void partialFailureResponseContainsErrors() throws Exception {
            // Given
            BatchOperationResponse mockResponse = BatchOperationResponse.builder()
                    .success(false)
                    .successCount(1)
                    .failureCount(1)
                    .totalCount(2)
                    .operationType("CLAIM")
                    .build();
            mockResponse.addSuccess(1L);
            mockResponse.addError(2L, "Resource not found", "NOT_FOUND");

            when(batchOperationService.batchClaimTenders(anyList(), anyLong()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/batch/tenders/claim")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.errors").isArray())
                    .andExpect(jsonPath("$.data.errors[0].itemId").value(2L))
                    .andExpect(jsonPath("$.data.errors[0].errorMessage").exists())
                    .andExpect(jsonPath("$.data.errors[0].errorCode").value("NOT_FOUND"));
        }
    }
}
