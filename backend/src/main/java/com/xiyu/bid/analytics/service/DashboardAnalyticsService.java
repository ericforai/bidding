// Input: Repository (Tender, Project, Task)
// Output: Dashboard Analytics Data
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.analytics.service;

import com.xiyu.bid.analytics.dto.*;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
}
