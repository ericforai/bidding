// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.project.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.demo.service.DemoDataProvider;
import com.xiyu.bid.demo.service.DemoFusionService;
import com.xiyu.bid.demo.service.DemoModeService;
import com.xiyu.bid.project.dto.ProjectDTO;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目服务层
 * 提供项目管理的业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectService {

    private static final Map<Project.Status, Set<Project.Status>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            Project.Status.INITIATED, Set.of(Project.Status.PREPARING, Project.Status.REVIEWING, Project.Status.SEALING, Project.Status.BIDDING, Project.Status.ARCHIVED),
            Project.Status.PREPARING, Set.of(Project.Status.REVIEWING, Project.Status.SEALING, Project.Status.BIDDING, Project.Status.ARCHIVED),
            Project.Status.REVIEWING, Set.of(Project.Status.PREPARING, Project.Status.SEALING, Project.Status.BIDDING, Project.Status.ARCHIVED),
            Project.Status.SEALING, Set.of(Project.Status.REVIEWING, Project.Status.BIDDING, Project.Status.ARCHIVED),
            Project.Status.BIDDING, Set.of(Project.Status.ARCHIVED),
            Project.Status.ARCHIVED, Set.of(Project.Status.ARCHIVED)
    );

    private final ProjectRepository projectRepository;
    private final ProjectAccessScopeService projectAccessScopeService;
    private final DemoModeService demoModeService;
    private final DemoDataProvider demoDataProvider;
    private final DemoFusionService demoFusionService;

    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        log.debug("Fetching all projects");
        List<ProjectDTO> projects = projectAccessScopeService.filterAccessibleProjects(projectRepository.findAll()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return mergeDemoProjectsIfNeeded(projects);
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long id) {
        log.debug("Fetching project by id: {}", id);
        if (isDemoEntityId(id)) {
            return demoDataProvider.findDemoProjectById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));
        }
        projectAccessScopeService.assertCurrentUserCanAccessProject(id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));
        return convertToDTO(project);
    }

    public ProjectDTO createProject(ProjectDTO projectDTO) {
        log.debug("Creating new project: {}", projectDTO.getName());
        ProjectDTO normalized = validateAndNormalizeProjectDTO(projectDTO, true);
        Project project = convertToEntity(normalized);
        Project savedProject = projectRepository.save(project);
        log.info("Created project with id: {}", savedProject.getId());
        return convertToDTO(savedProject);
    }

    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        log.debug("Updating project with id: {}", id);
        rejectDemoEntityMutation(id);
        projectAccessScopeService.assertCurrentUserCanAccessProject(id);
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));
        ProjectDTO normalized = validateAndNormalizeProjectDTO(projectDTO, false);
        applyProjectUpdates(existingProject, normalized);
        Project updatedProject = projectRepository.save(existingProject);
        log.info("Updated project with id: {}", id);
        return convertToDTO(updatedProject);
    }

    public void deleteProject(Long id) {
        log.debug("Deleting project with id: {}", id);
        rejectDemoEntityMutation(id);
        projectAccessScopeService.assertCurrentUserCanAccessProject(id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));
        projectRepository.delete(project);
        log.info("Deleted project with id: {}", id);
    }

    @Auditable(action = "UPDATE_STATUS", entityType = "Project", description = "更新项目状态")
    public ProjectDTO updateProjectStatus(Long id, Project.Status status) {
        log.debug("Updating status for project with id: {} to {}", id, status);
        rejectDemoEntityMutation(id);
        projectAccessScopeService.assertCurrentUserCanAccessProject(id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));
        changeProjectStatus(project, status);
        Project updatedProject = projectRepository.save(project);
        log.info("Updated project {} status to {}", id, status);
        return convertToDTO(updatedProject);
    }

    @Auditable(action = "UPDATE_TEAM", entityType = "Project", description = "更新项目团队成员")
    public ProjectDTO updateProjectTeam(Long id, List<Long> teamMembers) {
        log.debug("Updating team for project with id: {}", id);
        rejectDemoEntityMutation(id);
        projectAccessScopeService.assertCurrentUserCanAccessProject(id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));
        project.setTeamMembers(normalizeTeamMembers(teamMembers));
        Project updatedProject = projectRepository.save(project);
        log.info("Updated project {} team members", id);
        return convertToDTO(updatedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByStatus(Project.Status status) {
        log.debug("Fetching projects by status: {}", status);
        List<ProjectDTO> projects = projectAccessScopeService.filterAccessibleProjects(projectRepository.findByStatus(status)).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return mergeDemoProjectsIfNeeded(projects).stream()
                .filter(item -> item.getStatus() == status)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByManager(Long managerId) {
        log.debug("Fetching projects by manager: {}", managerId);
        List<ProjectDTO> projects = projectAccessScopeService.filterAccessibleProjects(projectRepository.findByManagerId(managerId)).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return mergeDemoProjectsIfNeeded(projects).stream()
                .filter(item -> managerId.equals(item.getManagerId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByTender(Long tenderId) {
        log.debug("Fetching projects by tender: {}", tenderId);
        List<ProjectDTO> projects = projectAccessScopeService.filterAccessibleProjects(projectRepository.findByTenderId(tenderId)).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return mergeDemoProjectsIfNeeded(projects).stream()
                .filter(item -> tenderId.equals(item.getTenderId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getActiveProjects() {
        log.debug("Fetching active projects");
        List<ProjectDTO> projects = projectAccessScopeService.filterAccessibleProjects(projectRepository.findActiveProjects()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return mergeDemoProjectsIfNeeded(projects).stream()
                .filter(item -> item.getStatus() != Project.Status.ARCHIVED)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> searchProjectsByName(String name) {
        log.debug("Searching projects by name: {}", name);
        List<ProjectDTO> projects = projectAccessScopeService.filterAccessibleProjects(projectRepository.findByNameContainingIgnoreCase(name)).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        String normalizedKeyword = name == null ? "" : name.toLowerCase();
        return mergeDemoProjectsIfNeeded(projects).stream()
                .filter(item -> item.getName() != null && item.getName().toLowerCase().contains(normalizedKeyword))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Project.Status, Long> getProjectStatistics() {
        log.debug("Fetching project statistics");
        List<Project> visibleProjects = new ArrayList<>(projectAccessScopeService.filterAccessibleProjects(projectRepository.findAll()));
        if (demoModeService.isEnabled()) {
            demoDataProvider.getDemoProjects().forEach(demo -> {
                Project project = new Project();
                project.setStatus(demo.getStatus());
                visibleProjects.add(project);
            });
        }
        return Map.of(
            Project.Status.INITIATED, countProjectsByStatus(visibleProjects, Project.Status.INITIATED),
            Project.Status.PREPARING, countProjectsByStatus(visibleProjects, Project.Status.PREPARING),
            Project.Status.REVIEWING, countProjectsByStatus(visibleProjects, Project.Status.REVIEWING),
            Project.Status.SEALING, countProjectsByStatus(visibleProjects, Project.Status.SEALING),
            Project.Status.BIDDING, countProjectsByStatus(visibleProjects, Project.Status.BIDDING),
            Project.Status.ARCHIVED, countProjectsByStatus(visibleProjects, Project.Status.ARCHIVED)
        );
    }

    private ProjectDTO convertToDTO(Project project) {
        return ProjectDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .tenderId(project.getTenderId())
                .status(project.getStatus())
                .managerId(project.getManagerId())
                .teamMembers(project.getTeamMembers() == null ? List.of() : new ArrayList<>(project.getTeamMembers()))
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .sourceModule(project.getSourceModule())
                .sourceCustomerId(project.getSourceCustomerId())
                .sourceCustomer(project.getSourceCustomer())
                .sourceOpportunityId(project.getSourceOpportunityId())
                .sourceReasoningSummary(project.getSourceReasoningSummary())
                .competitorAnalysisJson(project.getCompetitorAnalysisJson())
                .tasksJson(project.getTasksJson())
                .aiAnalysisJson(project.getAiAnalysisJson())
                .customer(project.getCustomer())
                .budget(project.getBudget())
                .industry(project.getIndustry())
                .customerType(project.getCustomerType())
                .region(project.getRegion())
                .platform(project.getPlatform())
                .deadline(project.getDeadline())
                .description(project.getDescription())
                .remark(project.getRemark())
                .tagsJson(project.getTagsJson())
                .customerManager(project.getCustomerManager())
                .customerManagerId(project.getCustomerManagerId())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private Project convertToEntity(ProjectDTO dto) {
        return Project.builder()
                .id(dto.getId())
                .name(dto.getName())
                .tenderId(dto.getTenderId())
                .status(dto.getStatus() != null ? dto.getStatus() : Project.Status.INITIATED)
                .managerId(dto.getManagerId())
                .teamMembers(dto.getTeamMembers() != null ? dto.getTeamMembers() : List.of())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .sourceModule(dto.getSourceModule())
                .sourceCustomerId(dto.getSourceCustomerId())
                .sourceCustomer(dto.getSourceCustomer())
                .sourceOpportunityId(dto.getSourceOpportunityId())
                .sourceReasoningSummary(dto.getSourceReasoningSummary())
                .competitorAnalysisJson(dto.getCompetitorAnalysisJson())
                .tasksJson(dto.getTasksJson())
                .aiAnalysisJson(dto.getAiAnalysisJson())
                .customer(dto.getCustomer())
                .budget(dto.getBudget())
                .industry(dto.getIndustry())
                .customerType(dto.getCustomerType())
                .region(dto.getRegion())
                .platform(dto.getPlatform())
                .deadline(dto.getDeadline())
                .description(dto.getDescription())
                .remark(dto.getRemark())
                .tagsJson(dto.getTagsJson())
                .customerManager(dto.getCustomerManager())
                .customerManagerId(dto.getCustomerManagerId())
                .build();
    }

    private void applyProjectUpdates(Project project, ProjectDTO updates) {
        if (updates.getName() != null) project.setName(updates.getName());
        if (updates.getTenderId() != null) project.setTenderId(updates.getTenderId());
        if (updates.getStatus() != null) changeProjectStatus(project, updates.getStatus());
        if (updates.getManagerId() != null) project.setManagerId(updates.getManagerId());
        if (updates.getStartDate() != null) project.setStartDate(updates.getStartDate());
        if (updates.getEndDate() != null) project.setEndDate(updates.getEndDate());
        if (updates.getTeamMembers() != null) project.setTeamMembers(normalizeTeamMembers(updates.getTeamMembers()));
        if (updates.getSourceModule() != null) project.setSourceModule(updates.getSourceModule());
        if (updates.getSourceCustomerId() != null) project.setSourceCustomerId(updates.getSourceCustomerId());
        if (updates.getSourceCustomer() != null) project.setSourceCustomer(updates.getSourceCustomer());
        if (updates.getSourceOpportunityId() != null) project.setSourceOpportunityId(updates.getSourceOpportunityId());
        if (updates.getSourceReasoningSummary() != null) project.setSourceReasoningSummary(updates.getSourceReasoningSummary());
        if (updates.getCompetitorAnalysisJson() != null) project.setCompetitorAnalysisJson(updates.getCompetitorAnalysisJson());
        if (updates.getTasksJson() != null) project.setTasksJson(updates.getTasksJson());
        if (updates.getAiAnalysisJson() != null) project.setAiAnalysisJson(updates.getAiAnalysisJson());
        project.setCustomer(updates.getCustomer());
        project.setBudget(updates.getBudget());
        project.setIndustry(updates.getIndustry());
        project.setCustomerType(updates.getCustomerType());
        project.setRegion(updates.getRegion());
        project.setPlatform(updates.getPlatform());
        project.setDeadline(updates.getDeadline());
        project.setDescription(updates.getDescription());
        project.setRemark(updates.getRemark());
        project.setTagsJson(updates.getTagsJson());
        if (updates.getCustomerManager() != null) project.setCustomerManager(updates.getCustomerManager());
        if (updates.getCustomerManagerId() != null) project.setCustomerManagerId(updates.getCustomerManagerId());
    }

    private ProjectDTO validateAndNormalizeProjectDTO(ProjectDTO projectDTO, boolean creating) {
        if (projectDTO == null) throw new IllegalArgumentException("Project payload is required");
        if (projectDTO.getName() == null || projectDTO.getName().trim().isEmpty()) throw new IllegalArgumentException("Project name is required");
        if (projectDTO.getTenderId() == null) throw new IllegalArgumentException("Tender ID is required");
        if (projectDTO.getManagerId() == null) throw new IllegalArgumentException("Manager ID is required");
        if (creating && projectDTO.getStatus() == Project.Status.ARCHIVED) throw new IllegalArgumentException("New projects cannot be created directly in ARCHIVED status");

        Project.Status normalizedStatus = projectDTO.getStatus();
        if (creating && normalizedStatus == null) {
            normalizedStatus = Project.Status.INITIATED;
        }

        List<Long> normalizedTeamMembers = null;
        if (creating || projectDTO.getTeamMembers() != null) {
            normalizedTeamMembers = normalizeTeamMembers(projectDTO.getTeamMembers());
        }

        return ProjectDTO.builder()
                .id(projectDTO.getId())
                .name(projectDTO.getName().trim())
                .tenderId(projectDTO.getTenderId())
                .status(normalizedStatus)
                .managerId(projectDTO.getManagerId())
                .teamMembers(normalizedTeamMembers)
                .startDate(projectDTO.getStartDate())
                .endDate(projectDTO.getEndDate())
                .sourceModule(trimToNull(projectDTO.getSourceModule()))
                .sourceCustomerId(trimToNull(projectDTO.getSourceCustomerId()))
                .sourceCustomer(trimToNull(projectDTO.getSourceCustomer()))
                .sourceOpportunityId(trimToNull(projectDTO.getSourceOpportunityId()))
                .sourceReasoningSummary(trimToNull(projectDTO.getSourceReasoningSummary()))
                .competitorAnalysisJson(projectDTO.getCompetitorAnalysisJson())
                .tasksJson(projectDTO.getTasksJson())
                .aiAnalysisJson(projectDTO.getAiAnalysisJson())
                .customer(trimToNull(projectDTO.getCustomer()))
                .budget(projectDTO.getBudget())
                .industry(trimToNull(projectDTO.getIndustry()))
                .customerType(trimToNull(projectDTO.getCustomerType()))
                .region(trimToNull(projectDTO.getRegion()))
                .platform(trimToNull(projectDTO.getPlatform()))
                .deadline(projectDTO.getDeadline())
                .description(trimToNull(projectDTO.getDescription()))
                .remark(trimToNull(projectDTO.getRemark()))
                .tagsJson(trimToNull(projectDTO.getTagsJson()))
                .customerManager(trimToNull(projectDTO.getCustomerManager()))
                .customerManagerId(trimToNull(projectDTO.getCustomerManagerId()))
                .build();
    }

    private void changeProjectStatus(Project project, Project.Status nextStatus) {
        if (nextStatus == null) return;
        Project.Status currentStatus = project.getStatus();
        if (currentStatus == nextStatus) return;
        Set<Project.Status> allowedTargets = ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(nextStatus)) throw new IllegalArgumentException("Invalid project status transition from " + currentStatus + " to " + nextStatus);
        project.setStatus(nextStatus);
    }

    private List<Long> normalizeTeamMembers(List<Long> teamMembers) {
        if (teamMembers == null) return List.of();
        return teamMembers.stream().filter(Objects::nonNull).collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long countProjectsByStatus(List<Project> projects, Project.Status status) {
        return projects.stream().filter(project -> project.getStatus() == status).count();
    }

    private List<ProjectDTO> mergeDemoProjectsIfNeeded(List<ProjectDTO> projects) {
        if (!demoModeService.isEnabled()) {
            return projects;
        }
        return demoFusionService.mergeByKey(projects, demoDataProvider.getDemoProjects(), ProjectDTO::getId);
    }

    private boolean isDemoEntityId(Long id) {
        return demoModeService.isEnabled() && id != null && id < 0;
    }

    private void rejectDemoEntityMutation(Long id) {
        if (isDemoEntityId(id)) {
            throw new IllegalArgumentException("Demo records are read-only in e2e mode");
        }
    }
}
