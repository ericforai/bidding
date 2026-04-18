// Input: approval command/query services and request DTOs
// Output: Approval workflow facade methods for controllers
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.approval.service;

import com.xiyu.bid.approval.dto.ApprovalDetailDTO;
import com.xiyu.bid.approval.dto.ApprovalStatisticsDTO;
import com.xiyu.bid.approval.dto.ApprovalSubmitRequest;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import com.xiyu.bid.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审批流程 facade，保留兼容 API，对内委托命令与查询服务
 */
@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    private final ApprovalCommandService commandService;
    private final ApprovalQueryService queryService;

    public ApprovalDetailDTO submitForApproval(ApprovalSubmitRequest request, Long userId, String userName) {
        return commandService.submitForApproval(request, userId, userName);
    }

    public ApprovalDetailDTO approve(UUID requestId, Long approverId, String approverName, String comment) {
        return commandService.approve(requestId, approverId, approverName, comment);
    }

    public ApprovalDetailDTO reject(UUID requestId, Long approverId, String approverName, String reason) {
        return commandService.reject(requestId, approverId, approverName, reason);
    }

    public void cancel(UUID requestId, Long userId, String userName) {
        commandService.cancel(requestId, userId, userName);
    }

    public Page<ApprovalDetailDTO> getPendingApprovals(
            Long currentUserId,
            User.Role currentUserRole,
            Long approverId,
            Pageable pageable
    ) {
        return queryService.getPendingApprovals(currentUserId, currentUserRole, approverId, pageable);
    }

    public ApprovalStatisticsDTO getStatistics() {
        return queryService.getStatistics();
    }

    public ApprovalDetailDTO getApprovalDetail(UUID requestId, Long currentUserId, User.Role currentUserRole) {
        return queryService.getApprovalDetail(requestId, currentUserId, currentUserRole);
    }

    public void markAsRead(UUID requestId, Long userId) {
        commandService.markAsRead(requestId, userId);
    }

    public Map<UUID, String> batchApprove(List<UUID> requestIds, Long approverId, String approverName, String comment) {
        return commandService.batchApprove(requestIds, approverId, approverName, comment);
    }

    public Map<UUID, String> batchReject(List<UUID> requestIds, Long approverId, String approverName, String reason) {
        return commandService.batchReject(requestIds, approverId, approverName, reason);
    }

    public Page<ApprovalDetailDTO> getMyApprovals(Long userId, ApprovalStatus status, Pageable pageable) {
        return queryService.getMyApprovals(userId, status, pageable);
    }

    public ApprovalDetailDTO resubmit(UUID requestId, Long userId, String userName, String newDescription) {
        return commandService.resubmit(requestId, userId, userName, newDescription);
    }
}
