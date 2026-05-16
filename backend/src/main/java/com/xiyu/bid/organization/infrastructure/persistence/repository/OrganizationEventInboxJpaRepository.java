package com.xiyu.bid.organization.infrastructure.persistence.repository;

import com.xiyu.bid.organization.infrastructure.persistence.entity.OrganizationEventInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationEventInboxJpaRepository extends JpaRepository<OrganizationEventInboxEntity, Long> {

    Optional<OrganizationEventInboxEntity> findByTraceIdAndSpanIdAndEventTopic(
            String traceId, String spanId, String eventTopic);

    List<OrganizationEventInboxEntity> findByStatusOrderByReceivedAtAsc(String status);

    Optional<OrganizationEventInboxEntity> findByEventKey(String eventKey);
}
