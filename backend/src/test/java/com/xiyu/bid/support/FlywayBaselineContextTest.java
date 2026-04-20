package com.xiyu.bid.support;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("flyway-baseline")
@Import(NoOpPasswordEncryptionTestConfig.class)
class FlywayBaselineContextTest {

    @Test
    void contextLoadsWithFlywayBaselineAndHibernateValidate() {
    }

}
