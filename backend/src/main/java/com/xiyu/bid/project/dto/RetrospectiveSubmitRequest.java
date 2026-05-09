// Input: 提交复盘的 HTTP 请求体
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrospectiveSubmitRequest {
    @NotNull
    private BidResultType resultType;
    private String summary;
    private String winFactors;
    private String lossReasons;
    private String competitorNotes;
    private String improvementActions;
}
