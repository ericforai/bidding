package com.xiyu.bid.integration.organization.application;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "xiyu.integrations.organization")
public class OrganizationIntegrationProperties {
    private boolean enabled = true;
    private String webhookSecret = "";
    private String ipWhitelist = "";
    private int eventLogRetentionDays = 90;
    private List<String> allowedSourceApps = new ArrayList<>(List.of("customer-org"));
    private List<String> adminRoleCodes = new ArrayList<>();
    private List<String> managerRoleCodes = new ArrayList<>();
}
