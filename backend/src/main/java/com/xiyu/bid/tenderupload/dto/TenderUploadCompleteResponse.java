package com.xiyu.bid.tenderupload.dto;

import com.xiyu.bid.tenderupload.entity.TenderTaskStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenderUploadCompleteResponse {
    Long fileId;
    Long taskId;
    TenderTaskStatus status;
    boolean deduplicated;
}
