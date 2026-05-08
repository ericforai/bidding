// Input: ProjectResult 实体
// Output: 出参 DTO
// Pos: project/dto/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultDTO {
    private Long id;
    private Long projectId;
    private String resultType;
    private BigDecimal awardAmount;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private List<Long> evidenceFileIds;
    private String summary;
    private LocalDateTime registeredAt;
    private Long registeredBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
