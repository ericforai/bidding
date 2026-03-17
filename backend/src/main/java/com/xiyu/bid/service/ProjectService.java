// Input: Repository, 相关依赖
// Output: 业务服务、数据操作
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.dto.ProjectDTO;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private final ProjectRepository projectRepository;

    /**
     * 获取所有项目
     */
    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        log.debug("Fetching all projects");
        return projectRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取项目
     */
    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long id) {
        log.debug("Fetching project by id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));
        return convertToDTO(project);
    }

    /**
     * 创建项目
     */
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        log.debug("Creating new project: {}", projectDTO.getName());
        Project project = convertToEntity(projectDTO);
        Project savedProject = projectRepository.save(project);
        log.info("Created project with id: {}", savedProject.getId());
        return convertToDTO(savedProject);
    }

    /**
     * 更新项目
     */
    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        log.debug("Updating project with id: {}", id);
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));

        // Update fields
        if (projectDTO.getName() != null) {
            existingProject.setName(projectDTO.getName());
        }
        if (projectDTO.getTenderId() != null) {
            existingProject.setTenderId(projectDTO.getTenderId());
        }
        if (projectDTO.getStatus() != null) {
            existingProject.setStatus(projectDTO.getStatus());
        }
        if (projectDTO.getManagerId() != null) {
            existingProject.setManagerId(projectDTO.getManagerId());
        }
        if (projectDTO.getStartDate() != null) {
            existingProject.setStartDate(projectDTO.getStartDate());
        }
        if (projectDTO.getEndDate() != null) {
            existingProject.setEndDate(projectDTO.getEndDate());
        }
        if (projectDTO.getTeamMembers() != null) {
            existingProject.setTeamMembers(projectDTO.getTeamMembers());
        }

        Project updatedProject = projectRepository.save(existingProject);
        log.info("Updated project with id: {}", id);
        return convertToDTO(updatedProject);
    }

    /**
     * 删除项目
     */
    public void deleteProject(Long id) {
        log.debug("Deleting project with id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));
        projectRepository.delete(project);
        log.info("Deleted project with id: {}", id);
    }

    /**
     * 更新项目状态
     */
    @Auditable(
        action = "UPDATE_STATUS",
        entityType = "Project",
        description = "更新项目状态"
    )
    public ProjectDTO updateProjectStatus(Long id, Project.Status status) {
        log.debug("Updating status for project with id: {} to {}", id, status);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));

        project.setStatus(status);
        Project updatedProject = projectRepository.save(project);
        log.info("Updated project {} status to {}", id, status);
        return convertToDTO(updatedProject);
    }

    /**
     * 更新项目团队
     */
    @Auditable(
        action = "UPDATE_TEAM",
        entityType = "Project",
        description = "更新项目团队成员"
    )
    public ProjectDTO updateProjectTeam(Long id, List<Long> teamMembers) {
        log.debug("Updating team for project with id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id.toString()));

        project.setTeamMembers(teamMembers);
        Project updatedProject = projectRepository.save(project);
        log.info("Updated project {} team members", id);
        return convertToDTO(updatedProject);
    }

    /**
     * 根据状态获取项目
     */
    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByStatus(Project.Status status) {
        log.debug("Fetching projects by status: {}", status);
        return projectRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据项目经理获取项目
     */
    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByManager(Long managerId) {
        log.debug("Fetching projects by manager: {}", managerId);
        return projectRepository.findByManagerId(managerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据标讯ID获取项目
     */
    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByTender(Long tenderId) {
        log.debug("Fetching projects by tender: {}", tenderId);
        return projectRepository.findByTenderId(tenderId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取活跃项目
     */
    @Transactional(readOnly = true)
    public List<ProjectDTO> getActiveProjects() {
        log.debug("Fetching active projects");
        return projectRepository.findActiveProjects().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据名称搜索项目
     */
    @Transactional(readOnly = true)
    public List<ProjectDTO> searchProjectsByName(String name) {
        log.debug("Searching projects by name: {}", name);
        return projectRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取项目统计数据
     */
    @Transactional(readOnly = true)
    public Map<Project.Status, Long> getProjectStatistics() {
        log.debug("Fetching project statistics");
        return Map.of(
            Project.Status.INITIATED, projectRepository.countByStatus(Project.Status.INITIATED),
            Project.Status.PREPARING, projectRepository.countByStatus(Project.Status.PREPARING),
            Project.Status.REVIEWING, projectRepository.countByStatus(Project.Status.REVIEWING),
            Project.Status.SEALING, projectRepository.countByStatus(Project.Status.SEALING),
            Project.Status.BIDDING, projectRepository.countByStatus(Project.Status.BIDDING),
            Project.Status.ARCHIVED, projectRepository.countByStatus(Project.Status.ARCHIVED)
        );
    }

    /**
     * 转换为DTO
     */
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
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    /**
     * 转换为实体
     */
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
                .build();
    }
}
