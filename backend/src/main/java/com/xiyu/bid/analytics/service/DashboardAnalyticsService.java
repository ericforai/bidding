package com.xiyu.bid.analytics.service;

import com.xiyu.bid.analytics.dto.AnalyticsDrillDownFileDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownProjectDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownResponse;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownResponseDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownRowDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownSummaryDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownTeamDTO;
import com.xiyu.bid.analytics.dto.CompetitorData;
import com.xiyu.bid.analytics.dto.DashboardOverviewDTO;
import com.xiyu.bid.analytics.dto.RegionalData;
import com.xiyu.bid.analytics.dto.SummaryStats;
import com.xiyu.bid.analytics.dto.TrendData;
import com.xiyu.bid.analytics.dto.ProductLineData;
import com.xiyu.bid.analytics.model.ProjectSnapshotAggregate;
import com.xiyu.bid.analytics.model.TeamAggregate;
import com.xiyu.bid.analytics.model.TeamTaskAggregate;
import com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Thin orchestration service for dashboard analytics.
 * Responsibilities are split into query/core/composition services.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardAnalyticsService {

    private final DashboardAnalyticsQueryService queryService;
    private final DashboardAnalyticsComputationService computation;
    private final DashboardAnalyticsAssemblerService assembler;

    @Cacheable(value = "dashboard:overview", key = "'overview'")
    public DashboardOverviewDTO getOverview() {
        return DashboardOverviewDTO.builder()
                .summaryStats(getSummaryStats())
                .tenderTrends(getTenderTrends())
                .projectTrends(getProjectTrends())
                .statusDistribution(getStatusDistribution())
                .topCompetitors(getTopCompetitors(5))
                .regionalDistribution(getRegionalDistribution())
                .build();
    }

    public SummaryStats getSummaryStats() {
        DashboardAnalyticsRepository.OverviewSnapshot snapshot = queryService.fetchOverviewSnapshot();
        double successRate = computation.calculateSuccessRate(snapshot);
        return assembler.buildSummaryStats(snapshot, successRate);
    }

    public List<TrendData> getTenderTrends() {
        return computation.buildTenderTrends(queryService.fetchTenderTrendRows());
    }

    public List<TrendData> getProjectTrends() {
        return computation.buildProjectTrends(queryService.fetchProjectTrendRows());
    }

    public Map<String, Long> getStatusDistribution() {
        return assembler.buildStatusDistribution(
                com.xiyu.bid.entity.Tender.Status.values(),
                computation.buildStatusDistributionCounts(queryService.fetchStatusCounts())
        );
    }

    public List<CompetitorData> getTopCompetitors(Integer limit) {
        int resolvedLimit = limit == null || limit < 1 ? 5 : limit;
        return computation.buildTopCompetitors(queryService.fetchSourceAggregateRows(resolvedLimit));
    }

    public List<RegionalData> getRegionalDistribution() {
        return computation.buildRegionalDistribution(queryService.fetchSourceAggregateRows(Integer.MAX_VALUE));
    }

    public List<ProductLineData> getProductLinePerformance() {
        return computation.buildProductLinePerformance(queryService.fetchProductLineCandidateRows());
    }

    @Transactional(readOnly = true)
    public AnalyticsDrillDownResponse getDrillDown(String type, String key) {
        List<DashboardAnalyticsRepository.TenderSummaryRow> allTenders = queryService.fetchTenderSummaryRows();
        List<DashboardAnalyticsRepository.TenderSummaryRow> matchedTenders =
                computation.filterTenderSummaryRowsByTypeAndKey(allTenders, type, key);
        Set<Long> tenderIds = matchedTenders.stream()
                .map(DashboardAnalyticsRepository.TenderSummaryRow::tenderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ProjectSnapshotAggregate> matchedProjects = queryService.fetchProjectSnapshotsByTenderIds(tenderIds);
        Set<Long> projectIds = matchedProjects.stream()
                .map(ProjectSnapshotAggregate::projectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<DashboardAnalyticsRepository.TaskSnapshotRow> matchedTasks = queryService.fetchTaskSnapshots(projectIds);
        List<DashboardAnalyticsRepository.ProjectDocumentRow> projectDocuments = queryService.fetchProjectDocuments(projectIds);
        List<DashboardAnalyticsRepository.DocumentExportRow> documentExports = queryService.fetchDocumentExports(projectIds);
        Map<Long, User> userById = queryService.fetchUsersByIds(queryService.collectProjectUserIds(matchedProjects));

        List<AnalyticsDrillDownProjectDTO> projectItems = assembler.buildProjectItems(matchedProjects, userById);
        List<AnalyticsDrillDownTeamDTO> teamItems = assembler.buildDrillDownTeamItems(matchedProjects, matchedTasks, userById);
        List<AnalyticsDrillDownFileDTO> fileItems = assembler.buildFileItems(matchedProjects, projectDocuments, documentExports);

        long totalParticipation = matchedTenders.size();
        long wonCount = matchedTenders.stream().filter(tender -> tender.status() == Tender.Status.BIDDED).count();
        double teamWinRate = totalParticipation == 0 ? 0.0 : (wonCount * 100.0) / totalParticipation;
        BigDecimal totalAmount = matchedTenders.stream()
                .map(DashboardAnalyticsRepository.TenderSummaryRow::budget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return assembler.assembleBasicDrillDownResponse(
                projectItems,
                teamItems,
                fileItems,
                totalParticipation,
                wonCount,
                teamWinRate,
                totalAmount
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsDrillDownResponseDTO getRevenueDrillDown(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Integer page,
            Integer size
    ) {
        List<AnalyticsDrillDownRowDTO> baseRows = assembler.toRevenueDrillDownRows(
                queryService.fetchRevenueDrillDownRows(startDate, endDate));

        List<AnalyticsDrillDownRowDTO> filteredRows = baseRows.stream()
                .filter(row -> assembler.matchesFilter(row.getStatus(), status))
                .toList();

        return assembler.buildMetricDrillDownResponse(
                "revenue",
                "中标金额明细",
                startDate,
                endDate,
                List.of(assembler.buildDimension("status", "状态", status, baseRows, AnalyticsDrillDownRowDTO::getStatus,
                        assembler::translateTenderStatus)),
                filteredRows,
                page,
                size,
                AnalyticsDrillDownSummaryDTO.builder()
                        .totalCount((long) filteredRows.size())
                        .totalAmount(assembler.sumAmounts(filteredRows))
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsDrillDownResponseDTO getWinRateDrillDown(
            String outcome,
            LocalDate startDate,
            LocalDate endDate,
            Integer page,
            Integer size
    ) {
        List<DashboardAnalyticsRepository.TenderSummaryRow> tenderRows = queryService.fetchTenderSummaryRows();
        List<DashboardAnalyticsRepository.TenderSummaryRow> filteredTenders =
                computation.filterTenderRowsByDateRange(tenderRows, startDate, endDate);

        Map<Long, DashboardAnalyticsRepository.TenderSummaryRow> tenderById = filteredTenders.stream()
                .collect(Collectors.toMap(DashboardAnalyticsRepository.TenderSummaryRow::tenderId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));

        Map<Long, ProjectSnapshotAggregate> projectByTenderId = queryService.fetchProjectSnapshotsByTenderIds(tenderById.keySet())
                .stream()
                .collect(Collectors.toMap(ProjectSnapshotAggregate::tenderId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));

        List<AnalyticsDrillDownRowDTO> baseRows = filteredTenders.stream()
                .map(tender -> {
                    ProjectSnapshotAggregate project = projectByTenderId.get(tender.tenderId());
                    String derivedOutcome = computation.deriveOutcome(tender.status(), project);

                    return AnalyticsDrillDownRowDTO.builder()
                            .id(tender.tenderId())
                            .relatedId(project != null ? project.projectId() : null)
                            .title(tender.title())
                            .subtitle(project != null ? project.projectName() : "未形成项目")
                            .status(project != null && project.projectStatus() != null ? project.projectStatus().name() : tender.status().name())
                            .outcome(derivedOutcome)
                            .ownerName(project != null ? assembler.resolveDisplayName(null, project.managerName(), project.managerId()) : "-")
                            .amount(tender.budget() == null ? BigDecimal.ZERO : tender.budget())
                            .rate("WON".equals(derivedOutcome) ? 100.0 : 0.0)
                            .createdAt(tender.createdAt())
                            .deadline(tender.deadline())
                            .build();
                })
                .sorted((left, right) -> {
                    int createdCompare = Comparator.nullsLast(LocalDateTime::compareTo)
                            .compare(right.getCreatedAt(), left.getCreatedAt());
                    if (createdCompare != 0) {
                        return createdCompare;
                    }
                    return Comparator.nullsLast(BigDecimal::compareTo)
                            .compare(right.getAmount(), left.getAmount());
                })
                .toList();

        List<AnalyticsDrillDownRowDTO> filteredRows = baseRows.stream()
                .filter(row -> assembler.matchesFilter(row.getOutcome(), outcome))
                .toList();

        long wonCount = filteredRows.stream().filter(row -> "WON".equals(row.getOutcome())).count();
        double winRate = filteredRows.isEmpty() ? 0.0 : (wonCount * 100.0) / filteredRows.size();

        return assembler.buildMetricDrillDownResponse(
                "win-rate",
                "中标率明细",
                startDate,
                endDate,
                List.of(assembler.buildDimension("outcome", "结果", outcome, baseRows,
                        AnalyticsDrillDownRowDTO::getOutcome, assembler::translateOutcome)),
                filteredRows,
                page,
                size,
                AnalyticsDrillDownSummaryDTO.builder()
                        .totalCount((long) filteredRows.size())
                        .totalAmount(assembler.sumAmounts(filteredRows))
                        .wonCount(wonCount)
                        .winRate(winRate)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsDrillDownResponseDTO getTeamDrillDown(
            String role,
            LocalDate startDate,
            LocalDate endDate,
            Integer page,
            Integer size
    ) {
        Map<Long, DashboardAnalyticsRepository.TenderSummaryRow> tenderById = queryService.fetchTenderSummaryRows().stream()
                .collect(Collectors.toMap(DashboardAnalyticsRepository.TenderSummaryRow::tenderId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));

        List<ProjectSnapshotAggregate> filteredProjects = queryService.fetchProjectSnapshotsByDateRange(startDate, endDate);
        Set<Long> projectIds = filteredProjects.stream()
                .map(ProjectSnapshotAggregate::projectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, TeamTaskAggregate> taskAggregateByAssignee = computation.summarizeTaskRows(
                queryService.fetchTaskSnapshots(projectIds),
                LocalDateTime.now()
        );
        Map<Long, User> userById = queryService.fetchUsersByIds(
                filteredProjects.stream()
                        .flatMap(project -> {
                            if (project.managerId() == null) {
                                return project.teamMemberIds().stream();
                            }
                            Set<Long> members = new LinkedHashSet<>(project.teamMemberIds());
                            members.add(project.managerId());
                            return members.stream();
                        })
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );

        Map<Long, TeamAggregate> aggregates = computation.buildTeamProjectAggregates(filteredProjects, tenderById);

        List<AnalyticsDrillDownRowDTO> baseRows = assembler.toTeamDrillDownRows(aggregates, userById, taskAggregateByAssignee);
        List<AnalyticsDrillDownRowDTO> filteredRows = baseRows.stream()
                .filter(row -> assembler.matchesFilter(row.getRole(), role))
                .toList();

        return assembler.buildMetricDrillDownResponse(
                "team",
                "人员绩效明细",
                startDate,
                endDate,
                List.of(assembler.buildDimension("role", "角色", role, baseRows, AnalyticsDrillDownRowDTO::getRole,
                        assembler::translateUserRole)),
                filteredRows,
                page,
                size,
                assembler.buildTeamSummary(filteredRows, filteredProjects, tenderById)
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsDrillDownResponseDTO getProjectDrillDown(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Integer page,
            Integer size
    ) {
        List<AnalyticsDrillDownRowDTO> baseRows = assembler.toProjectDrillDownRows(
                queryService.fetchProjectDrillDownRows(startDate, endDate));
        List<AnalyticsDrillDownRowDTO> filteredRows = baseRows.stream()
                .filter(row -> assembler.matchesProjectStatusFilter(row.getStatus(), status))
                .toList();
        long activeCount = filteredRows.stream().filter(row -> !"ARCHIVED".equals(row.getStatus())).count();

        return assembler.buildMetricDrillDownResponse(
                "projects",
                "进行中项目明细",
                startDate,
                endDate,
                List.of(assembler.buildProjectStatusDimension(status, baseRows)),
                filteredRows,
                page,
                size,
                AnalyticsDrillDownSummaryDTO.builder()
                        .totalCount((long) filteredRows.size())
                        .totalAmount(assembler.sumAmounts(filteredRows))
                        .activeCount(activeCount)
                        .build()
        );
    }

    /**
     * Clear the overview cache.
     */
    @CacheEvict(value = "dashboard:overview", key = "'overview'")
    public void clearOverviewCache() {
        log.debug("Clearing dashboard overview cache");
    }
}
