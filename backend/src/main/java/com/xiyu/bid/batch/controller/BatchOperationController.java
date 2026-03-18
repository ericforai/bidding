package com.xiyu.bid.batch.controller;

import com.xiyu.bid.batch.dto.BatchAssignRequest;
import com.xiyu.bid.batch.dto.BatchClaimRequest;
import com.xiyu.bid.batch.dto.BatchDeleteRequest;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.batch.service.BatchOperationService;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchOperationController {

    private final BatchOperationService batchOperationService;
    private final UserRepository userRepository;
    private static final int MAX_REMARK_LENGTH = 500;

    @PostMapping("/tenders/claim")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> batchClaimTenders(
            @Valid @RequestBody BatchClaimRequest request) {

        log.info("POST /api/batch/tenders/claim - Claiming {} tenders for user: {}",
                request.getItemIds().size(), request.getUserId());

        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Item IDs list cannot be empty"));
        }

        BatchOperationResponse response = batchOperationService.batchClaimTenders(
                request.getItemIds(), request.getUserId());

        String message = buildSuccessMessage("claimed", "tender", response.getSuccessCount());
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/tasks/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> batchAssignTasks(
            @Valid @RequestBody BatchAssignRequest request) {

        log.info("POST /api/batch/tasks/assign - Assigning {} tasks to user: {}",
                request.getTaskIds().size(), request.getAssigneeId());

        if (request.getTaskIds() == null || request.getTaskIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Task IDs list cannot be empty"));
        }
        if (request.getAssigneeId() == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Assignee ID cannot be null"));
        }

        sanitizeRequestRemark(request);
        BatchOperationResponse response = batchOperationService.batchAssignTasks(request);

        String message = buildSuccessMessage("assigned", "task", response.getSuccessCount());
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @DeleteMapping("/projects")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> batchDeleteProjects(
            @Valid @RequestBody BatchDeleteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = getCurrentUser(userDetails);
        log.info("DELETE /api/batch/projects - Deleting {} projects by user: {}",
                request.getItemIds().size(), currentUser.getId());

        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Item IDs list cannot be empty"));
        }
        sanitizeRequestReason(request);
        BatchOperationResponse response = batchOperationService.batchDeleteProjects(
                request.getItemIds(),
                currentUser.getId(),
                currentUser.getRole());

        String message = buildSuccessMessage("deleted", "project", response.getSuccessCount());
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @DeleteMapping("/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> batchDeleteItems(
            @PathVariable String type,
            @Valid @RequestBody BatchDeleteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = getCurrentUser(userDetails);

        log.info("DELETE /api/batch/{} - Deleting {} items", type, request.getItemIds().size());

        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Item IDs list cannot be empty"));
        }

        sanitizeRequestReason(request);

        try {
            BatchOperationResponse response = batchOperationService.batchDeleteItems(
                    type,
                    request.getItemIds(),
                    currentUser.getId(),
                    currentUser.getRole());
            String message = buildSuccessMessage("deleted", type, response.getSuccessCount());
            return ResponseEntity.ok(ApiResponse.success(message, response));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid item type for batch deletion: {}", type);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid item type: " + type + ". Supported types: tender, task, project"));
        } catch (Exception e) {
            log.error("Error during batch deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to delete items: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{operationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<BatchOperationResponse>> getBatchOperationStatus(
            @PathVariable String operationId) {

        log.info("GET /api/batch/status/{} - Querying batch operation status", operationId);

        BatchOperationResponse response = BatchOperationResponse.builder()
                .success(true)
                .successCount(0)
                .failureCount(0)
                .totalCount(0)
                .operationType("STATUS_QUERY")
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("Batch operation status query (placeholder)", response));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<String>>> getBatchOperationHistory(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/batch/history - Querying batch operation history, limit={}", limit);
        return ResponseEntity.ok(ApiResponse.success("Batch operation history (placeholder)", List.of()));
    }

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

    private void sanitizeRequestRemark(BatchAssignRequest request) {
        if (request.getRemark() != null && !request.getRemark().isEmpty()) {
            request.setRemark(InputSanitizer.sanitizeString(request.getRemark(), MAX_REMARK_LENGTH));
        }
    }

    private void sanitizeRequestReason(BatchDeleteRequest request) {
        if (request.getReason() != null && !request.getReason().isEmpty()) {
            request.setReason(InputSanitizer.sanitizeString(request.getReason(), MAX_REMARK_LENGTH));
        }
    }

    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().trim().isEmpty()) {
            throw new org.springframework.security.authentication.AuthenticationServiceException(
                    "Authenticated user is required");
        }

        return userRepository.findByUsername(userDetails.getUsername().trim())
                .orElseThrow(() -> new org.springframework.security.authentication.AuthenticationServiceException(
                        "Authenticated user not found: " + userDetails.getUsername()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<BatchOperationResponse>> handleIllegalArgumentException(
            IllegalArgumentException e) {

        log.error("Illegal argument in batch operation: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<BatchOperationResponse>> handleGenericException(Exception e) {
        log.error("Error in batch operation: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "An error occurred during batch operation: " + e.getMessage()));
    }
}
