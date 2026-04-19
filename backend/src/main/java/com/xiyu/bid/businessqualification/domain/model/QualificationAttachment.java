package com.xiyu.bid.businessqualification.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class QualificationAttachment {
    Long id;
    String fileName;
    String fileUrl;
    LocalDateTime uploadedAt;
}
