package com.xiyu.bid.support;

import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("flyway-postgres")
@Testcontainers(disabledWithoutDocker = true)
class FlywayPostgresContainerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.3")
            .withDatabaseName("xiyu_bid_test")
            .withUsername("xiyu")
            .withPassword("xiyu");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void contextLoadsWithFlywayBaselineOnRealPostgres() {
        Integer baselineRuns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from flyway_schema_history
                where success = true
                  and version = '2'
                  and script = 'B2__full_schema_baseline.sql'
                """,
                Integer.class
        );

        Integer resourcesIncrementalRuns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from flyway_schema_history
                where success = true
                  and version = '1'
                  and script = 'V1__resources_contracts.sql'
                """,
                Integer.class
        );

        Integer systemSettingsRuns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from flyway_schema_history
                where success = true
                  and version = '13'
                  and script = 'V13__create_system_settings_table.sql'
                """,
                Integer.class
        );

        Integer systemSettingsTableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name = 'system_settings'
                """,
                Integer.class
        );

        Integer refreshSessionRuns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from flyway_schema_history
                where success = true
                  and version = '14'
                  and script = 'V14__create_refresh_sessions_table.sql'
                """,
                Integer.class
        );

        Integer refreshSessionsTableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name = 'refresh_sessions'
                """,
                Integer.class
        );

        Integer userDepartmentRuns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from flyway_schema_history
                where success = true
                  and version = '15'
                  and script = 'V15__add_user_department_columns.sql'
                """,
                Integer.class
        );

        Integer departmentCodeColumnCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'users'
                  and column_name = 'department_code'
                """,
                Integer.class
        );

        org.junit.jupiter.api.Assertions.assertEquals(1, baselineRuns);
        org.junit.jupiter.api.Assertions.assertEquals(0, resourcesIncrementalRuns);
        org.junit.jupiter.api.Assertions.assertEquals(1, systemSettingsRuns);
        org.junit.jupiter.api.Assertions.assertEquals(1, systemSettingsTableCount);
        org.junit.jupiter.api.Assertions.assertEquals(1, refreshSessionRuns);
        org.junit.jupiter.api.Assertions.assertEquals(1, refreshSessionsTableCount);
        org.junit.jupiter.api.Assertions.assertEquals(1, userDepartmentRuns);
        org.junit.jupiter.api.Assertions.assertEquals(1, departmentCodeColumnCount);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean(name = "passwordEncryptionUtil")
        @Primary
        PasswordEncryptionUtil passwordEncryptionUtil() {
            return new PasswordEncryptionUtil() {
                @Override
                public void initialize() {
                }

                @Override
                public String encrypt(String plainPassword) {
                    return plainPassword;
                }

                @Override
                public String decrypt(String encryptedPassword) {
                    return encryptedPassword;
                }

                @Override
                public boolean isKeyValid() {
                    return true;
                }
            };
        }
    }
}
