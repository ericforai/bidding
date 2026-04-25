package com.xiyu.bid.analytics.service;

import com.xiyu.bid.analytics.model.ProjectSnapshotAggregate;
import com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository;
import com.xiyu.bid.analytics.repository.DashboardAnalyticsReadRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class DashboardAnalyticsQueryService {

    private final DashboardAnalyticsReadRepository readRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessScopeService projectAccessScopeService;

    DashboardAnalyticsRepository.OverviewSnapshot fetchOverviewSnapshot() {
        Set<Long> projectIds = scopedProjectIds();
        return readRepository.fetchOverviewSnapshot(projectIds);
    }

    List<DashboardAnalyticsRepository.MonthlyTrendRow> fetchTenderTrendRows() {
        return readRepository.fetchTenderTrendRows(scopedProjectIds());
    }

    List<DashboardAnalyticsRepository.MonthlyTrendRow> fetchProjectTrendRows() {
        return readRepository.fetchProjectTrendRows(scopedProjectIds());
    }

    List<DashboardAnalyticsRepository.StatusCountRow> fetchStatusCounts() {
        return readRepository.fetchStatusCounts(scopedProjectIds());
    }

    List<DashboardAnalyticsRepository.SourceAggregateRow> fetchSourceAggregateRows(int limit) {
        return readRepository.fetchSourceAggregateRows(scopedProjectIds(), limit);
    }

    List<DashboardAnalyticsRepository.ProductLineCandidateRow> fetchProductLineCandidateRows() {
        return readRepository.fetchProductLineCandidateRows(scopedProjectIds());
    }

    List<DashboardAnalyticsRepository.TenderSummaryRow> fetchTenderSummaryRows() {
        return readRepository.fetchTenderSummaryRows(scopedProjectIds());
    }

    List<ProjectSnapshotAggregate> fetchProjectSnapshotsByTenderIds(Collection<Long> tenderIds) {
        return aggregateProjectSnapshots(readRepository.fetchProjectSnapshotRowsByTenderIds(
                scopedProjectIds(),
                tenderIds
        ));
    }

    List<ProjectSnapshotAggregate> fetchProjectSnapshotsByDateRange(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {
        return aggregateProjectSnapshots(readRepository.fetchProjectSnapshotRowsByDateRange(
                scopedProjectIds(),
                startDate,
                endDate
        ));
    }

    List<DashboardAnalyticsRepository.TaskSnapshotRow> fetchTaskSnapshots(Set<Long> projectIds) {
        return readRepository.fetchTaskSnapshotRows(projectIds);
    }

    List<DashboardAnalyticsRepository.ProjectDocumentRow> fetchProjectDocuments(Set<Long> projectIds) {
        return readRepository.fetchProjectDocumentRows(projectIds);
    }

    List<DashboardAnalyticsRepository.DocumentExportRow> fetchDocumentExports(Set<Long> projectIds) {
        return readRepository.fetchDocumentExportRows(projectIds);
    }

    List<DashboardAnalyticsRepository.RevenueDrillDownRow> fetchRevenueDrillDownRows(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {
        return readRepository.fetchRevenueDrillDownRows(scopedProjectIds(), startDate, endDate);
    }

    List<DashboardAnalyticsRepository.ProjectDrillDownRow> fetchProjectDrillDownRows(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {
        return readRepository.fetchProjectDrillDownRows(scopedProjectIds(), startDate, endDate);
    }

    Map<Long, User> fetchUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds.stream().filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, java.util.LinkedHashMap::new));
    }

    Set<Long> collectProjectUserIds(List<ProjectSnapshotAggregate> projects) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (ProjectSnapshotAggregate project : projects) {
            if (project.managerId() != null) {
                userIds.add(project.managerId());
            }
            userIds.addAll(project.teamMemberIds());
        }
        return userIds;
    }

    private List<ProjectSnapshotAggregate> aggregateProjectSnapshots(
            List<DashboardAnalyticsRepository.ProjectSnapshotRow> rows
    ) {
        Map<Long, ProjectSnapshotAggregate> aggregates = rows.stream()
                .collect(Collectors.toMap(
                        DashboardAnalyticsRepository.ProjectSnapshotRow::projectId,
                        row -> ProjectSnapshotAggregate.create(
                                row.projectId(),
                                row.tenderId(),
                                row.projectName(),
                                row.projectStatus(),
                                row.managerId(),
                                row.managerName(),
                                row.tenderSource(),
                                row.budget(),
                                row.referenceDate(),
                                row.endDate()
                        ),
                        (left, right) -> left
                ));

        for (DashboardAnalyticsRepository.ProjectSnapshotRow row : rows) {
            if (row.teamMemberId() != null) {
                ProjectSnapshotAggregate existing = aggregates.get(row.projectId());
                if (existing != null) {
                    aggregates.put(row.projectId(), existing.withTeamMember(row.teamMemberId()));
                }
            }
        }
        return aggregates.values().stream().toList();
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
