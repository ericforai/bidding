package com.xiyu.bid.approval.service;

import com.xiyu.bid.approval.dto.*;
import com.xiyu.bid.approval.entity.ApprovalAction;
import com.xiyu.bid.approval.entity.ApprovalRequest;
import com.xiyu.bid.approval.enums.ApprovalActionType;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import com.xiyu.bid.approval.repository.ApprovalActionRepository;
import com.xiyu.bid.approval.repository.ApprovalRequestRepository;
import com.xiyu.bid.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApprovalWorkflowService测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalWorkflowService测试")
class ApprovalWorkflowServiceTest {

    @Mock
    private ApprovalRequestRepository requestRepository;

    @Mock
    private ApprovalActionRepository actionRepository;

    @InjectMocks
    private ApprovalWorkflowService service;

    private ApprovalRequest testRequest;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testRequest = ApprovalRequest.builder()
                .id(testId)
                .projectId(1L)
                .projectName("测试项目")
                .approvalType("BID_DOCUMENT")
                .status(ApprovalStatus.PENDING)
                .requesterId(100L)
                .requesterName("张三")
                .currentApproverId(200L)
                .currentApproverName("李四")
                .priority(1)
                .title("投标文档审批")
                .description("请审批投标文档")
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("应该成功提交审批")
    void submitForApproval_ShouldSucceed() {
        ApprovalSubmitRequest submitRequest = ApprovalSubmitRequest.builder()
                .projectId(1L)
                .projectName("测试项目")
                .approvalType("BID_DOCUMENT")
                .title("投标文档审批")
                .description("请审批")
                .priority(1)
                .approverId(200L)
                .dueDate(LocalDateTime.now().plusDays(3))
                .build();

        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.save(any(ApprovalAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalDetailDTO result = service.submitForApproval(submitRequest, 100L, "张三");

        assertNotNull(result);
        assertEquals("BID_DOCUMENT", result.getApprovalType());
        assertEquals(ApprovalStatus.PENDING, result.getStatus());
        assertEquals(100L, result.getRequesterId());
        assertEquals("张三", result.getRequesterName());

        verify(requestRepository, times(1)).save(any(ApprovalRequest.class));
        verify(actionRepository, times(1)).save(any(ApprovalAction.class));
    }

    @Test
    @DisplayName("应该成功审批通过")
    void approve_ShouldSucceed() {
        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.save(any(ApprovalAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.findByApprovalRequestIdOrderByActionTimeAsc(testId)).thenReturn(new ArrayList<>());

        ApprovalDetailDTO result = service.approve(testId, 200L, "李四", "同意");

        assertNotNull(result);
        assertEquals(ApprovalStatus.APPROVED, result.getStatus());
        assertNotNull(result.getCompletedAt());

        verify(requestRepository, times(1)).save(any(ApprovalRequest.class));
        verify(actionRepository, times(1)).save(any(ApprovalAction.class));
    }

    @Test
    @DisplayName("不应该允许审批非待审批状态的请求")
    void approve_ShouldThrowException_WhenStatusIsNotPending() {
        testRequest.setStatus(ApprovalStatus.APPROVED);
        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));

        assertThrows(BusinessException.class, () ->
                service.approve(testId, 200L, "李四", "同意"));

        verify(requestRepository, never()).save(any());
        verify(actionRepository, never()).save(any());
    }

    @Test
    @DisplayName("不应该允许非审批人进行审批")
    void approve_ShouldThrowException_WhenUserIsNotApprover() {
        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));

        assertThrows(BusinessException.class, () ->
                service.approve(testId, 300L, "王五", "同意"));

        verify(requestRepository, never()).save(any());
        verify(actionRepository, never()).save(any());
    }

    @Test
    @DisplayName("应该成功驳回审批")
    void reject_ShouldSucceed() {
        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.save(any(ApprovalAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalDetailDTO result = service.reject(testId, 200L, "李四", "内容不完整");

        assertNotNull(result);
        assertEquals(ApprovalStatus.REJECTED, result.getStatus());
        assertNotNull(result.getCompletedAt());

        verify(requestRepository, times(1)).save(any(ApprovalRequest.class));
        verify(actionRepository, times(1)).save(any(ApprovalAction.class));
    }

    @Test
    @DisplayName("应该成功取消审批")
    void cancel_ShouldSucceed() {
        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.save(any(ApprovalAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancel(testId, 100L, "张三");

        assertEquals(ApprovalStatus.CANCELLED, testRequest.getStatus());
        assertNotNull(testRequest.getCompletedAt());

        verify(requestRepository, times(1)).save(any(ApprovalRequest.class));
        verify(actionRepository, times(1)).save(any(ApprovalAction.class));
    }

    @Test
    @DisplayName("不应该允许非申请人取消审批")
    void cancel_ShouldThrowException_WhenUserIsNotRequester() {
        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));

        assertThrows(BusinessException.class, () ->
                service.cancel(testId, 200L, "李四"));

        verify(requestRepository, never()).save(any());
        verify(actionRepository, never()).save(any());
    }

    @Test
    @DisplayName("应该获取待审批列表")
    void getPendingApprovals_ShouldReturnList() {
        List<ApprovalRequest> requests = Arrays.asList(testRequest);
        when(requestRepository.findByStatusOrderByPriorityDescCreatedAtDesc(ApprovalStatus.PENDING))
                .thenReturn(requests);
        when(actionRepository.findByApprovalRequestIdOrderByActionTimeAsc(any())).thenReturn(new ArrayList<>());

        Page<ApprovalDetailDTO> result = service.getPendingApprovals(null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("应该获取审批人专属的待审批列表")
    void getPendingApprovals_ShouldReturnListForSpecificApprover() {
        List<ApprovalRequest> requests = Arrays.asList(testRequest);
        when(requestRepository.findByStatusAndCurrentApproverIdOrderByPriorityDescCreatedAtDesc(
                eq(ApprovalStatus.PENDING), eq(200L))).thenReturn(requests);
        when(actionRepository.findByApprovalRequestIdOrderByActionTimeAsc(any())).thenReturn(new ArrayList<>());

        Page<ApprovalDetailDTO> result = service.getPendingApprovals(200L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("应该获取统计数据")
    void getStatistics_ShouldReturnStatistics() {
        when(requestRepository.count()).thenReturn(100L);
        when(requestRepository.countByStatus()).thenReturn(Arrays.asList(
                new Object[]{"PENDING", 10L},
                new Object[]{"APPROVED", 70L},
                new Object[]{"REJECTED", 15L},
                new Object[]{"CANCELLED", 5L}
        ));
        when(requestRepository.countTodaySubmitted()).thenReturn(5L);
        when(requestRepository.countMonthSubmitted()).thenReturn(30L);
        when(requestRepository.countOverdue(any())).thenReturn(2L);
        when(requestRepository.countNearDue(any(), any())).thenReturn(3L);
        when(requestRepository.avgProcessingDuration()).thenReturn(24.0);
        when(requestRepository.countByType()).thenReturn(Arrays.asList(
                new Object[]{"BID_DOCUMENT", 50L},
                new Object[]{"PROJECT_START", 30L},
                new Object[]{"CONTRACT", 20L}
        ));
        when(requestRepository.countByPriority()).thenReturn(Arrays.asList(
                new Object[]{0, 70L},
                new Object[]{1, 25L},
                new Object[]{2, 5L}
        ));

        ApprovalStatisticsDTO stats = service.getStatistics();

        assertNotNull(stats);
        assertEquals(100L, stats.getTotalCount());
        assertEquals(10L, stats.getPendingCount());
        assertEquals(70L, stats.getApprovedCount());
        assertEquals(15L, stats.getRejectedCount());
        assertEquals(5L, stats.getCancelledCount());
        assertEquals(5L, stats.getTodaySubmitted());
        assertEquals(30L, stats.getMonthSubmitted());
        assertEquals(2L, stats.getOverdueCount());
        assertEquals(3L, stats.getNearDueCount());
        assertTrue(stats.getApprovalRate() > 0);
    }

    @Test
    @DisplayName("应该获取审批详情")
    void getApprovalDetail_ShouldReturnDetail() {
        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));

        List<ApprovalAction> actions = Arrays.asList(
                ApprovalAction.builder()
                        .id(UUID.randomUUID())
                        .approvalRequestId(testId)
                        .actionType(ApprovalActionType.SUBMIT)
                        .actorId(100L)
                        .actorName("张三")
                        .comment("提交审批")
                        .actionTime(LocalDateTime.now())
                        .build()
        );
        when(actionRepository.findByApprovalRequestIdOrderByActionTimeAsc(testId)).thenReturn(actions);

        ApprovalDetailDTO result = service.getApprovalDetail(testId);

        assertNotNull(result);
        assertEquals(testId, result.getId());
        assertNotNull(result.getActions());
        assertEquals(1, result.getActions().size());
    }

    @Test
    @DisplayName("获取不存在的审批详情应该抛出异常")
    void getApprovalDetail_ShouldThrowException_WhenNotFound() {
        when(requestRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getApprovalDetail(testId));
    }

    @Test
    @DisplayName("应该标记审批为已读")
    void markAsRead_ShouldSucceed() {
        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markAsRead(testId, 200L);

        assertTrue(testRequest.getIsRead());
        verify(requestRepository, times(1)).save(testRequest);
    }

    @Test
    @DisplayName("应该批量审批")
    void batchApprove_ShouldSucceed() {
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = Arrays.asList(testId, id2);

        ApprovalRequest request2 = ApprovalRequest.builder()
                .id(id2)
                .projectId(2L)
                .status(ApprovalStatus.PENDING)
                .currentApproverId(200L)
                .build();

        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));
        when(requestRepository.findById(id2)).thenReturn(Optional.of(request2));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.save(any(ApprovalAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<UUID, String> results = service.batchApprove(ids, 200L, "李四", "批量通过");

        assertNotNull(results);
        assertTrue(results.containsKey(testId));
        assertTrue(results.containsKey(id2));
    }

    @Test
    @DisplayName("批量审批时部分失败应该返回错误信息")
    void batchApprove_ShouldHandlePartialFailure() {
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = Arrays.asList(testId, id2);

        ApprovalRequest request2 = ApprovalRequest.builder()
                .id(id2)
                .projectId(2L)
                .status(ApprovalStatus.APPROVED) // 已完成，不能审批
                .currentApproverId(200L)
                .build();

        when(requestRepository.findById(testId)).thenReturn(Optional.of(testRequest));
        when(requestRepository.findById(id2)).thenReturn(Optional.of(request2));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.save(any(ApprovalAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<UUID, String> results = service.batchApprove(ids, 200L, "李四", "批量通过");

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.get(testId).contains("成功"));
        assertTrue(results.get(id2).contains("失败"));
    }
}
