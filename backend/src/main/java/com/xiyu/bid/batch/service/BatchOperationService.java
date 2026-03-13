package com.xiyu.bid.batch.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.batch.dto.BatchAssignRequest;
import com.xiyu.bid.batch.dto.BatchClaimRequest;
import com.xiyu.bid.batch.dto.BatchDeleteRequest;
import com.xiyu.bid.batch.dto.BatchOperationLog;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.service.AuditLogService;
import com.xiyu.bid.service.IAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 批量操作服务
 * 提供批量认领、分配、删除等功能
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
    private static final int MAX_REASON_LENGTH = 500;

    /**
     * 批量认领标讯
     * @param tenderIds 标讯ID列表
     * @param userId 认领人用户ID
     * @return 批量操作结果
     */
    @Auditable(
        action = "BATCH_CLAIM",
        entityType = "TENDER",
        description = "Batch claim tenders"
    )
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchClaimTenders(List<Long> tenderIds, Long userId) {
        log.info("Batch claiming tenders: count={}, userId={}", tenderIds.size(), userId);

        // 验证输入
        validateBatchInput(tenderIds, "Tender IDs");
        validateUserId(userId);

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("CLAIM")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(tenderIds.size());

        List<Tender> tendersToClaim = new ArrayList<>();

        // 处理每个标讯
        for (Long tenderId : tenderIds) {
            try {
                Optional<Tender> tenderOpt = tenderRepository.findById(tenderId);
                if (tenderOpt.isEmpty()) {
                    response.addError(tenderId, "Tender not found with ID: " + tenderId, "NOT_FOUND");
                    log.warn("Tender not found for claiming: {}", tenderId);
                    continue;
                }

                Tender tender = tenderOpt.get();
                tender.setStatus(Tender.Status.TRACKING);
                tendersToClaim.add(tender);
                response.addSuccess(tenderId);

                log.debug("Marked tender for claiming: {}", tenderId);

            } catch (Exception e) {
                response.addError(tenderId, "Failed to claim tender: " + e.getMessage(), "CLAIM_ERROR");
                log.error("Error claiming tender {}: {}", tenderId, e.getMessage());
            }
        }

        // 批量保存成功的更新
        if (!tendersToClaim.isEmpty()) {
            tenderRepository.saveAll(tendersToClaim);
            log.info("Successfully claimed {}/{} tenders", tendersToClaim.size(), tenderIds.size());
        }

        // 记录操作日志
        recordBatchOperationLog(response, "TENDER", "CLAIM", userId);

        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * 批量分配任务
     * @param taskIds 任务ID列表
     * @param assigneeId 目标分配人用户ID
     * @return 批量操作结果
     */
    @Auditable(
        action = "BATCH_ASSIGN",
        entityType = "TASK",
        description = "Batch assign tasks"
    )
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchAssignTasks(List<Long> taskIds, Long assigneeId) {
        log.info("Batch assigning tasks: count={}, assigneeId={}", taskIds.size(), assigneeId);

        // 验证输入
        validateBatchInput(taskIds, "Task IDs");
        validateUserId(assigneeId);

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("ASSIGN")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(taskIds.size());

        List<Task> tasksToUpdate = new ArrayList<>();

        // 处理每个任务
        for (Long taskId : taskIds) {
            try {
                Optional<Task> taskOpt = taskRepository.findById(taskId);
                if (taskOpt.isEmpty()) {
                    response.addError(taskId, "Task not found with ID: " + taskId, "NOT_FOUND");
                    log.warn("Task not found for assignment: {}", taskId);
                    continue;
                }

                Task task = taskOpt.get();
                task.setAssigneeId(assigneeId);
                tasksToUpdate.add(task);
                response.addSuccess(taskId);

                log.debug("Assigned task {} to user {}", taskId, assigneeId);

            } catch (Exception e) {
                response.addError(taskId, "Failed to assign task: " + e.getMessage(), "ASSIGN_ERROR");
                log.error("Error assigning task {}: {}", taskId, e.getMessage());
            }
        }

        // 批量保存成功的更新
        if (!tasksToUpdate.isEmpty()) {
            taskRepository.saveAll(tasksToUpdate);
            log.info("Successfully assigned {}/{} tasks", tasksToUpdate.size(), taskIds.size());
        }

        // 记录操作日志
        recordBatchOperationLog(response, "TASK", "ASSIGN", assigneeId);

        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * 批量分配任务（带请求对象）
     * @param request 批量分配请求
     * @return 批量操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchAssignTasks(BatchAssignRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch assign request cannot be null");
        }

        BatchOperationResponse response = batchAssignTasks(request.getTaskIds(), request.getAssigneeId());

        // 记录备注信息
        if (request.getRemark() != null && !request.getRemark().isEmpty()) {
            log.info("Batch assignment remark: {}", sanitizeRemark(request.getRemark()));
        }

        return response;
    }

    /**
     * 批量删除项目
     * @param projectIds 项目ID列表
     * @param userId 操作用户ID
     * @return 批量操作结果
     */
    @Auditable(
        action = "BATCH_DELETE",
        entityType = "PROJECT",
        description = "Batch delete projects"
    )
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchDeleteProjects(List<Long> projectIds, Long userId) {
        log.info("Batch deleting projects: count={}, userId={}", projectIds.size(), userId);

        // 验证输入
        validateBatchInput(projectIds, "Project IDs");
        validateUserId(userId);

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("DELETE")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(projectIds.size());

        List<Project> projectsToDelete = new ArrayList<>();

        // 处理每个项目
        for (Long projectId : projectIds) {
            try {
                Optional<Project> projectOpt = projectRepository.findById(projectId);
                if (projectOpt.isEmpty()) {
                    response.addError(projectId, "Project not found with ID: " + projectId, "NOT_FOUND");
                    log.warn("Project not found for deletion: {}", projectId);
                    continue;
                }

                Project project = projectOpt.get();

                // 权限检查：只有项目经理或管理员可以删除项目
                if (!hasDeletePermission(project, userId)) {
                    response.addError(projectId, "Permission denied: User is not the project manager",
                            "PERMISSION_DENIED");
                    log.warn("Permission denied for deleting project {} by user {}", projectId, userId);
                    continue;
                }

                projectsToDelete.add(project);
                response.addSuccess(projectId);

                log.debug("Marked project for deletion: {}", projectId);

            } catch (Exception e) {
                response.addError(projectId, "Failed to delete project: " + e.getMessage(), "DELETE_ERROR");
                log.error("Error deleting project {}: {}", projectId, e.getMessage());
            }
        }

        // 批量删除成功的项目
        if (!projectsToDelete.isEmpty()) {
            projectRepository.deleteAll(projectsToDelete);
            log.info("Successfully deleted {}/{} projects", projectsToDelete.size(), projectIds.size());
        }

        // 记录操作日志
        recordBatchOperationLog(response, "PROJECT", "DELETE", userId);

        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * 批量删除项目（带请求对象）
     * @param request 批量删除请求
     * @return 批量操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchDeleteProjects(BatchDeleteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch delete request cannot be null");
        }

        BatchOperationResponse response = batchDeleteProjects(request.getItemIds(), request.getUserId());

        // 记录删除原因
        if (request.getReason() != null && !request.getReason().isEmpty()) {
            log.info("Batch deletion reason: {}", sanitizeRemark(request.getReason()));
        }

        return response;
    }

    /**
     * 通用批量删除方法
     * @param itemType 项目类型 (tender, task, project)
     * @param ids 项目ID列表
     * @return 批量操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchDeleteItems(String itemType, List<Long> ids) {
        log.info("Batch deleting items: type={}, count={}", itemType, ids.size());

        // 验证输入
        if (itemType == null || itemType.trim().isEmpty()) {
            throw new IllegalArgumentException("Item type cannot be null or empty");
        }
        validateBatchInput(ids, "Item IDs");

        String normalizedType = itemType.trim().toUpperCase();

        switch (normalizedType) {
            case "TENDER":
                return batchDeleteTenders(ids);
            case "TASK":
                return batchDeleteTasks(ids);
            case "PROJECT":
                // 通用删除不进行权限检查，系统级操作
                return batchDeleteItemsDirect(ids);
            default:
                throw new IllegalArgumentException("Unsupported item type: " + itemType +
                        ". Supported types: tender, task, project");
        }
    }

    /**
     * 批量删除标讯
     */
    private BatchOperationResponse batchDeleteTenders(List<Long> tenderIds) {
        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("DELETE")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(tenderIds.size());

        List<Tender> tendersToDelete = new ArrayList<>();

        for (Long tenderId : tenderIds) {
            try {
                Optional<Tender> tenderOpt = tenderRepository.findById(tenderId);
                if (tenderOpt.isEmpty()) {
                    response.addError(tenderId, "Tender not found", "NOT_FOUND");
                    continue;
                }
                tendersToDelete.add(tenderOpt.get());
                response.addSuccess(tenderId);
            } catch (Exception e) {
                response.addError(tenderId, e.getMessage(), "DELETE_ERROR");
            }
        }

        if (!tendersToDelete.isEmpty()) {
            tenderRepository.deleteAll(tendersToDelete);
        }

        recordBatchOperationLog(response, "TENDER", "DELETE", null);
        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * 批量删除任务
     */
    private BatchOperationResponse batchDeleteTasks(List<Long> taskIds) {
        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("DELETE")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(taskIds.size());

        List<Task> tasksToDelete = new ArrayList<>();

        for (Long taskId : taskIds) {
            try {
                Optional<Task> taskOpt = taskRepository.findById(taskId);
                if (taskOpt.isEmpty()) {
                    response.addError(taskId, "Task not found", "NOT_FOUND");
                    continue;
                }
                tasksToDelete.add(taskOpt.get());
                response.addSuccess(taskId);
            } catch (Exception e) {
                response.addError(taskId, e.getMessage(), "DELETE_ERROR");
            }
        }

        if (!tasksToDelete.isEmpty()) {
            taskRepository.deleteAll(tasksToDelete);
        }

        recordBatchOperationLog(response, "TASK", "DELETE", null);
        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * 批量删除项目（无权限检查的内部方法）
     */
    private BatchOperationResponse batchDeleteItemsDirect(List<Long> projectIds) {
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
                    response.addError(projectId, "Project not found", "NOT_FOUND");
                    continue;
                }
                projectsToDelete.add(projectOpt.get());
                response.addSuccess(projectId);
            } catch (Exception e) {
                response.addError(projectId, e.getMessage(), "DELETE_ERROR");
            }
        }

        if (!projectsToDelete.isEmpty()) {
            projectRepository.deleteAll(projectsToDelete);
        }

        recordBatchOperationLog(response, "PROJECT", "DELETE", null);
        response.setSuccess(response.getFailureCount() == 0);
        return response;
    }

    /**
     * 检查用户是否有删除项目的权限
     */
    private boolean hasDeletePermission(Project project, Long userId) {
        // 项目经理可以删除
        if (project.getManagerId().equals(userId)) {
            return true;
        }
        // TODO: 添加管理员角色检查
        return false;
    }

    /**
     * 验证批量操作输入
     */
    private void validateBatchInput(List<?> ids, String fieldName) {
        if (ids == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "Batch size exceeds maximum allowed size of " + MAX_BATCH_SIZE);
        }
    }

    /**
     * 验证用户ID
     */
    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
    }

    /**
     * 清理备注信息（防止日志注入）
     */
    private String sanitizeRemark(String remark) {
        if (remark == null) {
            return "";
        }
        String sanitized = remark.replace("\n", " ").replace("\r", " ");
        return sanitized.length() > MAX_REASON_LENGTH
                ? sanitized.substring(0, MAX_REASON_LENGTH) + "..."
                : sanitized;
    }

    /**
     * 记录批量操作日志
     */
    private void recordBatchOperationLog(BatchOperationResponse response, String itemType,
                                         String operationType, Long userId) {
        try {
            String successIds = response.getSuccessIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            Long entityId = null;
            if (response.getSuccessCount() > 0 && !response.getSuccessIds().isEmpty()) {
                entityId = response.getSuccessIds().get(0);
            }

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
