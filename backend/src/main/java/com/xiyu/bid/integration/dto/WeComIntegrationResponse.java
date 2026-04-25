package com.xiyu.bid.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for WeCom integration configuration.
 * corpSecret is NEVER included — only secretConfigured (boolean) is returned.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeComIntegrationResponse(
        boolean configured,
        String corpId,
        String agentId,
        boolean secretConfigured,
        boolean ssoEnabled,
        boolean messageEnabled
) {

    public static WeComIntegrationResponse empty() {
        return new WeComIntegrationResponse(false, null, null, false, false, false);
    }

    public static WeComIntegrationResponse configured(
            String corpId,
            String agentId,
            boolean ssoEnabled,
            boolean messageEnabled
    ) {
        return new WeComIntegrationResponse(true, corpId, agentId, true, ssoEnabled, messageEnabled);
    }
}
