package com.xiyu.bid.tender.dto;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.tender.entity.TenderEvaluation.BidRecommendation;
import com.xiyu.bid.tender.entity.TenderEvaluation.EvaluationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 标讯项目评估详情 DTO（V119 新口径）。
 * <p>纯值载体，不在此类承载任何转换逻辑（由 service / mapper 负责）。
 */
public record TenderEvaluationDTO(

    Long tenderId,
    String tenderTitle,
    Tender.Status tenderStatus,

    // ---------- 评估表 7 字段 + 状态 ----------
    EvaluationStatus evaluationStatus,
    String projectBackground,
    String competitorAnalysis,
    LocalDate contractPeriodStart,
    LocalDate contractPeriodEnd,
    Integer shortlistedCount,
    BigDecimal platformServiceFee,
    String previousQuotation,
    BidRecommendation bidRecommendation,
    LocalDateTime submittedAt,

    // ---------- 审核 / 评估人 元数据 ----------
    Long evaluatorId,
    String evaluatorName,
    LocalDateTime evaluatedAt
) {}
