package com.xiyu.bid.templatecatalog.application.service;

import com.xiyu.bid.entity.Template;
import com.xiyu.bid.entity.TemplateDownloadRecord;
import com.xiyu.bid.entity.TemplateUseRecord;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.templatecatalog.application.command.TemplateCatalogDownloadRecordCommand;
import com.xiyu.bid.templatecatalog.application.command.TemplateCatalogUseRecordCommand;
import com.xiyu.bid.templatecatalog.application.mapper.TemplateDtoMapper;
import com.xiyu.bid.templatecatalog.application.mapper.TemplateUseRecordDtoMapper;
import com.xiyu.bid.templatecatalog.application.view.TemplateCatalogUseRecordView;
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
public class TemplateCatalogActivityAppService {

    private final TemplateCatalogRepository templateCatalogRepository;
    private final TemplateCatalogUseRecordRepository templateCatalogUseRecordRepository;
    private final TemplateCatalogDownloadRecordRepository templateCatalogDownloadRecordRepository;
    private final TemplateDtoMapper templateDtoMapper;
    private final TemplateUseRecordDtoMapper templateUseRecordDtoMapper;
    private final TemplateVersionBootstrapper templateVersionBootstrapper;

    @Transactional
    public TemplateCatalogUseRecordView createUseRecord(Long id, TemplateCatalogUseRecordCommand command) {
        Template template = templateCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        templateVersionBootstrapper.ensureInitialized(template);

        TemplateUseRecord record = templateCatalogUseRecordRepository.save(TemplateUseRecord.builder()
                .template(template)
                .documentName(command.getDocumentName())
                .docType(command.getDocType())
                .projectId(command.getProjectId())
                .appliedOptions(joinOptions(command.getApplyOptions()))
                .usedBy(command.getUsedBy())
                .build());
        return templateUseRecordDtoMapper.toDto(record);
    }

    @Transactional
    public TemplateCatalogView createDownloadRecord(Long id, TemplateCatalogDownloadRecordCommand command) {
        Template template = templateCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        templateVersionBootstrapper.ensureInitialized(template);

        templateCatalogDownloadRecordRepository.save(TemplateDownloadRecord.builder()
                .template(template)
                .downloadedBy(command != null ? command.getDownloadedBy() : null)
                .build());
        return templateDtoMapper.toDto(
                template,
                templateCatalogDownloadRecordRepository.countByTemplateId(template.getId()),
                templateCatalogUseRecordRepository.countByTemplateId(template.getId())
        );
    }

    private String joinOptions(List<String> options) {
        return (options == null || options.isEmpty()) ? "" : String.join(",", options);
    }
}
