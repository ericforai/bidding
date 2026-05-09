// Input: ProjectRetrospective 实体
// Output: 出参 DTO
// Pos: project/dto/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrospectiveDTO {
    private Long id;
    private Long projectId;
    private String resultType;
    private String summary;
    private String winFactors;
    private String lossReasons;
    private String competitorNotes;
    private String improvementActions;
    private String reviewStatus;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
