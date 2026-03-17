// Input: None
// Output: Platform Account Entity
// Pos: Entity/实体层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Platform Account Entity
 * Represents platform accounts used for bidding operations
 */
@Entity
@Table(name = "platform_accounts", indexes = {
    @Index(name = "idx_platform_username", columnList = "username"),
    @Index(name = "idx_platform_status", columnList = "status"),
    @Index(name = "idx_platform_type", columnList = "platform_type"),
    @Index(name = "idx_platform_borrowed_by", columnList = "borrowed_by")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 200)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false, length = 50)
    private PlatformType platformType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.AVAILABLE;

    @Column(name = "borrowed_by")
    private Long borrowedBy;

    @Column(name = "borrowed_at")
    private LocalDateTime borrowedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "return_count")
    @Builder.Default
    private Integer returnCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = AccountStatus.AVAILABLE;
        }
        if (returnCount == null) {
            returnCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Platform Type Enum
     */
    public enum PlatformType {
        GOV_PROCUREMENT("政府采购网"),
        BIDDING_PLATFORM("招投标平台"),
        CONSTRUCTION_PLATFORM("建设工程平台"),
        OTHER("其他");

        private final String description;

        PlatformType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Account Status Enum
     */
    public enum AccountStatus {
        AVAILABLE("可用"),
        IN_USE("使用中"),
        MAINTENANCE("维护中"),
        DISABLED("已禁用");

        private final String description;

        AccountStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
