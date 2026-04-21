package com.xiyu.bid.contractborrow.infrastructure.persistence.repository;

import com.xiyu.bid.contractborrow.domain.valueobject.ContractBorrowStatus;
import com.xiyu.bid.contractborrow.infrastructure.persistence.entity.ContractBorrowApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractBorrowApplicationJpaRepository extends JpaRepository<ContractBorrowApplicationEntity, Long> {

    List<ContractBorrowApplicationEntity> findByOrderBySubmittedAtDesc();

    long countByStatus(ContractBorrowStatus status);
}
