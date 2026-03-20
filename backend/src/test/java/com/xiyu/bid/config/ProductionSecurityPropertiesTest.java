package com.xiyu.bid.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionSecurityPropertiesTest {

    @Test
    void productionErrorExposureIsTightened() {
        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new ClassPathResource("application-prod.yml"));
        Properties properties = factoryBean.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("server.error.include-message")).isEqualTo("never");
        assertThat(properties.getProperty("server.error.include-binding-errors")).isEqualTo("never");
        assertThat(properties.getProperty("server.error.include-stacktrace")).isEqualTo("never");
    }

    @Test
    void developmentCorsAllowsTheLocalFrontendOrigins() {
        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new ClassPathResource("application.yml"));
        Properties properties = factoryBean.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("cors.allowed-origins"))
                .contains("http://localhost:1314")
                .contains("http://127.0.0.1:1314");
    }
}
