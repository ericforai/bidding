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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
}
