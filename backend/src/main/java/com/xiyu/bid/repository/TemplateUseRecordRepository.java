package com.xiyu.bid.repository;

import com.xiyu.bid.entity.TemplateUseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateUseRecordRepository extends JpaRepository<TemplateUseRecord, Long> {
    long countByTemplateId(Long templateId);
}
