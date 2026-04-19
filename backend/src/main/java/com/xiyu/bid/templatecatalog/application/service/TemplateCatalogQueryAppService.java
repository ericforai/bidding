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

@Service
@RequiredArgsConstructor
public class TemplateCatalogQueryAppService {

    private final TemplateCatalogRepository templateCatalogRepository;
    private final TemplateCatalogUseRecordRepository templateCatalogUseRecordRepository;
    private final TemplateCatalogDownloadRecordRepository templateCatalogDownloadRecordRepository;
    private final TemplateDtoMapper templateDtoMapper;
    private final TemplateVersionBootstrapper templateVersionBootstrapper;

    @Transactional(readOnly = true)
    public List<TemplateCatalogView> list(TemplateQueryCriteria criteria) {
        return templateCatalogRepository.findAll(criteria).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TemplateCatalogView getById(Long id) {
        Template template = templateCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        return toDto(templateVersionBootstrapper.ensureInitialized(template));
    }

    @Transactional(readOnly = true)
    public List<TemplateCatalogView> getByCategory(Template.Category category) {
        return list(TemplateQueryCriteria.builder().category(category).build());
    }

    @Transactional(readOnly = true)
    public List<TemplateCatalogView> getByCreator(Long createdBy) {
        return list(TemplateQueryCriteria.builder().createdBy(createdBy).build());
    }

    private TemplateCatalogView toDto(Template template) {
        return templateDtoMapper.toDto(
                template,
                templateCatalogDownloadRecordRepository.countByTemplateId(template.getId()),
                templateCatalogUseRecordRepository.countByTemplateId(template.getId())
        );
    }
}
