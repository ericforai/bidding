package com.xiyu.bid.repository;

import com.xiyu.bid.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {

    Optional<RefreshSession> findByTokenHash(String tokenHash);
}
