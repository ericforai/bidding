package com.xiyu.bid.tender.dto;

import com.xiyu.bid.tender.entity.TenderEvaluation.BidRecommendation;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 标讯项目评估保存 / 提交请求 DTO（V119 新口径）。
 * <p>携带项目评估表 7 字段（5 必填 + 2 非必填），是否走"提交"由 controller / service 决定。
 * <p>校验仅做基础长度与存在性约束；业务级"必填"由 TenderEvaluationFormPolicy（Phase 3）负责。
 */
public record TenderEvaluationSubmitRequest(

    /** 项目背景（业务必填）。M3: 与前端 maxlength=5000 对齐。 */
    @Size(max = 5000, message = "项目背景不能超过5000字符")
    String projectBackground,

    /** 竞争对手情况（业务必填）。M3: 与前端 maxlength=5000 对齐。 */
    @Size(max = 5000, message = "竞争对手情况不能超过5000字符")
    String competitorAnalysis,

    /** 项目合同周期起（业务必填）。 */
    LocalDate contractPeriodStart,

    /** 项目合同周期止（业务必填）。 */
    LocalDate contractPeriodEnd,

    /** 入围家数（业务必填）。 */
    Integer shortlistedCount,

    /** 平台服务费（元，业务必填）。 */
    BigDecimal platformServiceFee,

    /** 上一次报价（非必填）。M3: 与前端 maxlength=5000 对齐。 */
    @Size(max = 5000, message = "上一次报价不能超过5000字符")
    String previousQuotation,

    /** 建议是否投标（非必填）。 */
    BidRecommendation bidRecommendation
) {}
