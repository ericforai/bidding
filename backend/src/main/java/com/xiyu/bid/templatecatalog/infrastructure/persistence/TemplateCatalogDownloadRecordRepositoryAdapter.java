package com.xiyu.bid.templatecatalog.infrastructure.persistence;

import com.xiyu.bid.entity.TemplateDownloadRecord;
import com.xiyu.bid.repository.TemplateDownloadRecordRepository;
import com.xiyu.bid.templatecatalog.domain.port.TemplateCatalogDownloadRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemplateCatalogDownloadRecordRepositoryAdapter implements TemplateCatalogDownloadRecordRepository {

    private final TemplateDownloadRecordRepository templateDownloadRecordRepository;

    @Override
    public TemplateDownloadRecord save(TemplateDownloadRecord record) {
        return templateDownloadRecordRepository.save(record);
    }

    @Override
    public long countByTemplateId(Long templateId) {
        return templateDownloadRecordRepository.countByTemplateId(templateId);
    }
}
