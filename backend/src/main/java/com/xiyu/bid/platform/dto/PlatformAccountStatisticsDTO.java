package com.xiyu.bid.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistics DTO for Platform Accounts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAccountStatisticsDTO {

    private Long totalAccounts;
    private Long availableAccounts;
    private Long inUseAccounts;
    private Long maintenanceAccounts;
    private Long disabledAccounts;
}
