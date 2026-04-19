package com.xiyu.bid.templatecatalog.domain.port;

import com.xiyu.bid.entity.TemplateUseRecord;

public interface TemplateCatalogUseRecordRepository {
    TemplateUseRecord save(TemplateUseRecord record);

    long countByTemplateId(Long templateId);
}
