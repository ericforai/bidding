package com.xiyu.bid.security.service;

import com.xiyu.bid.dto.ProjectMemberDTO;
import com.xiyu.bid.dto.ProjectMemberRequest;
import com.xiyu.bid.entity.ProjectMember;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.ProjectMemberRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProjectMemberDTO> getProjectMembers(Long projectId) {
        log.info("Fetching members for project: {}", projectId);
        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectMemberDTO addProjectMember(Long projectId, ProjectMemberRequest request) {
        log.info("Adding user {} to project: {}", request.getUserId(), projectId);
        
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, request.getUserId())
                .orElse(ProjectMember.builder()
                        .projectId(projectId)
                        .userId(request.getUserId())
                        .build());
        
        member.setMemberRole(request.getMemberRole());
        member.setPermissionLevel(request.getPermissionLevel());
        member.setInherited(false);
        
        return convertToDTO(projectMemberRepository.save(member));
    }

    @Transactional
    public void removeProjectMember(Long projectId, Long userId) {
        log.info("Removing user {} from project: {}", userId, projectId);
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    private ProjectMemberDTO convertToDTO(ProjectMember entity) {
        User user = userRepository.findById(entity.getUserId()).orElse(null);
        return ProjectMemberDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .userId(entity.getUserId())
                .username(user != null ? user.getUsername() : "UNKNOWN")
                .fullName(user != null ? user.getFullName() : "未知用户")
                .memberRole(entity.getMemberRole())
                .permissionLevel(entity.getPermissionLevel())
                .isInherited(entity.isInherited())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
