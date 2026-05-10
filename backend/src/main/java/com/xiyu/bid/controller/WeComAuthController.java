package com.xiyu.bid.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.AuthResponse;
import com.xiyu.bid.dto.AuthSessionResult;
import com.xiyu.bid.integration.application.WeComAuthAppService;
import com.xiyu.bid.integration.domain.WeComApiErrCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth/wecom")
@RequiredArgsConstructor
public class WeComAuthController {

    private final WeComAuthAppService weComAuthAppService;
    private final com.xiyu.bid.auth.OAuthStateService oAuthStateService;

    @Value("${app.auth.refresh-cookie-name:refresh_token}")
    private String refreshCookieName;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie-same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    /**
     * Entry point to get the WeCom login URL (not implemented in this task but good to have).
     * For now, the frontend handles the redirect URL construction.
     */
    @GetMapping("/authorize-params")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> getAuthorizeParams() {
        String state = UUID.randomUUID().toString().replace("-", "");
        oAuthStateService.storeState(state);
        
        java.util.Map<String, String> params = weComAuthAppService.getAuthorizeParams(state);
        return ResponseEntity.ok(ApiResponse.success("Auth params generated", params));
    }

    /**
     * OAuth2 callback endpoint.
     */
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<?>> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ) {
        log.info("Received WeCom OAuth2 callback: code={}, state={}", code, state);

        // 1. Validate state (CSRF protection)
        if (!oAuthStateService.validateAndRemoveState(state)) {
            log.warn("OAuth2 callback state validation failed: {}", state);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "INVALID_STATE"));
        }
        
        // 2. Perform login
        var loginResultOpt = weComAuthAppService.loginByWeCom(code);

        if (loginResultOpt.isPresent()) {
            AuthSessionResult result = loginResultOpt.get();
            ResponseCookie cookie = buildRefreshCookie(result.getRefreshToken(), true);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(ApiResponse.success("WeCom login successful", result.getAuthResponse()));
        } else {
            // If user not found or mapping failed, return specific status for frontend to handle binding
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(40101, "WECOM_NOT_BOUND"));
        }
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
}
