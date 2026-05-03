package com.xiyu.bid.projectworkflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTaskViewDTO {

    private Long id;
    private Long projectId;
    private String name;
    private String description;
    /**
     * Markdown rich content persisted to {@code tasks.content TEXT} (V102, up to ~64KB).
     * Mirrors the {@code content} field on {@link com.xiyu.bid.task.dto.TaskDTO} so the
     * task drawer can round-trip Markdown across page reloads.
     */
    private String content;
    private Long assigneeId;
    private String assigneeDeptCode;
    private String assigneeRoleCode;
    private String owner;
    private String assignee;
    private String department;
    private String roleName;
    private String status;
    private String priority;
    private String dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deliverableCount;
}
