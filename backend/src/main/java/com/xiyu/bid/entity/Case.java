package com.xiyu.bid.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 案例实体
 */
@Entity
@Table(name = "cases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Industry industry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Outcome outcome;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "project_date")
    private LocalDate projectDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "project_period")
    private String projectPeriod;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "case_tags", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "case_highlights", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "highlight")
    @Builder.Default
    private List<String> highlights = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "case_technologies", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "technology")
    @Builder.Default
    private List<String> technologies = new ArrayList<>();

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "use_count", nullable = false)
    @Builder.Default
    private Long useCount = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
     * 行业枚举
     */
    public enum Industry {
        REAL_ESTATE,
        INFRASTRUCTURE,
        MANUFACTURING,
        ENERGY,
        TRANSPORTATION,
        ENVIRONMENTAL,
        OTHER
    }

    /**
     * 结果枚举
     */
    public enum Outcome {
        WON,
        LOST,
        IN_PROGRESS
    }
}
