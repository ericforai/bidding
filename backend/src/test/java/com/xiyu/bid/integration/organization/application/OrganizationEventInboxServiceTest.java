package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.domain.OrganizationEventNotice;
import com.xiyu.bid.integration.organization.domain.OrganizationEventStatus;
import com.xiyu.bid.integration.organization.domain.OrganizationEventType;
import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationEventLogEntity;
import com.xiyu.bid.integration.organization.infrastructure.persistence.repository.OrganizationEventLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationEventInboxService - idempotency and status")
class OrganizationEventInboxServiceTest {
    @Mock
    private OrganizationEventLogRepository repository;

    @Test
    @DisplayName("reserve stores notice identity and raw payload")
    void reserve_storesNoticeIdentity() {
        when(repository.saveAndFlush(any(OrganizationEventLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrganizationEventInboxService service = new OrganizationEventInboxService(repository);

        boolean reserved = service.reserve(notice(), "{\"data\":{\"userId\":\"10001\"}}");

        assertThat(reserved).isTrue();
        ArgumentCaptor<OrganizationEventLogEntity> saved = ArgumentCaptor.forClass(OrganizationEventLogEntity.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEventTopic()).isEqualTo("BaseOssUser");
        assertThat(saved.getValue().getExternalUserId()).isEqualTo("10001");
        assertThat(saved.getValue().getStatus()).isEqualTo(OrganizationEventStatus.PROCESSING);
    }

    @Test
    @DisplayName("duplicate unique key returns false")
    void reserve_duplicate_returnsFalse() {
        when(repository.saveAndFlush(any(OrganizationEventLogEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        OrganizationEventInboxService service = new OrganizationEventInboxService(repository);

        assertThat(service.reserve(notice(), "{}")).isFalse();
    }

    @Test
    @DisplayName("failed status increments retry metadata")
    void markFailed_updatesRetryFields() {
        OrganizationEventLogEntity existing = new OrganizationEventLogEntity();
        existing.setEventKey("event-key");
        existing.setRetryCount(0);
        when(repository.findByEventKey("event-key")).thenReturn(Optional.of(existing));
        OrganizationEventInboxService service = new OrganizationEventInboxService(repository);

        service.markFailed("event-key", "接口超时", "TIMEOUT");

        assertThat(existing.getStatus()).isEqualTo(OrganizationEventStatus.FAILED);
        assertThat(existing.getRetryCount()).isEqualTo(1);
        assertThat(existing.getLastErrorCode()).isEqualTo("TIMEOUT");
        assertThat(existing.getNextRetryAt()).isNotNull();
    }

    private OrganizationEventNotice notice() {
        return new OrganizationEventNotice(
                "trace-1", "span-1", "parent-1", "customer-org",
                OrganizationEventType.USER_NOTICE, "2026-04-30T10:15:30+08:00", "event-1", "10001"
        );
    }
}
