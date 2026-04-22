package com.xiyu.bid.tenderupload.dto;

import com.xiyu.bid.tenderupload.entity.TenderTaskStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TenderTaskStatusResponse {
    Long taskId;
    Long fileId;
    TenderTaskStatus status;
    Integer attempts;
    Integer priority;
    Long queuePosition;
    LocalDateTime estimatedStartAt;
    String errorCode;
    String errorMessage;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
