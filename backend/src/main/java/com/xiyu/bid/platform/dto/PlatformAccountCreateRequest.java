package com.xiyu.bid.platform.dto;

import com.xiyu.bid.platform.entity.PlatformAccount.PlatformType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating a Platform Account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAccountCreateRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotNull(message = "Platform type is required")
    private PlatformType platformType;
}
