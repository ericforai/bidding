package com.xiyu.bid.approval.service;

import com.xiyu.bid.approval.dto.*;
import com.xiyu.bid.approval.entity.ApprovalAction;
import com.xiyu.bid.approval.entity.ApprovalRequest;
import com.xiyu.bid.approval.enums.ApprovalActionType;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import com.xiyu.bid.approval.repository.ApprovalActionRepository;
import com.xiyu.bid.approval.repository.ApprovalRequestRepository;
import com.xiyu.bid.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批流程服务
 * 实现审批的状态机逻辑和操作记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalActionRepository actionRepository;

    /**
     * 默认审批人 (可以配置化)
     */
    private static final Long DEFAULT_APPROVER_ID = 1L;
    private static final String DEFAULT_APPROVER_NAME = "系统管理员";

    /**
     * 提交审批
     */
    @Transactional
    public ApprovalDetailDTO submitForApproval(ApprovalSubmitRequest request, Long userId, String userName) {
        log.info("用户 {} 提交审批: 项目={}, 类型={}", userId, request.getProjectId(), request.getApprovalType());

        // 检查项目是否已有待审批的请求
        List<ApprovalRequest> existingRequests = requestRepository.findByProjectIdOrderByCreatedAtDesc(request.getProjectId());
        boolean hasPending = existingRequests.stream()
                .anyMatch(r -> r.getStatus() == ApprovalStatus.PENDING);
        if (hasPending) {
            throw new BusinessException("该项目已有待审批的请求，请等待处理完成");
        }

        // 确定审批人
        Long approverId = request.getApproverId() != null ? request.getApproverId() : DEFAULT_APPROVER_ID;

        // 创建审批请求
        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .projectId(request.getProjectId())
                .projectName(request.getProjectName())
                .approvalType(request.getApprovalType())
                .status(ApprovalStatus.PENDING)
                .requesterId(userId)
                .requesterName(userName)
                .currentApproverId(approverId)
                .currentApproverName(null) // 由审批系统确定
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .submittedAt(LocalDateTime.now())
                .isRead(false)
                .build();

        // 处理附件
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            String attachmentIdsStr = request.getAttachmentIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            approvalRequest.setAttachmentIds(attachmentIdsStr);
        }

        approvalRequest = requestRepository.save(approvalRequest);

        // 创建提交操作记录
        ApprovalAction submitAction = ApprovalAction.builder()
                .approvalRequestId(approvalRequest.getId())
                .actionType(ApprovalActionType.SUBMIT)
                .actorId(userId)
                .actorName(userName)
                .comment(request.getDescription())
                .actionTime(LocalDateTime.now())
                .previousStatus(null)
                .newStatus(ApprovalStatus.PENDING)
                .build();
        actionRepository.save(submitAction);

        // Notification interface reserved for future implementation:
        // A notification service will be integrated here to alert the approver
        // when a new approval request is submitted. This depends on the
        // notification module which is planned for a future phase.

        return toDetailDTO(approvalRequest);
    }

    /**
     * 审批通过
     */
    @Transactional
    public ApprovalDetailDTO approve(UUID requestId, Long approverId, String approverName, String comment) {
        log.info("用户 {} 审批通过: {}", approverId, requestId);

        ApprovalRequest request = getApprovalRequestEntity(requestId);

        // 状态检查
        if (!request.canBeApproved()) {
            throw new BusinessException("当前状态不允许审批: " + request.getStatus().getDescription());
        }

        // 权限检查
        if (!canApprove(request, approverId)) {
            throw new BusinessException("您没有权限审批此请求");
        }

        ApprovalStatus previousStatus = request.getStatus();

        // 更新状态
        request.setStatus(ApprovalStatus.APPROVED);
        request.setCompletedAt(LocalDateTime.now());
        request.setIsRead(true);
        request = requestRepository.save(request);

        // 记录操作
        recordAction(request.getId(), ApprovalActionType.APPROVE, approverId, approverName,
                comment, previousStatus, ApprovalStatus.APPROVED);

        // Notification interface reserved for future implementation:
        // A notification service will be integrated here to alert the requester
        // when their approval request is approved. This depends on the
        // notification module which is planned for a future phase.

        return toDetailDTO(request);
    }

    /**
     * 审批驳回
     */
    @Transactional
    public ApprovalDetailDTO reject(UUID requestId, Long approverId, String approverName, String reason) {
        log.info("用户 {} 审批驳回: {}", approverId, requestId);

        ApprovalRequest request = getApprovalRequestEntity(requestId);

        // 状态检查
        if (!request.canBeApproved()) {
            throw new BusinessException("当前状态不允许审批: " + request.getStatus().getDescription());
        }

        // 权限检查
        if (!canApprove(request, approverId)) {
            throw new BusinessException("您没有权限审批此请求");
        }

        ApprovalStatus previousStatus = request.getStatus();

        // 更新状态
        request.setStatus(ApprovalStatus.REJECTED);
        request.setCompletedAt(LocalDateTime.now());
        request.setIsRead(true);
        request = requestRepository.save(request);

        // 记录操作
        recordAction(request.getId(), ApprovalActionType.REJECT, approverId, approverName,
                reason, previousStatus, ApprovalStatus.REJECTED);

        // Notification interface reserved for future implementation:
        // A notification service will be integrated here to alert the requester
        // when their approval request is rejected. This depends on the
        // notification module which is planned for a future phase.

        return toDetailDTO(request);
    }

    /**
     * 取消审批 (仅申请人可操作)
     */
    @Transactional
    public void cancel(UUID requestId, Long userId, String userName) {
        log.info("用户 {} 取消审批: {}", userId, requestId);

        ApprovalRequest request = getApprovalRequestEntity(requestId);

        // 权限检查
        if (!request.canBeCancelledBy(userId)) {
            throw new BusinessException("只有申请人可以取消待审批的请求");
        }

        ApprovalStatus previousStatus = request.getStatus();

        // 更新状态
        request.setStatus(ApprovalStatus.CANCELLED);
        request.setCompletedAt(LocalDateTime.now());
        requestRepository.save(request);

        // 记录操作
        recordAction(request.getId(), ApprovalActionType.CANCEL, userId, userName,
                "申请人取消审批", previousStatus, ApprovalStatus.CANCELLED);

        // Notification interface reserved for future implementation:
        // A notification service will be integrated here to alert the approver
        // when an approval request is cancelled by the requester. This depends on the
        // notification module which is planned for a future phase.
    }

    /**
     * 获取待审批列表
     */
    public Page<ApprovalDetailDTO> getPendingApprovals(Long approverId, Pageable pageable) {
        List<ApprovalRequest> requests;

        if (approverId != null) {
            requests = requestRepository.findByStatusAndCurrentApproverIdOrderByPriorityDescCreatedAtDesc(
                    ApprovalStatus.PENDING, approverId);
        } else {
            requests = requestRepository.findByStatusOrderByPriorityDescCreatedAtDesc(ApprovalStatus.PENDING);
        }

        List<ApprovalDetailDTO> dtos = requests.stream()
                .map(this::toDetailDTO)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<ApprovalDetailDTO> pageContent = start < dtos.size() ? dtos.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, dtos.size());
    }

    /**
     * 获取统计数据
     */
    public ApprovalStatisticsDTO getStatistics() {
        Long totalCount = requestRepository.count();

        Map<String, Long> statusCounts = new HashMap<>();
        List<Object[]> countByStatus = requestRepository.countByStatus();
        for (Object[] row : countByStatus) {
            statusCounts.put(String.valueOf(row[0]), (Long) row[1]);
        }

        Long pendingCount = statusCounts.getOrDefault(ApprovalStatus.PENDING.name(), 0L);
        Long approvedCount = statusCounts.getOrDefault(ApprovalStatus.APPROVED.name(), 0L);
        Long rejectedCount = statusCounts.getOrDefault(ApprovalStatus.REJECTED.name(), 0L);
        Long cancelledCount = statusCounts.getOrDefault(ApprovalStatus.CANCELLED.name(), 0L);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        Long todaySubmitted = requestRepository.countBySubmittedAtBetween(todayStart, tomorrowStart);
        Long monthSubmitted = requestRepository.countBySubmittedAtBetweenAndStatusIn(
                monthStart,
                nextMonthStart,
                List.of(ApprovalStatus.PENDING, ApprovalStatus.APPROVED, ApprovalStatus.REJECTED, ApprovalStatus.CANCELLED)
        );
        Long overdueCount = requestRepository.countByStatusAndDueDateBefore(ApprovalStatus.PENDING, now);
        Long nearDueCount = requestRepository.countByStatusAndDueDateBetween(ApprovalStatus.PENDING, now, now.plusHours(24));

        Double avgProcessingHours = requestRepository.findByStatusInAndCompletedAtIsNotNull(
                        List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED)
                ).stream()
                .filter(item -> item.getSubmittedAt() != null && item.getCompletedAt() != null)
                .mapToLong(item -> ChronoUnit.MINUTES.between(item.getSubmittedAt(), item.getCompletedAt()))
                .average()
                .stream()
                .map(avgMinutes -> avgMinutes / 60.0)
                .boxed()
                .findFirst()
                .orElse(null);

        // 计算通过率
        Double approvalRate = null;
        Long totalDecisions = approvedCount + rejectedCount;
        if (totalDecisions > 0) {
            approvalRate = (approvedCount.doubleValue() / totalDecisions) * 100;
        }

        // 按类型统计
        Map<String, Long> byType = new HashMap<>();
        for (Object[] row : requestRepository.countByType()) {
            byType.put((String) row[0], (Long) row[1]);
        }

        // 按优先级统计
        Map<Integer, Long> byPriority = new HashMap<>();
        for (Object[] row : requestRepository.countByPriority()) {
            byPriority.put((Integer) row[0], (Long) row[1]);
        }

        return ApprovalStatisticsDTO.builder()
                .totalCount(totalCount)
                .pendingCount(pendingCount)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .cancelledCount(cancelledCount)
                .todaySubmitted(todaySubmitted)
                .monthSubmitted(monthSubmitted)
                .overdueCount(overdueCount)
                .nearDueCount(nearDueCount)
                .avgProcessingHours(avgProcessingHours)
                .approvalRate(approvalRate)
                .byType(byType)
                .byPriority(byPriority)
                .build();
    }

    /**
     * 获取审批详情
     */
    public ApprovalDetailDTO getApprovalDetail(UUID requestId) {
        ApprovalRequest request = getApprovalRequestEntity(requestId);
        return toDetailDTO(request);
    }

    /**
     * 标记为已读
     */
    @Transactional
    public void markAsRead(UUID requestId, Long userId) {
        ApprovalRequest request = getApprovalRequestEntity(requestId);

        // 只有审批人可以标记已读
        if (!request.getCurrentApproverId().equals(userId)) {
            throw new BusinessException("只有当前审批人可以标记已读");
        }

        if (!request.getIsRead()) {
            request.setIsRead(true);
            requestRepository.save(request);
        }
    }

    /**
     * 批量审批
     */
    @Transactional
    public Map<UUID, String> batchApprove(List<UUID> requestIds, Long approverId, String approverName, String comment) {
        Map<UUID, String> results = new HashMap<>();

        for (UUID requestId : requestIds) {
            try {
                approve(requestId, approverId, approverName, comment);
                results.put(requestId, "审批成功");
            } catch (Exception e) {
                results.put(requestId, "审批失败: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * 批量驳回
     */
    @Transactional
    public Map<UUID, String> batchReject(List<UUID> requestIds, Long approverId, String approverName, String reason) {
        Map<UUID, String> results = new HashMap<>();

        for (UUID requestId : requestIds) {
            try {
                reject(requestId, approverId, approverName, reason);
                results.put(requestId, "驳回成功");
            } catch (Exception e) {
                results.put(requestId, "驳回失败: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * 获取我的审批列表 (我提交的)
     */
    public Page<ApprovalDetailDTO> getMyApprovals(Long userId, ApprovalStatus status, Pageable pageable) {
        List<ApprovalRequest> requests = requestRepository.findByRequesterIdOrderByCreatedAtDesc(userId);

        List<ApprovalDetailDTO> dtos = requests.stream()
                .filter(r -> status == null || r.getStatus() == status)
                .map(this::toDetailDTO)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<ApprovalDetailDTO> pageContent = start < dtos.size() ? dtos.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, dtos.size());
    }

    /**
     * 重新提交审批
     */
    @Transactional
    public ApprovalDetailDTO resubmit(UUID requestId, Long userId, String userName, String newDescription) {
        ApprovalRequest originalRequest = getApprovalRequestEntity(requestId);

        // 只有被驳回的请求可以重新提交
        if (originalRequest.getStatus() != ApprovalStatus.REJECTED) {
            throw new BusinessException("只有被驳回的请求可以重新提交");
        }

        // 只有申请人可以重新提交
        if (!originalRequest.getRequesterId().equals(userId)) {
            throw new BusinessException("只有申请人可以重新提交");
        }

        // 创建新的审批请求
        ApprovalSubmitRequest submitRequest = ApprovalSubmitRequest.builder()
                .projectId(originalRequest.getProjectId())
                .projectName(originalRequest.getProjectName())
                .approvalType(originalRequest.getApprovalType())
                .title(originalRequest.getTitle())
                .description(newDescription != null ? newDescription : originalRequest.getDescription())
                .priority(originalRequest.getPriority())
                .dueDate(originalRequest.getDueDate())
                .approverId(originalRequest.getCurrentApproverId())
                .build();

        return submitForApproval(submitRequest, userId, userName);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取审批请求实体
     */
    private ApprovalRequest getApprovalRequestEntity(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("审批请求不存在: " + requestId));
    }

    /**
     * 检查用户是否有权限审批
     */
    private boolean canApprove(ApprovalRequest request, Long userId) {
        // If a specific approver is assigned, verify it matches
        if (request.getCurrentApproverId() != null) {
            return request.getCurrentApproverId().equals(userId);
        }
        // Future enhancement: More complex permission logic can be added here
        // such as role-based approval chains, delegation rules, etc.
        return true;
    }

    /**
     * 记录操作
     */
    private void recordAction(UUID requestId, ApprovalActionType actionType,
                              Long actorId, String actorName, String comment,
                              ApprovalStatus previousStatus, ApprovalStatus newStatus) {
        ApprovalAction action = ApprovalAction.builder()
                .approvalRequestId(requestId)
                .actionType(actionType)
                .actorId(actorId)
                .actorName(actorName)
                .comment(comment)
                .actionTime(LocalDateTime.now())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .build();
        actionRepository.save(action);
    }

    /**
     * 转换为详情DTO
     */
    private ApprovalDetailDTO toDetailDTO(ApprovalRequest request) {
        List<ApprovalAction> actions = actionRepository.findByApprovalRequestIdOrderByActionTimeAsc(request.getId());

        List<ApprovalActionDTO> actionDTOs = actions.stream()
                .map(this::toActionDTO)
                .collect(Collectors.toList());

        Long processingHours = null;
        if (request.getCompletedAt() != null && request.getSubmittedAt() != null) {
            processingHours = ChronoUnit.HOURS.between(request.getSubmittedAt(), request.getCompletedAt());
        }

        return ApprovalDetailDTO.builder()
                .id(request.getId())
                .projectId(request.getProjectId())
                .projectName(request.getProjectName())
                .approvalType(request.getApprovalType())
                .status(request.getStatus())
                .statusDescription(request.getStatus().getDescription())
                .requesterId(request.getRequesterId())
                .requesterName(request.getRequesterName())
                .currentApproverId(request.getCurrentApproverId())
                .currentApproverName(request.getCurrentApproverName())
                .priority(request.getPriority())
                .title(request.getTitle())
                .description(request.getDescription())
                .submittedAt(request.getSubmittedAt())
                .completedAt(request.getCompletedAt())
                .dueDate(request.getDueDate())
                .isRead(request.getIsRead())
                .isOverdue(request.isOverdue())
                .isNearDueDate(request.isNearDueDate())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .actions(actionDTOs)
                .processingHours(processingHours)
                .build();
    }

    /**
     * 转换为操作DTO
     */
    private ApprovalActionDTO toActionDTO(ApprovalAction action) {
        return ApprovalActionDTO.builder()
                .id(action.getId())
                .actionType(action.getActionType())
                .actorId(action.getActorId())
                .actorName(action.getActorName())
                .comment(action.getComment())
                .actionTime(action.getActionTime())
                .previousStatus(action.getPreviousStatus() != null ? action.getPreviousStatus().name() : null)
                .newStatus(action.getNewStatus() != null ? action.getNewStatus().name() : null)
                .build();
    }
}
