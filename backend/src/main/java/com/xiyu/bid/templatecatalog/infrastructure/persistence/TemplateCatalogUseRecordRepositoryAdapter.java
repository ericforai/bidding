package com.xiyu.bid.templatecatalog.infrastructure.persistence;

import com.xiyu.bid.entity.TemplateUseRecord;
import com.xiyu.bid.repository.TemplateUseRecordRepository;
import com.xiyu.bid.templatecatalog.domain.port.TemplateCatalogUseRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public Map<Long, Long> countByTemplateIds(Collection<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Map.of();
        }
        return templateUseRecordRepository.countGroupedByTemplateIds(templateIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue(),
                        (left, right) -> right
                ));
    }
}
