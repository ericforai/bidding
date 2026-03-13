package com.xiyu.bid.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Analytics drill-down filter option
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsFilterOptionDTO {
    private String label;
    private String value;
    private Long count;
}
