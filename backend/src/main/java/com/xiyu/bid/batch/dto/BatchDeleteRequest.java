package com.xiyu.bid.batch.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量删除请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeleteRequest {

    /**
     * 要删除的项目ID列表
     */
    @NotEmpty(message = "项目ID列表不能为空")
    private List<@NotNull(message = "项目ID不能为空") Long> itemIds;

    /**
     * 操作用户ID
     */
    @NotNull(message = "操作用户ID不能为空")
    private Long userId;

    /**
     * 删除原因/备注
     */
    private String reason;

    /**
     * 是否强制删除 (包括关联数据)
     */
    private Boolean forceDelete = false;
}
