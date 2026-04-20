package com.xiyu.bid.support;

import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class NoOpPasswordEncryptionTestConfig {

    @Bean(name = "passwordEncryptionUtil")
    @Primary
    PasswordEncryptionUtil passwordEncryptionUtil() {
        return new TestPasswordEncryptionUtil();
    }
}
