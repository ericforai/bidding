package com.xiyu.bid.templatecatalog.domain.port;

import com.xiyu.bid.entity.TemplateDownloadRecord;

public interface TemplateCatalogDownloadRecordRepository {
    TemplateDownloadRecord save(TemplateDownloadRecord record);

    long countByTemplateId(Long templateId);
}
