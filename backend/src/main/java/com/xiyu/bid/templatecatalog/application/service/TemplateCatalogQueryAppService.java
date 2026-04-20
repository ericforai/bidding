package com.xiyu.bid.templatecatalog.application.service;

import com.xiyu.bid.entity.Template;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.templatecatalog.application.command.TemplateQueryCriteria;
import com.xiyu.bid.templatecatalog.application.mapper.TemplateDtoMapper;
import com.xiyu.bid.templatecatalog.application.view.TemplateCatalogView;
import com.xiyu.bid.templatecatalog.domain.port.TemplateCatalogDownloadRecordRepository;
import com.xiyu.bid.templatecatalog.domain.port.TemplateCatalogRepository;
import com.xiyu.bid.templatecatalog.domain.port.TemplateCatalogUseRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateCatalogQueryAppService {

    private final TemplateCatalogRepository templateCatalogRepository;
    private final TemplateCatalogUseRecordRepository templateCatalogUseRecordRepository;
    private final TemplateCatalogDownloadRecordRepository templateCatalogDownloadRecordRepository;
    private final TemplateDtoMapper templateDtoMapper;

    @Transactional(readOnly = true)
    public List<TemplateCatalogView> list(TemplateQueryCriteria criteria) {
        List<Template> templates = templateCatalogRepository.findAll(criteria);
        Map<Long, Long> downloads = templateCatalogDownloadRecordRepository.countByTemplateIds(extractIds(templates));
        Map<Long, Long> useCounts = templateCatalogUseRecordRepository.countByTemplateIds(extractIds(templates));
        return templates.stream()
                .map(template -> toDto(template, downloads, useCounts))
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateCatalogView getById(Long id) {
        Template template = templateCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        return toDto(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateCatalogView> getByCategory(Template.Category category) {
        return list(TemplateQueryCriteria.builder().category(category).build());
    }

    @Transactional(readOnly = true)
    public List<TemplateCatalogView> getByCreator(Long createdBy) {
        return list(TemplateQueryCriteria.builder().createdBy(createdBy).build());
    }

    private List<Long> extractIds(List<Template> templates) {
        return templates.stream().map(Template::getId).toList();
    }

    private TemplateCatalogView toDto(Template template) {
        return templateDtoMapper.toDto(
                template,
                templateCatalogDownloadRecordRepository.countByTemplateId(template.getId()),
                templateCatalogUseRecordRepository.countByTemplateId(template.getId())
        );
    }

    private TemplateCatalogView toDto(Template template, Map<Long, Long> downloads, Map<Long, Long> useCounts) {
        return templateDtoMapper.toDto(
                template,
                downloads.getOrDefault(template.getId(), 0L),
                useCounts.getOrDefault(template.getId(), 0L)
        );
    }
}
