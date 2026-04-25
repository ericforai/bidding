package com.xiyu.bid.analytics.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
@Transactional(readOnly = true)
public class DashboardProjectSnapshotRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<DashboardAnalyticsRepository.ProjectSnapshotRow> fetchProjectSnapshotRowsByTenderIds(
            Collection<Long> tenderIds
    ) {
        if (tenderIds == null || tenderIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$ProjectSnapshotRow(
                            p.id,
                            p.tenderId,
                            p.name,
                            p.status,
                            p.managerId,
                            u.fullName,
                            t.source,
                            t.budget,
                            coalesce(p.startDate, p.createdAt),
                            p.endDate,
                            tm
                        )
                        from Project p
                        left join Tender t on t.id = p.tenderId
                        left join User u on u.id = p.managerId
                        left join p.teamMembers tm
                        where p.tenderId in :tenderIds
                        order by coalesce(p.startDate, p.createdAt) desc, p.id desc
                        """, DashboardAnalyticsRepository.ProjectSnapshotRow.class)
                .setParameter("tenderIds", tenderIds)
                .getResultList();
    }

    public List<DashboardAnalyticsRepository.ProjectSnapshotRow> fetchProjectSnapshotRowsByTenderIdsAndProjectIds(
            Collection<Long> tenderIds,
            Set<Long> projectIds
    ) {
        if (tenderIds == null || tenderIds.isEmpty() || projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$ProjectSnapshotRow(
                            p.id,
                            p.tenderId,
                            p.name,
                            p.status,
                            p.managerId,
                            u.fullName,
                            t.source,
                            t.budget,
                            coalesce(p.startDate, p.createdAt),
                            p.endDate,
                            tm
                        )
                        from Project p
                        left join Tender t on t.id = p.tenderId
                        left join User u on u.id = p.managerId
                        left join p.teamMembers tm
                        where p.id in :projectIds and p.tenderId in :tenderIds
                        order by coalesce(p.startDate, p.createdAt) desc, p.id desc
                        """, DashboardAnalyticsRepository.ProjectSnapshotRow.class)
                .setParameter("projectIds", projectIds)
                .setParameter("tenderIds", tenderIds)
                .getResultList();
    }

    public List<DashboardAnalyticsRepository.ProjectSnapshotRow> fetchProjectSnapshotRowsByDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$ProjectSnapshotRow(
                            p.id,
                            p.tenderId,
                            p.name,
                            p.status,
                            p.managerId,
                            u.fullName,
                            t.source,
                            t.budget,
                            coalesce(p.startDate, p.createdAt),
                            p.endDate,
                            tm
                        )
                        from Project p
                        left join Tender t on t.id = p.tenderId
                        left join User u on u.id = p.managerId
                        left join p.teamMembers tm
                        where (:startDate is null or coalesce(p.startDate, p.createdAt) >= :startDate)
                          and (:endDate is null or coalesce(p.startDate, p.createdAt) <= :endDate)
                        order by coalesce(p.startDate, p.createdAt) desc, p.id desc
                        """, DashboardAnalyticsRepository.ProjectSnapshotRow.class)
                .setParameter("startDate", startDate == null ? null : startDate.atStartOfDay())
                .setParameter("endDate", endDate == null ? null : endDate.atTime(23, 59, 59))
                .getResultList();
    }

    public List<DashboardAnalyticsRepository.ProjectSnapshotRow> fetchProjectSnapshotRowsByProjectIdsAndDateRange(
            Set<Long> projectIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$ProjectSnapshotRow(
                            p.id,
                            p.tenderId,
                            p.name,
                            p.status,
                            p.managerId,
                            u.fullName,
                            t.source,
                            t.budget,
                            coalesce(p.startDate, p.createdAt),
                            p.endDate,
                            tm
                        )
                        from Project p
                        left join Tender t on t.id = p.tenderId
                        left join User u on u.id = p.managerId
                        left join p.teamMembers tm
                        where p.id in :projectIds
                          and (:startDate is null or coalesce(p.startDate, p.createdAt) >= :startDate)
                          and (:endDate is null or coalesce(p.startDate, p.createdAt) <= :endDate)
                        order by coalesce(p.startDate, p.createdAt) desc, p.id desc
                        """, DashboardAnalyticsRepository.ProjectSnapshotRow.class)
                .setParameter("projectIds", projectIds)
                .setParameter("startDate", startDate == null ? null : startDate.atStartOfDay())
                .setParameter("endDate", endDate == null ? null : endDate.atTime(23, 59, 59))
                .getResultList();
    }

    public List<DashboardAnalyticsRepository.TaskSnapshotRow> fetchTaskSnapshotRows(Set<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$TaskSnapshotRow(
                            t.projectId,
                            t.assigneeId,
                            t.status,
                            t.dueDate
                        )
                        from Task t
                        where t.projectId in :projectIds
                        order by t.createdAt desc, t.id desc
                        """, DashboardAnalyticsRepository.TaskSnapshotRow.class)
                .setParameter("projectIds", projectIds)
                .getResultList();
    }

    public List<DashboardAnalyticsRepository.ProjectDocumentRow> fetchProjectDocumentRows(Set<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$ProjectDocumentRow(
                            d.projectId,
                            d.id,
                            d.name,
                            d.uploaderName,
                            d.createdAt,
                            d.size
                        )
                        from ProjectDocument d
                        where d.projectId in :projectIds
                        order by d.createdAt desc, d.id desc
                        """, DashboardAnalyticsRepository.ProjectDocumentRow.class)
                .setParameter("projectIds", projectIds)
                .getResultList();
    }

    public List<DashboardAnalyticsRepository.DocumentExportRow> fetchDocumentExportRows(Set<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$DocumentExportRow(
                            e.projectId,
                            e.id,
                            e.fileName,
                            e.exportedByName,
                            e.exportedAt,
                            e.fileSize,
                            e.format
                        )
                        from DocumentExport e
                        where e.projectId in :projectIds
                        order by e.exportedAt desc, e.id desc
                        """, DashboardAnalyticsRepository.DocumentExportRow.class)
                .setParameter("projectIds", projectIds)
                .getResultList();
    }
}
