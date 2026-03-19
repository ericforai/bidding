// Input: HTTP 请求、路径参数、认证上下文和 DTO
// Output: 标准化 API 响应和用例入口
// Pos: Controller/接口适配层
// 维护声明: 仅维护协议适配与参数校验；业务规则下沉到 service.
package com.xiyu.bid.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.AuthResponse;
import com.xiyu.bid.dto.AuthSessionResult;
import com.xiyu.bid.dto.LoginRequest;
import com.xiyu.bid.dto.RegisterRequest;
import com.xiyu.bid.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.auth.refresh-cookie-name:refresh_token}")
    private String refreshCookieName;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie-same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        // Sanitize user input
        sanitizeRegisterRequest(request);
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        // Sanitize user input
        if (request.getUsername() != null) {
            request.setUsername(InputSanitizer.sanitizeString(request.getUsername(), 50));
        }
        AuthSessionResult sessionResult = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(sessionResult.getRefreshToken(), Boolean.TRUE.equals(request.getRememberMe())).toString())
                .body(ApiResponse.success("Login successful", sessionResult.getAuthResponse()));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(Authentication authentication) {
        AuthResponse response = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(extractRefreshToken(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(HttpServletRequest request) {
        AuthSessionResult sessionResult = authService.refreshToken(extractRefreshToken(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(sessionResult.getRefreshToken(), true).toString())
                .body(ApiResponse.success("Token refreshed successfully", sessionResult.getAuthResponse()));
    }

    /**
     * 清洗注册请求中的用户输入
     */
    private void sanitizeRegisterRequest(RegisterRequest request) {
        if (request.getUsername() != null) {
            request.setUsername(InputSanitizer.sanitizeString(request.getUsername(), 50));
        }
        if (request.getEmail() != null) {
            request.setEmail(InputSanitizer.sanitizeString(request.getEmail(), 100));
        }
        if (request.getFullName() != null) {
            request.setFullName(InputSanitizer.sanitizeString(request.getFullName(), 100));
        }
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (refreshCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie buildRefreshCookie(String refreshToken, boolean persistent) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/");

        if (persistent) {
            builder.maxAge(Duration.ofMillis(refreshExpiration));
        }

        return builder.build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
