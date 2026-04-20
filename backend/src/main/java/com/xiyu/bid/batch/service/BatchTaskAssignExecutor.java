package com.xiyu.bid.batch.service;

import com.xiyu.bid.batch.dto.BatchAssignRequest;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.task.dto.TaskAssignmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
class BatchTaskAssignExecutor {

    private static final int MAX_BATCH_SIZE = 100;

    private final TaskRepository taskRepository;
    private final BatchTaskAssignmentResolver batchTaskAssignmentResolver;
    private final BatchOperationLogService batchOperationLogService;

    BatchOperationResponse assign(List<Long> taskIds, Long assigneeId) {
        BatchValidationPolicy.validateBatchInput(taskIds, "Task IDs", MAX_BATCH_SIZE);
        BatchValidationPolicy.validateUserId(assigneeId);

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("ASSIGN")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(taskIds.size());

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
        response.setSuccess(response.getFailureCount() == 0);
        batchOperationLogService.record(response, "TASK", "ASSIGN", assigneeId);
        return response;
    }

    BatchOperationResponse assign(BatchAssignRequest request, User currentUser) {
        if (request == null) {
            throw new IllegalArgumentException("Batch assign request cannot be null");
        }
        BatchValidationPolicy.validateBatchInput(request.getTaskIds(), "Task IDs", MAX_BATCH_SIZE);

        TaskAssignmentRequest assignmentRequest = TaskAssignmentRequest.builder()
                .assigneeId(request.getAssigneeId())
                .assigneeDeptCode(request.getAssigneeDeptCode())
                .assigneeDeptName(request.getAssigneeDeptName())
                .assigneeRoleCode(request.getAssigneeRoleCode())
                .assigneeRoleName(request.getAssigneeRoleName())
                .allowCrossDeptCollaboration(Boolean.TRUE.equals(request.getAllowCrossDeptCollaboration()))
                .remark(request.getRemark())
                .build();
        BatchTaskAssignmentResolver.AssignmentSnapshot assignment =
                batchTaskAssignmentResolver.resolveAssignment(assignmentRequest, currentUser);

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("ASSIGN")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(request.getTaskIds().size());

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
        response.setSuccess(response.getFailureCount() == 0);
        batchOperationLogService.record(response, "TASK", "ASSIGN", currentUser == null ? assignment.assigneeId() : currentUser.getId());
        return response;
    }
}
