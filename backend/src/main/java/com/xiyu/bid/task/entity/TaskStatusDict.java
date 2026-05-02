package com.xiyu.bid.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务状态字典实体，映射 V101 迁移的 {@code task_status_dict} 表。
 *
 * <p>该实体持有业务判断与 UI 渲染所需的主数据：
 * code（逻辑主键）、category（大类）、color、sort_order、
 * is_initial、is_terminal、enabled。</p>
 *
 * <p>时间戳字段 {@code created_at} / {@code updated_at} 由数据库
 * 默认值填充（CURRENT_TIMESTAMP / ON UPDATE CURRENT_TIMESTAMP），
 * JPA 侧标记为不可插入/不可更新，避免覆盖 DB 默认值。</p>
 */
@Entity
@Table(name = "task_status_dict")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusDict {

    /** Column length for {@code code} (also primary key). */
    private static final int LEN_CODE = 32;
    /** Column length for {@code name} (display name). */
    private static final int LEN_NAME = 64;
    /** Column length for {@code category} (enum string). */
    private static final int LEN_CATEGORY = 16;
    /** Column length for {@code color} (CSS color literal). */
    private static final int LEN_COLOR = 16;

    /** 状态 code（逻辑主键），如 {@code TODO}、{@code COMPLETED}。 */
    @Id
    @Column(name = "code", nullable = false, length = LEN_CODE)
    private String code;

    /** 显示名，如"待办"、"已完成"。 */
    @Column(name = "name", nullable = false, length = LEN_NAME)
    private String name;

    /** 状态大类，持久化为字符串以匹配 CHECK 约束。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = LEN_CATEGORY)
    private TaskStatusCategory category;

    /** UI 展示色（十六进制色值或其他 CSS 色字面量）。 */
    @Column(name = "color", nullable = false, length = LEN_COLOR)
    private String color;

    /** 排序字段，升序。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /**
     * 是否为初始状态。全表至多一条为 true，由 service 层校验
     * （MySQL 8 不支持 partial index 的 WHERE 子句）。
     *
     * <p>使用 {@link Boolean} 包装类型而非 {@code boolean} 基础类型，
     * 以保留 {@code isInitial} JavaBean 属性名（供 Spring Data 派生方法
     * {@code findByIsInitialTrue} 使用）。</p>
     */
    @Column(name = "is_initial", nullable = false)
    private Boolean isInitial;

    /** 是否为终态（一般对应 category = CLOSED）。 */
    @Column(name = "is_terminal", nullable = false)
    private Boolean isTerminal;

    /** 是否启用（停用后不再出现在看板列与筛选器中）。 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /** 创建时间（由 DB 默认值填充）。 */
    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /** 更新时间（由 DB 默认值 + ON UPDATE 自动维护）。 */
    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /** 创建人 userId（审计字段，可空）。 */
    @Column(name = "created_by")
    private Long createdBy;

    /** 最后更新人 userId（审计字段，可空）。 */
    @Column(name = "updated_by")
    private Long updatedBy;
}
