package com.xiyu.bid.analytics.service;

import com.xiyu.bid.analytics.model.CustomerTypeProjectRow;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerTypeAnalyticsQueryService {

    @PersistenceContext
    private EntityManager entityManager;
    private final ProjectRepository projectRepository;
    private final ProjectAccessScopeService projectAccessScopeService;

    List<CustomerTypeProjectRow> fetchProjectRows(LocalDate startDate, LocalDate endDate) {
        Set<Long> projectIds = scopedProjectIds();
        if (projectIds != null && projectIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.model.CustomerTypeProjectRow(
                            p.id,
                            p.tenderId,
                            p.name,
                            t.title,
                            p.customer,
                            p.customerType,
                            p.status,
                            p.managerId,
                            u.fullName,
                            coalesce(p.budget, t.budget),
                            coalesce(p.startDate, p.createdAt),
                            p.endDate,
                            t.status
                        )
                        from Project p
                        left join Tender t on t.id = p.tenderId
                        left join User u on u.id = p.managerId
                        where (:allAccess = true or p.id in :projectIds)
                          and (:startDate is null or coalesce(p.startDate, p.createdAt) >= :startDate)
                          and (:endDate is null or coalesce(p.startDate, p.createdAt) <= :endDate)
                        order by coalesce(p.startDate, p.createdAt) desc, p.id desc
                        """, CustomerTypeProjectRow.class)
                .setParameter("allAccess", projectIds == null)
                .setParameter("projectIds", projectIds == null ? Set.of(-1L) : projectIds)
                .setParameter("startDate", startDate == null ? null : startDate.atStartOfDay())
                .setParameter("endDate", endDate == null ? null : endDate.atTime(23, 59, 59))
                .getResultList();
    }

    private Set<Long> scopedProjectIds() {
        if (projectAccessScopeService.currentUserHasAdminAccess()) {
            return null;
        }
        return projectAccessScopeService.filterAccessibleProjects(projectRepository.findAll()).stream()
                .map(Project::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
