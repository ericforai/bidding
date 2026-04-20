// Input: alerts repositories, dedup lookup, and request DTOs
// Output: Alert History business service operations with unresolved-alert dedup
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.alerts.service;

import com.xiyu.bid.alerts.dto.AlertHistoryCreateRequest;
import com.xiyu.bid.alerts.dto.AlertHistoryResponse;
import com.xiyu.bid.alerts.dto.AlertStatisticsResponse;
import com.xiyu.bid.alerts.entity.AlertHistory;
import com.xiyu.bid.alerts.entity.AlertRule;
import com.xiyu.bid.alerts.repository.AlertHistoryRepository;
import com.xiyu.bid.alerts.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertRuleRepository alertRuleRepository;

    @Transactional
    public AlertHistory createAlertHistory(AlertHistoryCreateRequest request) {
        if (request.getRuleId() == null) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        if (request.getLevel() == null) {
            throw new IllegalArgumentException("Level is required");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required");
        }

        AlertRule rule = alertRuleRepository.findById(request.getRuleId())
                .orElseThrow(() -> new RuntimeException("AlertRule not found with id: " + request.getRuleId()));

        AlertHistory existingAlert = null;
        if (request.getRelatedId() != null && !request.getRelatedId().trim().isEmpty()) {
            existingAlert = alertHistoryRepository.findFirstByRuleIdAndRelatedIdAndResolvedFalseOrderByCreatedAtDesc(
                    request.getRuleId(), request.getRelatedId()).orElse(null);
        }
        if (existingAlert != null) {
            log.debug("Returning existing unresolved alert for rule {} and relatedId {}", rule.getId(), request.getRelatedId());
            return existingAlert;
        }

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

    public AlertHistoryResponse getAlertHistoryResponseById(Long id) {
        AlertHistory entity = getAlertHistoryById(id);
        AlertRule rule = alertRuleRepository.findById(entity.getRuleId()).orElse(null);
        return AlertHistoryViewAssembler.toResponse(entity, rule);
    }

    public Page<AlertHistoryResponse> getAllAlertHistories(Pageable pageable, String status, AlertHistory.AlertLevel level, Long ruleId, String relatedId) {
        List<AlertHistory> all = alertHistoryRepository.findAll(pageable.getSort());
        Map<Long, AlertRule> rules = alertRuleRepository.findAllById(
                all.stream().map(AlertHistory::getRuleId).distinct().toList()
        ).stream().collect(Collectors.toMap(AlertRule::getId, item -> item));

        Predicate<AlertHistory> filter = item -> true;
        if (status != null && !status.isBlank()) {
            filter = filter.and(item -> AlertHistoryViewAssembler.resolveStatus(item).equalsIgnoreCase(status));
        }
        if (level != null) {
            filter = filter.and(item -> item.getLevel() == level);
        }
        if (ruleId != null) {
            filter = filter.and(item -> ruleId.equals(item.getRuleId()));
        }
        if (relatedId != null && !relatedId.isBlank()) {
            filter = filter.and(item -> relatedId.equals(item.getRelatedId()));
        }

        List<AlertHistoryResponse> filtered = all.stream()
                .filter(filter)
                .map(item -> AlertHistoryViewAssembler.toResponse(item, rules.get(item.getRuleId())))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<AlertHistoryResponse> content = start >= filtered.size() ? List.of() : filtered.subList(start, end);
        return new PageImpl<>(content, pageable, filtered.size());
    }

    public Page<AlertHistoryResponse> getUnresolvedAlertHistories(Pageable pageable) {
        return getAllAlertHistories(pageable, "ACTIVE", null, null, null);
    }

    @Transactional
    public AlertHistoryResponse acknowledgeAlertHistory(Long id) {
        AlertHistory alertHistory = getAlertHistoryById(id);
        if (Boolean.TRUE.equals(alertHistory.getResolved())) {
            throw new IllegalStateException("Resolved alert cannot be acknowledged again");
        }
        if (alertHistory.getAcknowledgedAt() == null) {
            alertHistory.setAcknowledgedAt(LocalDateTime.now());
        }
        AlertHistory saved = alertHistoryRepository.save(alertHistory);
        AlertRule rule = alertRuleRepository.findById(saved.getRuleId()).orElse(null);
        return AlertHistoryViewAssembler.toResponse(saved, rule);
    }

    @Transactional
    public AlertHistoryResponse resolveAlertHistory(Long id) {
        AlertHistory alertHistory = getAlertHistoryById(id);
        if (alertHistory.getAcknowledgedAt() == null) {
            alertHistory.setAcknowledgedAt(LocalDateTime.now());
        }
        alertHistory.setResolved(true);
        alertHistory.setResolvedAt(LocalDateTime.now());
        AlertHistory saved = alertHistoryRepository.save(alertHistory);
        AlertRule rule = alertRuleRepository.findById(saved.getRuleId()).orElse(null);
        return AlertHistoryViewAssembler.toResponse(saved, rule);
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
