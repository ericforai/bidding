package com.xiyu.bid.repository;

import com.xiyu.bid.entity.TemplateDownloadRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateDownloadRecordRepository extends JpaRepository<TemplateDownloadRecord, Long> {
    long countByTemplateId(Long templateId);
}
