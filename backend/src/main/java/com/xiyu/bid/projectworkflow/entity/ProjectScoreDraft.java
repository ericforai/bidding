package com.xiyu.bid.projectworkflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_score_drafts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectScoreDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "source_file_name", nullable = false)
    private String sourceFileName;

    @Column(nullable = false)
    private String category;

    @Column(name = "score_item_title", nullable = false)
    private String scoreItemTitle;

    @Column(name = "score_rule_text", nullable = false, columnDefinition = "TEXT")
    private String scoreRuleText;

    @Column(name = "score_value_text")
    private String scoreValueText;

    @Column(name = "task_action", nullable = false)
    private String taskAction;

    @Column(name = "generated_task_title", nullable = false)
    private String generatedTaskTitle;

    @Column(name = "generated_task_description", nullable = false, columnDefinition = "TEXT")
    private String generatedTaskDescription;

    @Column(name = "suggested_deliverables", nullable = false, columnDefinition = "TEXT")
    private String suggestedDeliverables;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "assignee_name")
    private String assigneeName;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "skip_reason")
    private String skipReason;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "source_table_index", nullable = false)
    private Integer sourceTableIndex;

    @Column(name = "source_row_index", nullable = false)
    private Integer sourceRowIndex;

    @Column(name = "generated_task_id")
    private Long generatedTaskId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = Status.DRAFT;
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        DRAFT,
        READY,
        SKIPPED,
        GENERATED
    }
}
