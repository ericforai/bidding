package com.xiyu.bid.biddraftagent.dto;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BidTenderDocumentImportDTO {

    private BidTenderDocumentDTO document;
    private TenderRequirementProfile requirementProfile;
    private BidDraftAgentRunDTO run;
    private BidDraftAgentApplyResponseDTO applyResult;
    private boolean appliedToEditor;
    private String message;
}
