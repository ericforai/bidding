package com.xiyu.bid.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class TemplateUseRecordRequest {

    @NotBlank
    private String documentName;

    @NotBlank
    private String docType;

    private Long projectId;
    private List<String> applyOptions;
    private Long usedBy;
}
