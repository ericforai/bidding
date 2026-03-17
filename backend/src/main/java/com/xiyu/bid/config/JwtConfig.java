package com.xiyu.bid.config;

import com.xiyu.bid.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")  // 默认24小时
    private Long expiration;

    @Bean
    public JwtUtil jwtUtil() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET environment variable must be set with at least 32 characters");
        }
        return new JwtUtil(secret, expiration);
    }
}
