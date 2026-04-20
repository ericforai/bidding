// Input: task repository, user repository, assignment policy, and log service
// Output: task batch command orchestration
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.batch.service;

import com.xiyu.bid.batch.core.BatchAssignmentPolicy;
import com.xiyu.bid.batch.core.BatchAssignmentSnapshot;
import com.xiyu.bid.batch.core.BatchValidationPolicy;
import com.xiyu.bid.batch.dto.BatchAssignRequest;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.task.dto.TaskAssignmentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务批处理命令
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchTaskCommandService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final BatchValidationPolicy validationPolicy;
    private final BatchAssignmentPolicy assignmentPolicy;
    private final BatchOperationLogService logService;

    public BatchOperationResponse batchAssignTasks(List<Long> taskIds, Long assigneeId) {
        validationPolicy.validateBatchInput(taskIds, "Task IDs");
        validationPolicy.validateUserId(assigneeId);
        log.info("Batch assigning tasks: count={}, assigneeId={}", taskIds.size(), assigneeId);

        BatchOperationResponse response = newResponse("ASSIGN", taskIds.size());
        List<Task> tasksToUpdate = new ArrayList<>();
        for (Long taskId : taskIds) {
            try {
                var taskOpt = taskRepository.findById(taskId);
                if (taskOpt.isEmpty()) {
                    response.addError(taskId, "Task not found with ID: " + taskId, "NOT_FOUND");
                    continue;
                }
                Task task = taskOpt.get();
                task.setAssigneeId(assigneeId);
                tasksToUpdate.add(task);
                response.addSuccess(taskId);
            } catch (RuntimeException exception) {
                response.addError(taskId, "Failed to assign task: " + exception.getMessage(), "ASSIGN_ERROR");
            }
        }
        if (!tasksToUpdate.isEmpty()) {
            taskRepository.saveAll(tasksToUpdate);
        }
        complete(response, "TASK", "ASSIGN", assigneeId);
        return response;
    }

    public BatchOperationResponse batchAssignTasks(BatchAssignRequest request, User currentUser) {
        validationPolicy.requireNonNull(request, "Batch assign request cannot be null");
        validationPolicy.validateBatchInput(request.getTaskIds(), "Task IDs");

        BatchAssignmentSnapshot assignment = resolveAssignmentSnapshot(request, currentUser);
        BatchOperationResponse response = newResponse("ASSIGN", request.getTaskIds().size());
        List<Task> tasksToUpdate = new ArrayList<>();
        for (Long taskId : request.getTaskIds()) {
            try {
                var taskOpt = taskRepository.findById(taskId);
                if (taskOpt.isEmpty()) {
                    response.addError(taskId, "Task not found with ID: " + taskId, "NOT_FOUND");
                    continue;
                }
                Task task = taskOpt.get();
                task.setAssigneeId(assignment.assigneeId());
                task.setAssigneeDeptCode(assignment.assigneeDeptCode());
                task.setAssigneeDeptName(assignment.assigneeDeptName());
                task.setAssigneeRoleCode(assignment.assigneeRoleCode());
                task.setAssigneeRoleName(assignment.assigneeRoleName());
                tasksToUpdate.add(task);
                response.addSuccess(taskId);
            } catch (RuntimeException exception) {
                response.addError(taskId, "Failed to assign task: " + exception.getMessage(), "ASSIGN_ERROR");
            }
        }
        if (!tasksToUpdate.isEmpty()) {
            taskRepository.saveAll(tasksToUpdate);
        }
        complete(response, "TASK", "ASSIGN", currentUser == null ? assignment.assigneeId() : currentUser.getId());
        return response;
    }

    public BatchOperationResponse batchDeleteTasks(List<Long> taskIds, Long userId) {
        BatchOperationResponse response = newResponse("DELETE", taskIds.size());
        List<Task> toDelete = new ArrayList<>();
        for (Long taskId : taskIds) {
            try {
                taskRepository.findById(taskId).ifPresent(task -> {
                    toDelete.add(task);
                    response.addSuccess(taskId);
                });
            } catch (RuntimeException exception) {
                response.addError(taskId, exception.getMessage(), "DELETE_ERROR");
            }
        }
        if (!toDelete.isEmpty()) {
            taskRepository.deleteAll(toDelete);
        }
        complete(response, "TASK", "DELETE", userId);
        return response;
    }

    private BatchAssignmentSnapshot resolveAssignmentSnapshot(BatchAssignRequest request, User currentUser) {
        TaskAssignmentRequest assignmentRequest = TaskAssignmentRequest.builder()
                .assigneeId(request.getAssigneeId())
                .assigneeDeptCode(request.getAssigneeDeptCode())
                .assigneeDeptName(request.getAssigneeDeptName())
                .assigneeRoleCode(request.getAssigneeRoleCode())
                .assigneeRoleName(request.getAssigneeRoleName())
                .allowCrossDeptCollaboration(Boolean.TRUE.equals(request.getAllowCrossDeptCollaboration()))
                .remark(request.getRemark())
                .build();

        if (assignmentRequest.getAssigneeId() != null) {
            User assignee = userRepository.findById(assignmentRequest.getAssigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found"));
            return assignmentPolicy.resolveUserAssignment(
                    assignee,
                    currentUser,
                    Boolean.TRUE.equals(assignmentRequest.getAllowCrossDeptCollaboration())
            );
        }
        return assignmentPolicy.resolveDepartmentAssignment(assignmentRequest, currentUser);
    }

    private BatchOperationResponse newResponse(String operationType, int totalCount) {
        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType(operationType)
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(totalCount);
        return response;
    }

    private void complete(BatchOperationResponse response, String itemType, String operationType, Long userId) {
        logService.record(response, itemType, operationType, userId);
        response.setSuccess(response.getFailureCount() == 0);
    }
}
