package com.xiyu.bid.projectworkflow.dto;

import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectScoreDraftDTO {
    private Long id;
    private Long projectId;
    private String sourceFileName;
    private String category;
    private String scoreItemTitle;
    private String scoreRuleText;
    private String scoreValueText;
    private String taskAction;
    private String generatedTaskTitle;
    private String generatedTaskDescription;
    private List<String> suggestedDeliverables;
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime dueDate;
    private ProjectScoreDraft.Status status;
    private String skipReason;
    private Integer sourcePage;
    private Integer sourceTableIndex;
    private Integer sourceRowIndex;
    private Long generatedTaskId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
