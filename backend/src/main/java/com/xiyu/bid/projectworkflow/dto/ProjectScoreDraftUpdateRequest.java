package com.xiyu.bid.projectworkflow.dto;

import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectScoreDraftUpdateRequest {

    private Long assigneeId;

    private String assigneeName;

    private LocalDateTime dueDate;

    private String generatedTaskTitle;

    private String generatedTaskDescription;

    private ProjectScoreDraft.Status status;

    private String skipReason;
}
