package com.xiyu.bid.batch.service;

import com.xiyu.bid.batch.dto.BatchAssignRequest;
import com.xiyu.bid.batch.dto.BatchClaimRequest;
import com.xiyu.bid.batch.dto.BatchDeleteRequest;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.exception.BusinessException;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.service.IAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 批量操作服务测试类
 * 测试批量认领、分配、删除等操作
 */
@ExtendWith(MockitoExtension.class)
class BatchOperationServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private IAuditLogService auditLogService;

    @InjectMocks
    private BatchOperationService batchOperationService;

    private Tender testTender1;
    private Tender testTender2;
    private Task testTask1;
    private Task testTask2;
    private Project testProject1;
    private Project testProject2;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testTender1 = Tender.builder()
                .id(1L)
                .title("Test Tender 1")
                .status(Tender.Status.PENDING)
                .build();

        testTender2 = Tender.builder()
                .id(2L)
                .title("Test Tender 2")
                .status(Tender.Status.PENDING)
                .build();

        testTask1 = Task.builder()
                .id(1L)
                .projectId(100L)
                .title("Test Task 1")
                .assigneeId(null)
                .status(Task.Status.TODO)
                .build();

        testTask2 = Task.builder()
                .id(2L)
                .projectId(100L)
                .title("Test Task 2")
                .assigneeId(null)
                .status(Task.Status.TODO)
                .build();

        testProject1 = Project.builder()
                .id(1L)
                .name("Test Project 1")
                .tenderId(10L)
                .status(Project.Status.INITIATED)
                .managerId(1L)
                .build();

        testProject2 = Project.builder()
                .id(2L)
                .name("Test Project 2")
                .tenderId(20L)
                .status(Project.Status.INITIATED)
                .managerId(1L)
                .build();
    }

    @Nested
    @DisplayName("批量认领标讯测试")
    class BatchClaimTendersTests {

        @Test
        @DisplayName("批量认领标讯 - 全部成功")
        void batchClaimTenders_AllSuccess() {
            // Given
            List<Long> tenderIds = Arrays.asList(1L, 2L);
            Long userId = 100L;

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            when(tenderRepository.findById(2L)).thenReturn(Optional.of(testTender2));
            when(tenderRepository.saveAll(anyList())).thenReturn(Arrays.asList(testTender1, testTender2));

            // When
            BatchOperationResponse response = batchOperationService.batchClaimTenders(tenderIds, userId);

            // Then
            assertTrue(response.getSuccess());
            assertEquals(2, response.getSuccessCount());
            assertEquals(0, response.getFailureCount());
            assertEquals(2, response.getTotalCount());
            assertTrue(response.isAllSuccess());
            assertTrue(response.getSuccessIds().containsAll(tenderIds));
            assertTrue(response.getErrors().isEmpty());

            verify(tenderRepository, times(2)).findById(anyLong());
            verify(tenderRepository, times(1)).saveAll(anyList());
            verify(auditLogService, times(1)).log(any());
        }

        @Test
        @DisplayName("批量认领标讯 - 部分失败")
        void batchClaimTenders_PartialFailure() {
            // Given
            List<Long> tenderIds = Arrays.asList(1L, 2L, 999L);
            Long userId = 100L;

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            when(tenderRepository.findById(2L)).thenReturn(Optional.of(testTender2));
            when(tenderRepository.findById(999L)).thenReturn(Optional.empty());
            when(tenderRepository.saveAll(anyList())).thenReturn(Arrays.asList(testTender1, testTender2));

            // When
            BatchOperationResponse response = batchOperationService.batchClaimTenders(tenderIds, userId);

            // Then
            assertFalse(response.getSuccess()); // Partial failure returns false
            assertEquals(2, response.getSuccessCount());
            assertEquals(1, response.getFailureCount());
            assertEquals(3, response.getTotalCount());
            assertFalse(response.isAllSuccess());
            assertEquals(1, response.getErrors().size());

            verify(tenderRepository, times(3)).findById(anyLong());
            verify(auditLogService, times(1)).log(any());
        }

        @Test
        @DisplayName("批量认领标讯 - 空列表")
        void batchClaimTenders_EmptyList() {
            // Given
            List<Long> tenderIds = Collections.emptyList();
            Long userId = 100L;

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchClaimTenders(tenderIds, userId);
            });

            verify(tenderRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("批量认领标讯 - 全部失败")
        void batchClaimTenders_AllFailure() {
            // Given
            List<Long> tenderIds = Arrays.asList(999L, 998L);
            Long userId = 100L;

            when(tenderRepository.findById(999L)).thenReturn(Optional.empty());
            when(tenderRepository.findById(998L)).thenReturn(Optional.empty());

            // When
            BatchOperationResponse response = batchOperationService.batchClaimTenders(tenderIds, userId);

            // Then
            assertFalse(response.getSuccess());
            assertEquals(0, response.getSuccessCount());
            assertEquals(2, response.getFailureCount());
            assertEquals(2, response.getErrors().size());

            verify(tenderRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("批量分配任务测试")
    class BatchAssignTasksTests {

        @Test
        @DisplayName("批量分配任务 - 全部成功")
        void batchAssignTasks_AllSuccess() {
            // Given
            List<Long> taskIds = Arrays.asList(1L, 2L);
            Long assigneeId = 200L;

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
            when(taskRepository.findById(2L)).thenReturn(Optional.of(testTask2));
            when(taskRepository.saveAll(anyList())).thenReturn(Arrays.asList(testTask1, testTask2));

            // When
            BatchOperationResponse response = batchOperationService.batchAssignTasks(taskIds, assigneeId);

            // Then
            assertTrue(response.getSuccess());
            assertEquals(2, response.getSuccessCount());
            assertEquals(0, response.getFailureCount());
            assertEquals(2, response.getTotalCount());
            assertTrue(response.isAllSuccess());

            verify(taskRepository, times(2)).findById(anyLong());
            verify(taskRepository, times(1)).saveAll(anyList());
            verify(auditLogService, times(1)).log(any());
        }

        @Test
        @DisplayName("批量分配任务 - 部分失败")
        void batchAssignTasks_PartialFailure() {
            // Given
            List<Long> taskIds = Arrays.asList(1L, 999L);
            Long assigneeId = 200L;

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());
            when(taskRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testTask1));

            // When
            BatchOperationResponse response = batchOperationService.batchAssignTasks(taskIds, assigneeId);

            // Then
            assertFalse(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
            assertEquals(1, response.getFailureCount());
            assertEquals(1, response.getErrors().size());
            assertEquals(999L, response.getErrors().get(0).getItemId());
        }

        @Test
        @DisplayName("批量分配任务 - 重新分配")
        void batchAssignTasks_Reassign() {
            // Given
            testTask1.setAssigneeId(100L); // Already assigned to user 100
            List<Long> taskIds = Collections.singletonList(1L);
            Long newAssigneeId = 200L;

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
            when(taskRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testTask1));

            // When
            BatchOperationResponse response = batchOperationService.batchAssignTasks(taskIds, newAssigneeId);

            // Then
            assertTrue(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
            assertEquals(newAssigneeId, testTask1.getAssigneeId());
        }

        @Test
        @DisplayName("批量分配任务 - 空列表抛出异常")
        void batchAssignTasks_EmptyList() {
            // Given
            List<Long> taskIds = Collections.emptyList();
            Long assigneeId = 200L;

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchAssignTasks(taskIds, assigneeId);
            });

            verify(taskRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("批量分配任务 - 无效分配人ID")
        void batchAssignTasks_InvalidAssigneeId() {
            // Given
            List<Long> taskIds = Collections.singletonList(1L);
            Long invalidAssigneeId = null;

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchAssignTasks(taskIds, invalidAssigneeId);
            });

            verify(taskRepository, never()).findById(anyLong());
        }
    }

    @Nested
    @DisplayName("批量删除项目测试")
    class BatchDeleteProjectsTests {

        @Test
        @DisplayName("批量删除项目 - 全部成功")
        void batchDeleteProjects_AllSuccess() {
            // Given
            List<Long> projectIds = Arrays.asList(1L, 2L);
            Long userId = 100L;

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));
            when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject2));
            doNothing().when(projectRepository).deleteAll(anyList());

            // When
            BatchOperationResponse response = batchOperationService.batchDeleteProjects(projectIds, userId);

            // Then
            assertTrue(response.getSuccess());
            assertEquals(2, response.getSuccessCount());
            assertEquals(0, response.getFailureCount());
            assertEquals(2, response.getTotalCount());
            assertTrue(response.isAllSuccess());

            verify(projectRepository, times(2)).findById(anyLong());
            verify(projectRepository, times(1)).deleteAll(anyList());
            verify(auditLogService, times(1)).log(any());
        }

        @Test
        @DisplayName("批量删除项目 - 部分失败")
        void batchDeleteProjects_PartialFailure() {
            // Given
            List<Long> projectIds = Arrays.asList(1L, 999L);
            Long userId = 100L;

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());
            doNothing().when(projectRepository).deleteAll(anyList());

            // When
            BatchOperationResponse response = batchOperationService.batchDeleteProjects(projectIds, userId);

            // Then
            assertFalse(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
            assertEquals(1, response.getFailureCount());
            assertEquals(1, response.getErrors().size());
        }

        @Test
        @DisplayName("批量删除项目 - 权限检查")
        void batchDeleteProjects_PermissionCheck() {
            // Given
            testProject1.setManagerId(999L); // Different manager
            List<Long> projectIds = Collections.singletonList(1L);
            Long userId = 100L; // Not the manager

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));

            // When
            BatchOperationResponse response = batchOperationService.batchDeleteProjects(projectIds, userId);

            // Then
            assertFalse(response.getSuccess());
            assertEquals(0, response.getSuccessCount());
            assertEquals(1, response.getFailureCount());
            assertEquals(1, response.getErrors().size());
            assertEquals("PERMISSION_DENIED", response.getErrors().get(0).getErrorCode());

            verify(projectRepository, never()).deleteAll(anyList());
        }

        @Test
        @DisplayName("批量删除项目 - 空列表")
        void batchDeleteProjects_EmptyList() {
            // Given
            List<Long> projectIds = Collections.emptyList();
            Long userId = 100L;

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchDeleteProjects(projectIds, userId);
            });

            verify(projectRepository, never()).deleteAll(anyList());
        }

        @Test
        @DisplayName("批量删除项目 - 无效用户ID")
        void batchDeleteProjects_InvalidUserId() {
            // Given
            List<Long> projectIds = Collections.singletonList(1L);
            Long invalidUserId = null;

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchDeleteProjects(projectIds, invalidUserId);
            });

            verify(projectRepository, never()).findById(anyLong());
        }
    }

    @Nested
    @DisplayName("通用批量删除测试")
    class BatchDeleteItemsTests {

        @Test
        @DisplayName("通用批量删除 - 标讯类型")
        void batchDeleteItems_TenderType() {
            // Given
            String itemType = "tender";
            List<Long> ids = Collections.singletonList(1L);

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            doNothing().when(tenderRepository).deleteAll(anyList());

            // When
            BatchOperationResponse response = batchOperationService.batchDeleteItems(itemType, ids);

            // Then
            assertTrue(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
            assertEquals(0, response.getFailureCount());

            verify(tenderRepository, times(1)).findById(anyLong());
            verify(tenderRepository, times(1)).deleteAll(anyList());
        }

        @Test
        @DisplayName("通用批量删除 - 任务类型")
        void batchDeleteItems_TaskType() {
            // Given
            String itemType = "task";
            List<Long> ids = Collections.singletonList(1L);

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
            doNothing().when(taskRepository).deleteAll(anyList());

            // When
            BatchOperationResponse response = batchOperationService.batchDeleteItems(itemType, ids);

            // Then
            assertTrue(response.getSuccess());
            assertEquals(1, response.getSuccessCount());

            verify(taskRepository, times(1)).findById(anyLong());
            verify(taskRepository, times(1)).deleteAll(anyList());
        }

        @Test
        @DisplayName("通用批量删除 - 项目类型")
        void batchDeleteItems_ProjectType() {
            // Given
            String itemType = "project";
            List<Long> ids = Collections.singletonList(1L);

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));
            doNothing().when(projectRepository).deleteAll(anyList());

            // When
            BatchOperationResponse response = batchOperationService.batchDeleteItems(itemType, ids);

            // Then
            assertTrue(response.getSuccess());
            assertEquals(1, response.getSuccessCount());

            verify(projectRepository, times(1)).findById(anyLong());
            verify(projectRepository, times(1)).deleteAll(anyList());
        }

        @Test
        @DisplayName("通用批量删除 - 不支持的类型")
        void batchDeleteItems_UnsupportedType() {
            // Given
            String itemType = "unsupported_type";
            List<Long> ids = Collections.singletonList(1L);

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchDeleteItems(itemType, ids);
            });
        }

        @Test
        @DisplayName("通用批量删除 - 空列表")
        void batchDeleteItems_EmptyList() {
            // Given
            String itemType = "tender";
            List<Long> ids = Collections.emptyList();

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchDeleteItems(itemType, ids);
            });
        }

        @Test
        @DisplayName("通用批量删除 - 大小写不敏感")
        void batchDeleteItems_CaseInsensitive() {
            // Given
            String itemType = "TENDER";
            List<Long> ids = Collections.singletonList(1L);

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            doNothing().when(tenderRepository).deleteAll(anyList());

            // When
            BatchOperationResponse response = batchOperationService.batchDeleteItems(itemType, ids);

            // Then
            assertTrue(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
        }
    }

    @Nested
    @DisplayName("批量操作异常处理测试")
    class BatchOperationExceptionHandlingTests {

        @Test
        @DisplayName("事务回滚测试 - 中途异常")
        void transactionRollback_OnMidwayException() {
            // Given
            List<Long> tenderIds = Arrays.asList(1L, 2L);
            Long userId = 100L;

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            when(tenderRepository.findById(2L)).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                batchOperationService.batchClaimTenders(tenderIds, userId);
            });

            // Verify that no saves occurred due to exception
            verify(tenderRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("批量操作失败率计算")
        void failureRateCalculation() {
            // Given
            List<Long> tenderIds = Arrays.asList(1L, 2L, 3L, 999L);
            Long userId = 100L;

            Tender testTender3 = Tender.builder()
                    .id(3L)
                    .title("Test Tender 3")
                    .status(Tender.Status.PENDING)
                    .build();

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            when(tenderRepository.findById(2L)).thenReturn(Optional.of(testTender2));
            when(tenderRepository.findById(3L)).thenReturn(Optional.of(testTender3));
            when(tenderRepository.findById(999L)).thenReturn(Optional.empty());
            when(tenderRepository.saveAll(anyList())).thenReturn(Arrays.asList(testTender1, testTender2, testTender3));

            // When
            BatchOperationResponse response = batchOperationService.batchClaimTenders(tenderIds, userId);

            // Then
            assertEquals(25.0, response.getFailureRate(), 0.01); // 1 failure out of 4 = 25%
        }

        @Test
        @DisplayName("批量操作 - 空ID列表处理")
        void batchOperation_NullIds() {
            // Given
            List<Long> nullIds = null;

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchClaimTenders(nullIds, 100L);
            });
        }
    }

    @Nested
    @DisplayName("批量操作请求DTO验证测试")
    class BatchRequestValidationTests {

        @Test
        @DisplayName("批量认领请求 - 有效请求")
        void batchClaimRequest_Valid() {
            // Given
            BatchClaimRequest request = new BatchClaimRequest(
                    Arrays.asList(1L, 2L),
                    100L,
                    "tender"
            );

            // When
            BatchOperationResponse response = batchOperationService.batchClaimTenders(
                    request.getItemIds(),
                    request.getUserId()
            );

            // Then
            assertNotNull(response);
        }

        @Test
        @DisplayName("批量分配请求 - 有效请求")
        void batchAssignRequest_Valid() {
            // Given
            BatchAssignRequest request = new BatchAssignRequest(
                    Arrays.asList(1L, 2L),
                    200L,
                    "Team reassignment"
            );

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
            when(taskRepository.findById(2L)).thenReturn(Optional.of(testTask2));
            when(taskRepository.saveAll(anyList())).thenReturn(Arrays.asList(testTask1, testTask2));

            // When
            BatchOperationResponse response = batchOperationService.batchAssignTasks(
                    request.getTaskIds(),
                    request.getAssigneeId()
            );

            // Then
            assertNotNull(response);
            assertEquals(2, response.getSuccessCount());
        }

        @Test
        @DisplayName("批量删除请求 - 有效请求")
        void batchDeleteRequest_Valid() {
            // Given
            BatchDeleteRequest request = new BatchDeleteRequest(
                    Arrays.asList(1L, 2L),
                    100L,
                    "Cleanup old projects",
                    false
            );

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));
            when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject2));
            doNothing().when(projectRepository).deleteAll(anyList());

            // When
            BatchOperationResponse response = batchOperationService.batchDeleteProjects(
                    request.getItemIds(),
                    request.getUserId()
            );

            // Then
            assertNotNull(response);
            assertEquals(2, response.getSuccessCount());
        }
    }
}
