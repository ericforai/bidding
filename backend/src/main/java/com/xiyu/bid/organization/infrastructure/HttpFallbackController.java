package com.xiyu.bid.organization.infrastructure;

import com.xiyu.bid.organization.application.EventSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/xiyu/org-events")
public class HttpFallbackController {

    private static final Logger log = LoggerFactory.getLogger(HttpFallbackController.class);

    private final EventSyncService eventSyncService;

    public HttpFallbackController(EventSyncService eventSyncService) {
        this.eventSyncService = eventSyncService;
    }

    @PostMapping("/fallback")
    public ResponseEntity<Map<String, Object>> receiveEvent(@RequestBody Map<String, Object> payload) {
        try {
            eventSyncService.receiveViaHttp(payload);
            return ResponseEntity.accepted().body(Map.of("result", "accepted"));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid event payload received via HTTP fallback: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("result", "rejected", "reason", e.getMessage()));
        }
    }
}
