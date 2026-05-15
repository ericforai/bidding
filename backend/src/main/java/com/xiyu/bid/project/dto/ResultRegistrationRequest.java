// Input: 登记结果的 HTTP 请求体
// Output: 校验后的 DTO
// Pos: project/dto/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.dto;

import com.xiyu.bid.project.core.BidResultType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultRegistrationRequest {
    @NotNull
    private BidResultType resultType;
    private BigDecimal awardAmount;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private List<Long> evidenceFileIds;
    private String summary;
}
