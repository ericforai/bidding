package com.xiyu.bid.bidresult.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BidResultDetailDTO {
    private BidResultFetchResultDTO fetchResult;
}
