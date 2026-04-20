package com.xiyu.bid.templatecatalog.domain.port;

import com.xiyu.bid.entity.TemplateUseRecord;

import java.util.Collection;
import java.util.Map;

public interface TemplateCatalogUseRecordRepository {
    TemplateUseRecord save(TemplateUseRecord record);

    long countByTemplateId(Long templateId);

    Map<Long, Long> countByTemplateIds(Collection<Long> templateIds);
}
