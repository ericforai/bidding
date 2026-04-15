package com.xiyu.bid.tender.dto;

import com.xiyu.bid.entity.Tender;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 标讯请求数传输对象
 * 用于接收前端请求时的数据验证
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderRequest {

    @NotBlank(message = "标讯标题不能为空")
    @Size(max = 500, message = "标讯标题长度不能超过500个字符")
    private String title;

    @Size(max = 200, message = "来源长度不能超过200个字符")
    private String source;

    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.00", message = "预算金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "预算金额格式不正确")
    private BigDecimal budget;

    @NotNull(message = "截止日期不能为空")
    @Future(message = "截止日期必须是未来的时间")
    private LocalDateTime deadline;

    private Tender.Status status;

    @Min(value = 0, message = "AI评分不能为负数")
    @Max(value = 100, message = "AI评分不能超过100")
    private Integer aiScore;

    private Tender.RiskLevel riskLevel;

    @Size(max = 2000, message = "原始链接长度不能超过2000个字符")
    private String originalUrl;

    @Size(max = 100, message = "外部标识长度不能超过100个字符")
    private String externalId;
}
