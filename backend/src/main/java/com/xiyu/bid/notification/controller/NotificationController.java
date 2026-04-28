package com.xiyu.bid.notification.controller;

import com.xiyu.bid.entity.User;
import com.xiyu.bid.notification.core.NotificationDispatchPolicy.DispatchResult;
import com.xiyu.bid.notification.core.NotificationReadPolicy.ReadResult;
import com.xiyu.bid.notification.dto.CreateNotificationRequest;
import com.xiyu.bid.notification.dto.NotificationSummary;
import com.xiyu.bid.notification.service.NotificationApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationApplicationService service;

    private static final int MAX_PAGE_SIZE = 100;

    public NotificationController(NotificationApplicationService service) {
        this.service = service;
    }

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> getNotifications(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Page<NotificationSummary> result = service.getNotifications(
            currentUser.getId(), PageRequest.of(safePage, safeSize));
        Map<String, Object> data = Map.of(
            "content", result.getContent(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages(),
            "number", result.getNumber(),
            "size", result.getSize()
        );
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
        @AuthenticationPrincipal User currentUser) {
        long count = service.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("count", count)));
    }

    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
        @PathVariable Long notificationId,
        @AuthenticationPrincipal User currentUser) {
        ReadResult result = service.markAsRead(notificationId, currentUser.getId());
        if (!result.isValid()) {
            return ResponseEntity.status(403).body(
                Map.of("success", false, "message", result.errorMessage()));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
        @AuthenticationPrincipal User currentUser) {
        service.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/admin/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createNotification(
        @Valid @RequestBody CreateNotificationRequest request,
        @AuthenticationPrincipal User currentUser) {
        DispatchResult result = service.createNotification(request, currentUser.getId());
        if (!result.isValid()) {
            return ResponseEntity.badRequest().body(
                Map.of("success", false, "message", result.errorMessage()));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}
