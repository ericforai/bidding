package com.xiyu.bid.alerts;

import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.alerts.repository.AlertRuleRepository;
import com.xiyu.bid.alerts.service.AlertHistoryService;
import com.xiyu.bid.alerts.service.AlertSchedulerService;
import com.xiyu.bid.businessqualification.application.service.ScanExpiringQualificationsAppService;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.resources.repository.ExpenseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertSchedulerService 单元测试")
class AlertSchedulerServiceTest {

    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private AlertHistoryService alertHistoryService;
    @Mock private ProjectRepository projectRepository;
    @Mock private TenderRepository tenderRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ScanExpiringQualificationsAppService scanExpiringQualificationsAppService;

    @InjectMocks
    private AlertSchedulerService alertSchedulerService;

    @Test
    @DisplayName("资质到期规则应委托给资质域扫描器")
    void shouldDelegateQualificationExpiryRuleToDomainScanner() {
        AlertRule rule = AlertRule.builder()
                .id(31L)
                .name("资质到期前 30 天提醒")
                .type(AlertRule.AlertType.QUALIFICATION_EXPIRY)
                .condition(AlertRule.ConditionType.LESS_THAN)
                .threshold(new BigDecimal("30"))
                .enabled(true)
                .createdBy("tester")
                .build();

        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        alertSchedulerService.checkAlertRules();

        verify(scanExpiringQualificationsAppService).scan(30);
    }
}
