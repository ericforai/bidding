package com.xiyu.bid.batch.service;

import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.service.IAuditLogService;
import org.junit.jupiter.api.BeforeEach;
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
    class BatchClaimTendersTests {

        @Test
        void batchClaimTenders_AllSuccess() {
            List<Long> tenderIds = Arrays.asList(1L, 2L);
            Long userId = 100L;

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            when(tenderRepository.findById(2L)).thenReturn(Optional.of(testTender2));
            when(tenderRepository.saveAll(anyList())).thenReturn(Arrays.asList(testTender1, testTender2));

            var response = batchOperationService.batchClaimTenders(tenderIds, userId);

            assertTrue(response.getSuccess());
            assertEquals(2, response.getSuccessCount());
            assertEquals(0, response.getFailureCount());
            assertTrue(response.isAllSuccess());

            verify(tenderRepository, times(1)).saveAll(anyList());
            verify(auditLogService, times(1)).log(any());
        }

        @Test
        void batchClaimTenders_PartialFailure() {
            List<Long> tenderIds = Arrays.asList(1L, 2L, 999L);
            Long userId = 100L;

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            when(tenderRepository.findById(2L)).thenReturn(Optional.of(testTender2));
            when(tenderRepository.findById(999L)).thenReturn(Optional.empty());
            when(tenderRepository.saveAll(anyList())).thenReturn(Arrays.asList(testTender1, testTender2));

            var response = batchOperationService.batchClaimTenders(tenderIds, userId);

            assertFalse(response.getSuccess());
            assertEquals(2, response.getSuccessCount());
            assertEquals(1, response.getFailureCount());
        }

        @Test
        void batchClaimTenders_EmptyList() {
            List<Long> tenderIds = Collections.emptyList();
            Long userId = 100L;

            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchClaimTenders(tenderIds, userId);
            });
        }
    }

    @Nested
    class BatchAssignTasksTests {

        @Test
        void batchAssignTasks_AllSuccess() {
            List<Long> taskIds = Arrays.asList(1L, 2L);
            Long assigneeId = 200L;

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
            when(taskRepository.findById(2L)).thenReturn(Optional.of(testTask2));
            when(taskRepository.saveAll(anyList())).thenReturn(Arrays.asList(testTask1, testTask2));

            var response = batchOperationService.batchAssignTasks(taskIds, assigneeId);

            assertTrue(response.getSuccess());
            assertEquals(2, response.getSuccessCount());
            assertEquals(0, response.getFailureCount());
            assertTrue(response.isAllSuccess());
        }

        @Test
        void batchAssignTasks_PartialFailure() {
            List<Long> taskIds = Arrays.asList(1L, 999L);
            Long assigneeId = 200L;

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());
            when(taskRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testTask1));

            var response = batchOperationService.batchAssignTasks(taskIds, assigneeId);

            assertFalse(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
            assertEquals(1, response.getFailureCount());
        }

        @Test
        void batchAssignTasks_EmptyList() {
            List<Long> taskIds = Collections.emptyList();
            Long assigneeId = 200L;

            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchAssignTasks(taskIds, assigneeId);
            });
        }
    }

    @Nested
    class BatchDeleteProjectsTests {

        @Test
        void batchDeleteProjects_AllSuccess() {
            List<Long> projectIds = Arrays.asList(1L, 2L);
            Long userId = 1L;

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));
            when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject2));
            doNothing().when(projectRepository).deleteAll(anyList());

            var response = batchOperationService.batchDeleteProjects(projectIds, userId);

            assertTrue(response.getSuccess());
            assertEquals(2, response.getSuccessCount());
            assertEquals(0, response.getFailureCount());
            assertTrue(response.isAllSuccess());
        }

        @Test
        void batchDeleteProjects_PermissionCheck() {
            testProject1.setManagerId(999L);
            List<Long> projectIds = Collections.singletonList(1L);
            Long userId = 100L;

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));

            var response = batchOperationService.batchDeleteProjects(projectIds, userId);

            assertFalse(response.getSuccess());
            assertEquals(0, response.getSuccessCount());
            assertEquals(1, response.getFailureCount());
            assertEquals("PERMISSION_DENIED", response.getErrors().get(0).getErrorCode());

            verify(projectRepository, never()).deleteAll(anyList());
        }

        @Test
        void batchDeleteProjects_EmptyList() {
            List<Long> projectIds = Collections.emptyList();
            Long userId = 100L;

            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchDeleteProjects(projectIds, userId);
            });
        }
    }

    @Nested
    class BatchDeleteItemsTests {

        @Test
        void batchDeleteItems_TenderType() {
            String itemType = "tender";
            List<Long> ids = Collections.singletonList(1L);

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            doNothing().when(tenderRepository).deleteAll(anyList());

            var response = batchOperationService.batchDeleteItems(itemType, ids);

            assertTrue(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
            assertEquals(0, response.getFailureCount());
        }

        @Test
        void batchDeleteItems_TaskType() {
            String itemType = "task";
            List<Long> ids = Collections.singletonList(1L);

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
            doNothing().when(taskRepository).deleteAll(anyList());

            var response = batchOperationService.batchDeleteItems(itemType, ids);

            assertTrue(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
        }

        @Test
        void batchDeleteItems_ProjectType() {
            String itemType = "project";
            List<Long> ids = Collections.singletonList(1L);

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));
            doNothing().when(projectRepository).deleteAll(anyList());

            var response = batchOperationService.batchDeleteItems(itemType, ids);

            assertTrue(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
        }

        @Test
        void batchDeleteItems_UnsupportedType() {
            String itemType = "unsupported_type";
            List<Long> ids = Collections.singletonList(1L);

            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchDeleteItems(itemType, ids);
            });
        }

        @Test
        void batchDeleteItems_CaseInsensitive() {
            String itemType = "TENDER";
            List<Long> ids = Collections.singletonList(1L);

            when(tenderRepository.findById(1L)).thenReturn(Optional.of(testTender1));
            doNothing().when(tenderRepository).deleteAll(anyList());

            var response = batchOperationService.batchDeleteItems(itemType, ids);

            assertTrue(response.getSuccess());
            assertEquals(1, response.getSuccessCount());
        }
    }

    @Nested
    class BatchOperationBoundaryTests {

        @Test
        void batchOperation_ExceedsMaxBatchSize() {
            List<Long> tooManyIds = java.util.Collections.nCopies(101, 1L);
            Long userId = 100L;

            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchClaimTenders(tooManyIds, userId);
            });
        }

        @Test
        void batchOperation_NullIds() {
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchClaimTenders(null, 100L);
            });
        }

        @Test
        void batchOperation_NullUserId() {
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchClaimTenders(Collections.singletonList(1L), null);
            });
        }

        @Test
        void batchOperation_NegativeUserId() {
            assertThrows(IllegalArgumentException.class, () -> {
                batchOperationService.batchClaimTenders(Collections.singletonList(1L), -1L);
            });
        }
    }
}
