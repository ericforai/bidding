package com.xiyu.bid.biddraftagent.application;

public record TenderDocumentAnalysisInput(
        Long projectId,
        Long tenderId,
        String fileName,
        String extractedText
) {
}
