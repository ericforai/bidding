// Input: REST requests from authenticated users
// Output: JSON responses with {success,data} envelope for subscribe flows
// Pos: Controller/订阅 REST 控制器
package com.xiyu.bid.subscription.controller;

import com.xiyu.bid.entity.User;
import com.xiyu.bid.subscription.core.SubscriptionPolicy;
import com.xiyu.bid.subscription.core.SubscriptionPolicy.ValidationResult;
import com.xiyu.bid.subscription.dto.SubscriptionRequest;
import com.xiyu.bid.subscription.dto.SubscriptionSummary;
import com.xiyu.bid.subscription.service.SubscriptionApplicationService;
import com.xiyu.bid.subscription.service.SubscriptionApplicationService.SubscribeResult;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@PreAuthorize("isAuthenticated()")
public class SubscriptionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SubscriptionApplicationService service;

    public SubscriptionController(SubscriptionApplicationService service) {
        this.service = service;
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> subscribe(
        @Valid @RequestBody SubscriptionRequest request,
        @AuthenticationPrincipal User currentUser) {
        SubscribeResult result = service.subscribe(
            currentUser.getId(), request.targetEntityType(), request.targetEntityId());
        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "errorCode", result.errorCode() == null ? "" : result.errorCode(),
                "message", result.errorMessage() == null ? "" : result.errorMessage()
            ));
        }
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of("subscriptionId", result.subscriptionId())
        ));
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> unsubscribe(
        @Valid @RequestBody SubscriptionRequest request,
        @AuthenticationPrincipal User currentUser) {
        int affected = service.unsubscribe(
            currentUser.getId(), request.targetEntityType(), request.targetEntityId());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of("affected", affected)
        ));
    }

    @GetMapping("/subscriptions/me")
    public ResponseEntity<Map<String, Object>> listMine(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Page<SubscriptionSummary> result = service.listMySubscriptions(
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

    @GetMapping("/entities/{entityType}/{entityId}/subscription")
    public ResponseEntity<Map<String, Object>> checkSubscribed(
        @PathVariable String entityType,
        @PathVariable Long entityId,
        @AuthenticationPrincipal User currentUser) {
        ValidationResult validation = SubscriptionPolicy.validate(currentUser.getId(), entityType, entityId);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "errorCode", validation.errorCode() == null ? "" : validation.errorCode(),
                "message", validation.errorMessage() == null ? "" : validation.errorMessage()
            ));
        }
        boolean subscribed = service.isSubscribed(currentUser.getId(), entityType, entityId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of("subscribed", subscribed)
        ));
    }
}
