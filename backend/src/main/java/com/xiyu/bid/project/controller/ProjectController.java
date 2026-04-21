// Input: HTTP 请求、路径参数、认证上下文和 DTO
// Output: 标准化 API 响应和用例入口
// Pos: Controller/接口适配层
// 维护声明: 仅维护协议适配与参数校验；业务规则下沉到 service.
package com.xiyu.bid.project.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.project.dto.ProjectRequest;
import com.xiyu.bid.project.dto.ProjectDTO;
import com.xiyu.bid.project.service.ProjectService;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getAllProjects() {
        log.info("GET /api/projects - Fetching all projects");
        List<ProjectDTO> projects = projectService.getAllProjects();
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved projects", projects));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProjectDTO>> getProjectById(@PathVariable Long id) {
        log.info("GET /api/projects/{} - Fetching project", id);
        ProjectDTO project = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved project", project));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProjectDTO>> createProject(@Valid @RequestBody ProjectRequest projectRequest) {
        log.info("POST /api/projects - Creating new project: {}", projectRequest.getName());
        sanitizeProjectRequest(projectRequest);
        ProjectDTO projectDTO = convertRequestToDTO(projectRequest);
        ProjectDTO createdProject = projectService.createProject(projectDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Project created successfully", createdProject));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProjectDTO>> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest projectRequest) {
        log.info("PUT /api/projects/{} - Updating project", id);
        sanitizeProjectRequest(projectRequest);
        ProjectDTO projectDTO = convertRequestToDTO(projectRequest);
        ProjectDTO updatedProject = projectService.updateProject(id, projectDTO);
        return ResponseEntity.ok(ApiResponse.success("Project updated successfully", updatedProject));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        log.info("DELETE /api/projects/{} - Deleting project", id);
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success("Project deleted successfully", null));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProjectDTO>> updateProjectStatus(@PathVariable Long id, @RequestParam com.xiyu.bid.entity.Project.Status status) {
        log.info("PUT /api/projects/{}/status - Updating status to {}", id, status);
        ProjectDTO updatedProject = projectService.updateProjectStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Project status updated successfully", updatedProject));
    }

    @PutMapping("/{id}/team")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProjectDTO>> updateProjectTeam(@PathVariable Long id, @RequestBody List<Long> teamMembers) {
        log.info("PUT /api/projects/{}/team - Updating team members", id);
        ProjectDTO updatedProject = projectService.updateProjectTeam(id, teamMembers);
        return ResponseEntity.ok(ApiResponse.success("Project team updated successfully", updatedProject));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getProjectsByStatus(@PathVariable com.xiyu.bid.entity.Project.Status status) {
        log.info("GET /api/projects/status/{} - Fetching projects by status", status);
        List<ProjectDTO> projects = projectService.getProjectsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved projects", projects));
    }

    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getProjectsByManager(@PathVariable Long managerId) {
        log.info("GET /api/projects/manager/{} - Fetching projects by manager", managerId);
        List<ProjectDTO> projects = projectService.getProjectsByManager(managerId);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved projects", projects));
    }

    @GetMapping("/tender/{tenderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getProjectsByTender(@PathVariable Long tenderId) {
        log.info("GET /api/projects/tender/{} - Fetching projects by tender", tenderId);
        List<ProjectDTO> projects = projectService.getProjectsByTender(tenderId);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved projects", projects));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getActiveProjects() {
        log.info("GET /api/projects/active - Fetching active projects");
        List<ProjectDTO> projects = projectService.getActiveProjects();
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved active projects", projects));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> searchProjects(@RequestParam String name) {
        log.info("GET /api/projects/search?name={} - Searching projects", name);
        String sanitizedName = InputSanitizer.sanitizeString(name, 100);
        List<ProjectDTO> projects = projectService.searchProjectsByName(sanitizedName);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved projects", projects));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<com.xiyu.bid.entity.Project.Status, Long>>> getStatistics() {
        log.info("GET /api/projects/statistics - Fetching project statistics");
        Map<com.xiyu.bid.entity.Project.Status, Long> statistics = projectService.getProjectStatistics();
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved statistics", statistics));
    }

    private ProjectDTO convertRequestToDTO(ProjectRequest request) {
        return ProjectDTO.builder()
                .name(request.getName())
                .tenderId(request.getTenderId())
                .status(request.getStatus())
                .managerId(request.getManagerId())
                .teamMembers(request.getTeamMembers())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .sourceModule(request.getSourceModule())
                .sourceCustomerId(request.getSourceCustomerId())
                .sourceCustomer(request.getSourceCustomer())
                .sourceOpportunityId(request.getSourceOpportunityId())
                .sourceReasoningSummary(request.getSourceReasoningSummary())
                .competitorAnalysisJson(request.getCompetitorAnalysisJson())
                .tasksJson(request.getTasksJson())
                .aiAnalysisJson(request.getAiAnalysisJson())
                .customer(request.getCustomer())
                .budget(request.getBudget())
                .industry(request.getIndustry())
                .customerType(request.getCustomerType())
                .region(request.getRegion())
                .platform(request.getPlatform())
                .deadline(request.getDeadline())
                .description(request.getDescription())
                .remark(request.getRemark())
                .tagsJson(request.getTagsJson())
                .customerManager(request.getCustomerManager())
                .customerManagerId(request.getCustomerManagerId())
                .build();
    }

    private void sanitizeProjectRequest(ProjectRequest request) {
        if (request.getName() != null) request.setName(InputSanitizer.sanitizeString(request.getName(), 200));
        if (request.getCustomer() != null) request.setCustomer(InputSanitizer.sanitizeString(request.getCustomer(), 255));
        if (request.getIndustry() != null) request.setIndustry(InputSanitizer.sanitizeString(request.getIndustry(), 50));
        if (request.getCustomerType() != null) request.setCustomerType(InputSanitizer.sanitizeString(request.getCustomerType(), 100));
        if (request.getRegion() != null) request.setRegion(InputSanitizer.sanitizeString(request.getRegion(), 100));
        if (request.getPlatform() != null) request.setPlatform(InputSanitizer.sanitizeString(request.getPlatform(), 255));
        if (request.getDescription() != null) request.setDescription(InputSanitizer.sanitizeString(request.getDescription(), 5000));
        if (request.getRemark() != null) request.setRemark(InputSanitizer.sanitizeString(request.getRemark(), 5000));
    }
}
