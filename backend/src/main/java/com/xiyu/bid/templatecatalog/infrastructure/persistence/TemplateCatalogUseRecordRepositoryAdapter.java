package com.xiyu.bid.templatecatalog.infrastructure.persistence;

import com.xiyu.bid.entity.TemplateUseRecord;
import com.xiyu.bid.repository.TemplateUseRecordRepository;
import com.xiyu.bid.templatecatalog.domain.port.TemplateCatalogUseRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemplateCatalogUseRecordRepositoryAdapter implements TemplateCatalogUseRecordRepository {

    private final TemplateUseRecordRepository templateUseRecordRepository;

    @Override
    public TemplateUseRecord save(TemplateUseRecord record) {
        return templateUseRecordRepository.save(record);
    }

    @Override
    public long countByTemplateId(Long templateId) {
        return templateUseRecordRepository.countByTemplateId(templateId);
    }
}
