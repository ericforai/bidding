package com.xiyu.bid.projectworkflow.dto;

import com.xiyu.bid.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTaskCreateRequest {

    @NotBlank(message = "任务名称不能为空")
    private String title;

    private String description;

    private Long assigneeId;

    private String assigneeName;

    private String assigneeDeptCode;

    private String assigneeDeptName;

    private String assigneeRoleCode;

    private String assigneeRoleName;

    @NotNull(message = "任务优先级不能为空")
    private Task.Priority priority;

    private LocalDateTime dueDate;
}
