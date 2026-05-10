package com.xiyu.bid.tender.dto;

import com.xiyu.bid.entity.Tender;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 标讯评估详情 DTO
 */
public record TenderEvaluationDTO(

    Long tenderId,
    String tenderTitle,
    Tender.Status tenderStatus,
    String evaluationContent,
    BigDecimal estimatedBudget,
    String riskAssessment,
    String notes,
    Long evaluatorId,
    String evaluatorName,
    LocalDateTime evaluatedAt
) {}
