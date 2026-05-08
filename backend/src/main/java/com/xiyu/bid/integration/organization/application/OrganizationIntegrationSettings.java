package com.xiyu.bid.integration.organization.application;

import java.util.List;

public record OrganizationIntegrationSettings(
        boolean enabled,
        String webhookSecret,
        List<String> allowedSourceApps,
        String ipWhitelist
) {
    public boolean sourceAllowed(String sourceApp) {
        return allowedSourceApps.stream().anyMatch(sourceApp::equals);
    }

    public boolean ipAllowed(String remoteAddress) {
        if (blank(ipWhitelist)) {
            return true;
        }
        if (blank(remoteAddress)) {
            return false;
        }
        String normalizedAddress = remoteAddress.trim();
        for (String item : ipWhitelist.split("[,;\\s]+")) {
            if (normalizedAddress.equals(item.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
