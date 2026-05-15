package com.xiyu.bid.integration.organization.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.integration.organization.application.DisabledOrganizationDirectoryGateway;
import com.xiyu.bid.integration.organization.application.OrganizationDirectoryGateway;
import com.xiyu.bid.integration.organization.application.OrganizationDirectoryGatewayFallbackConfiguration;
import com.xiyu.bid.integration.organization.application.OrganizationIntegrationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizationDirectoryGateway configuration")
class OrganizationDirectoryGatewayConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RestTemplateBuilder.class, RestTemplateBuilder::new)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(OrganizationIntegrationProperties.class, OrganizationIntegrationProperties::new)
            .withUserConfiguration(
                    OrganizationDirectoryHttpGateway.class,
                    OrganizationDirectoryGatewayFallbackConfiguration.class
            );

    @Test
    @DisplayName("blank directory base URL keeps disabled fallback gateway")
    void blankBaseUrl_usesDisabledFallbackGateway() {
        contextRunner
                .withPropertyValues("xiyu.integrations.organization.directory.base-url=")
                .run(context -> {
                    assertThat(context).hasSingleBean(OrganizationDirectoryGateway.class);
                    assertThat(context).doesNotHaveBean(OrganizationDirectoryHttpGateway.class);
                    assertThat(context.getBean(OrganizationDirectoryGateway.class))
                            .isInstanceOf(DisabledOrganizationDirectoryGateway.class);
                });
    }
}
