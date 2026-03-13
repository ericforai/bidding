package com.xiyu.bid.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Analytics drill-down row
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDrillDownRowDTO {
    private Long id;
    private Long relatedId;
    private String title;
    private String subtitle;
    private String status;
    private String outcome;
    private String ownerName;
    private String role;
    private BigDecimal amount;
    private Long count;
    private Long wonCount;
    private Long activeProjectCount;
    private Long managedProjectCount;
    private Long totalTaskCount;
    private Long completedTaskCount;
    private Long overdueTaskCount;
    private Double rate;
    private Double taskCompletionRate;
    private Integer score;
    private Integer teamSize;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;
}
