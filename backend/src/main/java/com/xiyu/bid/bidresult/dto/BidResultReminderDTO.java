package com.xiyu.bid.bidresult.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BidResultReminderDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long ownerId;
    private String owner;
    private Long lastResultId;
    private String type;
    private String status;
    private LocalDateTime remindTime;
    private String lastReminderComment;
}
