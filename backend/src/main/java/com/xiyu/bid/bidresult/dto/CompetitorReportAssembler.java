package com.xiyu.bid.bidresult.dto;

import com.xiyu.bid.bidresult.core.CompetitorReportRow;
import com.xiyu.bid.bidresult.core.CompetitorWinRow;
import com.xiyu.bid.bidresult.entity.CompetitorWinRecord;

public class CompetitorReportAssembler {

    public static CompetitorWinDTO toDTO(CompetitorWinRecord entity) {
        if (entity == null) return null;
        return CompetitorWinDTO.builder()
                .id(entity.getId())
                .competitorId(entity.getCompetitorId())
                .competitorName(entity.getCompetitorName())
                .projectId(entity.getProjectId())
                .projectName(entity.getProjectName())
                .skuCount(entity.getSkuCount())
                .category(entity.getCategory())
                .discount(entity.getDiscount())
                .paymentTerms(entity.getPaymentTerms())
                .wonAt(entity.getWonAt())
                .amount(entity.getAmount())
                .notes(entity.getNotes())
                .recordedBy(entity.getRecordedBy())
                .recordedByName(entity.getRecordedByName())
                .build();
    }

    public static CompetitorWinRow toRow(CompetitorWinRecord entity) {
        if (entity == null) return null;
        return new CompetitorWinRow(
                entity.getCompetitorId(),
                entity.getCompetitorName(),
                entity.getSkuCount(),
                entity.getCategory(),
                entity.getDiscount(),
                entity.getPaymentTerms(),
                entity.getAmount() != null ? entity.getAmount() : java.math.BigDecimal.ZERO // Simplified
        );
    }

    public static BidResultCompetitorReportRowDTO toReportDTO(CompetitorReportRow row) {
        if (row == null) return null;
        return BidResultCompetitorReportRowDTO.builder()
                .company(row.company())
                .skuCount(row.skuCount())
                .category(row.category())
                .discount(row.discount())
                .paymentTerms(row.paymentTerms())
                .winRate(row.winRate())
                .projectCount(row.projectCount())
                .trend(row.trend())
                .build();
    }
}
