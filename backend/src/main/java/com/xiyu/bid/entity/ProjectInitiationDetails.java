// Input: project_initiation_details 表行
// Output: JPA 实体 - WS-A 立项
// Pos: entity/ - 持久化模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "project_initiation_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInitiationDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;

    /** 业主单位 */
    @Column(name = "owner_unit", length = 255)
    private String ownerUnit;

    /** 入围家数 */
    @Column(name = "expected_bidders")
    private Integer expectedBidders;

    /** 客户类型（CENTRAL_SOE/LOCAL_SOE/OTHER） */
    @Column(name = "customer_type", length = 64)
    private String customerType;

    /** 项目类型（PUBLIC_BIDDING/INVITED_BIDDING/OTHER） */
    @Column(name = "project_type", length = 64)
    private String projectType;

    /** 营业收入 */
    @Column(name = "annual_revenue", precision = 20, scale = 2)
    private BigDecimal annualRevenue;

    /** 合同期限（月） */
    @Column(name = "contract_period_months")
    private Integer contractPeriodMonths;

    /** 开标时间 */
    @Column(name = "bid_open_time")
    private LocalDateTime bidOpenTime;

    /** 投标月份（自动派生） */
    @Column(name = "bid_month", length = 16)
    private String bidMonth;

    /** 业务负责人用户 id */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    /** 归属部门快照 */
    @Column(name = "department_snapshot", length = 255)
    private String departmentSnapshot;

    /** 保证金额 */
    @Column(name = "deposit_amount", precision = 20, scale = 2)
    private BigDecimal depositAmount;

    /** 缴纳方式 */
    @Column(name = "deposit_payment_method", length = 64)
    private String depositPaymentMethod;

    /** 竞争对手（可选） */
    @Column(name = "competitors", length = 1024)
    private String competitors;

    /** 提交后锁定 bidOpenTime/ownerUnit。 */
    @Column(name = "locked", nullable = false)
    @Builder.Default
    private Boolean locked = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (locked == null) locked = Boolean.FALSE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
