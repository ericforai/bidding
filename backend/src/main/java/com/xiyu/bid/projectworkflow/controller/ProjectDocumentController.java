package com.xiyu.bid.projectworkflow.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.projectworkflow.dto.ProjectDocumentCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectDocumentDTO;
import com.xiyu.bid.projectworkflow.service.ProjectWorkflowService;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/documents")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectWorkflowService projectWorkflowService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectDocumentDTO>>> getProjectDocuments(
            @PathVariable Long projectId,
            @RequestParam(required = false) String documentCategory,
            @RequestParam(required = false) String linkedEntityType,
            @RequestParam(required = false) Long linkedEntityId
    ) {
        return ResponseEntity.ok(ApiResponse.success(projectWorkflowService.getProjectDocuments(
                projectId,
                documentCategory,
                linkedEntityType,
                linkedEntityId
        )));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProjectDocumentDTO>> createProjectDocument(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectDocumentCreateRequest request
    ) {
        sanitizeDocumentRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Project document created successfully",
                        projectWorkflowService.createProjectDocument(projectId, request)
                ));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteProjectDocument(
            @PathVariable Long projectId,
            @PathVariable Long documentId
    ) {
        projectWorkflowService.deleteProjectDocument(projectId, documentId);
        return ResponseEntity.ok(ApiResponse.success("Project document deleted successfully", null));
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
        if (request.getDocumentCategory() != null) {
            request.setDocumentCategory(InputSanitizer.sanitizeString(request.getDocumentCategory(), 64));
        }
        if (request.getLinkedEntityType() != null) {
            request.setLinkedEntityType(InputSanitizer.sanitizeString(request.getLinkedEntityType(), 64));
        }
        if (request.getFileUrl() != null) {
            request.setFileUrl(InputSanitizer.sanitizeString(request.getFileUrl(), 1000));
        }
    }
}
