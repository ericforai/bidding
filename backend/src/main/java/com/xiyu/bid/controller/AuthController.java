package com.xiyu.bid.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.AuthResponse;
import com.xiyu.bid.dto.LoginRequest;
import com.xiyu.bid.dto.RegisterRequest;
import com.xiyu.bid.service.AuthService;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
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
}
