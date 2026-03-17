package com.xiyu.bid.alerts.controller;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.alerts.dto.AlertHistoryCreateRequest;
import com.xiyu.bid.alerts.dto.AlertStatisticsResponse;
import com.xiyu.bid.alerts.entity.AlertHistory;
import com.xiyu.bid.alerts.service.AlertHistoryService;
import com.xiyu.bid.config.PaginationConstants;
import com.xiyu.bid.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/alerts/history")
@RequiredArgsConstructor
public class AlertHistoryController {

    private final AlertHistoryService alertHistoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "CREATE", entityType = "AlertHistory", description = "Create alert history record")
    public ResponseEntity<ApiResponse<AlertHistory>> createAlertHistory(@Valid @RequestBody AlertHistoryCreateRequest request) {
        AlertHistory alertHistory = alertHistoryService.createAlertHistory(request);
        return ResponseEntity.ok(ApiResponse.success("Alert history created successfully", alertHistory));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<AlertHistory>> getAlertHistoryById(@PathVariable Long id) {
        AlertHistory alertHistory = alertHistoryService.getAlertHistoryById(id);
        return ResponseEntity.ok(ApiResponse.success(alertHistory));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<AlertHistory>>> getAllAlertHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // 安全限制：防止过大的分页请求
        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AlertHistory> alertHistories = alertHistoryService.getAllAlertHistories(pageable);
        return ResponseEntity.ok(ApiResponse.success(alertHistories));
    }

    @GetMapping("/rule/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<AlertHistory>>> getAlertHistoriesByRuleId(
            @PathVariable Long ruleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlertHistory> alertHistories = alertHistoryService.getAlertHistoriesByRuleId(ruleId, pageable);
        return ResponseEntity.ok(ApiResponse.success(alertHistories));
    }

    @GetMapping("/level/{level}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<AlertHistory>>> getAlertHistoriesByLevel(
            @PathVariable AlertHistory.AlertLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlertHistory> alertHistories = alertHistoryService.getAlertHistoriesByLevel(level, pageable);
        return ResponseEntity.ok(ApiResponse.success(alertHistories));
    }

    @GetMapping("/unresolved")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<AlertHistory>>> getUnresolvedAlertHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlertHistory> alertHistories = alertHistoryService.getUnresolvedAlertHistories(pageable);
        return ResponseEntity.ok(ApiResponse.success(alertHistories));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<AlertHistory>>> getAlertHistoriesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlertHistory> alertHistories = alertHistoryService.getAlertHistoriesByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(alertHistories));
    }

    @GetMapping("/related/{relatedId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<AlertHistory>>> getAlertHistoriesByRelatedId(
            @PathVariable String relatedId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlertHistory> alertHistories = alertHistoryService.getAlertHistoriesByRelatedId(relatedId, pageable);
        return ResponseEntity.ok(ApiResponse.success(alertHistories));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "RESOLVE", entityType = "AlertHistory", description = "Resolve alert history")
    public ResponseEntity<ApiResponse<AlertHistory>> resolveAlertHistory(@PathVariable Long id) {
        AlertHistory alertHistory = alertHistoryService.resolveAlertHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Alert history resolved successfully", alertHistory));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<AlertStatisticsResponse>> getAlertStatistics() {
        AlertStatisticsResponse statistics = alertHistoryService.getAlertStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
