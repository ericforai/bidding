package com.xiyu.bid.batch.controller;

import com.xiyu.bid.batch.dto.BatchAssignRequest;
import com.xiyu.bid.batch.dto.BatchClaimRequest;
import com.xiyu.bid.batch.dto.BatchDeleteRequest;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.batch.service.BatchOperationService;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 批量操作控制器
 * 处理批量认领、分配、删除等操作的HTTP请求
 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchOperationController {

    private final BatchOperationService batchOperationService;

    private static final int MAX_REMARK_LENGTH = 500;

    /**
     * 批量认领标讯
     * POST /api/batch/tenders/claim
     *
     * @param request 批量认领请求
     * @return 操作结果
     */
    @PostMapping("/tenders/claim")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> batchClaimTenders(
            @Valid @RequestBody BatchClaimRequest request) {

        log.info("POST /api/batch/tenders/claim - Claiming {} tenders for user: {}",
                request.getItemIds().size(), request.getUserId());

        // 额外验证
        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Item IDs list cannot be empty"));
        }

        // 执行批量认领
        BatchOperationResponse response = batchOperationService.batchClaimTenders(
                request.getItemIds(),
                request.getUserId()
        );

        String message = buildSuccessMessage("claimed", "tender", response.getSuccessCount());

        return ResponseEntity.ok(
                ApiResponse.success(message, response)
        );
    }

    /**
     * 批量分配任务
     * POST /api/batch/tasks/assign
     *
     * @param request 批量分配请求
     * @return 操作结果
     */
    @PostMapping("/tasks/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> batchAssignTasks(
            @Valid @RequestBody BatchAssignRequest request) {

        log.info("POST /api/batch/tasks/assign - Assigning {} tasks to user: {}",
                request.getTaskIds().size(), request.getAssigneeId());

        // 额外验证
        if (request.getTaskIds() == null || request.getTaskIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Task IDs list cannot be empty"));
        }
        if (request.getAssigneeId() == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Assignee ID cannot be null"));
        }

        // 清洗备注字段
        sanitizeRequestRemark(request);

        // 执行批量分配
        BatchOperationResponse response = batchOperationService.batchAssignTasks(request);

        String message = buildSuccessMessage("assigned", "task", response.getSuccessCount());

        return ResponseEntity.ok(
                ApiResponse.success(message, response)
        );
    }

    /**
     * 批量删除项目
     * DELETE /api/batch/projects
     *
     * @param request 批量删除请求
     * @return 操作结果
     */
    @DeleteMapping("/projects")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> batchDeleteProjects(
            @Valid @RequestBody BatchDeleteRequest request) {

        log.info("DELETE /api/batch/projects - Deleting {} projects by user: {}",
                request.getItemIds().size(), request.getUserId());

        // 额外验证
        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Item IDs list cannot be empty"));
        }
        if (request.getUserId() == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "User ID cannot be null"));
        }

        // 清洗原因字段
        sanitizeRequestReason(request);

        // 执行批量删除
        BatchOperationResponse response = batchOperationService.batchDeleteProjects(request);

        String message = buildSuccessMessage("deleted", "project", response.getSuccessCount());

        return ResponseEntity.ok(
                ApiResponse.success(message, response)
        );
    }

    /**
     * 通用批量删除
     * DELETE /api/batch/{type}
     *
     * 支持的类型: tender, task, project
     *
     * @param type 项目类型
     * @param request 批量删除请求
     * @return 操作结果
     */
    @DeleteMapping("/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> batchDeleteItems(
            @PathVariable String type,
            @Valid @RequestBody BatchDeleteRequest request) {

        log.info("DELETE /api/batch/{} - Deleting {} items", type, request.getItemIds().size());

        // 额外验证
        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Item IDs list cannot be empty"));
        }

        // 清洗原因字段
        sanitizeRequestReason(request);

        try {
            // 执行通用批量删除
            BatchOperationResponse response = batchOperationService.batchDeleteItems(type, request.getItemIds());

            String message = buildSuccessMessage("deleted", type, response.getSuccessCount());

            return ResponseEntity.ok(
                    ApiResponse.success(message, response)
            );

        } catch (IllegalArgumentException e) {
            log.warn("Invalid item type for batch deletion: {}", type);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid item type: " + type + ". Supported types: tender, task, project")
            );
        } catch (Exception e) {
            log.error("Error during batch deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to delete items: " + e.getMessage())
            );
        }
    }

    /**
     * 批量操作状态查询
     * GET /api/batch/status/{operationId}
     *
     * 注意: 这是一个预留接口，用于未来实现异步批量操作的状态查询
     *
     * @param operationId 操作ID
     * @return 操作状态
     */
    @GetMapping("/status/{operationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> getBatchOperationStatus(
            @PathVariable String operationId) {

        log.info("GET /api/batch/status/{} - Querying batch operation status", operationId);

        // TODO: 实现异步批量操作时，需要添加操作状态存储和查询逻辑
        // 当前返回一个示例响应
        BatchOperationResponse response = BatchOperationResponse.builder()
                .success(true)
                .successCount(0)
                .failureCount(0)
                .totalCount(0)
                .operationType("STATUS_QUERY")
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("Batch operation status query (placeholder)", response)
        );
    }

    /**
     * 获取批量操作历史记录
     * GET /api/batch/history
     *
     * 注意: 这是一个预留接口，用于未来实现批量操作历史记录查询
     *
     * @param limit 返回记录数量限制
     * @return 操作历史记录列表
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<String>>> getBatchOperationHistory(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/batch/history - Querying batch operation history, limit={}", limit);

        // TODO: 实现批量操作历史记录存储和查询
        // 当前返回空列表
        return ResponseEntity.ok(
                ApiResponse.success("Batch operation history (placeholder)", List.of())
        );
    }

    /**
     * 构建成功消息
     */
    private String buildSuccessMessage(String action, String itemType, int count) {
        if (count == 0) {
            return String.format("No %ss were %s. Check error details for more information.",
                    itemType.toLowerCase(), action);
        } else if (count == 1) {
            return String.format("Successfully %s 1 %s", action, itemType.toLowerCase());
        } else {
            return String.format("Successfully %s %d %ss", action, count, itemType.toLowerCase());
        }
    }

    /**
     * 清洗请求中的备注字段
     */
    private void sanitizeRequestRemark(BatchAssignRequest request) {
        if (request.getRemark() != null && !request.getRemark().isEmpty()) {
            String sanitized = InputSanitizer.sanitizeString(request.getRemark(), MAX_REMARK_LENGTH);
            request.setRemark(sanitized);
        }
    }

    /**
     * 清洗请求中的原因字段
     */
    private void sanitizeRequestReason(BatchDeleteRequest request) {
        if (request.getReason() != null && !request.getReason().isEmpty()) {
            String sanitized = InputSanitizer.sanitizeString(request.getReason(), MAX_REMARK_LENGTH);
            request.setReason(sanitized);
        }
    }

    /**
     * 异常处理器 - 处理批量操作中的异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<BatchOperationResponse>> handleIllegalArgumentException(
            IllegalArgumentException e) {

        log.error("Illegal argument in batch operation: {}", e.getMessage());

        BatchOperationResponse errorResponse = BatchOperationResponse.builder()
                .success(false)
                .successCount(0)
                .failureCount(0)
                .totalCount(0)
                .operationType("ERROR")
                .build();
        errorResponse.addError(null, e.getMessage(), "INVALID_ARGUMENT");

        return ResponseEntity.badRequest().body(
                ApiResponse.error(400, e.getMessage())
        );
    }

    /**
     * 异常处理器 - 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<BatchOperationResponse>> handleGenericException(
            Exception e) {

        log.error("Error in batch operation: {}", e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "An error occurred during batch operation: " + e.getMessage())
        );
    }
}
