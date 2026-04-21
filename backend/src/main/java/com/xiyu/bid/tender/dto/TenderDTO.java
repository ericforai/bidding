package com.xiyu.bid.tender.dto;

import com.xiyu.bid.entity.Tender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private String region;
    private String industry;
    private String purchaserName;
    private String purchaserHash;
    private LocalDate publishDate;
    private LocalDateTime deadline;
    private String contactName;
    private String contactPhone;
    private String description;
    private List<String> tags;
    private Tender.Status status;
    private Integer aiScore;
    private Tender.RiskLevel riskLevel;
    private String originalUrl;
    private String externalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
