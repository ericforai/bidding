package com.xiyu.bid.batch.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchAssignRequest {
    @NotEmpty(message = "任务ID列表不能为空")
    private List<@NotNull(message = "任务ID不能为空") Long> taskIds;
    @NotNull(message = "分配人ID不能为空")
    private Long assigneeId;
    private String remark;
}
