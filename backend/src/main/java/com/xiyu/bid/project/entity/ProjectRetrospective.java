// Input: project_retrospective 表行
// Output: JPA 实体 - WS-E 复盘
// Pos: entity/ - 持久化模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_retrospective")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRetrospective {

    public enum ReviewStatus {
        PENDING_REVIEW, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "result_type", length = 16)
    private String resultType;

    @Column(length = 4000)
    private String summary;

    @Column(name = "win_factors", length = 2048)
    private String winFactors;

    @Column(name = "loss_reasons", length = 2048)
    private String lossReasons;

    @Column(name = "competitor_notes", length = 2048)
    private String competitorNotes;

    @Column(name = "improvement_actions", length = 2048)
    private String improvementActions;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_status", nullable = false, length = 32)
    private String reviewStatus;

    @Column(name = "review_comment", length = 2048)
    private String reviewComment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (reviewStatus == null) reviewStatus = ReviewStatus.PENDING_REVIEW.name();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
