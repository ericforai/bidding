package com.xiyu.bid.projectworkflow.dto;

import com.xiyu.bid.entity.Task;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTaskStatusUpdateRequest {

    @NotNull(message = "任务状态不能为空")
    private Task.Status status;
}
