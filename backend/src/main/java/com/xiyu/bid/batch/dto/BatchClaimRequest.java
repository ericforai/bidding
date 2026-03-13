package com.xiyu.bid.batch.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量认领请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchClaimRequest {

    /**
     * 要认领的项目ID列表
     */
    @NotEmpty(message = "项目ID列表不能为空")
    private List<@NotNull(message = "项目ID不能为空") Long> itemIds;

    /**
     * 认领人用户ID
     */
    @NotNull(message = "认领人ID不能为空")
    private Long userId;

    /**
     * 项目类型 (tender, task等)
     */
    private String itemType;
}
