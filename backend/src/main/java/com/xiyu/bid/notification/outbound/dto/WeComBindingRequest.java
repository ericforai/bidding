package com.xiyu.bid.notification.outbound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WeComBindingRequest(
    @NotBlank @Size(max = 64) String wecomUserId
) {
}
