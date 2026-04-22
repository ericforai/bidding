package com.xiyu.bid.biddraftagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidDraftAgentApplyResponseDTO {
    private Long runId;
    private Long artifactId;
    private String artifactType;
    private String status;
    private boolean readyForWriter;
    private String handoffTarget;
    private String message;
}
