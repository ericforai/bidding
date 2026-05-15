// Input: project_closure 表行
// Output: JPA 实体 - WS-F 结项 + 保证金退回登记
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
@Table(name = "project_closure")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "deposit_returned", nullable = false)
    @Builder.Default
    private Boolean depositReturned = Boolean.FALSE;

    @Column(name = "deposit_return_evidence_id")
    private Long depositReturnEvidenceId;

    @Column(name = "archive_location", length = 512)
    private String archiveLocation;

    @Column(name = "stage_locked", nullable = false)
    @Builder.Default
    private Boolean stageLocked = Boolean.FALSE;

    @Column(length = 2048)
    private String notes;

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
        if (depositReturned == null) depositReturned = Boolean.FALSE;
        if (stageLocked == null) stageLocked = Boolean.FALSE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
