package com.xiyu.bid.batch.repository;

import com.xiyu.bid.batch.entity.TenderAssignmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenderAssignmentRecordRepository extends JpaRepository<TenderAssignmentRecord, Long> {
    List<TenderAssignmentRecord> findByTenderIdOrderByAssignedAtDesc(Long tenderId);
    Optional<TenderAssignmentRecord> findFirstByTenderIdOrderByAssignedAtDesc(Long tenderId);
}
