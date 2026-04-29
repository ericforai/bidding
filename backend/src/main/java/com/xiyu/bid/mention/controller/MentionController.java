package com.xiyu.bid.mention.controller;

import com.xiyu.bid.entity.User;
import com.xiyu.bid.mention.dto.CreateMentionRequest;
import com.xiyu.bid.mention.service.MentionApplicationService;
import com.xiyu.bid.mention.service.MentionApplicationService.MentionResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mentions")
public class MentionController {

    private final MentionApplicationService service;

    public MentionController(MentionApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
        @Valid @RequestBody CreateMentionRequest request,
        @AuthenticationPrincipal User currentUser) {
        MentionResult result = service.createMention(request, currentUser.getId());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of(
                "mentionCount", result.mentionCount(),
                "notificationId", result.notificationId() == null ? 0L : result.notificationId()
            )
        ));
    }
}
