package com.xiyu.bid.resources.application.service;

import com.xiyu.bid.alerts.dto.AlertHistoryCreateRequest;
import com.xiyu.bid.alerts.entity.AlertHistory;
import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.alerts.repository.AlertRuleRepository;
import com.xiyu.bid.alerts.service.AlertHistoryService;
import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import com.xiyu.bid.bidresult.repository.BidResultFetchResultRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.resources.domain.model.DepositReturnTrackingSnapshot;
import com.xiyu.bid.resources.domain.service.DepositReturnReminderPolicy;
import com.xiyu.bid.resources.dto.ResourceResponseMapper;
import com.xiyu.bid.resources.dto.ExpenseDTO;
import com.xiyu.bid.resources.entity.Expense;
import com.xiyu.bid.resources.repository.ExpenseRepository;
import com.xiyu.bid.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SendExpenseReturnReminderAppService {

    private final ExpenseRepository expenseRepository;
    private final BidResultFetchResultRepository bidResultFetchResultRepository;
    private final ProjectRepository projectRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryService alertHistoryService;
    private final SettingsService settingsService;

    private final DepositReturnReminderPolicy reminderPolicy = new DepositReturnReminderPolicy();

    @Transactional
    public ExpenseDTO send(Long expenseId, String actor, String comment) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", String.valueOf(expenseId)));
        if (!expense.isReturnable()) {
            throw new IllegalStateException("Only deposit-like expenses can be reminded");
        }
        BidResultFetchResult result = bidResultFetchResultRepository
                .findFirstByProjectIdAndStatusOrderByConfirmedAtDescFetchTimeDesc(
                        expense.getProjectId(),
                        BidResultFetchResult.Status.CONFIRMED)
                .orElse(null);
        if (!reminderPolicy.canSendManualReminder(DepositReturnTrackingSnapshot.builder()
                .expenseId(expense.getId())
                .projectId(expense.getProjectId())
                .expenseStatus(expense.getStatus())
                .expectedReturnDate(expense.getExpectedReturnDate())
                .lastReminderAt(expense.getLastReturnReminderAt())
                .bidResult(result == null ? null : result.getResult())
                .build())) {
            throw new IllegalStateException("Expense is not eligible for deposit return reminder");
        }

        AlertRule rule = alertRuleRepository.findByType(AlertRule.AlertType.DEPOSIT_RETURN).stream()
                .findFirst()
                .orElseGet(() -> alertRuleRepository.save(AlertRule.builder()
                        .name("保证金退还提醒")
                        .type(AlertRule.AlertType.DEPOSIT_RETURN)
                        .condition(AlertRule.ConditionType.LESS_THAN)
                        .threshold(BigDecimal.valueOf(settingsService.getSettings().getSystemConfig().getDepositWarnDays()))
                        .enabled(true)
                        .createdBy("system")
                        .build()));

        String projectName = projectRepository.findById(expense.getProjectId())
                .map(Project::getName)
                .orElse("项目#" + expense.getProjectId());
        AlertHistoryCreateRequest request = new AlertHistoryCreateRequest();
        request.setRuleId(rule.getId());
        request.setLevel(AlertHistory.AlertLevel.MEDIUM);
        request.setRelatedId(String.format("DepositReturn:%s:%s", expenseId, expense.getExpectedReturnDate()));
        request.setMessage(String.format(
                "%s 发起保证金退还跟进：%s，应退日期 %s。%s",
                actor,
                projectName,
                expense.getExpectedReturnDate(),
                comment == null ? "" : comment
        ).trim());
        alertHistoryService.createAlertHistory(request);
        expense.recordReturnReminder(LocalDateTime.now());
        return ResourceResponseMapper.toDto(expenseRepository.save(expense));
    }
}
