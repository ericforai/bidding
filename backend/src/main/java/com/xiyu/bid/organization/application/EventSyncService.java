package com.xiyu.bid.organization.application;

import com.xiyu.bid.organization.domain.EventDeduplicationPolicy;
import com.xiyu.bid.organization.domain.EventValidationException;
import com.xiyu.bid.organization.infrastructure.persistence.entity.OrganizationEventInboxEntity;
import com.xiyu.bid.organization.infrastructure.persistence.repository.OrganizationEventInboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class EventSyncService {

    /** Structured logger for event sync traceability. */
    private static final Logger LOG = LoggerFactory.getLogger(EventSyncService.class);

    private final OrganizationEventInboxJpaRepository inboxRepository;

    public EventSyncService(OrganizationEventInboxJpaRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Transactional
    public void receiveViaHttp(Map<String, Object> payload) {
        String traceId = requireField(payload, "traceId");
        String spanId = requireField(payload, "spanId");
        String eventTopic = requireField(payload, "eventTopic");
        String eventType = requireField(payload, "eventType");

        var dedupKey = EventDeduplicationPolicy.dedupKey(traceId, spanId, eventTopic);

        boolean alreadyExists = inboxRepository
                .findByTraceIdAndSpanIdAndEventTopic(traceId, spanId, eventTopic)
                .isPresent();

        if (alreadyExists) {
            LOG.info("Duplicate event skipped: traceId={}, spanId={}, topic={}",
                        traceId, spanId, eventTopic);
            return;
        }

        OrganizationEventInboxEntity entity = new OrganizationEventInboxEntity();
        entity.setEventKey(traceId + ":" + spanId);
        entity.setEventTopic(eventTopic);
        entity.setSourceApp("HTTP_FALLBACK");
        entity.setTraceId(traceId);
        entity.setSpanId(spanId);
        entity.setRawPayload(payload.toString());
        entity.setStatus("PROCESSING");
        entity.setRetryCount(0);
        entity.setReceivedAt(LocalDateTime.now());

        inboxRepository.save(entity);
        LOG.info("Event received via HTTP fallback: traceId={}, topic={}", traceId, eventTopic);
    }

    private static String requireField(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null || value.toString().isBlank()) {
            throw new EventValidationException("Missing required field: " + field);
        }
        return value.toString();
    }
}
