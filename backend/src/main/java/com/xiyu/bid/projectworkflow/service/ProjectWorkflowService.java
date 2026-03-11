package com.xiyu.bid.projectworkflow.service;

import com.xiyu.bid.dto.TaskDTO;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.projectworkflow.dto.*;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.entity.ProjectReminder;
import com.xiyu.bid.projectworkflow.entity.ProjectShareLink;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.projectworkflow.repository.ProjectReminderRepository;
import com.xiyu.bid.projectworkflow.repository.ProjectShareLinkRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectWorkflowService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA);

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectReminderRepository projectReminderRepository;
    private final ProjectShareLinkRepository projectShareLinkRepository;

    @Transactional(readOnly = true)
    public List<ProjectTaskViewDTO> getProjectTasks(Long projectId) {
        requireProject(projectId);
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::toTaskView)
                .toList();
    }

    public ProjectTaskViewDTO createProjectTask(Long projectId, ProjectTaskCreateRequest request) {
        requireProject(projectId);
        Task task = Task.builder()
                .projectId(projectId)
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .assigneeId(request.getAssigneeId())
                .priority(request.getPriority())
                .status(Task.Status.TODO)
                .dueDate(request.getDueDate())
                .build();
        Task saved = taskRepository.save(task);
        return toTaskView(saved, request.getAssigneeName());
    }

    public ProjectTaskViewDTO updateProjectTaskStatus(Long projectId, Long taskId, ProjectTaskStatusUpdateRequest request) {
        requireProject(projectId);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", String.valueOf(taskId)));
        if (!projectId.equals(task.getProjectId())) {
            throw new IllegalArgumentException("Task does not belong to the specified project");
        }
        task.setStatus(request.getStatus());
        Task saved = taskRepository.save(task);
        return toTaskView(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectDocumentDTO> getProjectDocuments(Long projectId) {
        requireProject(projectId);
        return projectDocumentRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toDocumentDTO)
                .toList();
    }

    public ProjectDocumentDTO createProjectDocument(Long projectId, ProjectDocumentCreateRequest request) {
        requireProject(projectId);
        ProjectDocument document = ProjectDocument.builder()
                .projectId(projectId)
                .name(request.getName().trim())
                .size(defaultString(request.getSize(), "1MB"))
                .fileType(trimToNull(request.getFileType()))
                .uploaderId(request.getUploaderId())
                .uploaderName(resolveDisplayName(request.getUploaderId(), request.getUploaderName()))
                .build();
        return toDocumentDTO(projectDocumentRepository.save(document));
    }

    public void deleteProjectDocument(Long projectId, Long documentId) {
        requireProject(projectId);
        ProjectDocument document = projectDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectDocument", String.valueOf(documentId)));
        if (!projectId.equals(document.getProjectId())) {
            throw new IllegalArgumentException("Document does not belong to the specified project");
        }
        projectDocumentRepository.delete(document);
    }

    @Transactional(readOnly = true)
    public List<ProjectReminderDTO> getProjectReminders(Long projectId) {
        requireProject(projectId);
        return projectReminderRepository.findByProjectIdOrderByRemindAtDesc(projectId).stream()
                .map(this::toReminderDTO)
                .toList();
    }

    public ProjectReminderDTO createProjectReminder(Long projectId, ProjectReminderCreateRequest request) {
        requireProject(projectId);
        ProjectReminder reminder = ProjectReminder.builder()
                .projectId(projectId)
                .title(request.getTitle().trim())
                .message(trimToNull(request.getMessage()))
                .remindAt(request.getRemindAt())
                .createdBy(request.getCreatedBy())
                .createdByName(resolveDisplayName(request.getCreatedBy(), request.getCreatedByName()))
                .recipient(defaultString(request.getRecipient(), "项目负责人"))
                .build();
        return toReminderDTO(projectReminderRepository.save(reminder));
    }

    @Transactional(readOnly = true)
    public List<ProjectShareLinkDTO> getProjectShareLinks(Long projectId) {
        requireProject(projectId);
        return projectShareLinkRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toShareLinkDTO)
                .toList();
    }

    public ProjectShareLinkDTO createProjectShareLink(Long projectId, ProjectShareLinkCreateRequest request) {
        requireProject(projectId);
        String token = UUID.randomUUID().toString().replace("-", "");
        String baseUrl = request.getBaseUrl().trim().replaceAll("/+$", "");
        ProjectShareLink shareLink = ProjectShareLink.builder()
                .projectId(projectId)
                .token(token)
                .url(baseUrl + "/project/" + projectId + "?share=" + token)
                .createdBy(request.getCreatedBy())
                .createdByName(resolveDisplayName(request.getCreatedBy(), request.getCreatedByName()))
                .expiresAt(request.getExpiresAt())
                .build();
        return toShareLinkDTO(projectShareLinkRepository.save(shareLink));
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
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
                .owner(assigneeName)
                .assignee(assigneeName)
                .department("投标管理部")
                .status(mapStatus(task.getStatus()))
                .priority(mapPriority(task.getPriority()))
                .dueDate(task.getDueDate() != null ? task.getDueDate().format(DISPLAY_DATE) : "")
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private ProjectDocumentDTO toDocumentDTO(ProjectDocument document) {
        return ProjectDocumentDTO.builder()
                .id(document.getId())
                .projectId(document.getProjectId())
                .name(document.getName())
                .size(document.getSize())
                .fileType(document.getFileType())
                .uploaderId(document.getUploaderId())
                .uploader(document.getUploaderName())
                .time(document.getCreatedAt() != null ? document.getCreatedAt().format(DISPLAY_TIME) : "")
                .createdAt(document.getCreatedAt())
                .build();
    }

    private ProjectReminderDTO toReminderDTO(ProjectReminder reminder) {
        return ProjectReminderDTO.builder()
                .id(reminder.getId())
                .projectId(reminder.getProjectId())
                .title(reminder.getTitle())
                .message(reminder.getMessage())
                .remindAt(reminder.getRemindAt())
                .createdBy(reminder.getCreatedBy())
                .createdByName(reminder.getCreatedByName())
                .recipient(reminder.getRecipient())
                .createdAt(reminder.getCreatedAt())
                .build();
    }

    private ProjectShareLinkDTO toShareLinkDTO(ProjectShareLink shareLink) {
        return ProjectShareLinkDTO.builder()
                .id(shareLink.getId())
                .projectId(shareLink.getProjectId())
                .token(shareLink.getToken())
                .url(shareLink.getUrl())
                .createdBy(shareLink.getCreatedBy())
                .createdByName(shareLink.getCreatedByName())
                .expiresAt(shareLink.getExpiresAt())
                .createdAt(shareLink.getCreatedAt())
                .build();
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

    private String mapStatus(Task.Status status) {
        if (status == null) {
            return "todo";
        }
        return switch (status) {
            case TODO -> "todo";
            case IN_PROGRESS -> "doing";
            case COMPLETED -> "done";
            case CANCELLED -> "review";
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
