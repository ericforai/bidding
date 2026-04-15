package com.xiyu.bid.bidresult.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BidResultDetailDTO {
    private Long id;
    private String source;
    private Long tenderId;
    private Long projectId;
    private String projectName;
    private String result;
    private BigDecimal amount;
    private String status;
    private LocalDateTime fetchTime;
    private String ignoredReason;
    private String ownerName;
    private List<String> reminderTypes;
}
