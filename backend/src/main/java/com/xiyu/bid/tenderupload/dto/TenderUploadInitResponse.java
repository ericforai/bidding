package com.xiyu.bid.tenderupload.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenderUploadInitResponse {
    String uploadId;
    String relativePath;
    String uploadMode;
}
