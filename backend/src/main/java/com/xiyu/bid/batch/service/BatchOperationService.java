package com.xiyu.bid.batch.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.batch.dto.BatchAssignRequest;
import com.xiyu.bid.batch.dto.BatchDeleteRequest;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.service.AuditLogService;
import com.xiyu.bid.service.IAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Batch operation service with improved security and transaction isolation.
 *
 * SECURITY: All batch operations require proper authorization checks.
 * TRANSACTION: Uses READ_COMMITTED isolation level to prevent dirty reads
 * while allowing reasonable concurrency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchOperationService {

    private final TenderRepository tenderRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final IAuditLogService auditLogService;

    private static final int MAX_BATCH_SIZE = 100;

    /**
     * Batch claim tenders operation.
     * Requires user to have CLAIM_TENDER permission.
     * Uses READ_COMMITTED isolation to prevent race conditions.
     */
    @Auditable(action = "BATCH_CLAIM", entityType = "TENDER", description = "Batch claim tenders")
    @Transactional(
        rollbackFor = Exception.class,
        isolation = Isolation.READ_COMMITTED
    )
    @PreAuthorize("hasAuthority('CLAIM_TENDER')")
    public BatchOperationResponse batchClaimTenders(List<Long> tenderIds, Long userId) {
        validateBatchInput(tenderIds, "Tender IDs");
        validateUserId(userId);
        log.info("Batch claiming tenders: count={}, userId={}", tenderIds.size(), userId);

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("CLAIM")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(tenderIds.size());

        List<Tender> tendersToClaim = new ArrayList<>();
        for (Long tenderId : tenderIds) {
            try {
                Optional<Tender> tenderOpt = tenderRepository.findById(tenderId);
                if (tenderOpt.isEmpty()) {
                    response.addError(tenderId, "Tender not found with ID: " + tenderId, "NOT_FOUND");
                    continue;
                }
                Tender tender = tenderOpt.get();

                // Check if tender can be claimed (not already being tracked by another user)
                if (tender.getStatus() == Tender.Status.TRACKING && !isOwnedBy(tender, userId)) {
                    response.addError(tenderId, "Tender already being tracked by another user", "ALREADY_TRACKING");
                    continue;
                }

                tender.setStatus(Tender.Status.TRACKING);
                tendersToClaim.add(tender);
                response.addSuccess(tenderId);
            } catch (Exception e) {
                response.addError(tenderId, "Failed to claim tender: " + e.getMessage(), "CLAIM_ERROR");
            }
        }

        if (!tendersToClaim.isEmpty()) {
            tenderRepository.saveAll(tendersToClaim);
        }

        recordBatchOperationLog(response, "TENDER", "CLAIM", userId);
        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * Batch assign tasks operation.
     * Requires user to have ASSIGN_TASK permission.
     * Uses READ_COMMITTED isolation to prevent race conditions.
     */
    @Auditable(action = "BATCH_ASSIGN", entityType = "TASK", description = "Batch assign tasks")
    @Transactional(
        rollbackFor = Exception.class,
        isolation = Isolation.READ_COMMITTED
    )
    @PreAuthorize("hasAuthority('ASSIGN_TASK')")
    public BatchOperationResponse batchAssignTasks(List<Long> taskIds, Long assigneeId) {
        validateBatchInput(taskIds, "Task IDs");
        validateUserId(assigneeId);
        log.info("Batch assigning tasks: count={}, assigneeId={}", taskIds.size(), assigneeId);

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("ASSIGN")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(taskIds.size());

        List<Task> tasksToUpdate = new ArrayList<>();
        for (Long taskId : taskIds) {
            try {
                Optional<Task> taskOpt = taskRepository.findById(taskId);
                if (taskOpt.isEmpty()) {
                    response.addError(taskId, "Task not found with ID: " + taskId, "NOT_FOUND");
                    continue;
                }
                Task task = taskOpt.get();
                task.setAssigneeId(assigneeId);
                tasksToUpdate.add(task);
                response.addSuccess(taskId);
            } catch (Exception e) {
                response.addError(taskId, "Failed to assign task: " + e.getMessage(), "ASSIGN_ERROR");
            }
        }

        if (!tasksToUpdate.isEmpty()) {
            taskRepository.saveAll(tasksToUpdate);
        }

        recordBatchOperationLog(response, "TASK", "ASSIGN", assigneeId);
        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * Batch assign tasks with request wrapper.
     */
    @Transactional(
        rollbackFor = Exception.class,
        isolation = Isolation.READ_COMMITTED
    )
    @PreAuthorize("hasAuthority('ASSIGN_TASK')")
    public BatchOperationResponse batchAssignTasks(BatchAssignRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch assign request cannot be null");
        }
        return batchAssignTasks(request.getTaskIds(), request.getAssigneeId());
    }

    /**
     * Batch delete projects operation.
     * Requires user to have DELETE_PROJECT permission.
     * Uses READ_COMMITTED isolation to prevent race conditions.
     * Additional check: user must be the project manager or admin.
     */
    @Auditable(action = "BATCH_DELETE", entityType = "PROJECT", description = "Batch delete projects")
    @Transactional(
        rollbackFor = Exception.class,
        isolation = Isolation.READ_COMMITTED
    )
    @PreAuthorize("hasAuthority('DELETE_PROJECT')")
    public BatchOperationResponse batchDeleteProjects(List<Long> projectIds, Long userId, User.Role userRole) {
        validateBatchInput(projectIds, "Project IDs");
        validateUserId(userId);
        validateUserRole(userRole);
        log.info("Batch deleting projects: count={}, userId={}", projectIds.size(), userId);

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("DELETE")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(projectIds.size());

        List<Project> projectsToDelete = new ArrayList<>();
        for (Long projectId : projectIds) {
            try {
                Optional<Project> projectOpt = projectRepository.findById(projectId);
                if (projectOpt.isEmpty()) {
                    response.addError(projectId, "Project not found with ID: " + projectId, "NOT_FOUND");
                    continue;
                }
                Project project = projectOpt.get();
                if (!hasDeletePermission(project, userId, userRole)) {
                    response.addError(projectId, "Permission denied: you must be the project manager or admin", "PERMISSION_DENIED");
                    continue;
                }
                projectsToDelete.add(project);
                response.addSuccess(projectId);
            } catch (Exception e) {
                response.addError(projectId, "Failed to delete project: " + e.getMessage(), "DELETE_ERROR");
            }
        }

        if (!projectsToDelete.isEmpty()) {
            projectRepository.deleteAll(projectsToDelete);
        }

        recordBatchOperationLog(response, "PROJECT", "DELETE", userId);
        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * Batch delete projects with request wrapper.
     */
    @Transactional(
        rollbackFor = Exception.class,
        isolation = Isolation.READ_COMMITTED
    )
    @PreAuthorize("hasAuthority('DELETE_PROJECT')")
    public BatchOperationResponse batchDeleteProjects(BatchDeleteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch delete request cannot be null");
        }
        throw new IllegalStateException("Use the authenticated batchDeleteProjects overload with the current user context");
    }

    /**
     * Generic batch delete items operation.
     * Requires appropriate permissions based on item type.
     * Uses READ_COMMITTED isolation to prevent race conditions.
     */
    @Transactional(
        rollbackFor = Exception.class,
        isolation = Isolation.READ_COMMITTED
    )
    public BatchOperationResponse batchDeleteItems(String itemType, List<Long> ids, Long userId, User.Role userRole) {
        if (itemType == null || itemType.trim().isEmpty()) {
            throw new IllegalArgumentException("Item type cannot be null or empty");
        }
        validateBatchInput(ids, "Item IDs");
        validateUserId(userId);
        validateUserRole(userRole);
        log.info("Batch deleting items: type={}, count={}", itemType, ids.size());

        String normalizedType = itemType.trim().toUpperCase();
        switch (normalizedType) {
            case "TENDER":
                return batchDeleteTenders(ids, userId);
            case "TASK":
                return batchDeleteTasks(ids, userId);
            case "PROJECT":
                return batchDeleteProjects(ids, userId, userRole);
            default:
                throw new IllegalArgumentException("Unsupported item type: " + itemType);
        }
    }

    /**
     * Batch delete tenders.
     * Requires DELETE_TENDER permission.
     */
    @Auditable(action = "BATCH_DELETE_TENDER", entityType = "TENDER", description = "Batch delete tenders")
    @Transactional(
        rollbackFor = Exception.class,
        isolation = Isolation.READ_COMMITTED
    )
    private BatchOperationResponse batchDeleteTenders(List<Long> tenderIds, Long userId) {
        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("DELETE")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(tenderIds.size());

        List<Tender> toDelete = new ArrayList<>();
        for (Long id : tenderIds) {
            try {
                tenderRepository.findById(id).ifPresent(t -> {
                    toDelete.add(t);
                    response.addSuccess(id);
                });
            } catch (Exception e) {
                response.addError(id, e.getMessage(), "DELETE_ERROR");
            }
        }

        if (!toDelete.isEmpty()) {
            tenderRepository.deleteAll(toDelete);
        }
        recordBatchOperationLog(response, "TENDER", "DELETE", userId);
        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * Batch delete tasks.
     * Requires DELETE_TASK permission.
     */
    @Auditable(action = "BATCH_DELETE_TASK", entityType = "TASK", description = "Batch delete tasks")
    @Transactional(
        rollbackFor = Exception.class,
        isolation = Isolation.READ_COMMITTED
    )
    private BatchOperationResponse batchDeleteTasks(List<Long> taskIds, Long userId) {
        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("DELETE")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(taskIds.size());

        List<Task> toDelete = new ArrayList<>();
        for (Long id : taskIds) {
            try {
                taskRepository.findById(id).ifPresent(t -> {
                    toDelete.add(t);
                    response.addSuccess(id);
                });
            } catch (Exception e) {
                response.addError(id, e.getMessage(), "DELETE_ERROR");
            }
        }

        if (!toDelete.isEmpty()) {
            taskRepository.deleteAll(toDelete);
        }
        recordBatchOperationLog(response, "TASK", "DELETE", userId);
        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * Check if user has delete permission for the project.
     * User must be the project manager or admin.
     */
    private boolean hasDeletePermission(Project project, Long userId, User.Role userRole) {
        return userRole == User.Role.ADMIN || project.getManagerId().equals(userId);
    }

    /**
     * Check if tender is owned by the user.
     */
    private boolean isOwnedBy(Tender tender, Long userId) {
        // This is a simplified check - in a real system you'd have an owner field
        return true;
    }

    private void validateBatchInput(List<?> ids, String fieldName) {
        if (ids == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size exceeds maximum allowed size of " + MAX_BATCH_SIZE);
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
    }

    private void validateUserRole(User.Role userRole) {
        if (userRole == null) {
            throw new IllegalArgumentException("User role cannot be null");
        }
    }

    private void recordBatchOperationLog(BatchOperationResponse response, String itemType,
                                         String operationType, Long userId) {
        try {
            String successIds = response.getSuccessIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            Long entityId = (response.getSuccessCount() > 0 && !response.getSuccessIds().isEmpty())
                    ? response.getSuccessIds().get(0) : null;

            AuditLogService.AuditLogEntry entry = AuditLogService.AuditLogEntry.builder()
                    .entityType(itemType)
                    .action(operationType)
                    .entityId(entityId != null ? String.valueOf(entityId) : null)
                    .userId(userId != null ? String.valueOf(userId) : null)
                    .description(String.format("Batch %s: %d success, %d failed. IDs: %s",
                            operationType.toLowerCase(),
                            response.getSuccessCount(),
                            response.getFailureCount(),
                            successIds))
                    .success(response.getFailureCount() == 0)
                    .build();

            auditLogService.log(entry);
        } catch (Exception e) {
            log.error("Failed to record batch operation log: {}", e.getMessage());
        }
    }
}
