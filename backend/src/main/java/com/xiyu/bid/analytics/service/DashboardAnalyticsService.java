// Input: Repository (Tender, Project, Task)
// Output: Dashboard Analytics Data
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.analytics.service;

import com.xiyu.bid.analytics.dto.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String ALL_FILTER = "ALL";

    /**
     * Get complete dashboard overview with caching
     */
    @Cacheable(value = "dashboard:overview", key = "'overview'")
    public DashboardOverviewDTO getOverview() {
        log.debug("Fetching dashboard overview from database");

        SummaryStats summaryStats = getSummaryStats();
        List<TrendData> tenderTrends = getTenderTrends();
        List<TrendData> projectTrends = getProjectTrends();
        Map<String, Long> statusDistribution = getStatusDistribution();
        List<CompetitorData> topCompetitors = getTopCompetitors(5);
        List<RegionalData> regionalDistribution = getRegionalDistribution();

        return DashboardOverviewDTO.builder()
                .summaryStats(summaryStats)
                .tenderTrends(tenderTrends)
                .projectTrends(projectTrends)
                .statusDistribution(statusDistribution)
                .topCompetitors(topCompetitors)
                .regionalDistribution(regionalDistribution)
                .build();
    }

    /**
     * Get summary statistics
     */
    public SummaryStats getSummaryStats() {
        List<Tender> allTenders = tenderRepository.findAll();
        List<Project> activeProjects = projectRepository.findActiveProjects();
        List<Task> pendingTasks = taskRepository.findByStatus(Task.Status.TODO);

        long totalTenders = allTenders.size();
        long activeProjectsCount = activeProjects.size();
        long pendingTasksCount = pendingTasks.size();

        // Calculate total budget
        BigDecimal totalBudget = allTenders.stream()
                .map(Tender::getBudget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate success rate
        double successRate = calculateSuccessRate(allTenders, activeProjects);

        return SummaryStats.builder()
                .totalTenders(totalTenders)
                .activeProjects(activeProjectsCount)
                .pendingTasks(pendingTasksCount)
                .totalBudget(totalBudget)
                .successRate(successRate)
                .build();
    }

    /**
     * Get tender trends grouped by month
     */
    public List<TrendData> getTenderTrends() {
        List<Tender> tenders = tenderRepository.findAll();

        // Group by month
        Map<String, List<Tender>> groupedByMonth = tenders.stream()
                .collect(Collectors.groupingBy(tender ->
                        tender.getCreatedAt().format(MONTH_FORMATTER)));

        // Calculate trends
        List<TrendData> trends = new ArrayList<>();
        List<String> sortedMonths = new ArrayList<>(groupedByMonth.keySet());
        Collections.sort(sortedMonths);

        Long previousCount = null;
        for (String month : sortedMonths) {
            List<Tender> monthTenders = groupedByMonth.get(month);
            long count = monthTenders.size();
            BigDecimal value = monthTenders.stream()
                    .map(Tender::getBudget)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Double changePercentage = null;
            if (previousCount != null && previousCount > 0) {
                changePercentage = ((count - previousCount) * 100.0) / previousCount;
            }

            trends.add(TrendData.builder()
                    .period(month)
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
        List<Project> projects = projectRepository.findAll();

        // Group by month
        Map<String, List<Project>> groupedByMonth = projects.stream()
                .collect(Collectors.groupingBy(project ->
                        project.getCreatedAt().format(MONTH_FORMATTER)));

        // Calculate trends
        List<TrendData> trends = new ArrayList<>();
        List<String> sortedMonths = new ArrayList<>(groupedByMonth.keySet());
        Collections.sort(sortedMonths);

        Long previousCount = null;
        for (String month : sortedMonths) {
            List<Project> monthProjects = groupedByMonth.get(month);
            long count = monthProjects.size();

            Double changePercentage = null;
            if (previousCount != null && previousCount > 0) {
                changePercentage = ((count - previousCount) * 100.0) / previousCount;
            }

            trends.add(TrendData.builder()
                    .period(month)
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
        Map<String, Long> distribution = new ConcurrentHashMap<>();

        distribution.put("PENDING", tenderRepository.countByStatus(Tender.Status.PENDING));
        distribution.put("TRACKING", tenderRepository.countByStatus(Tender.Status.TRACKING));
        distribution.put("BIDDED", tenderRepository.countByStatus(Tender.Status.BIDDED));
        distribution.put("ABANDONED", tenderRepository.countByStatus(Tender.Status.ABANDONED));

        return distribution;
    }

    /**
     * Get top competitors by bid count
     */
    public List<CompetitorData> getTopCompetitors(Integer limit) {
        // Since we don't have competitor data in Tender entity,
        // this is a placeholder implementation
        // In real scenario, you would have a separate Competitor entity or bid history

        List<Tender> tenders = tenderRepository.findAll();
        Map<String, List<Tender>> tendersBySource = tenders.stream()
                .filter(t -> t.getSource() != null)
                .collect(Collectors.groupingBy(Tender::getSource));

        List<CompetitorData> competitors = tendersBySource.entrySet().stream()
                .map(entry -> {
                    String source = entry.getKey();
                    List<Tender> sourceTenders = entry.getValue();

                    // Simplified calculation - treating sources as competitors
                    long bidCount = sourceTenders.size();
                    long winCount = sourceTenders.stream()
                            .filter(t -> t.getStatus() == Tender.Status.BIDDED)
                            .count();
                    double winRate = bidCount > 0 ? (winCount * 100.0) / bidCount : 0.0;

                    BigDecimal totalBidAmount = sourceTenders.stream()
                            .map(Tender::getBudget)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return CompetitorData.builder()
                            .name(source)
                            .bidCount(bidCount)
                            .winCount(winCount)
                            .winRate(winRate)
                            .totalBidAmount(totalBidAmount)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getBidCount(), a.getBidCount()))
                .collect(Collectors.toList());

        // Apply limit if specified
        if (limit != null && limit > 0 && competitors.size() > limit) {
            return competitors.subList(0, limit);
        }

        return competitors;
    }

    /**
     * Get regional distribution of tenders
     */
    public List<RegionalData> getRegionalDistribution() {
        List<Tender> tenders = tenderRepository.findAll();

        if (tenders.isEmpty()) {
            return Collections.emptyList();
        }

        // Since we don't have region field in Tender entity,
        // we'll use source as a proxy for region
        // In real scenario, you would have a region field

        Map<String, List<Tender>> tendersBySource = tenders.stream()
                .filter(t -> t.getSource() != null)
                .collect(Collectors.groupingBy(Tender::getSource));

        long totalTenders = tenders.size();
        BigDecimal grandTotalBudget = tenders.stream()
                .map(Tender::getBudget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RegionalData> regionalData = tendersBySource.entrySet().stream()
                .map(entry -> {
                    String source = entry.getKey();
                    List<Tender> sourceTenders = entry.getValue();

                    long count = sourceTenders.size();
                    BigDecimal budget = sourceTenders.stream()
                            .map(Tender::getBudget)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    double percentage = totalTenders > 0 ? (count * 100.0) / totalTenders : 0.0;

                    return RegionalData.builder()
                            .region(source)
                            .tenderCount(count)
                            .totalBudget(budget)
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getTenderCount(), a.getTenderCount()))
                .collect(Collectors.toList());

        return regionalData;
    }

    public List<ProductLineData> getProductLinePerformance() {
        Map<String, List<Tender>> tendersByProductLine = tenderRepository.findAll().stream()
                .collect(Collectors.groupingBy(tender -> classifyProductLine(tender.getTitle())));

        return tendersByProductLine.entrySet().stream()
                .map(entry -> {
                    List<Tender> tenders = entry.getValue();
                    long bidCount = tenders.size();
                    long wonCount = tenders.stream()
                            .filter(tender -> tender.getStatus() == Tender.Status.BIDDED)
                            .count();
                    BigDecimal revenue = tenders.stream()
                            .map(Tender::getBudget)
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
        List<Tender> allTenders = tenderRepository.findAll();
        List<Project> allProjects = projectRepository.findAll();
        List<Task> allTasks = taskRepository.findAll();

        List<Tender> matchedTenders = switch (type.toLowerCase(Locale.ROOT)) {
            case "trend" -> allTenders.stream()
                    .filter(tender -> tender.getCreatedAt() != null)
                    .filter(tender -> tender.getCreatedAt().format(MONTH_FORMATTER).equals(key))
                    .collect(Collectors.toList());
            case "competitor", "region" -> allTenders.stream()
                    .filter(tender -> key.equals(tender.getSource()))
                    .collect(Collectors.toList());
            case "product" -> allTenders.stream()
                    .filter(tender -> classifyProductLine(tender.getTitle()).equals(key))
                    .collect(Collectors.toList());
            default -> Collections.emptyList();
        };

        Set<Long> tenderIds = matchedTenders.stream()
                .map(Tender::getId)
                .collect(Collectors.toSet());

        List<Project> matchedProjects = allProjects.stream()
                .filter(project -> tenderIds.contains(project.getTenderId()))
                .collect(Collectors.toList());

        Set<Long> projectIds = matchedProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toSet());

        List<Task> matchedTasks = allTasks.stream()
                .filter(task -> projectIds.contains(task.getProjectId()))
                .collect(Collectors.toList());

        List<AnalyticsDrillDownProjectDTO> projectItems = matchedProjects.stream()
                .map(project -> AnalyticsDrillDownProjectDTO.builder()
                        .id(project.getId())
                        .name(project.getName())
                        .customer(resolveProjectCustomer(project))
                        .budget(resolveTenderBudget(project.getTenderId(), matchedTenders))
                        .status(project.getStatus() == null ? "-" : project.getStatus().name().toLowerCase(Locale.ROOT))
                        .manager(resolveUserDisplayName(project.getManagerId()))
                        .result(resolveProjectResult(project))
                        .build())
                .collect(Collectors.toList());

        List<AnalyticsDrillDownTeamDTO> teamItems = buildTeamItems(matchedProjects, matchedTasks);
        List<AnalyticsDrillDownFileDTO> fileItems = buildFileItems(matchedProjects);
        long totalParticipation = matchedTenders.size();
        long wonCount = matchedTenders.stream().filter(tender -> tender.getStatus() == Tender.Status.BIDDED).count();
        double teamWinRate = totalParticipation > 0 ? (wonCount * 100.0) / totalParticipation : 0.0;
        BigDecimal totalAmount = matchedTenders.stream()
                .map(Tender::getBudget)
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
        Map<Long, Project> projectByTenderId = projectRepository.findAll().stream()
                .collect(Collectors.toMap(Project::getTenderId, Function.identity(), (left, right) -> left));

        List<Tender> dateFilteredTenders = tenderRepository.findAll().stream()
                .filter(tender -> isWithinDateRange(tender.getCreatedAt(), startDate, endDate))
                .toList();

        List<AnalyticsDrillDownRowDTO> baseRows = dateFilteredTenders.stream()
                .map(tender -> {
                    Project project = projectByTenderId.get(tender.getId());
                    return AnalyticsDrillDownRowDTO.builder()
                            .id(tender.getId())
                            .relatedId(project != null ? project.getId() : null)
                            .title(tender.getTitle())
                            .subtitle(defaultString(tender.getSource(), "未知来源"))
                            .status(tender.getStatus().name())
                            .ownerName(project != null ? project.getName() : "未关联项目")
                            .amount(defaultAmount(tender.getBudget()))
                            .score(tender.getAiScore())
                            .createdAt(tender.getCreatedAt())
                            .deadline(tender.getDeadline())
                            .build();
                })
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
        Map<Long, Tender> tenderById = tenderRepository.findAll().stream()
                .collect(Collectors.toMap(Tender::getId, Function.identity()));
        Map<Long, String> userNameMap = getUserNameMap();

        List<AnalyticsDrillDownRowDTO> baseRows = tenderRepository.findAll().stream()
                .filter(tender -> isWithinDateRange(tender.getCreatedAt(), startDate, endDate))
                .map(tender -> {
                    Project project = projectRepository.findByTenderId(tender.getId()).stream().findFirst().orElse(null);
                    String derivedOutcome = deriveOutcome(tender, project);

                    return AnalyticsDrillDownRowDTO.builder()
                            .id(tender.getId())
                            .relatedId(project != null ? project.getId() : null)
                            .title(tender.getTitle())
                            .subtitle(project != null ? project.getName() : "未形成项目")
                            .status(project != null ? project.getStatus().name() : tender.getStatus().name())
                            .outcome(derivedOutcome)
                            .ownerName(project != null ? userNameMap.getOrDefault(project.getManagerId(), fallbackUserName(project.getManagerId())) : "-")
                            .amount(defaultAmount(tender.getBudget()))
                            .rate("WON".equals(derivedOutcome) ? 100.0 : 0.0)
                            .createdAt(tender.getCreatedAt())
                            .deadline(tender.getDeadline())
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
        List<Project> filteredProjects = projectRepository.findAll().stream()
                .filter(project -> isWithinDateRange(getProjectReferenceDate(project), startDate, endDate))
                .toList();
        Map<Long, Tender> tenderById = tenderRepository.findAll().stream()
                .collect(Collectors.toMap(Tender::getId, Function.identity()));
        Map<Long, User> userById = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Set<Long> relevantProjectIds = filteredProjects.stream().map(Project::getId).collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        Map<Long, TeamTaskAggregate> taskAggregateByAssignee = taskRepository.findAll().stream()
                .filter(task -> relevantProjectIds.contains(task.getProjectId()))
                .filter(task -> task.getAssigneeId() != null)
                .collect(Collectors.groupingBy(Task::getAssigneeId, Collectors.collectingAndThen(Collectors.toList(), tasks -> {
                    TeamTaskAggregate aggregate = new TeamTaskAggregate();
                    aggregate.totalTaskCount = tasks.size();
                    aggregate.completedTaskCount = tasks.stream()
                            .filter(task -> task.getStatus() == Task.Status.COMPLETED)
                            .count();
                    aggregate.overdueTaskCount = tasks.stream()
                            .filter(task -> task.getDueDate() != null)
                            .filter(task -> task.getDueDate().isBefore(now))
                            .filter(task -> task.getStatus() != Task.Status.COMPLETED)
                            .filter(task -> task.getStatus() != Task.Status.CANCELLED)
                            .count();
                    return aggregate;
                })));

        Map<Long, TeamAggregate> aggregates = new HashMap<>();
        for (Project project : filteredProjects) {
            Tender tender = tenderById.get(project.getTenderId());
            BigDecimal amount = tender != null ? defaultAmount(tender.getBudget()) : BigDecimal.ZERO;
            boolean won = tender != null && tender.getStatus() == Tender.Status.BIDDED;
            boolean active = project.getStatus() != Project.Status.ARCHIVED;

            Long managerId = project.getManagerId();
            if (managerId != null) {
                accumulateTeamAggregate(aggregates, managerId, amount, won, active, true);
            }

            Set<Long> uniqueMembers = new LinkedHashSet<>(Optional.ofNullable(project.getTeamMembers()).orElse(List.of()));
            uniqueMembers.remove(managerId);
            for (Long memberId : uniqueMembers) {
                accumulateTeamAggregate(aggregates, memberId, amount, won, active, false);
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
                            .role(user != null ? user.getRole().name() : "UNKNOWN")
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
                                .map(project -> tenderById.get(project.getTenderId()))
                                .filter(Objects::nonNull)
                                .filter(tender -> tender.getStatus() == Tender.Status.BIDDED)
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
        Map<Long, Tender> tenderById = tenderRepository.findAll().stream()
                .collect(Collectors.toMap(Tender::getId, Function.identity()));
        Map<Long, String> userNameMap = getUserNameMap();

        List<AnalyticsDrillDownRowDTO> baseRows = projectRepository.findAll().stream()
                .filter(project -> isWithinDateRange(getProjectReferenceDate(project), startDate, endDate))
                .map(project -> {
                    Tender tender = tenderById.get(project.getTenderId());
                    return AnalyticsDrillDownRowDTO.builder()
                            .id(project.getId())
                            .relatedId(project.getTenderId())
                            .title(project.getName())
                            .subtitle(tender != null ? tender.getTitle() : "未关联标讯")
                            .status(project.getStatus().name())
                            .ownerName(userNameMap.getOrDefault(project.getManagerId(), fallbackUserName(project.getManagerId())))
                            .amount(tender != null ? defaultAmount(tender.getBudget()) : BigDecimal.ZERO)
                            .teamSize(Optional.ofNullable(project.getTeamMembers()).orElse(List.of()).size())
                            .createdAt(getProjectReferenceDate(project))
                            .deadline(project.getEndDate())
                            .build();
                })
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
                            .role(user != null ? user.getRole().name().toLowerCase(Locale.ROOT) : "member")
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

    private Map<Long, String> getUserNameMap() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
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
