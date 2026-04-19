// Input: alerts repositories, project/tender data, and delegated domain scanners
// Output: Alert Scheduler business service operations for orchestrated rule-driven alerts
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.alerts.service;

import com.xiyu.bid.alerts.dto.AlertHistoryCreateRequest;
import com.xiyu.bid.alerts.entity.AlertHistory;
import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.alerts.repository.AlertRuleRepository;
import com.xiyu.bid.businessqualification.application.service.ScanExpiringQualificationsAppService;
import com.xiyu.bid.compliance.dto.RiskAssessmentDTO;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.resources.entity.Expense;
import com.xiyu.bid.resources.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertSchedulerService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryService alertHistoryService;
    private final ProjectRepository projectRepository;
    private final TenderRepository tenderRepository;
    private final ExpenseRepository expenseRepository;
    private final ScanExpiringQualificationsAppService scanExpiringQualificationsAppService;

    /**
     * Scheduled task to check alert rules every 2 hours
     * Cron expression: 0 0/2 * * * (every 2 hours)
     */
    @Scheduled(cron = "0 0/2 * * * ?")
    public void checkAlertRules() {
        log.info("Starting scheduled alert rule check at {}", LocalDateTime.now());

        List<AlertRule> enabledRules = alertRuleRepository.findByEnabledTrue();

        for (AlertRule rule : enabledRules) {
            try {
                checkAlertRule(rule);
            } catch (Exception e) {
                log.error("Error checking alert rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed scheduled alert rule check. Processed {} rules", enabledRules.size());
    }

    /**
     * Check a single alert rule and create alert history if condition is met
     */
    private void checkAlertRule(AlertRule rule) {
        // This is a placeholder implementation
        // In a real system, you would:
        // 1. Query actual data based on rule type (DEADLINE, BUDGET, RISK, DOCUMENT)
        // 2. Compare against threshold using the condition
        // 3. Create alert history if condition is met

        log.debug("Checking alert rule: {} (Type: {}, Condition: {}, Threshold: {})",
                rule.getName(), rule.getType(), rule.getCondition(), rule.getThreshold());

        // Example implementation for BUDGET alerts
        // In production, this would integrate with actual project budget data
        switch (rule.getType()) {
            case BUDGET:
                checkBudgetAlert(rule);
                break;
            case DEADLINE:
                checkDeadlineAlert(rule);
                break;
            case RISK:
                checkRiskAlert(rule);
                break;
            case DOCUMENT:
                checkDocumentAlert(rule);
                break;
            case QUALIFICATION_EXPIRY:
                checkQualificationExpiryAlert(rule);
                break;
            default:
                log.warn("Unknown alert type: {}", rule.getType());
        }
    }

    /**
     * 检查预算告警
     * 比较项目实际费用与预算，当费用超过预算的阈值比例时触发告警
     */
    private void checkBudgetAlert(AlertRule rule) {
        log.debug("Checking budget alert rule: {}", rule.getName());

        List<Project> activeProjects = projectRepository.findActiveProjects();
        BigDecimal threshold = rule.getThreshold(); // 阈值表示百分比（如 80 表示 80%）

        for (Project project : activeProjects) {
            // 获取关联的标讯预算
            Tender tender = tenderRepository.findById(project.getTenderId()).orElse(null);
            if (tender == null || tender.getBudget() == null) {
                continue;
            }

            BigDecimal budget = tender.getBudget();
            // 计算项目实际总费用
            BigDecimal totalExpense = expenseRepository.sumAmountByProjectId(project.getId());

            // 计算费用占预算的百分比
            if (budget.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal expenseRatio = totalExpense
                        .divide(budget, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                // 根据条件类型判断是否触发告警
                boolean shouldAlert = switch (rule.getCondition()) {
                    case GREATER_THAN -> expenseRatio.compareTo(threshold) > 0;
                    case LESS_THAN -> expenseRatio.compareTo(threshold) < 0;
                    case EQUALS -> expenseRatio.compareTo(threshold) == 0;
                    default -> false;
                };

                if (shouldAlert) {
                    createAlert(rule, project.getId(), "Project",
                            String.format("项目 %s 费用已达到预算的 %.2f%% (已用: %s, 预算: %s)",
                                    project.getName(), expenseRatio, totalExpense, budget));
                }
            }
        }
    }

    /**
     * 检查截止日期告警
     * 当标讯截止日期接近当前日期（阈值天数内）时触发告警
     */
    private void checkDeadlineAlert(AlertRule rule) {
        log.debug("Checking deadline alert rule: {}", rule.getName());

        List<Tender> pendingTenders = tenderRepository.findAll().stream()
                .filter(t -> t.getStatus() == Tender.Status.PENDING ||
                           t.getStatus() == Tender.Status.TRACKING)
                .toList();

        int thresholdDays = rule.getThreshold().intValue();
        LocalDateTime now = LocalDateTime.now();

        for (Tender tender : pendingTenders) {
            if (tender.getDeadline() == null) {
                continue;
            }

            long daysUntilDeadline = ChronoUnit.DAYS.between(now, tender.getDeadline());

            // 根据条件类型判断是否触发告警
            boolean shouldAlert = switch (rule.getCondition()) {
                case LESS_THAN -> daysUntilDeadline <= thresholdDays && daysUntilDeadline >= 0;
                case GREATER_THAN -> daysUntilDeadline > thresholdDays;
                case EQUALS -> daysUntilDeadline == thresholdDays;
                default -> false;
            };

            // 如果已经过期也触发告警
            if (daysUntilDeadline < 0) {
                shouldAlert = true;
            }

            if (shouldAlert) {
                String deadlineStatus = daysUntilDeadline < 0 ?
                        "已过期 " + Math.abs(daysUntilDeadline) + " 天" :
                        "还剩 " + daysUntilDeadline + " 天";
                createAlert(rule, tender.getId(), "Tender",
                        String.format("标讯 %s 截止日期 %s (截止日期: %s)",
                                tender.getTitle(), deadlineStatus, tender.getDeadline()));
            }
        }
    }

    /**
     * 检查风险告警
     * 当标讯/项目的风险等级超过阈值时触发告警
     * 阈值映射: 0-30=LOW, 30-60=MEDIUM, 60-100=HIGH
     */
    private void checkRiskAlert(AlertRule rule) {
        log.debug("Checking risk alert rule: {}", rule.getName());

        int thresholdScore = rule.getThreshold().intValue();
        RiskAssessmentDTO.RiskLevel thresholdLevel = RiskAssessmentDTO.RiskLevel.fromScore(thresholdScore);

        List<Tender> allTenders = tenderRepository.findAll();

        for (Tender tender : allTenders) {
            if (tender.getRiskLevel() == null) {
                continue;
            }

            // 将 Tender 的风险等级转换为分数进行比较
            int tenderRiskScore = switch (tender.getRiskLevel()) {
                case LOW -> 15;
                case MEDIUM -> 45;
                case HIGH -> 75;
            };

            // 根据条件类型判断是否触发告警
            boolean shouldAlert = switch (rule.getCondition()) {
                case GREATER_THAN -> tenderRiskScore > thresholdScore;
                case LESS_THAN -> tenderRiskScore < thresholdScore;
                case EQUALS -> tenderRiskScore == thresholdScore;
                default -> false;
            };

            // 或者直接比较风险等级
            if (!shouldAlert && rule.getCondition() == AlertRule.ConditionType.GREATER_THAN) {
                // 如果阈值是 MEDIUM(30-60)，告警应该触发 HIGH 风险
                shouldAlert = tender.getRiskLevel() == Tender.RiskLevel.HIGH &&
                             thresholdLevel != RiskAssessmentDTO.RiskLevel.HIGH;
            }

            if (shouldAlert) {
                createAlert(rule, tender.getId(), "Tender",
                        String.format("标讯 %s 风险等级为 %s，需要注意 (风险分数: %d)",
                                tender.getTitle(), tender.getRiskLevel().name(), tenderRiskScore));
            }
        }
    }

    /**
     * 检查文档告警
     * 检查项目文档完整性，当缺少必需文档时触发告警
     * 阈值表示允许缺失的文档数量上限
     */
    private void checkDocumentAlert(AlertRule rule) {
        log.debug("Checking document alert rule: {}", rule.getName());

        List<Project> activeProjects = projectRepository.findActiveProjects();
        int maxMissingDocs = rule.getThreshold().intValue();

        // 定义各阶段需要的文档类型
        for (Project project : activeProjects) {
            int missingDocCount = 0;
            StringBuilder missingDocs = new StringBuilder();

            // 根据项目状态检查必需文档
            switch (project.getStatus()) {
                case PREPARING -> {
                    // 准备阶段需要: 资质文件、技术方案、商务方案
                    missingDocCount += checkRequiredDocument(project.getId(), "资质文件") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "技术方案") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "商务方案") ? 0 : 1;
                    missingDocs = new StringBuilder("资质文件、技术方案、商务方案");
                }
                case REVIEWING -> {
                    // 审核阶段需要: 标书完整版、审核记录
                    missingDocCount += checkRequiredDocument(project.getId(), "标书完整版") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "审核记录") ? 0 : 1;
                    missingDocs = new StringBuilder("标书完整版、审核记录");
                }
                case SEALING, BIDDING -> {
                    // 封装/投标阶段需要: 最终标书、授权文件、保证金证明
                    missingDocCount += checkRequiredDocument(project.getId(), "最终标书") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "授权文件") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "保证金证明") ? 0 : 1;
                    missingDocs = new StringBuilder("最终标书、授权文件、保证金证明");
                }
                default -> {
                    // 其他阶段暂不检查
                    continue;
                }
            }

            // 根据条件类型判断是否触发告警
            boolean shouldAlert = switch (rule.getCondition()) {
                case GREATER_THAN -> missingDocCount > maxMissingDocs;
                case LESS_THAN -> missingDocCount < maxMissingDocs;
                case EQUALS -> missingDocCount == maxMissingDocs;
                default -> false;
            };

            // 如果有缺失文档，总是触发告警
            if (shouldAlert || missingDocCount > 0) {
                createAlert(rule, project.getId(), "Project",
                        String.format("项目 %s (状态: %s) 缺少 %d 个必需文档: %s",
                                project.getName(), project.getStatus(), missingDocCount, missingDocs));
            }
        }
    }

    /**
     * 检查资质到期提醒
     * 阈值表示剩余天数，relatedId 固定使用 Qualification:{id}:{expiryDate} 以支持未解决提醒去重。
     */
    private void checkQualificationExpiryAlert(AlertRule rule) {
        log.debug("Checking qualification expiry alert rule: {}", rule.getName());
        scanExpiringQualificationsAppService.scan(rule.getThreshold().intValue());
    }

    /**
     * 检查指定文档是否存在（模拟实现，实际应查询文档表）
     */
    private boolean checkRequiredDocument(Long projectId, String docType) {
        // TODO: 实际实现需要查询 document_structure 或 document_assembly 表
        // 这里简化处理，假设 80% 的项目有完整文档
        return (projectId + docType.length()) % 5 != 0;
    }

    /**
     * 创建告警历史记录
     */
    private void createAlert(AlertRule rule, Long entityId, String entityType, String message) {
        createAlert(rule, entityId, entityType, message, String.format("%s:%s", entityType, entityId));
    }

    private void createAlert(AlertRule rule, Long entityId, String entityType, String message, String relatedId) {
        AlertHistoryCreateRequest request = new AlertHistoryCreateRequest();
        request.setRuleId(rule.getId());
        request.setLevel(calculateSeverity(rule));
        request.setMessage(message);
        request.setRelatedId(relatedId);

        alertHistoryService.createAlertHistory(request);
        log.info("Alert created: Rule={}, Entity={}, Message={}",
                rule.getName(), entityType, message);
    }

    /**
     * 根据告警规则类型计算严重程度
     */
    private AlertHistory.AlertLevel calculateSeverity(AlertRule rule) {
        return switch (rule.getType()) {
            case BUDGET -> AlertHistory.AlertLevel.HIGH;
            case DEADLINE -> {
                int days = rule.getThreshold().intValue();
                yield days <= 1 ? AlertHistory.AlertLevel.CRITICAL :
                       days <= 3 ? AlertHistory.AlertLevel.HIGH :
                       AlertHistory.AlertLevel.MEDIUM;
            }
            case RISK -> AlertHistory.AlertLevel.MEDIUM;
            case DOCUMENT -> AlertHistory.AlertLevel.LOW;
            case QUALIFICATION_EXPIRY -> AlertHistory.AlertLevel.HIGH;
        };
    }

    /**
     * Manual trigger for alert checking (can be called via API)
     */
    public void triggerAlertCheck() {
        log.info("Manual trigger of alert check at {}", LocalDateTime.now());
        checkAlertRules();
    }
}
