package com.xiyu.bid.integration.organization.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrganizationDirectoryGatewayFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(OrganizationDirectoryGateway.class)
    public OrganizationDirectoryGateway disabledOrganizationDirectoryGateway() {
        return new DisabledOrganizationDirectoryGateway();
    }
}
