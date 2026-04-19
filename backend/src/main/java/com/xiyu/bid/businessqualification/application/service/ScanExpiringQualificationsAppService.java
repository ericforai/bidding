package com.xiyu.bid.businessqualification.application.service;

import com.xiyu.bid.alerts.dto.AlertHistoryCreateRequest;
import com.xiyu.bid.alerts.entity.AlertHistory;
import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.alerts.repository.AlertRuleRepository;
import com.xiyu.bid.alerts.service.AlertHistoryService;
import com.xiyu.bid.businessqualification.domain.model.BusinessQualification;
import com.xiyu.bid.businessqualification.domain.port.BusinessQualificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanExpiringQualificationsAppService {

    private final BusinessQualificationRepository qualificationRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryService alertHistoryService;

    @Transactional
    public int scan(int thresholdDays) {
        AlertRule rule = ensureAlertRule(thresholdDays);
        List<BusinessQualification> qualifications = qualificationRepository.findExpiringWithinDays(thresholdDays);
        int created = 0;
        for (BusinessQualification qualification : qualifications) {
            if (qualification.remainingDays() < 0 || !qualification.reminderPolicy().isEnabled()) {
                continue;
            }
            AlertHistoryCreateRequest request = new AlertHistoryCreateRequest();
            request.setRuleId(rule.getId());
            request.setLevel(AlertHistory.AlertLevel.HIGH);
            request.setRelatedId(String.format("Qualification:%s:%s", qualification.id(), qualification.validityPeriod().getExpiryDate()));
            request.setMessage(String.format(
                    "资质 %s 将在 %d 天后到期",
                    qualification.name(),
                    qualification.remainingDays()));
            alertHistoryService.createAlertHistory(request);
            created++;
        }
        return created;
    }

    private AlertRule ensureAlertRule(int thresholdDays) {
        return alertRuleRepository.findByType(AlertRule.AlertType.QUALIFICATION_EXPIRY).stream()
                .findFirst()
                .orElseGet(() -> alertRuleRepository.save(AlertRule.builder()
                        .name("资质到期提醒")
                        .type(AlertRule.AlertType.QUALIFICATION_EXPIRY)
                        .condition(AlertRule.ConditionType.LESS_THAN)
                        .threshold(BigDecimal.valueOf(thresholdDays))
                        .enabled(true)
                        .createdBy("system")
                        .build()));
    }
}
