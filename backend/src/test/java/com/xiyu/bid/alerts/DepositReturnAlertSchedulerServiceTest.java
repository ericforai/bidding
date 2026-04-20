package com.xiyu.bid.alerts;

import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.alerts.service.AlertHistoryService;
import com.xiyu.bid.alerts.service.AlertRuleExecutionService;
import com.xiyu.bid.businessqualification.application.service.ScanExpiringQualificationsAppService;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.resources.application.service.ScanDepositReturnTrackingAppService;
import com.xiyu.bid.resources.repository.ExpenseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositReturnAlertSchedulerServiceTest {

    @Mock
    private AlertHistoryService alertHistoryService;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TenderRepository tenderRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ScanExpiringQualificationsAppService scanExpiringQualificationsAppService;
    @Mock
    private ScanDepositReturnTrackingAppService scanDepositReturnTrackingAppService;

    @InjectMocks
    private AlertRuleExecutionService alertRuleExecutionService;

    @Test
    @DisplayName("DEPOSIT_RETURN 规则应委托保证金扫描应用服务")
    void shouldDelegateDepositReturnRuleToScanner() {
        when(scanDepositReturnTrackingAppService.scan()).thenReturn(2);

        alertRuleExecutionService.execute(AlertRule.builder()
                .id(91L)
                .name("保证金退还提醒")
                .type(AlertRule.AlertType.DEPOSIT_RETURN)
                .condition(AlertRule.ConditionType.LESS_THAN)
                .threshold(java.math.BigDecimal.valueOf(7))
                .enabled(true)
                .createdBy("tester")
                .build());

        verify(scanDepositReturnTrackingAppService).scan();
    }
}
