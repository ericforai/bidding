package com.xiyu.bid.tender.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 标讯评估请求 DTO
 * 项目经理提交评估时使用
 */
public record TenderEvaluationRequest(

    /**
     * 评估内容
     */
    @NotBlank(message = "评估内容不能为空")
    @Size(max = 10000, message = "评估内容不能超过10000字符")
    String evaluationContent,

    /**
     * 预估预算
     */
    BigDecimal estimatedBudget,

    /**
     * 风险评估
     */
    @Size(max = 500, message = "风险评估不能超过500字符")
    String riskAssessment,

    /**
     * 备注
     */
    @Size(max = 2000, message = "备注不能超过2000字符")
    String notes
) {}
