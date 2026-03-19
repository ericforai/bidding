package com.xiyu.bid.analytics.repository;

import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
@Transactional(readOnly = true)
public class DashboardAnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public OverviewSnapshot fetchOverviewSnapshot() {
        Long totalTenders = entityManager.createQuery("select count(t) from Tender t", Long.class)
                .getSingleResult();
        BigDecimal totalBudget = entityManager.createQuery("select sum(t.budget) from Tender t", BigDecimal.class)
                .getSingleResult();
        Long activeProjects = entityManager.createQuery(
                        "select count(p) from Project p where p.status <> :archived",
                        Long.class)
                .setParameter("archived", Project.Status.ARCHIVED)
                .getSingleResult();
        Long pendingTasks = entityManager.createQuery(
                        "select count(t) from Task t where t.status = :todo",
                        Long.class)
                .setParameter("todo", com.xiyu.bid.entity.Task.Status.TODO)
                .getSingleResult();
        Long bidTenderCount = entityManager.createQuery(
                        "select count(t) from Tender t where t.status = :bidded",
                        Long.class)
                .setParameter("bidded", Tender.Status.BIDDED)
                .getSingleResult();
        Long winningProjectCount = entityManager.createQuery(
                        "select count(p) from Project p where p.status in :winningStatuses",
                        Long.class)
                .setParameter(
                        "winningStatuses",
                        List.of(Project.Status.BIDDING, Project.Status.REVIEWING, Project.Status.SEALING)
                )
                .getSingleResult();

        return new OverviewSnapshot(
                totalTenders == null ? 0L : totalTenders,
                totalBudget == null ? BigDecimal.ZERO : totalBudget,
                activeProjects == null ? 0L : activeProjects,
                pendingTasks == null ? 0L : pendingTasks,
                bidTenderCount == null ? 0L : bidTenderCount,
                winningProjectCount == null ? 0L : winningProjectCount
        );
    }

    public List<MonthlyTrendRow> fetchTenderTrends() {
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$MonthlyTrendRow(
                            year(t.createdAt),
                            month(t.createdAt),
                            count(t),
                            sum(t.budget)
                        )
                        from Tender t
                        group by year(t.createdAt), month(t.createdAt)
                        order by year(t.createdAt), month(t.createdAt)
                        """, MonthlyTrendRow.class)
                .getResultList();
    }

    public List<MonthlyTrendRow> fetchProjectTrends() {
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$MonthlyTrendRow(
                            year(p.createdAt),
                            month(p.createdAt),
                            count(p),
                            null
                        )
                        from Project p
                        group by year(p.createdAt), month(p.createdAt)
                        order by year(p.createdAt), month(p.createdAt)
                        """, MonthlyTrendRow.class)
                .getResultList();
    }

    public List<StatusCountRow> fetchStatusDistribution() {
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$StatusCountRow(
                            t.status,
                            count(t)
                        )
                        from Tender t
                        group by t.status
                        """, StatusCountRow.class)
                .getResultList();
    }

    public List<SourceAggregateRow> fetchSourceAggregates(int limit) {
        var query = entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$SourceAggregateRow(
                            t.source,
                            count(t),
                            sum(case when t.status = :bidded then 1 else 0 end),
                            sum(t.budget)
                        )
                        from Tender t
                        where t.source is not null
                        group by t.source
                        order by count(t) desc, t.source asc
                        """, SourceAggregateRow.class)
                .setParameter("bidded", Tender.Status.BIDDED);
        if (limit > 0 && limit < Integer.MAX_VALUE) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    public List<RevenueDrillDownRow> fetchRevenueDrillDownRows(LocalDate startDate, LocalDate endDate) {
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$RevenueDrillDownRow(
                            t.id,
                            p.id,
                            t.title,
                            t.source,
                            t.status,
                            p.name,
                            p.status,
                            p.managerId,
                            u.fullName,
                            t.budget,
                            t.aiScore,
                            t.createdAt,
                            t.deadline
                        )
                        from Tender t
                        left join Project p on p.tenderId = t.id
                        left join User u on u.id = p.managerId
                        where (:startDate is null or t.createdAt >= :startDate)
                          and (:endDate is null or t.createdAt <= :endDate)
                        order by t.createdAt desc, t.id desc
                        """, RevenueDrillDownRow.class)
                .setParameter("startDate", startDate == null ? null : startDate.atStartOfDay())
                .setParameter("endDate", endDate == null ? null : endDate.atTime(23, 59, 59))
                .getResultList();
    }

    public List<ProjectDrillDownRow> fetchProjectDrillDownRows(LocalDate startDate, LocalDate endDate) {
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$ProjectDrillDownRow(
                            p.id,
                            p.tenderId,
                            p.name,
                            t.title,
                            p.status,
                            p.managerId,
                            u.fullName,
                            t.budget,
                            coalesce(p.startDate, p.createdAt),
                            p.endDate,
                            size(p.teamMembers)
                        )
                        from Project p
                        left join Tender t on t.id = p.tenderId
                        left join User u on u.id = p.managerId
                        where (:startDate is null or coalesce(p.startDate, p.createdAt) >= :startDate)
                          and (:endDate is null or coalesce(p.startDate, p.createdAt) <= :endDate)
                        order by coalesce(p.startDate, p.createdAt) desc, p.id desc
                        """, ProjectDrillDownRow.class)
                .setParameter("startDate", startDate == null ? null : startDate.atStartOfDay())
                .setParameter("endDate", endDate == null ? null : endDate.atTime(23, 59, 59))
                .getResultList();
    }

    public List<ProductLineCandidateRow> fetchProductLineCandidateRows() {
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$ProductLineCandidateRow(
                            t.title,
                            t.status,
                            t.budget
                        )
                        from Tender t
                        """, ProductLineCandidateRow.class)
                .getResultList();
    }

    public List<TenderSummaryRow> fetchTenderSummaryRows() {
        return entityManager.createQuery("""
                        select new com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository$TenderSummaryRow(
                            t.id,
                            t.title,
                            t.source,
                            t.status,
                            t.budget,
                            t.createdAt,
                            t.deadline
                        )
                        from Tender t
                        order by t.createdAt desc, t.id desc
                        """, TenderSummaryRow.class)
                .getResultList();
    }

    public List<ProjectSnapshotRow> fetchProjectSnapshotRowsByTenderIds(Collection<Long> tenderIds) {
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
                        """, ProjectSnapshotRow.class)
                .setParameter("tenderIds", tenderIds)
                .getResultList();
    }

    public List<ProjectSnapshotRow> fetchProjectSnapshotRowsByDateRange(LocalDate startDate, LocalDate endDate) {
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
                        """, ProjectSnapshotRow.class)
                .setParameter("startDate", startDate == null ? null : startDate.atStartOfDay())
                .setParameter("endDate", endDate == null ? null : endDate.atTime(23, 59, 59))
                .getResultList();
    }

    public List<TaskSnapshotRow> fetchTaskSnapshotRows(Set<Long> projectIds) {
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
                        """, TaskSnapshotRow.class)
                .setParameter("projectIds", projectIds)
                .getResultList();
    }

    public List<ProjectDocumentRow> fetchProjectDocumentRows(Set<Long> projectIds) {
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
                        """, ProjectDocumentRow.class)
                .setParameter("projectIds", projectIds)
                .getResultList();
    }

    public List<DocumentExportRow> fetchDocumentExportRows(Set<Long> projectIds) {
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
                        """, DocumentExportRow.class)
                .setParameter("projectIds", projectIds)
                .getResultList();
    }

    public record OverviewSnapshot(
            Long totalTenders,
            BigDecimal totalBudget,
            Long activeProjects,
            Long pendingTasks,
            Long biddedTenders,
            Long winningProjects
    ) {
    }

    public record MonthlyTrendRow(Integer year, Integer month, Long count, BigDecimal totalValue) {
    }

    public record StatusCountRow(Tender.Status status, Long count) {
    }

    public record SourceAggregateRow(String source, Long bidCount, Long winCount, BigDecimal totalBidAmount) {
    }

    public record RevenueDrillDownRow(
            Long tenderId,
            Long projectId,
            String title,
            String source,
            Tender.Status tenderStatus,
            String projectName,
            Project.Status projectStatus,
            Long managerId,
            String managerName,
            BigDecimal budget,
            Integer score,
            LocalDateTime createdAt,
            LocalDateTime deadline
    ) {
    }

    public record ProjectDrillDownRow(
            Long projectId,
            Long tenderId,
            String projectName,
            String tenderTitle,
            Project.Status projectStatus,
            Long managerId,
            String managerName,
            BigDecimal budget,
            LocalDateTime referenceDate,
            LocalDateTime endDate,
            Integer teamSize
    ) {
    }

    public record ProductLineCandidateRow(
            String title,
            Tender.Status status,
            BigDecimal budget
    ) {
    }

    public record TenderSummaryRow(
            Long tenderId,
            String title,
            String source,
            Tender.Status status,
            BigDecimal budget,
            LocalDateTime createdAt,
            LocalDateTime deadline
    ) {
    }

    public record ProjectSnapshotRow(
            Long projectId,
            Long tenderId,
            String projectName,
            Project.Status projectStatus,
            Long managerId,
            String managerName,
            String tenderSource,
            BigDecimal budget,
            LocalDateTime referenceDate,
            LocalDateTime endDate,
            Long teamMemberId
    ) {
    }

    public record TaskSnapshotRow(
            Long projectId,
            Long assigneeId,
            com.xiyu.bid.entity.Task.Status status,
            LocalDateTime dueDate
    ) {
    }

    public record ProjectDocumentRow(
            Long projectId,
            Long documentId,
            String name,
            String uploaderName,
            LocalDateTime createdAt,
            String size
    ) {
    }

    public record DocumentExportRow(
            Long projectId,
            Long exportId,
            String fileName,
            String exportedByName,
            LocalDateTime exportedAt,
            Long fileSize,
            String format
    ) {
    }
}
