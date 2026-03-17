package com.xiyu.bid.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/public/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "XiYu Bid POC Backend is running");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserProfile(Authentication authentication) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", authentication.getName());
        profile.put("authorities", authentication.getAuthorities());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminDashboard() {
        return ResponseEntity.ok(
                ApiResponse.success("Welcome to Admin Dashboard")
        );
    }

    @GetMapping("/manager/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> managerDashboard() {
        return ResponseEntity.ok(
                ApiResponse.success("Welcome to Manager Dashboard")
        );
    }
}
