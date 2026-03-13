package com.xiyu.bid.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Analytics drill-down filter metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDrillDownFiltersDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<AnalyticsFilterDimensionDTO> dimensions;
}
