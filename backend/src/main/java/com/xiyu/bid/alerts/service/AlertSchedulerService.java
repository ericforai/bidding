// Input: alert rules repository and delegated rule execution service
// Output: Alert Scheduler orchestration for enabled alert rules
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.alerts.service;

import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.alerts.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertSchedulerService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRuleExecutionService alertRuleExecutionService;

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
                alertRuleExecutionService.execute(rule);
            } catch (Exception e) {
                log.error("Error checking alert rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed scheduled alert rule check. Processed {} rules", enabledRules.size());
    }

    /**
     * Manual trigger for alert checking (can be called via API)
     */
    public void triggerAlertCheck() {
        log.info("Manual trigger of alert check at {}", LocalDateTime.now());
        checkAlertRules();
    }
}
