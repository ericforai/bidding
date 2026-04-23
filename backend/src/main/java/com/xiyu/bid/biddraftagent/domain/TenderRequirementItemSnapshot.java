package com.xiyu.bid.biddraftagent.domain;

public record TenderRequirementItemSnapshot(
        String category,
        String title,
        String content,
        boolean mandatory,
        String sourceExcerpt,
        Integer confidence
) {
}
