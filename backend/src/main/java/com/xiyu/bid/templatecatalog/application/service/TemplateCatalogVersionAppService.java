package com.xiyu.bid.templatecatalog.application.service;

import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.templatecatalog.application.mapper.TemplateVersionDtoMapper;
import com.xiyu.bid.templatecatalog.application.view.TemplateCatalogVersionView;
import com.xiyu.bid.templatecatalog.domain.port.TemplateCatalogRepository;
import com.xiyu.bid.templatecatalog.domain.port.TemplateCatalogVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateCatalogVersionAppService {

    private final TemplateCatalogRepository templateCatalogRepository;
    private final TemplateCatalogVersionRepository templateCatalogVersionRepository;
    private final TemplateVersionDtoMapper templateVersionDtoMapper;
    private final TemplateVersionBootstrapper templateVersionBootstrapper;

    @Transactional
    public List<TemplateCatalogVersionView> list(Long id) {
        templateCatalogRepository.findById(id)
                .map(templateVersionBootstrapper::ensureInitialized)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));

        return templateCatalogVersionRepository.findByTemplateIdOrderByCreatedAtDesc(id).stream()
                .map(templateVersionDtoMapper::toDto)
                .toList();
    }
}
