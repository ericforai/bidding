package com.xiyu.bid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 标讯实体
 * 管理招标信息的基础数据
 */
@Entity
@Table(name = "tenders", indexes = {
    @Index(name = "idx_tender_status", columnList = "status"),
    @Index(name = "idx_tender_source", columnList = "source"),
    @Index(name = "idx_tender_deadline", columnList = "deadline"),
    @Index(name = "idx_tender_ai_score", columnList = "ai_score"),
    @Index(name = "idx_tender_external_id", columnList = "external_id", unique = true),
    @Index(name = "idx_tender_region", columnList = "region"),
    @Index(name = "idx_tender_industry", columnList = "industry"),
    @Index(name = "idx_tender_purchaser_hash", columnList = "purchaser_hash"),
    @Index(name = "idx_tender_status_region_industry", columnList = "status, region, industry"),
    @Index(name = "idx_tender_source_normalized", columnList = "source_normalized"),
    @Index(name = "idx_tender_region_normalized", columnList = "region_normalized"),
    @Index(name = "idx_tender_industry_normalized", columnList = "industry_normalized"),
    @Index(name = "idx_tender_purchaser_hash_normalized", columnList = "purchaser_hash_normalized"),
    @Index(name = "idx_tender_status_region_industry_normalized",
            columnList = "status, region_normalized, industry_normalized")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 外部唯一标识 (来自 CEB 平台)
     */
    @Column(name = "external_id", length = 100, unique = true)
    private String externalId;

    /**
     * 标题
     */
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * 来源
     */
    @Column(length = 200)
    private String source;

    /**
     * 原始链接
     */
    @Column(name = "original_url", length = 1000)
    private String originalUrl;

    /**
     * 预算金额
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal budget;

    /**
     * 所属地区
     */
    @Column(length = 100)
    private String region;

    /**
     * 所属行业
     */
    @Column(length = 100)
    private String industry;

    /**
     * 采购单位名称
     */
    @Column(name = "purchaser_name", length = 255)
    private String purchaserName;

    /**
     * 采购单位稳定哈希
     */
    @Column(name = "purchaser_hash", length = 64)
    private String purchaserHash;

    @Column(name = "source_normalized", length = 200)
    private String sourceNormalized;

    @Column(name = "region_normalized", length = 100)
    private String regionNormalized;

    @Column(name = "industry_normalized", length = 100)
    private String industryNormalized;

    @Column(name = "purchaser_hash_normalized", length = 64)
    private String purchaserHashNormalized;

    @Column(name = "purchaser_name_normalized", length = 255)
    private String purchaserNameNormalized;

    @Column(name = "search_text_normalized", columnDefinition = "text")
    private String searchTextNormalized;

    /**
     * 发布日期
     */
    @Column(name = "publish_date")
    private LocalDate publishDate;

    /**
     * 截止日期
     */
    @Column(name = "deadline")
    private LocalDateTime deadline;

    /**
     * 联系人姓名
     */
    @Column(name = "contact_name", length = 100)
    private String contactName;

    /**
     * 联系电话
     */
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    /**
     * 标讯描述
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * 标签，使用逗号分隔存储
     */
    @Column(columnDefinition = "text")
    private String tags;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Tender.Status status = Tender.Status.PENDING;

    /**
     * AI评分（0-100）
     */
    @Column(name = "ai_score")
    private Integer aiScore;

    /**
     * 风险等级
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private Tender.RiskLevel riskLevel;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        refreshSearchColumns();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        refreshSearchColumns();
        updatedAt = LocalDateTime.now();
    }

    private void refreshSearchColumns() {
        sourceNormalized = normalize(source);
        regionNormalized = normalize(region);
        industryNormalized = normalize(industry);
        purchaserHashNormalized = normalize(purchaserHash);
        purchaserNameNormalized = normalize(purchaserName);
        searchTextNormalized = normalizeSearchText();
    }

    private String normalizeSearchText() {
        return normalize(String.join(" ",
                nullToBlank(title),
                nullToBlank(description),
                nullToBlank(purchaserName),
                nullToBlank(tags),
                nullToBlank(region),
                nullToBlank(industry),
                nullToBlank(source)
        ));
    }

    private static String normalize(String value) {
        return nullToBlank(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    /**
     * 标讯状态枚举
     */
    public enum Status {
        PENDING,      // 待处理
        TRACKING,     // 跟踪中
        BIDDED,       // 已投标
        ABANDONED     // 已放弃
    }

    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        LOW,      // 低风险
        MEDIUM,   // 中风险
        HIGH      // 高风险
    }
}
