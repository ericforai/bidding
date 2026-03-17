package com.xiyu.bid.alerts.service;

import com.xiyu.bid.alerts.dto.AlertHistoryCreateRequest;
import com.xiyu.bid.alerts.dto.AlertStatisticsResponse;
import com.xiyu.bid.alerts.entity.AlertHistory;
import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.alerts.repository.AlertHistoryRepository;
import com.xiyu.bid.alerts.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertRuleRepository alertRuleRepository;

    @Transactional
    public AlertHistory createAlertHistory(AlertHistoryCreateRequest request) {
        // Validation
        if (request.getRuleId() == null) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        if (request.getLevel() == null) {
            throw new IllegalArgumentException("Level is required");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required");
        }

        // Verify rule exists
        AlertRule rule = alertRuleRepository.findById(request.getRuleId())
                .orElseThrow(() -> new RuntimeException("AlertRule not found with id: " + request.getRuleId()));

        AlertHistory alertHistory = AlertHistory.builder()
                .ruleId(request.getRuleId())
                .level(request.getLevel())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .resolved(false)
                .build();

        return alertHistoryRepository.save(alertHistory);
    }

    public AlertHistory getAlertHistoryById(Long id) {
        return alertHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AlertHistory not found with id: " + id));
    }

    public Page<AlertHistory> getAllAlertHistories(Pageable pageable) {
        return alertHistoryRepository.findAll(pageable);
    }

    public Page<AlertHistory> getAlertHistoriesByRuleId(Long ruleId, Pageable pageable) {
        return alertHistoryRepository.findByRuleId(ruleId, pageable);
    }

    public Page<AlertHistory> getAlertHistoriesByLevel(AlertHistory.AlertLevel level, Pageable pageable) {
        return alertHistoryRepository.findByLevel(level, pageable);
    }

    public Page<AlertHistory> getUnresolvedAlertHistories(Pageable pageable) {
        return alertHistoryRepository.findByResolvedFalse(pageable);
    }

    public Page<AlertHistory> getAlertHistoriesByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return alertHistoryRepository.findByCreatedAtBetween(startDate, endDate, pageable);
    }

    public Page<AlertHistory> getAlertHistoriesByRelatedId(String relatedId, Pageable pageable) {
        return alertHistoryRepository.findByRelatedId(relatedId, pageable);
    }

    @Transactional
    public AlertHistory resolveAlertHistory(Long id) {
        AlertHistory alertHistory = getAlertHistoryById(id);
        alertHistory.setResolved(true);
        alertHistory.setResolvedAt(LocalDateTime.now());
        return alertHistoryRepository.save(alertHistory);
    }

    @Transactional
    public int deleteOldResolvedAlertHistories(LocalDateTime cutoffDate) {
        return alertHistoryRepository.deleteByResolvedTrueAndResolvedAtBefore(cutoffDate);
    }

    public AlertStatisticsResponse getAlertStatistics() {
        return AlertStatisticsResponse.builder()
                .totalAlerts(alertHistoryRepository.count())
                .unresolvedAlerts(alertHistoryRepository.countByResolvedFalse())
                .highAlerts(alertHistoryRepository.countByLevel(AlertHistory.AlertLevel.HIGH))
                .mediumAlerts(alertHistoryRepository.countByLevel(AlertHistory.AlertLevel.MEDIUM))
                .lowAlerts(alertHistoryRepository.countByLevel(AlertHistory.AlertLevel.LOW))
                .criticalAlerts(alertHistoryRepository.countByLevel(AlertHistory.AlertLevel.CRITICAL))
                .build();
    }
}
