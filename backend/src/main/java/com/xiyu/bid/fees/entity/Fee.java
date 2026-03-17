package com.xiyu.bid.fees.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 费用实体
 * 管理投标项目相关的各类费用（投标保证金、服务费等）
 */
@Entity
@Table(name = "fees", indexes = {
    @Index(name = "idx_fee_project", columnList = "project_id"),
    @Index(name = "idx_fee_status", columnList = "status"),
    @Index(name = "idx_fee_type", columnList = "fee_type"),
    @Index(name = "idx_fee_project_status", columnList = "project_id, status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的项目ID
     */
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 费用类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 30)
    private FeeType feeType;

    /**
     * 费用金额
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal amount;

    /**
     * 费用发生日期
     */
    @Column(name = "fee_date", nullable = false)
    private LocalDateTime feeDate;

    /**
     * 费用状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * 支付日期
     */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    /**
     * 退还日期
     */
    @Column(name = "return_date")
    private LocalDateTime returnDate;

    /**
     * 支付人/支付账户
     */
    @Column(name = "paid_by", length = 200)
    private String paidBy;

    /**
     * 退还到账户
     */
    @Column(name = "return_to", length = 200)
    private String returnTo;

    /**
     * 备注说明
     */
    @Column(length = 1000)
    private String remarks;

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
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 费用类型枚举
     */
    public enum FeeType {
        BID_BOND,           // 投标保证金
        SERVICE_FEE,        // 服务费
        DOCUMENT_FEE,       // 文档费
        TRAVEL_FEE,         // 差旅费
        NOTARY_FEE,         // 公证费
        OTHER_FEE           // 其他费用
    }

    /**
     * 费用状态枚举
     */
    public enum Status {
        PENDING,            // 待支付
        PAID,               // 已支付
        RETURNED,           // 已退还
        CANCELLED           // 已取消
    }
}
