package com.xiyu.bid.bidresult.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BidResultFetchResultDTO {
    private Long id;
    private String source;
    private Long tenderId;
    private Long projectId;
    private String projectName;
    private String result;
    private BigDecimal amount;
    private LocalDateTime fetchTime;
    private String status;
    private String registrationType;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private Integer contractDurationMonths;
    private String remark;
    private Integer skuCount;
    private String winAnnounceDocUrl;
}
