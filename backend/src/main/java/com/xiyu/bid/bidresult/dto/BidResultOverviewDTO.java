package com.xiyu.bid.bidresult.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BidResultOverviewDTO {
    private LocalDateTime lastSyncTime;
    private long pendingCount;
    private long uploadPending;
    private long competitorCount;
}
