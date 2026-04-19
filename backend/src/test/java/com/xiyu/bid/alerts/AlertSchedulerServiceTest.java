package com.xiyu.bid.alerts;

import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.alerts.repository.AlertRuleRepository;
import com.xiyu.bid.alerts.service.AlertRuleExecutionService;
import com.xiyu.bid.alerts.service.AlertSchedulerService;
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
    @Mock private AlertRuleExecutionService alertRuleExecutionService;

    @InjectMocks
    private AlertSchedulerService alertSchedulerService;

    @Test
    @DisplayName("中央调度器应把启用规则委托给规则执行器")
    void shouldDelegateEnabledRuleToExecutionService() {
        AlertRule rule = AlertRule.builder()
                .id(31L)
                .name("保证金退还提醒")
                .type(AlertRule.AlertType.DEPOSIT_RETURN)
                .condition(AlertRule.ConditionType.LESS_THAN)
                .threshold(new BigDecimal("7"))
                .enabled(true)
                .createdBy("tester")
                .build();

        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        alertSchedulerService.checkAlertRules();

        verify(alertRuleExecutionService).execute(rule);
    }
}
