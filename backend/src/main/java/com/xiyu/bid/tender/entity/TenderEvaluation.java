package com.xiyu.bid.tender.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 标讯项目评估实体（V119 重设计）。
 * <p>承载 7 个项目评估字段 + 评估状态（DRAFT/SUBMITTED）+ 投标建议 +
 * V118 起即存在的审核字段（reviewStatus/reviewer.../reviewedAt/reviewComment）。
 * <p>本实体为纯数据 + JPA 注解，不承载业务逻辑。
 *
 * <p>TODO(post-V119): consider replacing {@code @Data} with {@code @Getter} and
 * making fields final via a builder copy pattern so the entity stops violating
 * the Split-First mutability guard. Out of scope for V119.
 */
@Entity
@Table(name = "tender_evaluations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * M1: 乐观锁版本号。并发草稿保存场景使用 JPA @Version 防止丢失写入。
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    /** 关联的标讯 ID。 */
    @Column(name = "tender_id", nullable = false, unique = true)
    private Long tenderId;

    /** 评估表状态：DRAFT/SUBMITTED。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false, length = 20)
    @Builder.Default
    private EvaluationStatus evaluationStatus = EvaluationStatus.DRAFT;

    /** 项目背景（必填，TEXT）。 */
    @Column(name = "project_background", columnDefinition = "text")
    private String projectBackground;

    /** 竞争对手情况（必填，TEXT）。 */
    @Column(name = "competitor_analysis", columnDefinition = "text")
    private String competitorAnalysis;

    /** 项目合同周期起（必填）。 */
    @Column(name = "contract_period_start")
    private LocalDate contractPeriodStart;

    /** 项目合同周期止（必填）。 */
    @Column(name = "contract_period_end")
    private LocalDate contractPeriodEnd;

    /** 入围家数（必填）。 */
    @Column(name = "shortlisted_count")
    private Integer shortlistedCount;

    /** 平台服务费（元，必填）。 */
    @Column(name = "platform_service_fee", precision = 19, scale = 2)
    private BigDecimal platformServiceFee;

    /** 上一次报价（非必填，TEXT）。 */
    @Column(name = "previous_quotation", columnDefinition = "text")
    private String previousQuotation;

    /** 建议是否投标（非必填）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "bid_recommendation", length = 20)
    private BidRecommendation bidRecommendation;

    /** 评估表提交时间（DRAFT->SUBMITTED 时填充）。 */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // ---------- 审核字段（V118 保留，V119 不动） ----------

    /** 评估人 ID。 */
    @Column(name = "evaluator_id")
    private Long evaluatorId;

    /** 评估人姓名。 */
    @Column(name = "evaluator_name", length = 100)
    private String evaluatorName;

    /** 评估时间。 */
    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    /** 审核状态。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    /** 审核人 ID。 */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    /** 审核人姓名。 */
    @Column(name = "reviewer_name", length = 100)
    private String reviewerName;

    /** 审核时间。 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 审核意见。 */
    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    /** 评估表状态枚举。 */
    public enum EvaluationStatus {
        DRAFT,      // 草稿
        SUBMITTED   // 已提交
    }

    /** 建议是否投标枚举（M2: 移除 PENDING_REVIEW — 前端不再展示）。 */
    public enum BidRecommendation {
        RECOMMEND,        // 建议投标
        NOT_RECOMMEND     // 不建议投标
    }

    /** 审核状态枚举（V118 起即存在，V119 保留）。 */
    public enum ReviewStatus {
        PENDING,    // 待审核
        APPROVED,   // 已通过（投标）
        REJECTED    // 已拒绝（弃标）
    }
}
