package com.xiyu.bid.approval.controller;

import com.xiyu.bid.approval.dto.ApprovalDecisionRequest;
import com.xiyu.bid.approval.dto.ApprovalDetailDTO;
import com.xiyu.bid.approval.dto.ApprovalStatisticsDTO;
import com.xiyu.bid.approval.dto.ApprovalSubmitRequest;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import com.xiyu.bid.approval.service.ApprovalWorkflowService;
import com.xiyu.bid.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审批流程Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalWorkflowService approvalWorkflowService;

    /**
     * 提交审批
     */
    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> submitApproval(
            @Valid @RequestBody ApprovalSubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromDetails(userDetails);
        String userName = userDetails.getUsername();

        ApprovalDetailDTO result = approvalWorkflowService.submitForApproval(request, userId, userName);

        Map<String, Object> data = new HashMap<>();
        data.put("id", result.getId());
        data.put("status", result.getStatus());
        data.put("message", "审批提交成功");

        return ApiResponse.success(data);
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromDetails(userDetails);
        String userName = userDetails.getUsername();

        ApprovalDetailDTO result = approvalWorkflowService.approve(id, userId, userName, request.getComment());

        Map<String, Object> data = new HashMap<>();
        data.put("id", result.getId());
        data.put("status", result.getStatus());
        data.put("message", "审批通过");

        return ApiResponse.success(data);
    }

    /**
     * 审批驳回
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromDetails(userDetails);
        String userName = userDetails.getUsername();

        ApprovalDetailDTO result = approvalWorkflowService.reject(id, userId, userName, request.getComment());

        Map<String, Object> data = new HashMap<>();
        data.put("id", result.getId());
        data.put("status", result.getStatus());
        data.put("requireResubmit", request.getRequireResubmit());
        data.put("message", "审批已驳回");

        return ApiResponse.success(data);
    }

    /**
     * 取消审批
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromDetails(userDetails);
        String userName = userDetails.getUsername();

        approvalWorkflowService.cancel(id, userId, userName);

        return ApiResponse.success("审批已取消", null);
    }

    /**
     * 获取待审批列表
     */
    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<ApprovalDetailDTO>> getPendingApprovals(
            @RequestParam(required = false) Long approverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<ApprovalDetailDTO> result = approvalWorkflowService.getPendingApprovals(approverId, pageable);

        return ApiResponse.success(result);
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<ApprovalStatisticsDTO> getStatistics() {
        ApprovalStatisticsDTO stats = approvalWorkflowService.getStatistics();
        return ApiResponse.success(stats);
    }

    /**
     * 获取审批详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ApprovalDetailDTO> getApprovalDetail(@PathVariable UUID id) {
        ApprovalDetailDTO detail = approvalWorkflowService.getApprovalDetail(id);
        return ApiResponse.success(detail);
    }

    /**
     * 标记为已读
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromDetails(userDetails);
        approvalWorkflowService.markAsRead(id, userId);

        return ApiResponse.success("已标记为已读", "已标记为已读");
    }

    /**
     * 获取我的审批列表 (我提交的)
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<ApprovalDetailDTO>> getMyApprovals(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromDetails(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<ApprovalDetailDTO> result = approvalWorkflowService.getMyApprovals(userId, status, pageable);

        return ApiResponse.success(result);
    }

    /**
     * 批量审批通过
     */
    @PostMapping("/batch/approve")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<UUID, String>> batchApprove(
            @RequestBody Map<String, Object> requestBody,
            @AuthenticationPrincipal UserDetails userDetails) {

        @SuppressWarnings("unchecked")
        List<UUID> ids = (List<UUID>) requestBody.get("ids");
        String comment = (String) requestBody.getOrDefault("comment", "批量通过");

        Long userId = getUserIdFromDetails(userDetails);
        String userName = userDetails.getUsername();

        Map<UUID, String> results = approvalWorkflowService.batchApprove(ids, userId, userName, comment);

        return ApiResponse.success(results);
    }

    /**
     * 批量驳回
     */
    @PostMapping("/batch/reject")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<UUID, String>> batchReject(
            @RequestBody Map<String, Object> requestBody,
            @AuthenticationPrincipal UserDetails userDetails) {

        @SuppressWarnings("unchecked")
        List<UUID> ids = (List<UUID>) requestBody.get("ids");
        String comment = (String) requestBody.getOrDefault("comment", "批量驳回");

        Long userId = getUserIdFromDetails(userDetails);
        String userName = userDetails.getUsername();

        Map<UUID, String> results = approvalWorkflowService.batchReject(ids, userId, userName, comment);

        return ApiResponse.success(results);
    }

    /**
     * 重新提交审批
     */
    @PostMapping("/{id}/resubmit")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ApprovalDetailDTO> resubmit(
            @PathVariable UUID id,
            @RequestBody Map<String, String> requestBody,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserIdFromDetails(userDetails);
        String userName = userDetails.getUsername();
        String newDescription = requestBody.get("description");

        ApprovalDetailDTO result = approvalWorkflowService.resubmit(id, userId, userName, newDescription);

        return ApiResponse.success(result);
    }

    /**
     * 从UserDetails获取用户ID
     *
     * SECURITY: 验证用户ID格式并拒绝无效值
     * - 拒绝非数字用户名 (防止认证绕过)
     * - 拒绝负数和零值用户ID (防止权限提升)
     * - 抛出异常而非返回默认值 (确保审计跟踪)
     */
    private Long getUserIdFromDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.security.authentication.AuthenticationServiceException(
                    "UserDetails cannot be null");
        }

        String username = userDetails.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new org.springframework.security.authentication.AuthenticationServiceException(
                    "Username cannot be null or empty");
        }

        try {
            Long userId = Long.parseLong(username.trim());
            if (userId <= 0) {
                throw new org.springframework.security.authentication.AuthenticationServiceException(
                        "Invalid user identifier: must be positive");
            }
            return userId;
        } catch (NumberFormatException e) {
            throw new org.springframework.security.authentication.AuthenticationServiceException(
                    "Invalid user identifier: username must be numeric", e);
        }
    }
}
