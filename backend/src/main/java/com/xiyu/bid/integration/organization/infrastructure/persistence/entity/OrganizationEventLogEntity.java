package com.xiyu.bid.integration.organization.infrastructure.persistence.entity;

import com.xiyu.bid.integration.organization.domain.OrganizationEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "organization_event_logs")
public class OrganizationEventLogEntity {
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

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrganizationEventStatus status;

    @Column(length = 500)
    private String message;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
