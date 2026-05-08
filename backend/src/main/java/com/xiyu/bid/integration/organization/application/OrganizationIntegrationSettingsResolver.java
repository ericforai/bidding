package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.settings.dto.SettingsResponse;
import com.xiyu.bid.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrganizationIntegrationSettingsResolver {
    private final SettingsService settingsService;
    private final OrganizationIntegrationProperties properties;

    public OrganizationIntegrationSettings resolve() {
        SettingsResponse.IntegrationConfig integrationConfig = settingsIntegrationConfig();
        return new OrganizationIntegrationSettings(
                enabled(integrationConfig),
                firstNonBlank(secretFromSettings(integrationConfig), properties.getWebhookSecret()),
                allowedSourceApps(integrationConfig),
                firstNonBlank(ipWhitelistFromSettings(integrationConfig), properties.getIpWhitelist())
        );
    }

    private SettingsResponse.IntegrationConfig settingsIntegrationConfig() {
        if (settingsService == null) {
            return null;
        }
        SettingsResponse settings = settingsService.getSettings();
        return settings == null ? null : settings.getIntegrationConfig();
    }

    private boolean enabled(SettingsResponse.IntegrationConfig integrationConfig) {
        if (integrationConfig == null || integrationConfig.getOrgEnabled() == null) {
            return properties.isEnabled();
        }
        return integrationConfig.getOrgEnabled();
    }

    private List<String> allowedSourceApps(SettingsResponse.IntegrationConfig integrationConfig) {
        List<String> values = new ArrayList<>(properties.getAllowedSourceApps());
        addIfPresent(values, integrationConfig == null ? null : integrationConfig.getOrgSystem());
        addIfPresent(values, integrationConfig == null ? null : integrationConfig.getOrgAppKey());
        return values.stream().filter(value -> !blank(value)).distinct().toList();
    }

    private String secretFromSettings(SettingsResponse.IntegrationConfig integrationConfig) {
        return integrationConfig == null ? "" : integrationConfig.getOrgAppSecret();
    }

    private String ipWhitelistFromSettings(SettingsResponse.IntegrationConfig integrationConfig) {
        return integrationConfig == null ? "" : integrationConfig.getIpWhitelist();
    }

    private void addIfPresent(List<String> values, String value) {
        if (!blank(value)) {
            values.add(value.trim());
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        return blank(primary) ? safe(fallback) : primary.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
