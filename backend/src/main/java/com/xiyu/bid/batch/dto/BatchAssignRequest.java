package com.xiyu.bid.batch.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量分配请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchAssignRequest {

    /**
     * 要分配的任务ID列表
     */
    @NotEmpty(message = "任务ID列表不能为空")
    private List<@NotNull(message = "任务ID不能为空") Long> taskIds;

    /**
     * 目标分配人用户ID
     */
    @NotNull(message = "分配人ID不能为空")
    private Long assigneeId;

    /**
     * 分配原因/备注
     */
    private String remark;
}
