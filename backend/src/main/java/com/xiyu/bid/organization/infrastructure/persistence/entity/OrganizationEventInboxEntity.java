package com.xiyu.bid.organization.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_event_logs")
@Getter
@Setter
public class OrganizationEventInboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, unique = true, length = 128)
    private String eventKey;

    @Column(name = "event_topic", nullable = false, length = 100)
    private String eventTopic;

    @Column(name = "source_app", nullable = false, length = 100)
    private String sourceApp;

    @Column(name = "trace_id", nullable = false, length = 128)
    private String traceId;

    @Column(name = "span_id", length = 128)
    private String spanId;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "entity_type", length = 32)
    private String entityType;

    @Column(name = "external_user_id", length = 128)
    private String externalUserId;

    @Column(name = "external_dept_id", length = 128)
    private String externalDeptId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
