package com.xiyu.bid.notification.outbound.controller;

import com.xiyu.bid.notification.outbound.dto.WeComBindingRequest;
import com.xiyu.bid.notification.outbound.service.WeComBindingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users/{userId}/wecom-binding")
@PreAuthorize("hasRole('ADMIN')")
public class WeComBindingController {

    private final WeComBindingService bindingService;

    public WeComBindingController(WeComBindingService bindingService) {
        this.bindingService = bindingService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long userId) {
        String binding = bindingService.currentBinding(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("wecomUserId", binding);
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> bind(
        @PathVariable Long userId,
        @Valid @RequestBody WeComBindingRequest request) {
        bindingService.bind(userId, request.wecomUserId());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of("wecomUserId", request.wecomUserId())
        ));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> unbind(@PathVariable Long userId) {
        bindingService.unbind(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
