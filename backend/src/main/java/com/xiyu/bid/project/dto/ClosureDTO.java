// Input: ProjectClosure 实体
// Output: 结项出参 DTO
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
public class ClosureDTO {
    private Long id;
    private Long projectId;
    private LocalDateTime closedAt;
    private Long closedBy;
    private Boolean depositReturned;
    private Long depositReturnEvidenceId;
    private String archiveLocation;
    private Boolean stageLocked;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
