// Input: Task DTOs, repositories, assignment support, and project access scope
// Output: task command/query results filtered by project data permissions
// Pos: Service/业务编排层
// 维护声明: 仅维护任务 CRUD 与项目数据权限编排；人员分配和团队工作量留在 TaskAssignmentSupport。
package com.xiyu.bid.task.service;

import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import com.xiyu.bid.task.core.TaskProjectVisibilityPolicy;
import com.xiyu.bid.task.dto.TaskAssignmentCandidateDTO;
import com.xiyu.bid.task.dto.TaskAssignmentRequest;
import com.xiyu.bid.task.dto.TaskDTO;
import com.xiyu.bid.task.dto.TeamTaskWorkloadDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectAccessScopeService projectAccessScopeService;
    private final TaskAssignmentSupport assignmentSupport;

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {
        log.info("Creating task: {}", taskDTO.getTitle());
        assertCanAccessProject(taskDTO.getProjectId());
        TaskAssignmentSupport.AssignmentSnapshot assignment = assignmentSupport.resolveAssignmentSnapshot(
                assignmentRequestFrom(taskDTO),
                null
        );
        Task savedTask = taskRepository.save(Task.builder()
                .projectId(taskDTO.getProjectId())
                .title(taskDTO.getTitle())
                .description(taskDTO.getDescription())
                .assigneeId(assignment.assigneeId())
                .assigneeDeptCode(assignment.assigneeDeptCode())
                .assigneeDeptName(assignment.assigneeDeptName())
                .assigneeRoleCode(assignment.assigneeRoleCode())
                .assigneeRoleName(assignment.assigneeRoleName())
                .status(taskDTO.getStatus() != null ? taskDTO.getStatus() : Task.Status.TODO)
                .priority(taskDTO.getPriority() != null ? taskDTO.getPriority() : Task.Priority.MEDIUM)
                .dueDate(taskDTO.getDueDate())
                .build());
        log.info("Task created successfully with id: {}", savedTask.getId());
        return toDTO(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasks() {
        log.debug("Fetching all tasks");
        return toDTOs(visibleTasks(taskRepository.findAll()));
    }

    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long id) {
        log.debug("Fetching task by id: {}", id);
        Task task = findTask(id);
        assertCanAccessProject(task.getProjectId());
        return toDTO(task);
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        log.info("Updating task: {}", id);
        Task task = findTask(id);
        assertCanAccessProject(task.getProjectId());
        if (taskDTO.getTitle() != null) {
            task.setTitle(taskDTO.getTitle());
        }
        if (taskDTO.getDescription() != null) {
            task.setDescription(taskDTO.getDescription());
        }
        if (hasAssignmentChange(taskDTO)) {
            assignmentSupport.applyAssignment(task, assignmentSupport.resolveAssignmentSnapshot(assignmentRequestFrom(taskDTO), null));
        }
        if (taskDTO.getStatus() != null) {
            task.setStatus(taskDTO.getStatus());
        }
        if (taskDTO.getPriority() != null) {
            task.setPriority(taskDTO.getPriority());
        }
        if (taskDTO.getDueDate() != null) {
            task.setDueDate(taskDTO.getDueDate());
        }
        return toDTO(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long id) {
        log.info("Deleting task: {}", id);
        Task task = findTask(id);
        assertCanAccessProject(task.getProjectId());
        taskRepository.deleteById(id);
        log.info("Task deleted successfully: {}", id);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProjectId(Long projectId) {
        log.debug("Fetching tasks for project: {}", projectId);
        assertCanAccessProject(projectId);
        return toDTOs(taskRepository.findByProjectId(projectId));
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByAssigneeId(Long assigneeId) {
        log.debug("Fetching tasks for assignee: {}", assigneeId);
        return toDTOs(visibleTasks(taskRepository.findByAssigneeId(assigneeId)));
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getAccessibleTasksByAssigneeId(Long assigneeId, String username) {
        User currentUser = assignmentSupport.resolveEnabledUserByUsername(username);
        if (assigneeId == null || Objects.equals(currentUser.getId(), assigneeId)) {
            return getTasksByAssigneeId(currentUser.getId());
        }
        User targetUser = assignmentSupport.resolveEnabledUserById(assigneeId);
        assignmentSupport.assertCanAccessTargetUser(currentUser, targetUser, false);
        return getTasksByAssigneeId(assigneeId);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByStatus(Task.Status status) {
        log.debug("Fetching tasks with status: {}", status);
        return toDTOs(visibleTasks(taskRepository.findByStatus(status)));
    }

    @Transactional
    public TaskDTO updateTaskStatus(Long id, Task.Status status) {
        log.info("Updating task {} status to: {}", id, status);
        Task task = findTask(id);
        assertCanAccessProject(task.getProjectId());
        task.setStatus(status);
        return toDTO(taskRepository.save(task));
    }

    @Transactional
    public TaskDTO assignTask(Long id, TaskAssignmentRequest request, String username) {
        log.info("Assigning task {} to user: {}", id, request == null ? null : request.getAssigneeId());
        Task task = findTask(id);
        assertCanAccessProject(task.getProjectId());
        User currentUser = assignmentSupport.resolveEnabledUserByUsername(username);
        assignmentSupport.applyAssignment(task, assignmentSupport.resolveAssignmentSnapshot(request, currentUser));
        return toDTO(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskAssignmentCandidateDTO> getAssignmentCandidates(String deptCode, String roleCode, String username) {
        return assignmentSupport.getAssignmentCandidates(deptCode, roleCode, username);
    }

    @Transactional(readOnly = true)
    public TeamTaskWorkloadDTO getTeamTaskWorkload(String username) {
        return assignmentSupport.getTeamTaskWorkload(username);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getUpcomingTasks(LocalDateTime beforeDate) {
        log.debug("Fetching tasks due before: {}", beforeDate);
        return toDTOs(visibleTasks(taskRepository.findByDueDateBefore(beforeDate)));
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getOverdueTasks() {
        log.debug("Fetching overdue tasks");
        return toDTOs(visibleTasks(taskRepository.findByDueDateBeforeAndStatusNot(LocalDateTime.now(), Task.Status.COMPLETED)));
    }

    public Long countProjectTasks(Long projectId) {
        assertCanAccessProject(projectId);
        return taskRepository.countByProjectId(projectId);
    }

    public Long countUserTasks(Long assigneeId) {
        return (long) visibleTasks(taskRepository.findByAssigneeId(assigneeId)).size();
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));
    }

    private List<Task> visibleTasks(List<Task> tasks) {
        return TaskProjectVisibilityPolicy.filterVisibleTasks(
                tasks,
                projectAccessScopeService.getAllowedProjectIdsForCurrentUser()
        );
    }

    private void assertCanAccessProject(Long projectId) {
        if (!TaskProjectVisibilityPolicy.canAccessProject(
                projectId,
                projectAccessScopeService.getAllowedProjectIdsForCurrentUser()
        )) {
            throw new AccessDeniedException("权限不足，无法访问该项目任务");
        }
    }

    private List<TaskDTO> toDTOs(List<Task> tasks) {
        return tasks.stream().map(this::toDTO).toList();
    }

    private TaskDTO toDTO(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .assigneeId(task.getAssigneeId())
                .assigneeDeptCode(task.getAssigneeDeptCode())
                .assigneeDeptName(task.getAssigneeDeptName())
                .assigneeRoleCode(task.getAssigneeRoleCode())
                .assigneeRoleName(task.getAssigneeRoleName())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private static TaskAssignmentRequest assignmentRequestFrom(TaskDTO taskDTO) {
        return TaskAssignmentRequest.builder()
                .assigneeId(taskDTO.getAssigneeId())
                .assigneeDeptCode(taskDTO.getAssigneeDeptCode())
                .assigneeDeptName(taskDTO.getAssigneeDeptName())
                .assigneeRoleCode(taskDTO.getAssigneeRoleCode())
                .assigneeRoleName(taskDTO.getAssigneeRoleName())
                .build();
    }

    private static boolean hasAssignmentChange(TaskDTO taskDTO) {
        return taskDTO.getAssigneeId() != null
                || hasText(taskDTO.getAssigneeDeptCode())
                || hasText(taskDTO.getAssigneeRoleCode());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
