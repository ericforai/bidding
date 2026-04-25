package com.xiyu.bid.platform.dto;

import com.xiyu.bid.platform.entity.PlatformAccount.PlatformType;
import com.xiyu.bid.platform.entity.PlatformAccount.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Platform Account
 * Note: Password field is intentionally excluded for security
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAccountDTO {

    private Long id;
    private String username;
    // Password is excluded for security reasons
    private String accountName;
    private PlatformType platformType;
    private AccountStatus status;
    private Long borrowedBy;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueAt;
    private Integer returnCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
