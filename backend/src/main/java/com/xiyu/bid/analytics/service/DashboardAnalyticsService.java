// Input: Repository (Tender, Project, Task)
// Output: Dashboard Analytics Data
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.analytics.service;

import com.xiyu.bid.analytics.dto.AnalyticsDrillDownFileDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownFiltersDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownProjectDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownResponse;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownResponseDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownRowDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownStatsDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownSummaryDTO;
import com.xiyu.bid.analytics.dto.AnalyticsDrillDownTeamDTO;
import com.xiyu.bid.analytics.dto.AnalyticsFilterDimensionDTO;
import com.xiyu.bid.analytics.dto.AnalyticsFilterOptionDTO;
import com.xiyu.bid.analytics.dto.AnalyticsPaginationDTO;
import com.xiyu.bid.analytics.dto.CompetitorData;
import com.xiyu.bid.analytics.dto.DashboardOverviewDTO;
import com.xiyu.bid.analytics.dto.ProductLineData;
import com.xiyu.bid.analytics.dto.RegionalData;
import com.xiyu.bid.analytics.dto.SummaryStats;
import com.xiyu.bid.analytics.dto.TrendData;
import com.xiyu.bid.analytics.repository.DashboardAnalyticsRepository;
import com.xiyu.bid.documentexport.entity.DocumentExport;
import com.xiyu.bid.documentexport.repository.DocumentExportRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Service for dashboard analytics and aggregation
 * Provides cached dashboard data for performance optimization
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardAnalyticsService {

    private final TenderRepository tenderRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final DocumentExportRepository documentExportRepository;
    private final DashboardAnalyticsRepository dashboardAnalyticsRepository;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String ALL_FILTER = "ALL";

    /**
     * Get complete dashboard overview with caching
     */
    @Cacheable(value = "dashboard:overview", key = "'overview'")
    public DashboardOverviewDTO getOverview() {
        log.debug("Fetching dashboard overview from database");
        return DashboardOverviewDTO.builder()
                .summaryStats(getSummaryStats())
                .tenderTrends(getTenderTrends())
                .projectTrends(getProjectTrends())
                .statusDistribution(getStatusDistribution())
                .topCompetitors(getTopCompetitors(5))
                .regionalDistribution(getRegionalDistribution())
                .build();
    }

    /**
     * Get summary statistics
     */
    public SummaryStats getSummaryStats() {
        DashboardAnalyticsRepository.OverviewSnapshot snapshot = dashboardAnalyticsRepository.fetchOverviewSnapshot();
        double successRate = snapshot.biddedTenders() == 0
                ? 0.0
                : (snapshot.winningProjects() * 100.0) / snapshot.biddedTenders();

        return SummaryStats.builder()
                .totalTenders(snapshot.totalTenders())
                .activeProjects(snapshot.activeProjects())
                .pendingTasks(snapshot.pendingTasks())
                .totalBudget(snapshot.totalBudget())
                .successRate(successRate)
                .build();
    }

    /**
     * Get tender trends grouped by month
     */
    public List<TrendData> getTenderTrends() {
        List<DashboardAnalyticsRepository.MonthlyTrendRow> rows = dashboardAnalyticsRepository.fetchTenderTrends();
        List<TrendData> trends = new ArrayList<>();
        Long previousCount = null;
        for (DashboardAnalyticsRepository.MonthlyTrendRow row : rows) {
            int year = row.year();
            int month = row.month();
            long count = row.count() == null ? 0L : row.count();
            BigDecimal value = row.totalValue() == null ? BigDecimal.ZERO : row.totalValue();

            Double changePercentage = null;
            if (previousCount != null && previousCount > 0) {
                changePercentage = ((count - previousCount) * 100.0) / previousCount;
            }

            trends.add(TrendData.builder()
                    .period(String.format("%04d-%02d", year, month))
                    .count(count)
                    .value(value)
                    .changePercentage(changePercentage)
                    .build());

            previousCount = count;
        }

        return trends;
    }

    /**
     * Get project trends grouped by month
     */
    public List<TrendData> getProjectTrends() {
        List<DashboardAnalyticsRepository.MonthlyTrendRow> rows = dashboardAnalyticsRepository.fetchProjectTrends();
        List<TrendData> trends = new ArrayList<>();
        Long previousCount = null;
        for (DashboardAnalyticsRepository.MonthlyTrendRow row : rows) {
            int year = row.year();
            int month = row.month();
            long count = row.count() == null ? 0L : row.count();

            Double changePercentage = null;
            if (previousCount != null && previousCount > 0) {
                changePercentage = ((count - previousCount) * 100.0) / previousCount;
            }

            trends.add(TrendData.builder()
                    .period(String.format("%04d-%02d", year, month))
                    .count(count)
                    .value(null) // No monetary value for projects
                    .changePercentage(changePercentage)
                    .build());

            previousCount = count;
        }

        return trends;
    }

    /**
     * Get status distribution for tenders
     */
        public Map<String, Long> getStatusDistribution() {
        Map<Tender.Status, Long> countsByStatus = new EnumMap<>(Tender.Status.class);
        for (DashboardAnalyticsRepository.StatusCountRow row : dashboardAnalyticsRepository.fetchStatusDistribution()) {
            countsByStatus.put(row.status(), row.count() == null ? 0L : row.count());
        }
        Map<String, Long> distribution = new LinkedHashMap<>();
        Arrays.stream(Tender.Status.values()).forEach(status ->
                distribution.put(status.name(), countsByStatus.getOrDefault(status, 0L)));
        return distribution;
    }

    /**
     * Get top competitors by bid count
     */
    public List<CompetitorData> getTopCompetitors(Integer limit) {
        List<DashboardAnalyticsRepository.SourceAggregateRow> rows = dashboardAnalyticsRepository.fetchSourceAggregates(limit == null || limit < 1 ? 5 : limit);
        return rows.stream()
                .map(row -> {
                    String source = row.source();
                    long bidCount = row.bidCount() == null ? 0L : row.bidCount();
                    long winCount = row.winCount() == null ? 0L : row.winCount();
                    double winRate = bidCount == 0 ? 0.0 : (winCount * 100.0) / bidCount;
                    return CompetitorData.builder()
                            .name(source)
                            .bidCount(bidCount)
                            .winCount(winCount)
                            .winRate(winRate)
                            .totalBidAmount(row.totalBidAmount() == null ? BigDecimal.ZERO : row.totalBidAmount())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get regional distribution of tenders
     */
    public List<RegionalData> getRegionalDistribution() {
        List<DashboardAnalyticsRepository.SourceAggregateRow> rows = dashboardAnalyticsRepository.fetchSourceAggregates(Integer.MAX_VALUE);
        long totalTenders = rows.stream()
                .mapToLong(row -> row.bidCount() == null ? 0L : row.bidCount())
                .sum();
        return rows.stream()
                .map(row -> {
                    long count = row.bidCount() == null ? 0L : row.bidCount();
                    double percentage = totalTenders == 0 ? 0.0 : (count * 100.0) / totalTenders;
                    return RegionalData.builder()
                            .region(row.source())
                            .tenderCount(count)
                            .totalBudget(row.totalBidAmount() == null ? BigDecimal.ZERO : row.totalBidAmount())
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<ProductLineData> getProductLinePerformance() {
        Map<String, List<DashboardAnalyticsRepository.ProductLineCandidateRow>> tendersByProductLine = dashboardAnalyticsRepository
                .fetchProductLineCandidateRows()
                .stream()
                .collect(Collectors.groupingBy(row -> classifyProductLine(row.title())));

        return tendersByProductLine.entrySet().stream()
                .map(entry -> {
                    List<DashboardAnalyticsRepository.ProductLineCandidateRow> tenders = entry.getValue();
                    long bidCount = tenders.size();
                    long wonCount = tenders.stream()
                            .filter(tender -> tender.status() == Tender.Status.BIDDED)
                            .count();
                    BigDecimal revenue = tenders.stream()
                            .map(DashboardAnalyticsRepository.ProductLineCandidateRow::budget)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal cost = revenue.multiply(new BigDecimal("0.72"));
                    double winRate = bidCount > 0 ? (wonCount * 100.0) / bidCount : 0.0;

                    return ProductLineData.builder()
                            .name(entry.getKey())
                            .revenue(revenue)
                            .cost(cost)
                            .bids(bidCount)
                            .rate(winRate)
                            .build();
                })
                .sorted((left, right) -> right.getRevenue().compareTo(left.getRevenue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnalyticsDrillDownResponse getDrillDown(String type, String key) {
        List<DashboardAnalyticsRepository.TenderSummaryRow> allTenders = dashboardAnalyticsRepository.fetchTenderSummaryRows();
        List<DashboardAnalyticsRepository.TenderSummaryRow> matchedTenders = switch (type.toLowerCase(Locale.ROOT)) {
            case "trend" -> allTenders.stream()
                    .filter(tender -> tender.createdAt() != null)
                    .filter(tender -> tender.createdAt().format(MONTH_FORMATTER).equals(key))
                    .collect(Collectors.toList());
            case "competitor", "region" -> allTenders.stream()
                    .filter(tender -> key.equals(tender.source()))
                    .collect(Collectors.toList());
            case "product" -> allTenders.stream()
                    .filter(tender -> classifyProductLine(tender.title()).equals(key))
                    .collect(Collectors.toList());
            default -> Collections.emptyList();
        };

        Set<Long> tenderIds = matchedTenders.stream()
                .map(DashboardAnalyticsRepository.TenderSummaryRow::tenderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ProjectSnapshotAggregate> matchedProjects = tenderIds.isEmpty()
                ? List.of()
                : aggregateProjectSnapshots(dashboardAnalyticsRepository.fetchProjectSnapshotRowsByTenderIds(tenderIds));

        Set<Long> projectIds = matchedProjects.stream()
                .map(ProjectSnapshotAggregate::projectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<DashboardAnalyticsRepository.TaskSnapshotRow> matchedTasks = dashboardAnalyticsRepository.fetchTaskSnapshotRows(projectIds);
        List<DashboardAnalyticsRepository.ProjectDocumentRow> projectDocuments = dashboardAnalyticsRepository.fetchProjectDocumentRows(projectIds);
        List<DashboardAnalyticsRepository.DocumentExportRow> documentExports = dashboardAnalyticsRepository.fetchDocumentExportRows(projectIds);
        Map<Long, User> userById = loadUsersById(collectProjectUserIds(matchedProjects));
        Map<Long, DashboardAnalyticsRepository.TenderSummaryRow> tenderById = matchedTenders.stream()
                .collect(Collectors.toMap(DashboardAnalyticsRepository.TenderSummaryRow::tenderId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<AnalyticsDrillDownProjectDTO> projectItems = matchedProjects.stream()
                .map(project -> {
                    return AnalyticsDrillDownProjectDTO.builder()
                            .id(project.projectId())
                            .name(project.projectName())
                            .customer(defaultString(project.tenderSource(), "-"))
                            .budget(defaultAmount(project.budget()))
                            .status(project.projectStatus() == null ? "-" : project.projectStatus().name().toLowerCase(Locale.ROOT))
                            .manager(resolveDisplayName(userById.get(project.managerId()), project.managerName(), project.managerId()))
                            .result(resolveProjectResult(project.projectStatus()))
                            .build();
                })
                .collect(Collectors.toList());

        List<AnalyticsDrillDownTeamDTO> teamItems = buildTeamItems(matchedProjects, matchedTasks, userById);
        List<AnalyticsDrillDownFileDTO> fileItems = buildFileItems(matchedProjects, projectDocuments, documentExports);
        long totalParticipation = matchedTenders.size();
        long wonCount = matchedTenders.stream().filter(tender -> tender.status() == Tender.Status.BIDDED).count();
        double teamWinRate = totalParticipation > 0 ? (wonCount * 100.0) / totalParticipation : 0.0;
        BigDecimal totalAmount = matchedTenders.stream()
                .map(DashboardAnalyticsRepository.TenderSummaryRow::budget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AnalyticsDrillDownResponse.builder()
                .projects(projectItems)
                .team(teamItems)
                .files(fileItems)
                .stats(AnalyticsDrillDownStatsDTO.builder()
                        .totalParticipation(totalParticipation)
                        .wonCount(wonCount)
                        .teamWinRate(teamWinRate)
                        .totalAmount(totalAmount)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsDrillDownResponseDTO getRevenueDrillDown(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Integer page,
            Integer size
    ) {
        List<AnalyticsDrillDownRowDTO> baseRows = dashboardAnalyticsRepository.fetchRevenueDrillDownRows(startDate, endDate).stream()
                .map(row -> AnalyticsDrillDownRowDTO.builder()
                        .id(row.tenderId())
                        .relatedId(row.projectId())
                        .title(row.title())
                        .subtitle(defaultString(row.source(), "未知来源"))
                        .status(row.tenderStatus() == null ? null : row.tenderStatus().name())
                        .ownerName(defaultString(row.projectName(), "未关联项目"))
                        .amount(defaultAmount(row.budget()))
                        .score(row.score())
                        .createdAt(row.createdAt())
                        .deadline(row.deadline())
                        .build())
                .sorted((left, right) -> {
                    int amountCompare = Comparator.nullsLast(BigDecimal::compareTo)
                            .compare(right.getAmount(), left.getAmount());
                    if (amountCompare != 0) {
                        return amountCompare;
                    }
                    return Comparator.nullsLast(LocalDateTime::compareTo)
                            .compare(right.getCreatedAt(), left.getCreatedAt());
                })
                .toList();

        List<AnalyticsDrillDownRowDTO> filteredRows = baseRows.stream()
                .filter(row -> matchesFilter(row.getStatus(), status))
                .toList();

        return buildMetricDrillDownResponse(
                "revenue",
                "中标金额明细",
                startDate,
                endDate,
                List.of(buildDimension("status", "状态", status, baseRows, AnalyticsDrillDownRowDTO::getStatus, this::translateTenderStatus)),
                filteredRows,
                page,
                size,
                AnalyticsDrillDownSummaryDTO.builder()
                        .totalCount((long) filteredRows.size())
                        .totalAmount(sumAmounts(filteredRows))
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
        List<DashboardAnalyticsRepository.TenderSummaryRow> tenderRows = dashboardAnalyticsRepository.fetchTenderSummaryRows();
        List<DashboardAnalyticsRepository.TenderSummaryRow> filteredTenders = tenderRows.stream()
                .filter(tender -> isWithinDateRange(tender.createdAt(), startDate, endDate))
                .toList();

        Map<Long, DashboardAnalyticsRepository.TenderSummaryRow> tenderById = filteredTenders.stream()
                .collect(Collectors.toMap(DashboardAnalyticsRepository.TenderSummaryRow::tenderId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<ProjectSnapshotAggregate> projectSnapshots = tenderById.isEmpty()
                ? List.of()
                : aggregateProjectSnapshots(dashboardAnalyticsRepository.fetchProjectSnapshotRowsByTenderIds(tenderById.keySet()));
        Map<Long, ProjectSnapshotAggregate> projectByTenderId = projectSnapshots.stream()
                .collect(Collectors.toMap(ProjectSnapshotAggregate::tenderId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<AnalyticsDrillDownRowDTO> baseRows = filteredTenders.stream()
                .map(tender -> {
                    ProjectSnapshotAggregate project = projectByTenderId.get(tender.tenderId());
                    String derivedOutcome = deriveOutcome(tender, project);

                    return AnalyticsDrillDownRowDTO.builder()
                            .id(tender.tenderId())
                            .relatedId(project != null ? project.projectId() : null)
                            .title(tender.title())
                            .subtitle(project != null ? project.projectName() : "未形成项目")
                            .status(project != null && project.projectStatus() != null ? project.projectStatus().name() : tender.status().name())
                            .outcome(derivedOutcome)
                            .ownerName(project != null ? resolveDisplayName(null, project.managerName(), project.managerId()) : "-")
                            .amount(defaultAmount(tender.budget()))
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
                .filter(row -> matchesFilter(row.getOutcome(), outcome))
                .toList();

        long wonCount = filteredRows.stream().filter(row -> "WON".equals(row.getOutcome())).count();
        double winRate = filteredRows.isEmpty() ? 0.0 : (wonCount * 100.0) / filteredRows.size();

        return buildMetricDrillDownResponse(
                "win-rate",
                "中标率明细",
                startDate,
                endDate,
                List.of(buildDimension("outcome", "结果", outcome, baseRows, AnalyticsDrillDownRowDTO::getOutcome, this::translateOutcome)),
                filteredRows,
                page,
                size,
                AnalyticsDrillDownSummaryDTO.builder()
                        .totalCount((long) filteredRows.size())
                        .totalAmount(sumAmounts(filteredRows))
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
        List<DashboardAnalyticsRepository.TenderSummaryRow> tenderRows = dashboardAnalyticsRepository.fetchTenderSummaryRows();
        Map<Long, DashboardAnalyticsRepository.TenderSummaryRow> tenderById = tenderRows.stream()
                .collect(Collectors.toMap(DashboardAnalyticsRepository.TenderSummaryRow::tenderId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<ProjectSnapshotAggregate> filteredProjects = aggregateProjectSnapshots(
                dashboardAnalyticsRepository.fetchProjectSnapshotRowsByDateRange(startDate, endDate)
        );

        Set<Long> relevantProjectIds = filteredProjects.stream()
                .map(ProjectSnapshotAggregate::projectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LocalDateTime now = LocalDateTime.now();
        Map<Long, TeamTaskAggregate> taskAggregateByAssignee = dashboardAnalyticsRepository.fetchTaskSnapshotRows(relevantProjectIds).stream()
                .filter(task -> task.assigneeId() != null)
                .collect(Collectors.groupingBy(DashboardAnalyticsRepository.TaskSnapshotRow::assigneeId, Collectors.collectingAndThen(Collectors.toList(), tasks -> {
                    TeamTaskAggregate aggregate = new TeamTaskAggregate();
                    aggregate.totalTaskCount = tasks.size();
                    aggregate.completedTaskCount = tasks.stream()
                            .filter(task -> task.status() == Task.Status.COMPLETED)
                            .count();
                    aggregate.overdueTaskCount = tasks.stream()
                            .filter(task -> task.dueDate() != null)
                            .filter(task -> task.dueDate().isBefore(now))
                            .filter(task -> task.status() != Task.Status.COMPLETED)
                            .filter(task -> task.status() != Task.Status.CANCELLED)
                            .count();
                    return aggregate;
                })));

        Set<Long> userIds = filteredProjects.stream()
                .flatMap(project -> Stream.concat(
                        project.managerId() == null ? Stream.empty() : Stream.of(project.managerId()),
                        project.teamMemberIds().stream()
                ))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, User> userById = loadUsersById(userIds);

        Map<Long, TeamAggregate> aggregates = new HashMap<>();
        for (ProjectSnapshotAggregate project : filteredProjects) {
            DashboardAnalyticsRepository.TenderSummaryRow tender = tenderById.get(project.tenderId());
            BigDecimal amount = tender != null ? defaultAmount(tender.budget()) : defaultAmount(project.budget());
            boolean won = tender != null && tender.status() == Tender.Status.BIDDED;
            boolean active = project.projectStatus() != Project.Status.ARCHIVED;

            Long managerId = project.managerId();
            if (managerId != null) {
                accumulateTeamAggregate(aggregates, managerId, amount, won, active, true);
            }

            for (Long memberId : project.teamMemberIds()) {
                if (!Objects.equals(memberId, managerId)) {
                    accumulateTeamAggregate(aggregates, memberId, amount, won, active, false);
                }
            }
        }

        aggregates.forEach((userId, aggregate) -> {
            TeamTaskAggregate taskAggregate = taskAggregateByAssignee.getOrDefault(userId, TeamTaskAggregate.empty());
            aggregate.setTaskMetrics(taskAggregate.totalTaskCount, taskAggregate.completedTaskCount, taskAggregate.overdueTaskCount);
        });

        List<AnalyticsDrillDownRowDTO> baseRows = aggregates.entrySet().stream()
                .map(entry -> {
                    Long userId = entry.getKey();
                    TeamAggregate aggregate = entry.getValue();
                    User user = userById.get(userId);
                    double winRate = aggregate.projectCount == 0 ? 0.0 : (aggregate.wonCount * 100.0) / aggregate.projectCount;
                    double taskCompletionRate = aggregate.totalTaskCount == 0 ? 0.0 : (aggregate.completedTaskCount * 100.0) / aggregate.totalTaskCount;
                    int performanceScore = calculatePerformanceScore(winRate, taskCompletionRate, aggregate.overdueTaskCount, aggregate.totalTaskCount);
                    return AnalyticsDrillDownRowDTO.builder()
                            .id(userId)
                            .title(user != null ? user.getFullName() : fallbackUserName(userId))
                            .subtitle(user != null ? user.getEmail() : "-")
                            .role(user != null ? user.getRoleCode().toUpperCase(Locale.ROOT) : "UNKNOWN")
                            .count(aggregate.projectCount)
                            .wonCount(aggregate.wonCount)
                            .activeProjectCount(aggregate.activeProjectCount)
                            .managedProjectCount(aggregate.managedProjectCount)
                            .totalTaskCount(aggregate.totalTaskCount)
                            .completedTaskCount(aggregate.completedTaskCount)
                            .overdueTaskCount(aggregate.overdueTaskCount)
                            .rate(winRate)
                            .taskCompletionRate(taskCompletionRate)
                            .amount(aggregate.totalAmount)
                            .score(performanceScore)
                            .teamSize(Math.toIntExact(aggregate.managedProjectCount))
                            .createdAt(null)
                            .deadline(null)
                            .build();
                })
                .sorted((left, right) -> {
                    int scoreCompare = Comparator.nullsLast(Integer::compareTo)
                            .compare(right.getScore(), left.getScore());
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return Comparator.nullsLast(Double::compareTo)
                            .compare(right.getRate(), left.getRate());
                })
                .toList();

        List<AnalyticsDrillDownRowDTO> filteredRows = baseRows.stream()
                .filter(row -> matchesFilter(row.getRole(), role))
                .toList();

        return buildMetricDrillDownResponse(
                "team",
                "人员绩效明细",
                startDate,
                endDate,
                List.of(buildDimension("role", "角色", role, baseRows, AnalyticsDrillDownRowDTO::getRole, this::translateUserRole)),
                filteredRows,
                page,
                size,
                AnalyticsDrillDownSummaryDTO.builder()
                        .totalCount((long) filteredRows.size())
                        .totalAmount(sumAmounts(filteredRows))
                        .totalTeamMembers((long) filteredRows.size())
                        .wonCount(filteredProjects.stream()
                                .map(project -> tenderById.get(project.tenderId()))
                                .filter(Objects::nonNull)
                                .filter(tender -> tender.status() == Tender.Status.BIDDED)
                                .count())
                        .winRate(filteredRows.isEmpty() ? 0.0 : filteredRows.stream()
                                .map(AnalyticsDrillDownRowDTO::getRate)
                                .filter(Objects::nonNull)
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElse(0.0))
                        .totalCompletedTasks(filteredRows.stream()
                                .map(AnalyticsDrillDownRowDTO::getCompletedTaskCount)
                                .filter(Objects::nonNull)
                                .reduce(0L, Long::sum))
                        .totalOverdueTasks(filteredRows.stream()
                                .map(AnalyticsDrillDownRowDTO::getOverdueTaskCount)
                                .filter(Objects::nonNull)
                                .reduce(0L, Long::sum))
                        .averageTaskCompletionRate(filteredRows.isEmpty() ? 0.0 : filteredRows.stream()
                                .map(AnalyticsDrillDownRowDTO::getTaskCompletionRate)
                                .filter(Objects::nonNull)
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElse(0.0))
                        .build()
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
        List<AnalyticsDrillDownRowDTO> baseRows = dashboardAnalyticsRepository.fetchProjectDrillDownRows(startDate, endDate).stream()
                .map(row -> AnalyticsDrillDownRowDTO.builder()
                        .id(row.projectId())
                        .relatedId(row.tenderId())
                        .title(row.projectName())
                        .subtitle(defaultString(row.tenderTitle(), "未关联标讯"))
                        .status(row.projectStatus() == null ? null : row.projectStatus().name())
                        .ownerName(defaultString(row.managerName(), fallbackUserName(row.managerId())))
                        .amount(defaultAmount(row.budget()))
                        .teamSize(row.teamSize())
                        .createdAt(row.referenceDate())
                        .deadline(row.endDate())
                        .build())
                .sorted(Comparator.comparing(AnalyticsDrillDownRowDTO::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();

        List<AnalyticsDrillDownRowDTO> filteredRows = baseRows.stream()
                .filter(row -> matchesProjectStatusFilter(row.getStatus(), status))
                .toList();

        long activeCount = filteredRows.stream().filter(row -> !"ARCHIVED".equals(row.getStatus())).count();

        return buildMetricDrillDownResponse(
                "projects",
                "进行中项目明细",
                startDate,
                endDate,
                List.of(buildProjectStatusDimension(status, baseRows)),
                filteredRows,
                page,
                size,
                AnalyticsDrillDownSummaryDTO.builder()
                        .totalCount((long) filteredRows.size())
                        .totalAmount(sumAmounts(filteredRows))
                        .activeCount(activeCount)
                        .build()
        );
    }

    /**
     * Clear the overview cache
     */
    @CacheEvict(value = "dashboard:overview", key = "'overview'")
    public void clearOverviewCache() {
        log.debug("Clearing dashboard overview cache");
    }

    /**
     * Calculate success rate based on won vs total bids
     */
    private double calculateSuccessRate(List<Tender> tenders, List<Project> projects) {
        if (tenders.isEmpty()) {
            return 0.0;
        }

        long totalBidded = tenders.stream()
                .filter(t -> t.getStatus() == Tender.Status.BIDDED)
                .count();

        if (totalBidded == 0) {
            return 0.0;
        }

        // Simplified: Assuming all BIDDED tenders that have projects are wins
        long wins = projects.stream()
                .filter(p -> p.getStatus() == Project.Status.BIDDING ||
                           p.getStatus() == Project.Status.REVIEWING ||
                           p.getStatus() == Project.Status.SEALING)
                .count();

        return (wins * 100.0) / totalBidded;
    }

    private String classifyProductLine(String sourceText) {
        String text = sourceText == null ? "" : sourceText.toLowerCase(Locale.ROOT);
        if (text.contains("办公") || text.contains("oa") || text.contains("协同")) {
            return "智慧办公";
        }
        if (text.contains("云") || text.contains("cloud")) {
            return "云服务";
        }
        if (text.contains("工业") || text.contains("mes") || text.contains("制造")) {
            return "工业软件";
        }
        if (text.contains("数据中心") || text.contains("机房") || text.contains("idc")) {
            return "数据中心";
        }
        return "综合解决方案";
    }

    private BigDecimal resolveTenderBudget(Long tenderId, List<Tender> matchedTenders) {
        return matchedTenders.stream()
                .filter(tender -> Objects.equals(tender.getId(), tenderId))
                .map(Tender::getBudget)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private String resolveProjectCustomer(Project project) {
        return tenderRepository.findById(project.getTenderId())
                .map(Tender::getSource)
                .filter(source -> source != null && !source.isBlank())
                .orElse("-");
    }

    private String resolveProjectResult(Project project) {
        if (project.getStatus() == Project.Status.ARCHIVED) {
            return "won";
        }
        if (project.getStatus() == Project.Status.BIDDING
                || project.getStatus() == Project.Status.REVIEWING
                || project.getStatus() == Project.Status.SEALING) {
            return "won";
        }
        return null;
    }

    private String resolveProjectResult(Project.Status status) {
        if (status == null) {
            return null;
        }
        if (status == Project.Status.ARCHIVED
                || status == Project.Status.BIDDING
                || status == Project.Status.REVIEWING
                || status == Project.Status.SEALING) {
            return "won";
        }
        return null;
    }

    private String resolveDisplayName(User user, String fallbackName, Long userId) {
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (fallbackName != null && !fallbackName.isBlank()) {
            return fallbackName;
        }
        return fallbackUserName(userId);
    }

    private Map<Long, User> loadUsersById(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return StreamSupport.stream(userRepository.findAllById(userIds).spliterator(), false)
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Set<Long> collectProjectUserIds(List<ProjectSnapshotAggregate> projects) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (ProjectSnapshotAggregate project : projects) {
            if (project.managerId() != null) {
                userIds.add(project.managerId());
            }
            userIds.addAll(project.teamMemberIds());
        }
        return userIds;
    }

    private List<ProjectSnapshotAggregate> aggregateProjectSnapshots(List<DashboardAnalyticsRepository.ProjectSnapshotRow> rows) {
        Map<Long, ProjectSnapshotAggregate> aggregates = new LinkedHashMap<>();
        for (DashboardAnalyticsRepository.ProjectSnapshotRow row : rows) {
            ProjectSnapshotAggregate aggregate = aggregates.computeIfAbsent(row.projectId(), ignored -> new ProjectSnapshotAggregate(
                    row.projectId(),
                    row.tenderId(),
                    row.projectName(),
                    row.projectStatus(),
                    row.managerId(),
                    row.managerName(),
                    row.tenderSource(),
                    row.budget(),
                    row.referenceDate(),
                    row.endDate(),
                    new LinkedHashSet<>()
            ));
            if (row.teamMemberId() != null) {
                aggregate.teamMemberIds().add(row.teamMemberId());
            }
        }
        return new ArrayList<>(aggregates.values());
    }

    private List<AnalyticsDrillDownTeamDTO> buildTeamItems(
            List<ProjectSnapshotAggregate> projects,
            List<DashboardAnalyticsRepository.TaskSnapshotRow> tasks,
            Map<Long, User> usersById
    ) {
        Map<Long, List<DashboardAnalyticsRepository.TaskSnapshotRow>> tasksByAssignee = tasks.stream()
                .filter(task -> task.assigneeId() != null)
                .collect(Collectors.groupingBy(DashboardAnalyticsRepository.TaskSnapshotRow::assigneeId));

        Set<Long> teamMemberIds = new LinkedHashSet<>();
        projects.forEach(project -> {
            if (project.managerId() != null) {
                teamMemberIds.add(project.managerId());
            }
            teamMemberIds.addAll(project.teamMemberIds());
        });

        return teamMemberIds.stream()
                .map(userId -> {
                    User user = usersById.get(userId);
                    long participation = tasksByAssignee.getOrDefault(userId, List.of()).size();
                    long completedWins = tasksByAssignee.getOrDefault(userId, List.of()).stream()
                            .filter(task -> task.status() == Task.Status.COMPLETED)
                            .count();
                    double winRate = participation > 0 ? (completedWins * 100.0) / participation : 0.0;

                    return AnalyticsDrillDownTeamDTO.builder()
                            .name(user != null ? user.getFullName() : fallbackUserName(userId))
                            .role(user != null ? user.getRoleCode().toLowerCase(Locale.ROOT) : "member")
                            .dept("-")
                            .participation(participation)
                            .winRate(winRate)
                            .build();
                })
                .toList();
    }

    private List<AnalyticsDrillDownFileDTO> buildFileItems(
            List<ProjectSnapshotAggregate> projects,
            List<DashboardAnalyticsRepository.ProjectDocumentRow> projectDocuments,
            List<DashboardAnalyticsRepository.DocumentExportRow> documentExports
    ) {
        Map<Long, ProjectSnapshotAggregate> projectById = projects.stream()
                .collect(Collectors.toMap(ProjectSnapshotAggregate::projectId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<AnalyticsDrillDownFileDTO> files = new ArrayList<>();

        for (DashboardAnalyticsRepository.ProjectDocumentRow document : projectDocuments) {
            ProjectSnapshotAggregate project = projectById.get(document.projectId());
            if (project != null) {
                files.add(AnalyticsDrillDownFileDTO.builder()
                        .id("doc-" + document.documentId())
                        .name(document.name())
                        .project(project.projectName())
                        .uploader(document.uploaderName())
                        .uploadTime(document.createdAt() == null ? null : document.createdAt().toString())
                        .size(document.size())
                        .build());
            }
        }

        for (DashboardAnalyticsRepository.DocumentExportRow export : documentExports) {
            ProjectSnapshotAggregate project = projectById.get(export.projectId());
            if (project != null) {
                files.add(AnalyticsDrillDownFileDTO.builder()
                        .id("export-" + export.exportId())
                        .name(export.fileName())
                        .project(project.projectName())
                        .uploader(export.exportedByName())
                        .uploadTime(export.exportedAt() == null ? null : export.exportedAt().toString())
                        .size(export.fileSize() == null ? "-" : export.fileSize() + "B")
                        .build());
            }
        }

        return files.stream()
                .sorted(Comparator.comparing(AnalyticsDrillDownFileDTO::getUploadTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return "-";
        }
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse("用户" + userId);
    }

    private List<AnalyticsDrillDownTeamDTO> buildTeamItems(List<Project> projects, List<Task> tasks) {
        Map<Long, List<Task>> tasksByAssignee = tasks.stream()
                .filter(task -> task.getAssigneeId() != null)
                .collect(Collectors.groupingBy(Task::getAssigneeId));

        Set<Long> teamMemberIds = new LinkedHashSet<>();
        projects.forEach(project -> {
            if (project.getManagerId() != null) {
                teamMemberIds.add(project.getManagerId());
            }
            if (project.getTeamMembers() != null) {
                teamMemberIds.addAll(project.getTeamMembers());
            }
        });

        return teamMemberIds.stream()
                .map(userId -> {
                    User user = userRepository.findById(userId).orElse(null);
                    long participation = tasksByAssignee.getOrDefault(userId, Collections.emptyList()).size();
                    long completedWins = tasksByAssignee.getOrDefault(userId, Collections.emptyList()).stream()
                            .filter(task -> task.getStatus() == Task.Status.COMPLETED)
                            .count();
                    double winRate = participation > 0 ? (completedWins * 100.0) / participation : 0.0;

                    return AnalyticsDrillDownTeamDTO.builder()
                            .name(user != null ? user.getFullName() : ("用户" + userId))
                            .role(user != null ? user.getRoleCode().toLowerCase(Locale.ROOT) : "member")
                            .dept("-")
                            .participation(participation)
                            .winRate(winRate)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<AnalyticsDrillDownFileDTO> buildFileItems(List<Project> projects) {
        return projects.stream()
                .flatMap(project -> {
                    List<AnalyticsDrillDownFileDTO> projectDocuments = projectDocumentRepository
                            .findByProjectIdOrderByCreatedAtDesc(project.getId())
                            .stream()
                            .map(document -> mapProjectDocument(project, document))
                            .collect(Collectors.toList());

                    List<AnalyticsDrillDownFileDTO> exports = documentExportRepository
                            .findByProjectIdOrderByExportedAtDesc(project.getId())
                            .stream()
                            .map(documentExport -> mapDocumentExport(project, documentExport))
                            .collect(Collectors.toList());

                    List<AnalyticsDrillDownFileDTO> merged = new ArrayList<>(projectDocuments);
                    merged.addAll(exports);
                    return merged.stream();
                })
                .sorted(Comparator.comparing(AnalyticsDrillDownFileDTO::getUploadTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private AnalyticsDrillDownFileDTO mapProjectDocument(Project project, ProjectDocument document) {
        return AnalyticsDrillDownFileDTO.builder()
                .id("doc-" + document.getId())
                .name(document.getName())
                .project(project.getName())
                .uploader(document.getUploaderName())
                .uploadTime(document.getCreatedAt() == null ? null : document.getCreatedAt().toString())
                .size(document.getSize())
                .build();
    }

    private AnalyticsDrillDownFileDTO mapDocumentExport(Project project, DocumentExport export) {
        return AnalyticsDrillDownFileDTO.builder()
                .id("export-" + export.getId())
                .name(export.getFileName())
                .project(project.getName())
                .uploader(export.getExportedByName())
                .uploadTime(export.getExportedAt() == null ? null : export.getExportedAt().toString())
                .size(export.getFileSize() == null ? "-" : export.getFileSize() + "B")
                .build();
    }

    private AnalyticsDrillDownResponseDTO buildMetricDrillDownResponse(
            String metricKey,
            String metricLabel,
            LocalDate startDate,
            LocalDate endDate,
            List<AnalyticsFilterDimensionDTO> dimensions,
            List<AnalyticsDrillDownRowDTO> filteredRows,
            Integer requestedPage,
            Integer requestedSize,
            AnalyticsDrillDownSummaryDTO summary
    ) {
        int page = normalizePage(requestedPage);
        int size = normalizeSize(requestedSize);
        int fromIndex = Math.min((page - 1) * size, filteredRows.size());
        int toIndex = Math.min(fromIndex + size, filteredRows.size());
        List<AnalyticsDrillDownRowDTO> pageItems = filteredRows.subList(fromIndex, toIndex);
        int totalPages = filteredRows.isEmpty() ? 0 : (int) Math.ceil((double) filteredRows.size() / size);

        return AnalyticsDrillDownResponseDTO.builder()
                .metricKey(metricKey)
                .metricLabel(metricLabel)
                .filters(AnalyticsDrillDownFiltersDTO.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .dimensions(dimensions)
                        .build())
                .pagination(AnalyticsPaginationDTO.builder()
                        .page(page)
                        .size(size)
                        .total((long) filteredRows.size())
                        .totalPages(totalPages)
                        .hasNext(page < totalPages)
                        .build())
                .summary(summary)
                .items(pageItems)
                .build();
    }

    private AnalyticsFilterDimensionDTO buildDimension(
            String key,
            String label,
            String selectedValue,
            List<AnalyticsDrillDownRowDTO> rows,
            Function<AnalyticsDrillDownRowDTO, String> extractor,
            Function<String, String> labelTranslator
    ) {
        Map<String, Long> counts = rows.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

        List<AnalyticsFilterOptionDTO> options = new ArrayList<>();
        options.add(AnalyticsFilterOptionDTO.builder()
                .label("全部")
                .value(ALL_FILTER)
                .count((long) rows.size())
                .build());
        counts.forEach((value, count) -> options.add(AnalyticsFilterOptionDTO.builder()
                .label(labelTranslator.apply(value))
                .value(value)
                .count(count)
                .build()));

        return AnalyticsFilterDimensionDTO.builder()
                .key(key)
                .label(label)
                .selectedValue(normalizeFilterValue(selectedValue))
                .options(options)
                .build();
    }

    private AnalyticsFilterDimensionDTO buildProjectStatusDimension(String selectedValue, List<AnalyticsDrillDownRowDTO> rows) {
        long inProgressCount = rows.stream().filter(row -> !"ARCHIVED".equals(row.getStatus())).count();
        Map<String, Long> counts = rows.stream()
                .map(AnalyticsDrillDownRowDTO::getStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

        List<AnalyticsFilterOptionDTO> options = new ArrayList<>();
        options.add(AnalyticsFilterOptionDTO.builder().label("全部").value(ALL_FILTER).count((long) rows.size()).build());
        options.add(AnalyticsFilterOptionDTO.builder().label("进行中").value("IN_PROGRESS").count(inProgressCount).build());
        counts.forEach((value, count) -> options.add(AnalyticsFilterOptionDTO.builder()
                .label(translateProjectStatus(value))
                .value(value)
                .count(count)
                .build()));

        return AnalyticsFilterDimensionDTO.builder()
                .key("status")
                .label("项目状态")
                .selectedValue(normalizeProjectStatusFilter(selectedValue))
                .options(options)
                .build();
    }

    private boolean isWithinDateRange(LocalDateTime value, LocalDate startDate, LocalDate endDate) {
        if (value == null) {
            return false;
        }
        LocalDate date = value.toLocalDate();
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    private boolean matchesFilter(String value, String selectedValue) {
        String normalized = normalizeFilterValue(selectedValue);
        return ALL_FILTER.equals(normalized) || Objects.equals(value, normalized);
    }

    private boolean matchesProjectStatusFilter(String status, String filter) {
        String normalized = normalizeProjectStatusFilter(filter);
        if (ALL_FILTER.equals(normalized)) {
            return true;
        }
        if ("IN_PROGRESS".equals(normalized)) {
            return !"ARCHIVED".equals(status);
        }
        return Objects.equals(status, normalized);
    }

    private String normalizeFilterValue(String value) {
        if (value == null || value.isBlank()) {
            return ALL_FILTER;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeProjectStatusFilter(String value) {
        String normalized = normalizeFilterValue(value);
        if ("IN_PROGRESS".equals(normalized) || "INPROGRESS".equals(normalized)) {
            return "IN_PROGRESS";
        }
        return normalized;
    }

    private LocalDateTime getProjectReferenceDate(Project project) {
        return project.getStartDate() != null ? project.getStartDate() : project.getCreatedAt();
    }

    private String deriveOutcome(Tender tender, Project project) {
        if (tender.getStatus() == Tender.Status.BIDDED && project != null) {
            return "WON";
        }
        if (tender.getStatus() == Tender.Status.ABANDONED) {
            return "LOST";
        }
        return "IN_PROGRESS";
    }

    private String deriveOutcome(DashboardAnalyticsRepository.TenderSummaryRow tender, ProjectSnapshotAggregate project) {
        if (tender.status() == Tender.Status.BIDDED && project != null) {
            return "WON";
        }
        if (tender.status() == Tender.Status.ABANDONED) {
            return "LOST";
        }
        return "IN_PROGRESS";
    }

    private void accumulateTeamAggregate(
            Map<Long, TeamAggregate> aggregates,
            Long userId,
            BigDecimal amount,
            boolean won,
            boolean active,
            boolean manager
    ) {
        if (userId == null) {
            return;
        }
        TeamAggregate aggregate = aggregates.computeIfAbsent(userId, ignored -> new TeamAggregate());
        aggregate.projectCount++;
        aggregate.totalAmount = aggregate.totalAmount.add(amount);
        if (won) {
            aggregate.wonCount++;
        }
        if (active) {
            aggregate.activeProjectCount++;
        }
        if (manager) {
            aggregate.managedProjectCount++;
        }
    }

    private int calculatePerformanceScore(double winRate, double taskCompletionRate, long overdueTaskCount, long totalTaskCount) {
        double overduePenalty = totalTaskCount == 0 ? 0.0 : (overdueTaskCount * 100.0) / totalTaskCount;
        double score = (winRate * 0.45) + (taskCompletionRate * 0.4) + (Math.max(0.0, 100.0 - overduePenalty) * 0.15);
        return (int) Math.round(score);
    }

    private BigDecimal sumAmounts(List<AnalyticsDrillDownRowDTO> rows) {
        return rows.stream()
                .map(AnalyticsDrillDownRowDTO::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private record ProjectSnapshotAggregate(
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
            Set<Long> teamMemberIds
    ) {
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }

    private String fallbackUserName(Long userId) {
        return userId == null ? "未分配" : "用户#" + userId;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String translateTenderStatus(String status) {
        return switch (normalizeFilterValue(status)) {
            case "PENDING" -> "待处理";
            case "TRACKING" -> "跟踪中";
            case "BIDDED" -> "已投标";
            case "ABANDONED" -> "已放弃";
            default -> status;
        };
    }

    private String translateProjectStatus(String status) {
        return switch (normalizeFilterValue(status)) {
            case "INITIATED" -> "已启动";
            case "PREPARING" -> "准备中";
            case "REVIEWING" -> "审核中";
            case "SEALING" -> "封装中";
            case "BIDDING" -> "投标中";
            case "ARCHIVED" -> "已归档";
            default -> status;
        };
    }

    private String translateOutcome(String outcome) {
        return switch (normalizeFilterValue(outcome)) {
            case "WON" -> "已中标";
            case "LOST" -> "未中标";
            case "IN_PROGRESS" -> "进行中";
            default -> outcome;
        };
    }

    private String translateUserRole(String role) {
        return switch (normalizeFilterValue(role)) {
            case "ADMIN" -> "管理员";
            case "MANAGER" -> "经理";
            case "STAFF" -> "员工";
            default -> role;
        };
    }

    private static class TeamAggregate {
        private long projectCount;
        private long managedProjectCount;
        private long wonCount;
        private long activeProjectCount;
        private long totalTaskCount;
        private long completedTaskCount;
        private long overdueTaskCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        public void setTaskMetrics(long totalTaskCount, long completedTaskCount, long overdueTaskCount) {
            this.totalTaskCount = totalTaskCount;
            this.completedTaskCount = completedTaskCount;
            this.overdueTaskCount = overdueTaskCount;
        }
    }

    private static class TeamTaskAggregate {
        private long totalTaskCount;
        private long completedTaskCount;
        private long overdueTaskCount;

        public static TeamTaskAggregate empty() {
            return new TeamTaskAggregate();
        }
    }
}
