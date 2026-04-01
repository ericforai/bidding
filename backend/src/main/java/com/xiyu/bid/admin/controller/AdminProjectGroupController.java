package com.xiyu.bid.admin.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.ProjectGroupConfigRequest;
import com.xiyu.bid.dto.ProjectGroupConfigResponse;
import com.xiyu.bid.admin.service.ProjectGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/project-groups")
@RequiredArgsConstructor
public class AdminProjectGroupController {

    private final ProjectGroupService projectGroupService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectGroupConfigResponse>> getProjectGroups() {
        log.info("GET /api/admin/project-groups - fetching project groups");
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved project groups", projectGroupService.getProjectGroups()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectGroupConfigResponse.ProjectGroupItem>> createProjectGroup(@RequestBody ProjectGroupConfigRequest.ProjectGroupItem request) {
        log.info("POST /api/admin/project-groups - creating project group");
        return ResponseEntity.ok(ApiResponse.success("Project group created successfully", projectGroupService.createProjectGroup(request)));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectGroupConfigResponse>> saveProjectGroups(@RequestBody ProjectGroupConfigRequest request) {
        log.info("PUT /api/admin/project-groups - saving project groups");
        return ResponseEntity.ok(ApiResponse.success("Project groups saved successfully", projectGroupService.saveProjectGroups(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectGroupConfigResponse.ProjectGroupItem>> updateProjectGroup(
            @PathVariable Long id,
            @RequestBody ProjectGroupConfigRequest.ProjectGroupItem request
    ) {
        log.info("PATCH /api/admin/project-groups/{} - updating project group", id);
        return ResponseEntity.ok(ApiResponse.success("Project group updated successfully", projectGroupService.updateProjectGroup(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProjectGroup(@PathVariable Long id) {
        log.info("DELETE /api/admin/project-groups/{} - deleting project group", id);
        projectGroupService.deleteProjectGroup(id);
        return ResponseEntity.ok(ApiResponse.success("Project group deleted successfully", null));
    }
}
