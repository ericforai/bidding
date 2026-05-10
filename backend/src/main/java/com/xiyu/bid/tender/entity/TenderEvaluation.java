package com.xiyu.bid.tender.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 标讯评估实体
 * 存储项目经理对标讯的评估内容
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
     * 关联的标讯ID
     */
    @Column(name = "tender_id", nullable = false, unique = true)
    private Long tenderId;

    /**
     * 评估内容
     */
    @Column(name = "evaluation_content", columnDefinition = "text")
    private String evaluationContent;

    /**
     * 预估预算
     */
    @Column(name = "estimated_budget", precision = 19, scale = 2)
    private BigDecimal estimatedBudget;

    /**
     * 风险评估
     */
    @Column(name = "risk_assessment", length = 500)
    private String riskAssessment;

    /**
     * 备注
     */
    @Column(length = 2000)
    private String notes;

    /**
     * 评估人ID
     */
    @Column(name = "evaluator_id")
    private Long evaluatorId;

    /**
     * 评估人姓名
     */
    @Column(name = "evaluator_name", length = 100)
    private String evaluatorName;

    /**
     * 评估时间
     */
    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    /**
     * 审核状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    /**
     * 审核人ID
     */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    /**
     * 审核人姓名
     */
    @Column(name = "reviewer_name", length = 100)
    private String reviewerName;

    /**
     * 审核时间
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * 审核意见
     */
    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    /**
     * 审核状态枚举
     */
    public enum ReviewStatus {
        PENDING,    // 待审核
        APPROVED,   // 已通过（投标）
        REJECTED    // 已拒绝（弃标）
    }

    @PrePersist
    void onCreate() {
        if (evaluatedAt == null) {
            evaluatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        if (reviewStatus == ReviewStatus.PENDING && reviewedAt != null) {
            // 审核时更新审核时间
        }
    }
}
