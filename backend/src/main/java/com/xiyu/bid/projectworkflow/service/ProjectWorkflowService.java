// Input: projectworkflow repositories, DTOs, and support services
// Output: Project Workflow orchestration and score draft rule decisions
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.projectworkflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.projectworkflow.dto.ProjectDocumentCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectDocumentDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectReminderCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectReminderDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftGenerateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftParseResponse;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftUpdateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectShareLinkCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectShareLinkDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskStatusUpdateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskViewDTO;
import com.xiyu.bid.projectworkflow.core.ScoreDraftPolicy;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import com.xiyu.bid.projectworkflow.entity.ProjectReminder;
import com.xiyu.bid.projectworkflow.entity.ProjectShareLink;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.projectworkflow.repository.ProjectScoreDraftRepository;
import com.xiyu.bid.projectworkflow.repository.ProjectReminderRepository;
import com.xiyu.bid.projectworkflow.repository.ProjectShareLinkRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
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
    private final ProjectAccessScopeService projectAccessScopeService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectReminderRepository projectReminderRepository;
    private final ProjectShareLinkRepository projectShareLinkRepository;
    private final ProjectScoreDraftRepository projectScoreDraftRepository;
    private final ScoreDraftParserService scoreDraftParserService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<ProjectScoreDraftDTO> getProjectScoreDrafts(Long projectId) {
        requireProject(projectId);
        return projectScoreDraftRepository.findByProjectIdOrderByCategoryAscSourceTableIndexAscSourceRowIndexAsc(projectId)
                .stream()
                .map(this::toScoreDraftDTO)
                .toList();
    }

    public ProjectScoreDraftParseResponse parseProjectScoreDrafts(Long projectId, MultipartFile file) {
        requireProject(projectId);
        clearNonGeneratedDrafts(projectId);
        List<ProjectScoreDraft> parsedDrafts = scoreDraftParserService.parse(projectId, file);
        List<ProjectScoreDraftDTO> draftDTOs = projectScoreDraftRepository.saveAll(parsedDrafts)
                .stream()
                .map(this::toScoreDraftDTO)
                .toList();
        return buildParseResponse(draftDTOs);
    }

    public ProjectScoreDraftDTO updateProjectScoreDraft(Long projectId, Long draftId, ProjectScoreDraftUpdateRequest request) {
        requireProject(projectId);
        ProjectScoreDraft draft = requireDraft(projectId, draftId);
        ScoreDraftPolicy.UpdateDecision decision = ScoreDraftPolicy.decideUpdate(new ScoreDraftPolicy.UpdateCommand(
                toCoreStatus(draft.getStatus()),
                request.getAssigneeId(),
                request.getAssigneeName(),
                request.getDueDate(),
                request.getGeneratedTaskTitle(),
                request.getGeneratedTaskDescription(),
                toCoreStatus(request.getStatus()),
                request.getSkipReason()
        ));
        if (!decision.ok()) {
            throw toScoreDraftRuleException(decision.failure());
        }
        applyScoreDraftUpdate(draft, decision);
        return toScoreDraftDTO(projectScoreDraftRepository.save(draft));
    }

    public List<ProjectTaskViewDTO> generateTasksFromScoreDrafts(Long projectId, ProjectScoreDraftGenerateRequest request) {
        requireProject(projectId);
        List<ProjectScoreDraft> drafts = request.getDraftIds().stream()
                .map(draftId -> requireDraft(projectId, draftId))
                .toList();

        ScoreDraftPolicy.GenerationDecision decision = ScoreDraftPolicy.decideGeneration(
                drafts.stream().map(draft -> toCoreStatus(draft.getStatus())).toList()
        );
        if (!decision.ok()) {
            throw toScoreDraftRuleException(decision.failure());
        }

        return drafts.stream()
                .map(this::createTaskFromDraft)
                .toList();
    }

    public void clearNonGeneratedDrafts(Long projectId) {
        requireProject(projectId);
        projectScoreDraftRepository.deleteByProjectIdAndStatusIn(
                projectId,
                List.of(ProjectScoreDraft.Status.DRAFT, ProjectScoreDraft.Status.READY, ProjectScoreDraft.Status.SKIPPED)
        );
    }

    private Project requireProject(Long projectId) {
        projectAccessScopeService.assertCurrentUserCanAccessProject(projectId);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
    }

    private ProjectScoreDraft requireDraft(Long projectId, Long draftId) {
        ProjectScoreDraft draft = projectScoreDraftRepository.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectScoreDraft", String.valueOf(draftId)));
        if (!projectId.equals(draft.getProjectId())) {
            throw new IllegalArgumentException("Score draft does not belong to the specified project");
        }
        return draft;
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

    private ProjectScoreDraftDTO toScoreDraftDTO(ProjectScoreDraft draft) {
        return ProjectScoreDraftDTO.builder()
                .id(draft.getId())
                .projectId(draft.getProjectId())
                .sourceFileName(draft.getSourceFileName())
                .category(draft.getCategory())
                .scoreItemTitle(draft.getScoreItemTitle())
                .scoreRuleText(draft.getScoreRuleText())
                .scoreValueText(draft.getScoreValueText())
                .taskAction(draft.getTaskAction())
                .generatedTaskTitle(draft.getGeneratedTaskTitle())
                .generatedTaskDescription(draft.getGeneratedTaskDescription())
                .suggestedDeliverables(readDeliverables(draft.getSuggestedDeliverables()))
                .assigneeId(draft.getAssigneeId())
                .assigneeName(draft.getAssigneeName())
                .dueDate(draft.getDueDate())
                .status(draft.getStatus())
                .skipReason(draft.getSkipReason())
                .sourcePage(draft.getSourcePage())
                .sourceTableIndex(draft.getSourceTableIndex())
                .sourceRowIndex(draft.getSourceRowIndex())
                .generatedTaskId(draft.getGeneratedTaskId())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }

    private ProjectScoreDraftParseResponse buildParseResponse(List<ProjectScoreDraftDTO> draftDTOs) {
        return ProjectScoreDraftParseResponse.builder()
                .drafts(draftDTOs)
                .totalCount(draftDTOs.size())
                .draftCount(countByStatus(draftDTOs, ProjectScoreDraft.Status.DRAFT))
                .readyCount(countByStatus(draftDTOs, ProjectScoreDraft.Status.READY))
                .skippedCount(countByStatus(draftDTOs, ProjectScoreDraft.Status.SKIPPED))
                .build();
    }

    private long countByStatus(Collection<ProjectScoreDraftDTO> drafts, ProjectScoreDraft.Status status) {
        return drafts.stream().filter(draft -> draft.getStatus() == status).count();
    }

    private List<String> readDeliverables(String serializedValue) {
        if (serializedValue == null || serializedValue.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(serializedValue, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return List.of(serializedValue);
        }
    }

    private ProjectTaskViewDTO createTaskFromDraft(ProjectScoreDraft draft) {
        TaskCreationPlan plan = planTaskCreation(draft);
        Task task = buildTask(plan);
        Task saved = taskRepository.save(task);
        draft.setGeneratedTaskId(saved.getId());
        draft.setStatus(ProjectScoreDraft.Status.GENERATED);
        projectScoreDraftRepository.save(draft);
        return toTaskView(saved, draft.getAssigneeName());
    }

    private void applyScoreDraftUpdate(ProjectScoreDraft draft, ScoreDraftPolicy.UpdateDecision decision) {
        draft.setAssigneeId(decision.assigneeId());
        draft.setAssigneeName(decision.assigneeName());
        draft.setDueDate(decision.dueDate());
        if (decision.generatedTaskTitle() != null) {
            draft.setGeneratedTaskTitle(decision.generatedTaskTitle());
        }
        if (decision.generatedTaskDescription() != null) {
            draft.setGeneratedTaskDescription(decision.generatedTaskDescription());
        }
        draft.setStatus(toEntityStatus(decision.status()));
        draft.setSkipReason(decision.skipReason());
    }

    private TaskCreationPlan planTaskCreation(ProjectScoreDraft draft) {
        return new TaskCreationPlan(
                draft.getProjectId(),
                draft.getGeneratedTaskTitle(),
                draft.getGeneratedTaskDescription(),
                draft.getAssigneeId(),
                resolvePriority(draft.getScoreValueText()),
                draft.getDueDate()
        );
    }

    private Task buildTask(TaskCreationPlan plan) {
        return Task.builder()
                .projectId(plan.projectId())
                .title(plan.title())
                .description(plan.description())
                .assigneeId(plan.assigneeId())
                .priority(plan.priority())
                .status(Task.Status.TODO)
                .dueDate(plan.dueDate())
                .build();
    }

    private RuntimeException toScoreDraftRuleException(ScoreDraftPolicy.RuleFailure failure) {
        return switch (failure) {
            case GENERATED_NOT_EDITABLE -> new IllegalStateException("已生成正式任务的草稿不可修改");
            case READY_REQUIRES_ASSIGNEE -> new IllegalArgumentException("生成正式任务前必须指定责任人");
            case ONLY_READY_DRAFTS_CAN_GENERATE_TASKS -> new IllegalArgumentException("仅 READY 状态的评分草稿可生成正式任务");
        };
    }

    private ScoreDraftPolicy.DraftStatus toCoreStatus(ProjectScoreDraft.Status status) {
        if (status == null) {
            return null;
        }
        return ScoreDraftPolicy.DraftStatus.valueOf(status.name());
    }

    private ProjectScoreDraft.Status toEntityStatus(ScoreDraftPolicy.DraftStatus status) {
        if (status == null) {
            return null;
        }
        return ProjectScoreDraft.Status.valueOf(status.name());
    }

    private Task.Priority resolvePriority(String scoreValueText) {
        String scoreValue = defaultString(scoreValueText, "");
        if (scoreValue.contains("10") || scoreValue.contains("最高")) {
            return Task.Priority.HIGH;
        }
        return Task.Priority.MEDIUM;
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

    private record TaskCreationPlan(
            Long projectId,
            String title,
            String description,
            Long assigneeId,
            Task.Priority priority,
            LocalDateTime dueDate
    ) {
    }
}
