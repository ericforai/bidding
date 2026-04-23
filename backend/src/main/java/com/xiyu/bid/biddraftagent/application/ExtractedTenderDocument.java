package com.xiyu.bid.biddraftagent.application;

public record ExtractedTenderDocument(
        String fileName,
        String contentType,
        String text,
        int textLength,
        String extractorKey
) {
}
