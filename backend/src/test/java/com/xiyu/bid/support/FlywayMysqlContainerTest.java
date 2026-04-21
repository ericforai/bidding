package com.xiyu.bid.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("flyway-mysql")
@Testcontainers(disabledWithoutDocker = true)
@Import(NoOpPasswordEncryptionTestConfig.class)
class FlywayMysqlContainerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("xiyu_bid_test")
            .withUsername("xiyu")
            .withPassword("xiyu");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Test
    void contextLoadsWithFlywayBaselineOnRealMysql() {
        Integer baselineRuns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from flyway_schema_history
                where success = 1
                  and version = '73'
                  and script = 'B73__full_schema_baseline.sql'
                """,
                Integer.class
        );

        Integer projectQualityTableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and table_name = 'project_quality_checks'
                """,
                Integer.class
        );

        Integer tenderAssignmentTableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and table_name = 'tender_assignment_records'
                """,
                Integer.class
        );

        Integer roleSeedCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from roles
                where code in ('admin', 'manager', 'staff')
                """,
                Integer.class
        );

        Integer postgresIncrementalRuns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from flyway_schema_history
                where script = 'V1__resources_contracts.sql'
                """,
                Integer.class
        );

        assertEquals(1, baselineRuns);
        assertEquals(1, projectQualityTableCount);
        assertEquals(1, tenderAssignmentTableCount);
        assertEquals(3, roleSeedCount);
        assertEquals(0, postgresIncrementalRuns);
    }
}
