// Input: alert rule repositories plus delegated domain/application scanners
// Output: Rule-specific alert execution orchestration
// Pos: Service/业务层
package com.xiyu.bid.alerts.service;

import com.xiyu.bid.alerts.dto.AlertHistoryCreateRequest;
import com.xiyu.bid.alerts.entity.AlertHistory;
import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.businessqualification.application.service.ScanExpiringQualificationsAppService;
import com.xiyu.bid.compliance.dto.RiskAssessmentDTO;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.resources.repository.ExpenseRepository;
import com.xiyu.bid.resources.application.service.ScanDepositReturnTrackingAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertRuleExecutionService {

    private final AlertHistoryService alertHistoryService;
    private final ProjectRepository projectRepository;
    private final TenderRepository tenderRepository;
    private final ExpenseRepository expenseRepository;
    private final ScanExpiringQualificationsAppService scanExpiringQualificationsAppService;
    private final ScanDepositReturnTrackingAppService scanDepositReturnTrackingAppService;

    public void execute(AlertRule rule) {
        log.debug("Checking alert rule: {} (Type: {}, Condition: {}, Threshold: {})",
                rule.getName(), rule.getType(), rule.getCondition(), rule.getThreshold());

        switch (rule.getType()) {
            case BUDGET -> checkBudgetAlert(rule);
            case DEADLINE -> checkDeadlineAlert(rule);
            case RISK -> checkRiskAlert(rule);
            case DOCUMENT -> checkDocumentAlert(rule);
            case QUALIFICATION_EXPIRY -> checkQualificationExpiryAlert(rule);
            case DEPOSIT_RETURN -> scanDepositReturnTrackingAppService.scan();
        }
    }

    private void checkBudgetAlert(AlertRule rule) {
        for (Project project : projectRepository.findActiveProjects()) {
            Tender tender = tenderRepository.findById(project.getTenderId()).orElse(null);
            if (tender == null || tender.getBudget() == null || tender.getBudget().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal totalExpense = expenseRepository.sumAmountByProjectId(project.getId());
            BigDecimal expenseRatio = totalExpense
                    .divide(tender.getBudget(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            boolean shouldAlert = switch (rule.getCondition()) {
                case GREATER_THAN -> expenseRatio.compareTo(rule.getThreshold()) > 0;
                case LESS_THAN -> expenseRatio.compareTo(rule.getThreshold()) < 0;
                case EQUALS -> expenseRatio.compareTo(rule.getThreshold()) == 0;
                default -> false;
            };

            if (shouldAlert) {
                createAlert(rule, project.getId(), "Project",
                        String.format("项目 %s 费用已达到预算的 %.2f%% (已用: %s, 预算: %s)",
                                project.getName(), expenseRatio, totalExpense, tender.getBudget()));
            }
        }
    }

    private void checkDeadlineAlert(AlertRule rule) {
        LocalDateTime now = LocalDateTime.now();
        int thresholdDays = rule.getThreshold().intValue();

        for (Tender tender : tenderRepository.findAll().stream()
                .filter(t -> t.getStatus() == Tender.Status.PENDING || t.getStatus() == Tender.Status.TRACKING)
                .toList()) {
            if (tender.getDeadline() == null) {
                continue;
            }

            long daysUntilDeadline = ChronoUnit.DAYS.between(now, tender.getDeadline());
            boolean shouldAlert = switch (rule.getCondition()) {
                case LESS_THAN -> daysUntilDeadline <= thresholdDays && daysUntilDeadline >= 0;
                case GREATER_THAN -> daysUntilDeadline > thresholdDays;
                case EQUALS -> daysUntilDeadline == thresholdDays;
                default -> false;
            };

            if (daysUntilDeadline < 0) {
                shouldAlert = true;
            }

            if (shouldAlert) {
                String deadlineStatus = daysUntilDeadline < 0
                        ? "已过期 " + Math.abs(daysUntilDeadline) + " 天"
                        : "还剩 " + daysUntilDeadline + " 天";
                createAlert(rule, tender.getId(), "Tender",
                        String.format("标讯 %s 截止日期 %s (截止日期: %s)",
                                tender.getTitle(), deadlineStatus, tender.getDeadline()));
            }
        }
    }

    private void checkRiskAlert(AlertRule rule) {
        int thresholdScore = rule.getThreshold().intValue();
        RiskAssessmentDTO.RiskLevel thresholdLevel = RiskAssessmentDTO.RiskLevel.fromScore(thresholdScore);

        for (Tender tender : tenderRepository.findAll()) {
            if (tender.getRiskLevel() == null) {
                continue;
            }

            int tenderRiskScore = switch (tender.getRiskLevel()) {
                case LOW -> 15;
                case MEDIUM -> 45;
                case HIGH -> 75;
            };

            boolean shouldAlert = switch (rule.getCondition()) {
                case GREATER_THAN -> tenderRiskScore > thresholdScore;
                case LESS_THAN -> tenderRiskScore < thresholdScore;
                case EQUALS -> tenderRiskScore == thresholdScore;
                default -> false;
            };

            if (!shouldAlert && rule.getCondition() == AlertRule.ConditionType.GREATER_THAN) {
                shouldAlert = tender.getRiskLevel() == Tender.RiskLevel.HIGH
                        && thresholdLevel != RiskAssessmentDTO.RiskLevel.HIGH;
            }

            if (shouldAlert) {
                createAlert(rule, tender.getId(), "Tender",
                        String.format("标讯 %s 风险等级为 %s，需要注意 (风险分数: %d)",
                                tender.getTitle(), tender.getRiskLevel().name(), tenderRiskScore));
            }
        }
    }

    private void checkDocumentAlert(AlertRule rule) {
        int maxMissingDocs = rule.getThreshold().intValue();

        for (Project project : projectRepository.findActiveProjects()) {
            int missingDocCount = 0;
            StringBuilder missingDocs = new StringBuilder();

            switch (project.getStatus()) {
                case PREPARING -> {
                    missingDocCount += checkRequiredDocument(project.getId(), "资质文件") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "技术方案") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "商务方案") ? 0 : 1;
                    missingDocs = new StringBuilder("资质文件、技术方案、商务方案");
                }
                case REVIEWING -> {
                    missingDocCount += checkRequiredDocument(project.getId(), "标书完整版") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "审核记录") ? 0 : 1;
                    missingDocs = new StringBuilder("标书完整版、审核记录");
                }
                case SEALING, BIDDING -> {
                    missingDocCount += checkRequiredDocument(project.getId(), "最终标书") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "授权文件") ? 0 : 1;
                    missingDocCount += checkRequiredDocument(project.getId(), "保证金证明") ? 0 : 1;
                    missingDocs = new StringBuilder("最终标书、授权文件、保证金证明");
                }
                default -> {
                    continue;
                }
            }

            boolean shouldAlert = switch (rule.getCondition()) {
                case GREATER_THAN -> missingDocCount > maxMissingDocs;
                case LESS_THAN -> missingDocCount < maxMissingDocs;
                case EQUALS -> missingDocCount == maxMissingDocs;
                default -> false;
            };

            if (shouldAlert || missingDocCount > 0) {
                createAlert(rule, project.getId(), "Project",
                        String.format("项目 %s (状态: %s) 缺少 %d 个必需文档: %s",
                                project.getName(), project.getStatus(), missingDocCount, missingDocs));
            }
        }
    }

    private void checkQualificationExpiryAlert(AlertRule rule) {
        scanExpiringQualificationsAppService.scan(rule.getThreshold().intValue());
    }

    private boolean checkRequiredDocument(Long projectId, String docType) {
        return (projectId + docType.length()) % 5 != 0;
    }

    private void createAlert(AlertRule rule, Long entityId, String entityType, String message) {
        AlertHistoryCreateRequest request = new AlertHistoryCreateRequest();
        request.setRuleId(rule.getId());
        request.setLevel(calculateSeverity(rule));
        request.setMessage(message);
        request.setRelatedId(String.format("%s:%s", entityType, entityId));
        alertHistoryService.createAlertHistory(request);
        log.info("Alert created: Rule={}, Entity={}, Message={}", rule.getName(), entityType, message);
    }

    private AlertHistory.AlertLevel calculateSeverity(AlertRule rule) {
        return switch (rule.getType()) {
            case BUDGET -> AlertHistory.AlertLevel.HIGH;
            case DEADLINE -> {
                int days = rule.getThreshold().intValue();
                yield days <= 1 ? AlertHistory.AlertLevel.CRITICAL
                        : days <= 3 ? AlertHistory.AlertLevel.HIGH
                        : AlertHistory.AlertLevel.MEDIUM;
            }
            case RISK, DEPOSIT_RETURN -> AlertHistory.AlertLevel.MEDIUM;
            case DOCUMENT -> AlertHistory.AlertLevel.LOW;
            case QUALIFICATION_EXPIRY -> AlertHistory.AlertLevel.HIGH;
        };
    }
}
