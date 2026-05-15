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
    private String tenderAgency;
    private String purchaserName;
    private String purchaserHash;
    private LocalDate publishDate;
    private LocalDateTime deadline;
    private LocalDateTime bidOpeningTime;
    private LocalDateTime registrationDeadline;
    private String contactName;
    private String contactPhone;
    private String sourceDocumentName;
    private String sourceDocumentFileType;
    private String sourceDocumentFileUrl;
    private String customerType;
    private String priority;
    private String description;
    private List<String> tags;
    private Tender.Status status;
    private Integer aiScore;
    private Tender.RiskLevel riskLevel;
    private Tender.SourceType sourceType;
    private String originalUrl;
    private String externalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 项目经理名称（从关联的项目获取）
     */
    private String projectManagerName;

    /**
     * 分配人名称（从标讯分配记录获取）
     */
    private String assigneeName;
}
