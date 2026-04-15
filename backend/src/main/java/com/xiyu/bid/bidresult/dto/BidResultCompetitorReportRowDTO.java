package com.xiyu.bid.bidresult.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BidResultCompetitorReportRowDTO {
    private String company;
    private String skuCount;
    private String category;
    private String discount;
    private String payment;
    private String winRate;
    private long projectCount;
    private String trend;
}
