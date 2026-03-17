package com.xiyu.bid.projectworkflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDocumentCreateRequest {

    @NotBlank(message = "文档名称不能为空")
    private String name;

    private String size;

    private String fileType;

    private Long uploaderId;

    private String uploaderName;
}
