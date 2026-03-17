package com.xiyu.bid.projectworkflow.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.projectworkflow.dto.*;
import com.xiyu.bid.projectworkflow.service.ProjectWorkflowService;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectWorkflowController {

    private final ProjectWorkflowService projectWorkflowService;

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectTaskViewDTO>>> getProjectTasks(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(projectWorkflowService.getProjectTasks(projectId)));
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProjectTaskViewDTO>> createProjectTask(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectTaskCreateRequest request) {
        sanitizeTaskRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project task created successfully", projectWorkflowService.createProjectTask(projectId, request)));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProjectTaskViewDTO>> updateProjectTaskStatus(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody ProjectTaskStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Project task status updated successfully",
                projectWorkflowService.updateProjectTaskStatus(projectId, taskId, request)));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectDocumentDTO>>> getProjectDocuments(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(projectWorkflowService.getProjectDocuments(projectId)));
    }

    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProjectDocumentDTO>> createProjectDocument(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectDocumentCreateRequest request) {
        sanitizeDocumentRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project document created successfully",
                        projectWorkflowService.createProjectDocument(projectId, request)));
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteProjectDocument(
            @PathVariable Long projectId,
            @PathVariable Long documentId) {
        projectWorkflowService.deleteProjectDocument(projectId, documentId);
        return ResponseEntity.ok(ApiResponse.success("Project document deleted successfully", null));
    }

    @GetMapping("/reminders")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectReminderDTO>>> getProjectReminders(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(projectWorkflowService.getProjectReminders(projectId)));
    }

    @PostMapping("/reminders")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProjectReminderDTO>> createProjectReminder(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectReminderCreateRequest request) {
        sanitizeReminderRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project reminder created successfully",
                        projectWorkflowService.createProjectReminder(projectId, request)));
    }

    @GetMapping("/share-links")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectShareLinkDTO>>> getProjectShareLinks(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(projectWorkflowService.getProjectShareLinks(projectId)));
    }

    @PostMapping("/share-links")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProjectShareLinkDTO>> createProjectShareLink(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectShareLinkCreateRequest request) {
        sanitizeShareLinkRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project share link created successfully",
                        projectWorkflowService.createProjectShareLink(projectId, request)));
    }

    @PostMapping("/score-drafts/parse")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProjectScoreDraftParseResponse>> parseProjectScoreDrafts(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project score drafts parsed successfully",
                        projectWorkflowService.parseProjectScoreDrafts(projectId, file)));
    }

    @GetMapping("/score-drafts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectScoreDraftDTO>>> getProjectScoreDrafts(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(projectWorkflowService.getProjectScoreDrafts(projectId)));
    }

    @PatchMapping("/score-drafts/{draftId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProjectScoreDraftDTO>> updateProjectScoreDraft(
            @PathVariable Long projectId,
            @PathVariable Long draftId,
            @RequestBody ProjectScoreDraftUpdateRequest request) {
        sanitizeScoreDraftRequest(request);
        return ResponseEntity.ok(ApiResponse.success("Project score draft updated successfully",
                projectWorkflowService.updateProjectScoreDraft(projectId, draftId, request)));
    }

    @PostMapping("/score-drafts/generate-tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectTaskViewDTO>>> generateProjectTasksFromScoreDrafts(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectScoreDraftGenerateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project tasks generated from score drafts successfully",
                        projectWorkflowService.generateTasksFromScoreDrafts(projectId, request)));
    }

    @DeleteMapping("/score-drafts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> clearProjectScoreDrafts(@PathVariable Long projectId) {
        projectWorkflowService.clearNonGeneratedDrafts(projectId);
        return ResponseEntity.ok(ApiResponse.success("Project score drafts cleared successfully", null));
    }

    private void sanitizeTaskRequest(ProjectTaskCreateRequest request) {
        request.setTitle(InputSanitizer.sanitizeString(request.getTitle(), 200));
        if (request.getDescription() != null) {
            request.setDescription(InputSanitizer.sanitizeString(request.getDescription(), 2000));
        }
        if (request.getAssigneeName() != null) {
            request.setAssigneeName(InputSanitizer.sanitizeString(request.getAssigneeName(), 100));
        }
    }

    private void sanitizeDocumentRequest(ProjectDocumentCreateRequest request) {
        request.setName(InputSanitizer.sanitizeString(request.getName(), 255));
        if (request.getUploaderName() != null) {
            request.setUploaderName(InputSanitizer.sanitizeString(request.getUploaderName(), 100));
        }
        if (request.getFileType() != null) {
            request.setFileType(InputSanitizer.sanitizeString(request.getFileType(), 50));
        }
        if (request.getSize() != null) {
            request.setSize(InputSanitizer.sanitizeString(request.getSize(), 50));
        }
    }

    private void sanitizeReminderRequest(ProjectReminderCreateRequest request) {
        request.setTitle(InputSanitizer.sanitizeString(request.getTitle(), 200));
        if (request.getMessage() != null) {
            request.setMessage(InputSanitizer.sanitizeString(request.getMessage(), 1000));
        }
        if (request.getCreatedByName() != null) {
            request.setCreatedByName(InputSanitizer.sanitizeString(request.getCreatedByName(), 100));
        }
        if (request.getRecipient() != null) {
            request.setRecipient(InputSanitizer.sanitizeString(request.getRecipient(), 100));
        }
    }

    private void sanitizeShareLinkRequest(ProjectShareLinkCreateRequest request) {
        request.setBaseUrl(InputSanitizer.sanitizeString(request.getBaseUrl(), 500));
        if (request.getCreatedByName() != null) {
            request.setCreatedByName(InputSanitizer.sanitizeString(request.getCreatedByName(), 100));
        }
    }

    private void sanitizeScoreDraftRequest(ProjectScoreDraftUpdateRequest request) {
        if (request.getAssigneeName() != null) {
            request.setAssigneeName(InputSanitizer.sanitizeString(request.getAssigneeName(), 100));
        }
        if (request.getGeneratedTaskTitle() != null) {
            request.setGeneratedTaskTitle(InputSanitizer.sanitizeString(request.getGeneratedTaskTitle(), 255));
        }
        if (request.getGeneratedTaskDescription() != null) {
            request.setGeneratedTaskDescription(InputSanitizer.sanitizeString(request.getGeneratedTaskDescription(), 4000));
        }
        if (request.getSkipReason() != null) {
            request.setSkipReason(InputSanitizer.sanitizeString(request.getSkipReason(), 255));
        }
    }
}
