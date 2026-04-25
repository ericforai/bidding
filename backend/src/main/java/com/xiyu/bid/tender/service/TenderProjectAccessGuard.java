// Input: tender ids and linked projects
// Output: tender visibility decisions backed by project data permissions
// Pos: Service/权限支撑层
// 维护声明: 仅维护标讯与关联项目的数据权限判断；标讯业务流转留在命令/查询服务。
package com.xiyu.bid.tender.service;

import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class TenderProjectAccessGuard {

    private final ProjectRepository projectRepository;
    private final ProjectAccessScopeService projectAccessScopeService;

    void assertCanAccessTender(Tender tender) {
        for (Project project : linkedProjects(tender)) {
            projectAccessScopeService.assertCurrentUserCanAccessProject(project.getId());
        }
    }

    List<Tender> filterVisibleTenders(List<Tender> tenders) {
        return tenders.stream()
                .filter(this::canAccessTender)
                .toList();
    }

    private boolean canAccessTender(Tender tender) {
        try {
            assertCanAccessTender(tender);
            return true;
        } catch (AccessDeniedException exception) {
            return false;
        }
    }

    private List<Project> linkedProjects(Tender tender) {
        if (tender == null || tender.getId() == null) {
            return List.of();
        }
        List<Project> projects = projectRepository.findByTenderId(tender.getId());
        return projects == null ? List.of() : projects;
    }
}
