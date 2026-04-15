package com.xiyu.bid.tender.dto;

import com.xiyu.bid.entity.Tender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 标讯数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderDTO {

    private Long id;
    private String title;
    private String source;
    private BigDecimal budget;
    private LocalDateTime deadline;
    private Tender.Status status;
    private Integer aiScore;
    private Tender.RiskLevel riskLevel;
    private String originalUrl;
    private String externalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
