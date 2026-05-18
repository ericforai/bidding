// Input: ProjectInitiationDetails 实体
// Output: 出参视图（含派生字段 bidMonth、locked）
// Pos: project/dto/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiationViewDto {
    private Long id;
    private Long projectId;
    private String ownerUnit;
    private Integer expectedBidders;
    private Integer contractPeriodMonths;
    private String projectType;
    private String customerType;
    private BigDecimal annualRevenue;
    private LocalDateTime bidOpenTime;
    private String bidMonth;
    private Long ownerUserId;
    private String departmentSnapshot;
    private BigDecimal depositAmount;
    private String depositPaymentMethod;
    private String competitors;
    private Boolean locked;
    private String reviewStatus;
    private String rejectionReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String aiRiskLevel;
    private Long tenderDocumentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
