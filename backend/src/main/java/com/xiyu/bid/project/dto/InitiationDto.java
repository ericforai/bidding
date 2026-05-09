// Input: 提交/更新立项的 HTTP 请求体
// Output: 通过 Bean Validation 与 InitiationFieldPolicy 共同校验
// Pos: project/dto/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.dto;

import com.xiyu.bid.project.core.InitiationFieldPolicy;
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
public class InitiationDto {
    private String ownerUnit;
    private Integer expectedBidders;
    private Integer contractPeriodMonths;
    private InitiationFieldPolicy.ProjectType projectType;
    private InitiationFieldPolicy.CustomerType customerType;
    private BigDecimal annualRevenue;
    private LocalDateTime bidOpenTime;
    private Long ownerUserId;
    private String departmentSnapshot;
    private BigDecimal depositAmount;
    private String depositPaymentMethod;
    private String competitors;
}
