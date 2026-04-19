package com.xiyu.bid.resources.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class BarSiteVerificationDTO {
    Long id;
    Long barAssetId;
    String verifiedBy;
    LocalDateTime verifiedAt;
    String status;
    String message;
}
