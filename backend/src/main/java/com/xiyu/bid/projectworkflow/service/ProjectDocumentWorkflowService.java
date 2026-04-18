// Input: project/document/reminder/share-link repositories and access scope service
// Output: document and lightweight project workflow orchestration
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.projectworkflow.service;

import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.projectworkflow.dto.ProjectDocumentCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectDocumentDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectReminderCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectReminderDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectShareLinkCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectShareLinkDTO;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.entity.ProjectReminder;
import com.xiyu.bid.projectworkflow.entity.ProjectShareLink;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.projectworkflow.repository.ProjectReminderRepository;
import com.xiyu.bid.projectworkflow.repository.ProjectShareLinkRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectDocumentWorkflowService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private final ProjectWorkflowGuard projectWorkflowGuard;
    private final UserRepository userRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectReminderRepository projectReminderRepository;
    private final ProjectShareLinkRepository projectShareLinkRepository;

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

    private void requireProject(Long projectId) {
        projectWorkflowGuard.requireProject(projectId);
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
