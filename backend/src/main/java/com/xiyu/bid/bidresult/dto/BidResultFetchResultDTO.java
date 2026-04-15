package com.xiyu.bid.bidresult.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
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
}
