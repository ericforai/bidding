package com.xiyu.bid.organization.infrastructure.persistence.repository;

import com.xiyu.bid.organization.infrastructure.persistence.entity.LocalUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalUserJpaRepository extends JpaRepository<LocalUserEntity, Long> {

    Optional<LocalUserEntity> findByUserId(String userId);

    List<LocalUserEntity> findByStatus(String status);

    List<LocalUserEntity> findByDeptId(String deptId);
}
