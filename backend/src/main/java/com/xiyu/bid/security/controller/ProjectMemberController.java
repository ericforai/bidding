package com.xiyu.bid.security.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.ProjectMemberDTO;
import com.xiyu.bid.dto.ProjectMemberRequest;
import com.xiyu.bid.security.service.ProjectMemberService;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
@RequiredArgsConstructor
@Slf4j
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;
    private final ProjectAccessScopeService projectAccessScopeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectMemberDTO>>> getMembers(@PathVariable Long projectId) {
        assertCanAccessProject(projectId);
        log.info("GET /api/projects/{}/members - Fetching project members", projectId);
        return ResponseEntity.ok(ApiResponse.success(projectMemberService.getProjectMembers(projectId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProjectMemberDTO>> addMember(
            @PathVariable Long projectId,
            @RequestBody ProjectMemberRequest request) {
        assertCanAccessProject(projectId);
        log.info("POST /api/projects/{}/members - Adding member", projectId);
        return ResponseEntity.ok(ApiResponse.success("Member added successfully", projectMemberService.addProjectMember(projectId, request)));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        assertCanAccessProject(projectId);
        log.info("DELETE /api/projects/{}/members/{} - Removing member", projectId, userId);
        projectMemberService.removeProjectMember(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", null));
    }

    private void assertCanAccessProject(Long projectId) {
        if (projectId == null || projectAccessScopeService.currentUserHasAdminAccess()) {
            return;
        }
        if (!projectAccessScopeService.getAllowedProjectIdsForCurrentUser().contains(projectId)) {
            throw new AccessDeniedException("无权访问该项目");
        }
    }
}
