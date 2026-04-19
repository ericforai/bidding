package com.xiyu.bid.casework.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record CaseSearchOptions(
        List<String> industries,
        List<String> outcomes,
        List<String> statuses,
        List<String> visibilities,
        List<String> productLines,
        List<String> tags,
        List<String> sortOptions
) {
}
