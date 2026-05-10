// Input: HTTP 请求、路径参数、认证上下文和 DTO
// Output: 标准化 API 响应和用例入口
// Pos: Controller/接口适配层
// 维护声明: 仅维护协议适配与参数校验；业务规则下沉到 service.
package com.xiyu.bid.tender.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.project.dto.ProjectDTO;
import com.xiyu.bid.service.AuthService;
import com.xiyu.bid.tender.dto.TenderEvaluationDTO;
import com.xiyu.bid.tender.dto.TenderEvaluationRequest;
import com.xiyu.bid.tender.dto.TenderReviewRequest;
import com.xiyu.bid.tender.service.TenderEvaluationService;
import com.xiyu.bid.task.dto.TaskDTO;
import com.xiyu.bid.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 标讯评估与审核控制器
 * 处理项目经理提交评估和投标部管理员审核
 */
@RestController
@RequestMapping("/api/tenders")
@RequiredArgsConstructor
@Slf4j
public class TenderEvaluationController {

    private final TenderEvaluationService tenderEvaluationService;
    private final TaskService taskService;
    private final AuthService authService;

    /**
     * 获取标讯评估详情
     */
    @GetMapping("/{tenderId}/evaluation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TenderEvaluationDTO>> getEvaluation(@PathVariable Long tenderId) {
        log.info("GET /api/tenders/{}/evaluation", tenderId);
        return tenderEvaluationService.getEvaluation(tenderId)
                .map(evaluation -> ResponseEntity.ok(ApiResponse.success("ok", evaluation)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "标讯尚未提交评估"));
    }

    /**
     * 项目经理提交评估（状态变为已评估）
     */
    @PostMapping("/{tenderId}/evaluation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TenderEvaluationDTO>> submitEvaluation(
            @PathVariable Long tenderId,
            @Valid @RequestBody TenderEvaluationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /api/tenders/{}/evaluation - Submitting evaluation", tenderId);
        Long userId = currentUserId(userDetails);
        TenderEvaluationDTO evaluation = tenderEvaluationService.submitEvaluation(tenderId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("评估提交成功", evaluation));
    }

    /**
     * 管理员审核标讯（投标或弃标）
     */
    @PostMapping("/{tenderId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenderEvaluationDTO>> reviewTender(
            @PathVariable Long tenderId,
            @Valid @RequestBody TenderReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /api/tenders/{}/review - Reviewing tender, approved={}", tenderId, request.approved());
        Long userId = currentUserId(userDetails);
        TenderEvaluationDTO evaluation = tenderEvaluationService.reviewTender(tenderId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("审核完成", evaluation));
    }

    /**
     * 投标操作：投标部管理员审核通过后，创建项目并生成待办
     */
    @PostMapping("/{tenderId}/bid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenderBidResult>> proceedToBid(
            @PathVariable Long tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /api/tenders/{}/bid - Proceeding to bid", tenderId);
        Long userId = currentUserId(userDetails);
        TenderBidResult result = tenderEvaluationService.proceedToBid(tenderId, userId);
        return ResponseEntity.ok(ApiResponse.success("投标立项成功", result));
    }

    private Long currentUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法识别当前用户");
        }
        return authService.resolveUserIdByUsername(userDetails.getUsername().trim());
    }

    /**
     * 投标结果 DTO
     */
    public record TenderBidResult(
            Long projectId,
            String projectName,
            Long taskId,
            String taskTitle
    ) {}
}
