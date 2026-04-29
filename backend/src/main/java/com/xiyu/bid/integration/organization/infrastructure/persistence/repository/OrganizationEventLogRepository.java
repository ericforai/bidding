package com.xiyu.bid.integration.organization.infrastructure.persistence.repository;

import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationEventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OrganizationEventLogRepository extends JpaRepository<OrganizationEventLogEntity, Long> {
    boolean existsByEventKey(String eventKey);

    Optional<OrganizationEventLogEntity> findByEventKey(String eventKey);

    int deleteByReceivedAtBefore(LocalDateTime cutoff);
}
