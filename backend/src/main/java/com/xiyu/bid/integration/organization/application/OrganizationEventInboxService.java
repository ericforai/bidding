package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.domain.OrganizationEventNotice;
import com.xiyu.bid.integration.organization.domain.OrganizationEventStatus;
import com.xiyu.bid.integration.organization.domain.OrganizationSyncPolicy;
import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationEventLogEntity;
import com.xiyu.bid.integration.organization.infrastructure.persistence.repository.OrganizationEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class OrganizationEventInboxService {
    private final OrganizationEventLogRepository eventLogRepository;

    public String eventKey(OrganizationEventNotice notice) {
        return OrganizationEventKeyFactory.hash(OrganizationSyncPolicy.idempotencyKey(notice));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reserve(OrganizationEventNotice notice, String rawPayload) {
        try {
            eventLogRepository.saveAndFlush(buildLog(notice, rawPayload, OrganizationEventStatus.PROCESSING, "processing", ""));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRejected(String eventKey, String message, String rawPayload) {
        OrganizationEventLogEntity log = eventLogRepository.findByEventKey(eventKey).orElseGet(OrganizationEventLogEntity::new);
        log.setEventKey(eventKey);
        log.setEventTopic("");
        log.setSourceApp("");
        log.setTraceId("");
        log.setPayloadHash(OrganizationEventKeyFactory.hash(rawPayload == null ? "" : rawPayload));
        applyStatus(log, OrganizationEventStatus.REJECTED, message, "VALIDATION_FAILED");
        eventLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(String eventKey) {
        updateStatus(eventKey, OrganizationEventStatus.PROCESSED, "success", "");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String eventKey, String message, String errorCode) {
        updateStatus(eventKey, OrganizationEventStatus.FAILED, message, errorCode);
    }

    private void updateStatus(String eventKey, OrganizationEventStatus status, String message, String errorCode) {
        eventLogRepository.findByEventKey(eventKey).ifPresent(log -> {
            applyStatus(log, status, message, errorCode);
            eventLogRepository.save(log);
        });
    }

    private OrganizationEventLogEntity buildLog(
            OrganizationEventNotice notice,
            String rawPayload,
            OrganizationEventStatus status,
            String message,
            String errorCode
    ) {
        OrganizationEventLogEntity log = new OrganizationEventLogEntity();
        log.setEventKey(eventKey(notice));
        log.setUpstreamEventKey(notice.key());
        log.setEventTopic(notice.topic().topic());
        log.setSourceApp(notice.eventSource());
        log.setTraceId(notice.traceId());
        log.setSpanId(notice.spanId());
        log.setParentId(notice.parentId());
        log.setEventTime(parseEventTime(notice.time()));
        log.setEntityType(notice.topic().entityType());
        if (notice.topic().entityType().equals("USER")) {
            log.setExternalUserId(notice.subjectId());
        } else {
            log.setExternalDeptId(notice.subjectId());
        }
        log.setPayloadHash(OrganizationEventKeyFactory.hash(rawPayload));
        log.setRawPayload(rawPayload);
        applyStatus(log, status, message, errorCode);
        return log;
    }

    private void applyStatus(OrganizationEventLogEntity log, OrganizationEventStatus status, String message, String errorCode) {
        log.setStatus(status);
        log.setMessage(message == null ? "" : message);
        log.setLastErrorCode(errorCode == null ? "" : errorCode);
        log.setProcessedAt(LocalDateTime.now());
        if (status == OrganizationEventStatus.FAILED) {
            log.setRetryCount(log.getRetryCount() == null ? 1 : log.getRetryCount() + 1);
            log.setNextRetryAt(LocalDateTime.now().plusMinutes(5));
        }
    }

    private LocalDateTime parseEventTime(String eventTime) {
        try {
            return OffsetDateTime.parse(eventTime).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
