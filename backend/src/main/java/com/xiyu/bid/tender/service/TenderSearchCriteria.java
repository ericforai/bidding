package com.xiyu.bid.tender.service;

import com.xiyu.bid.entity.Tender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderSearchCriteria {

    private String keyword;
    private Tender.Status status;
    private String source;
    private String region;
    private String industry;
    private String purchaserName;
    private String purchaserHash;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deadlineFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deadlineTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publishDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publishDateTo;

    private Integer aiScoreMin;
    private Integer aiScoreMax;

    public static TenderSearchCriteria empty() {
        return new TenderSearchCriteria();
    }
}
