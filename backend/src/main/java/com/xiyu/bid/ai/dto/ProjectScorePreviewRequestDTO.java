package com.xiyu.bid.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectScorePreviewRequestDTO {
    private Long projectId;
    private Long tenderId;
    private String projectName;
    private String industry;
    @NotNull
    private BigDecimal budget;
    private List<String> tags;

    @JsonIgnore
    @AssertTrue(message = "projectId 或 tenderId 至少提供一个")
    public boolean isTargetProvided() {
        return projectId != null || tenderId != null;
    }
}
