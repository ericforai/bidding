// Input: project/task/user repositories and access scope service
// Output: project task workflow orchestration
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.projectworkflow.service;

import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskStatusUpdateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskViewDTO;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProjectTaskWorkflowService {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA);

    private final ProjectWorkflowGuard projectWorkflowGuard;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public List<ProjectTaskViewDTO> getProjectTasks(Long projectId) {
        requireProject(projectId);
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::toTaskView)
                .toList();
    }

    public ProjectTaskViewDTO createProjectTask(Long projectId, ProjectTaskCreateRequest request) {
        requireProject(projectId);
        User assigneeUser = resolveAssignee(request.getAssigneeId());
        Task task = Task.builder()
                .projectId(projectId)
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .assigneeId(request.getAssigneeId())
                .assigneeDeptCode(assigneeUser != null ? assigneeUser.getDepartmentCode() : trimToNull(request.getAssigneeDeptCode()))
                .assigneeDeptName(assigneeUser != null ? assigneeUser.getDepartmentName() : defaultString(trimToNull(request.getAssigneeDeptName()), "未配置部门"))
                .assigneeRoleCode(assigneeUser != null ? assigneeUser.getRoleCode() : trimToNull(request.getAssigneeRoleCode()))
                .assigneeRoleName(assigneeUser != null ? assigneeUser.getRoleName() : trimToNull(request.getAssigneeRoleName()))
                .priority(request.getPriority())
                .status(Task.Status.TODO)
                .dueDate(request.getDueDate())
                .build();
        return toTaskView(taskRepository.save(task), request.getAssigneeName());
    }

    public ProjectTaskViewDTO updateProjectTaskStatus(Long projectId, Long taskId, ProjectTaskStatusUpdateRequest request) {
        requireProject(projectId);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", String.valueOf(taskId)));
        if (!projectId.equals(task.getProjectId())) {
            throw new IllegalArgumentException("Task does not belong to the specified project");
        }
        task.setStatus(request.getStatus());
        return toTaskView(taskRepository.save(task));
    }

    public ProjectTaskViewDTO createTaskFromDraft(
            Long projectId,
            String generatedTaskTitle,
            String generatedTaskDescription,
            Long assigneeId,
            String assigneeName,
            String scoreValueText,
            LocalDateTime dueDate
    ) {
        Task task = Task.builder()
                .projectId(projectId)
                .title(generatedTaskTitle)
                .description(generatedTaskDescription)
                .assigneeId(assigneeId)
                .priority(resolvePriority(scoreValueText))
                .status(Task.Status.TODO)
                .dueDate(dueDate)
                .build();
        return toTaskView(taskRepository.save(task), assigneeName);
    }

    private void requireProject(Long projectId) {
        projectWorkflowGuard.requireProject(projectId);
    }

    private ProjectTaskViewDTO toTaskView(Task task) {
        return toTaskView(task, null);
    }

    private ProjectTaskViewDTO toTaskView(Task task, String fallbackAssigneeName) {
        String assigneeName = resolveDisplayName(task.getAssigneeId(), fallbackAssigneeName);
        return ProjectTaskViewDTO.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .name(task.getTitle())
                .description(task.getDescription())
                .assigneeId(task.getAssigneeId())
                .assigneeDeptCode(task.getAssigneeDeptCode())
                .assigneeRoleCode(task.getAssigneeRoleCode())
                .owner(assigneeName)
                .assignee(assigneeName)
                .department(defaultString(task.getAssigneeDeptName(), "未配置部门"))
                .roleName(defaultString(task.getAssigneeRoleName(), "未配置角色"))
                .status(mapStatus(task.getStatus()))
                .priority(mapPriority(task.getPriority()))
                .dueDate(task.getDueDate() != null ? task.getDueDate().format(DISPLAY_DATE) : "")
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private User resolveAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository.findById(assigneeId).orElse(null);
    }

    private String resolveDisplayName(Long userId, String fallback) {
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
                return user.getFullName();
            }
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return "未分配";
    }

    private Task.Priority resolvePriority(String scoreValueText) {
        String scoreValue = defaultString(scoreValueText, "");
        if (scoreValue.contains("10") || scoreValue.contains("最高")) {
            return Task.Priority.HIGH;
        }
        return Task.Priority.MEDIUM;
    }

    private String mapStatus(Task.Status status) {
        if (status == null) {
            return "todo";
        }
        return switch (status) {
            case TODO -> "todo";
            case IN_PROGRESS -> "doing";
            case REVIEW -> "review";
            case COMPLETED -> "done";
            case CANCELLED -> "cancelled";
        };
    }

    private String mapPriority(Task.Priority priority) {
        if (priority == null) {
            return "medium";
        }
        return switch (priority) {
            case LOW -> "low";
            case MEDIUM -> "medium";
            case HIGH -> "high";
            case URGENT -> "urgent";
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultString(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized != null ? normalized : fallback;
    }
}
