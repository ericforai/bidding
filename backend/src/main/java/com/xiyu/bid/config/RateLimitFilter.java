// Input: Spring 配置属性、环境变量、外部 bean 依赖
// Output: 配置 Bean、过滤器、线程池和启动级常量
// Pos: Config/基础设施层
// 维护声明: 仅维护配置与启动约束；业务规则变更请同步到对应 service/controller.
package com.xiyu.bid.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting filter for login endpoints
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig.RateLimiter rateLimiter;

    @Value("${rate.limit.login.max-attempts:5}")
    private int maxLoginAttempts;

    @Value("${rate.limit.login.window-minutes:15}")
    private int loginWindowMinutes;

    @Autowired
    public RateLimitFilter(RateLimitConfig.RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // Apply rate limiting only to POST /api/auth/login
        if ("/api/auth/login".equals(requestURI) && "POST".equalsIgnoreCase(method)) {
            String clientIp = getClientIp(request);
            String rateLimitKey = "login:" + clientIp;

            boolean allowed = rateLimiter.allowRequest(
                rateLimitKey,
                maxLoginAttempts,
                Duration.ofMinutes(loginWindowMinutes)
            );

            if (!allowed) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                response.setStatus(429); // HTTP 429 Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many login attempts. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
