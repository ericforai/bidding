package com.xiyu.bid.bidresult.dto;

import lombok.Data;

import java.util.List;

@Data
public class BidResultActionRequest {
    private List<Long> ids;
    private Long resultId;
    private String comment;
}
