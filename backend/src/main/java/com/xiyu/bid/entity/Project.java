package com.xiyu.bid.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目实体
 * 管理投标项目信息
 */
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_project_status", columnList = "status"),
    @Index(name = "idx_project_manager", columnList = "manager_id"),
    @Index(name = "idx_project_tender", columnList = "tender_id"),
    @Index(name = "idx_project_dates", columnList = "start_date, end_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 项目名称
     */
    @Column(nullable = false, length = 500)
    private String name;

    /**
     * 关联的标讯ID
     */
    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    /**
     * 项目状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Project.Status status = Project.Status.INITIATED;

    /**
     * 项目经理ID
     */
    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    /**
     * 团队成员ID列表（JSON格式存储）
     */
    @ElementCollection
    @CollectionTable(name = "project_team_members", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "member_id")
    @Builder.Default
    private List<Long> teamMembers = new ArrayList<>();

    /**
     * 开始日期
     */
    @Column(name = "start_date")
    private LocalDateTime startDate;

    /**
     * 结束日期
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 项目状态枚举
     */
    public enum Status {
        INITIATED,   // 已启动
        PREPARING,   // 准备中
        REVIEWING,   // 审核中
        SEALING,     // 封装中
        BIDDING,     // 投标中
        ARCHIVED     // 已归档
    }
}
