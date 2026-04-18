// Input: approval repositories, policies, and DTO assemblers
// Output: approval command orchestration results
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.approval.service;

import com.xiyu.bid.approval.core.ApprovalDecisionPolicy;
import com.xiyu.bid.approval.core.ApprovalPermissionPolicy;
import com.xiyu.bid.approval.core.ApprovalRuleResult;
import com.xiyu.bid.approval.dto.ApprovalDetailDTO;
import com.xiyu.bid.approval.dto.ApprovalSubmitRequest;
import com.xiyu.bid.approval.entity.ApprovalRequest;
import com.xiyu.bid.approval.enums.ApprovalActionType;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import com.xiyu.bid.approval.repository.ApprovalRequestRepository;
import com.xiyu.bid.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 审批命令编排服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalCommandService {

    private static final Long DEFAULT_APPROVER_ID = 1L;

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalDecisionPolicy decisionPolicy;
    private final ApprovalPermissionPolicy permissionPolicy;
    private final ApprovalActionRecorder actionRecorder;
    private final ApprovalDetailAssembler detailAssembler;

    @Transactional
    public ApprovalDetailDTO submitForApproval(ApprovalSubmitRequest request, Long userId, String userName) {
        log.info("用户 {} 提交审批: 项目={}, 类型={}", userId, request.getProjectId(), request.getApprovalType());

        assertAllowed(decisionPolicy.canSubmit(
                requestRepository.findByProjectIdOrderByCreatedAtDesc(request.getProjectId())
        ));

        ApprovalRequest approvalRequest = buildApprovalRequest(request, userId, userName);
        ApprovalRequest savedRequest = requestRepository.save(approvalRequest);

        actionRecorder.record(
                savedRequest.getId(),
                ApprovalActionType.SUBMIT,
                userId,
                userName,
                request.getDescription(),
                null,
                ApprovalStatus.PENDING
        );

        return detailAssembler.toDetailDTO(savedRequest);
    }

    @Transactional
    public ApprovalDetailDTO approve(UUID requestId, Long approverId, String approverName, String comment) {
        log.info("用户 {} 审批通过: {}", approverId, requestId);

        ApprovalRequest request = getApprovalRequestEntity(requestId);
        assertAllowed(decisionPolicy.canApprove(request));
        assertAllowed(permissionPolicy.canApprove(request, approverId));

        ApprovalStatus previousStatus = request.getStatus();
        request.setStatus(ApprovalStatus.APPROVED);
        request.setCompletedAt(LocalDateTime.now());
        request.setIsRead(true);
        ApprovalRequest savedRequest = requestRepository.save(request);

        actionRecorder.record(
                savedRequest.getId(),
                ApprovalActionType.APPROVE,
                approverId,
                approverName,
                comment,
                previousStatus,
                ApprovalStatus.APPROVED
        );

        return detailAssembler.toDetailDTO(savedRequest);
    }

    @Transactional
    public ApprovalDetailDTO reject(UUID requestId, Long approverId, String approverName, String reason) {
        log.info("用户 {} 审批驳回: {}", approverId, requestId);

        ApprovalRequest request = getApprovalRequestEntity(requestId);
        assertAllowed(decisionPolicy.canReject(request));
        assertAllowed(permissionPolicy.canApprove(request, approverId));

        ApprovalStatus previousStatus = request.getStatus();
        request.setStatus(ApprovalStatus.REJECTED);
        request.setCompletedAt(LocalDateTime.now());
        request.setIsRead(true);
        ApprovalRequest savedRequest = requestRepository.save(request);

        actionRecorder.record(
                savedRequest.getId(),
                ApprovalActionType.REJECT,
                approverId,
                approverName,
                reason,
                previousStatus,
                ApprovalStatus.REJECTED
        );

        return detailAssembler.toDetailDTO(savedRequest);
    }

    @Transactional
    public void cancel(UUID requestId, Long userId, String userName) {
        log.info("用户 {} 取消审批: {}", userId, requestId);

        ApprovalRequest request = getApprovalRequestEntity(requestId);
        assertAllowed(decisionPolicy.canCancel(request, userId));

        ApprovalStatus previousStatus = request.getStatus();
        request.setStatus(ApprovalStatus.CANCELLED);
        request.setCompletedAt(LocalDateTime.now());
        ApprovalRequest savedRequest = requestRepository.save(request);

        actionRecorder.record(
                savedRequest.getId(),
                ApprovalActionType.CANCEL,
                userId,
                userName,
                "申请人取消审批",
                previousStatus,
                ApprovalStatus.CANCELLED
        );
    }

    @Transactional
    public void markAsRead(UUID requestId, Long userId) {
        ApprovalRequest request = getApprovalRequestEntity(requestId);
        assertAllowed(permissionPolicy.canMarkAsRead(request, userId));

        if (!request.getIsRead()) {
            request.setIsRead(true);
            requestRepository.save(request);
        }
    }

    @Transactional
    public ApprovalDetailDTO resubmit(UUID requestId, Long userId, String userName, String newDescription) {
        ApprovalRequest originalRequest = getApprovalRequestEntity(requestId);
        assertAllowed(decisionPolicy.canResubmit(originalRequest, userId));

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

    @Transactional
    public Map<UUID, String> batchApprove(List<UUID> requestIds, Long approverId, String approverName, String comment) {
        return executeBatch(requestIds, requestId -> approve(requestId, approverId, approverName, comment), "审批成功", "审批失败");
    }

    @Transactional
    public Map<UUID, String> batchReject(List<UUID> requestIds, Long approverId, String approverName, String reason) {
        return executeBatch(requestIds, requestId -> reject(requestId, approverId, approverName, reason), "驳回成功", "驳回失败");
    }

    private Map<UUID, String> executeBatch(
            List<UUID> requestIds,
            ApprovalCommand command,
            String successMessage,
            String failurePrefix
    ) {
        Map<UUID, String> results = new HashMap<>();
        for (UUID requestId : requestIds) {
            try {
                command.execute(requestId);
                results.put(requestId, successMessage);
            } catch (RuntimeException ex) {
                results.put(requestId, failurePrefix + ": " + ex.getMessage());
            }
        }
        return results;
    }

    private ApprovalRequest buildApprovalRequest(ApprovalSubmitRequest request, Long userId, String userName) {
        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .projectId(request.getProjectId())
                .projectName(request.getProjectName())
                .approvalType(request.getApprovalType())
                .status(ApprovalStatus.PENDING)
                .requesterId(userId)
                .requesterName(userName)
                .currentApproverId(decisionPolicy.resolveApproverId(request.getApproverId(), DEFAULT_APPROVER_ID))
                .currentApproverName(null)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .submittedAt(LocalDateTime.now())
                .isRead(false)
                .build();

        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            approvalRequest.setAttachmentIds(request.getAttachmentIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }
        return approvalRequest;
    }

    private ApprovalRequest getApprovalRequestEntity(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("审批请求不存在: " + requestId));
    }

    private void assertAllowed(ApprovalRuleResult result) {
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
    }

    @FunctionalInterface
    private interface ApprovalCommand {
        void execute(UUID requestId);
    }
}
