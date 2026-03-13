package com.xiyu.bid.projectworkflow.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectScoreDraftGenerateRequest {

    @NotEmpty(message = "至少选择一个草稿项")
    private List<Long> draftIds;
}
