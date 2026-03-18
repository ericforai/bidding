// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.service;

import com.xiyu.bid.dto.TemplateCopyRequest;
import com.xiyu.bid.dto.TemplateDownloadRecordRequest;
import com.xiyu.bid.dto.TemplateDTO;
import com.xiyu.bid.dto.TemplateUseRecordDTO;
import com.xiyu.bid.dto.TemplateUseRecordRequest;
import com.xiyu.bid.dto.TemplateVersionDTO;
import com.xiyu.bid.entity.TemplateDownloadRecord;
import com.xiyu.bid.entity.Template;
import com.xiyu.bid.entity.TemplateUseRecord;
import com.xiyu.bid.entity.TemplateVersion;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TemplateDownloadRecordRepository;
import com.xiyu.bid.repository.TemplateRepository;
import com.xiyu.bid.repository.TemplateUseRecordRepository;
import com.xiyu.bid.repository.TemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateUseRecordRepository templateUseRecordRepository;
    private final TemplateDownloadRecordRepository templateDownloadRecordRepository;

    /**
     * 创建模板
     */
    @Transactional
    public TemplateDTO createTemplate(TemplateDTO dto) {
        log.info("Creating template: {}", dto.getName());

        Template template = Template.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .fileUrl(dto.getFileUrl())
                .description(dto.getDescription())
                .currentVersion("1.0")
                .fileSize(dto.getFileSize() != null ? dto.getFileSize() : "未知")
                .tags(copyTags(dto.getTags()))
                .createdBy(dto.getCreatedBy())
                .build();

        Template saved = templateRepository.save(template);
        createVersionRecord(saved, saved.getCurrentVersion(), "初始版本", saved.getName(), saved.getCreatedBy());
        log.info("Template created successfully with id: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * 获取所有模板（限制返回1000条）
     */
    @Transactional(readOnly = true)
    public List<TemplateDTO> getAllTemplates() {
        log.debug("Fetching all templates");
        return templateRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取模板
     */
    @Transactional(readOnly = true)
    public TemplateDTO getTemplateById(Long id) {
        log.debug("Fetching template by id: {}", id);
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        ensureInitialVersion(template);
        return toDTO(template);
    }

    /**
     * 更新模板
     */
    @Transactional
    public TemplateDTO updateTemplate(Long id, TemplateDTO dto) {
        log.info("Updating template: {}", id);

        Template existing = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        ensureInitialVersion(existing);

        String nextVersion = incrementVersion(existing.getCurrentVersion());

        // 使用Builder模式创建新对象而非直接修改
        Template updated = Template.builder()
                .id(existing.getId())
                .name(dto.getName() != null ? dto.getName() : existing.getName())
                .category(dto.getCategory() != null ? dto.getCategory() : existing.getCategory())
                .fileUrl(dto.getFileUrl() != null ? dto.getFileUrl() : existing.getFileUrl())
                .description(dto.getDescription() != null ? dto.getDescription() : existing.getDescription())
                .currentVersion(nextVersion)
                .fileSize(dto.getFileSize() != null ? dto.getFileSize() : existing.getFileSize())
                .tags(dto.getTags() != null ? copyTags(dto.getTags()) : copyTags(existing.getTags()))
                .createdBy(existing.getCreatedBy())
                .createdAt(existing.getCreatedAt())
                .updatedAt(existing.getUpdatedAt())
                .build();

        updated = templateRepository.save(updated);
        createVersionRecord(updated, nextVersion, "模板更新", updated.getName(), updated.getCreatedBy());
        log.info("Template updated successfully: {}", id);

        return toDTO(updated);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long id) {
        log.info("Deleting template: {}", id);

        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Template", id.toString());
        }

        templateRepository.deleteById(id);
        log.info("Template deleted successfully: {}", id);
    }

    /**
     * 根据类别查找模板（限制返回1000条）
     */
    @Transactional(readOnly = true)
    public List<TemplateDTO> getTemplatesByCategory(Template.Category category) {
        log.debug("Fetching templates by category: {}", category);
        return templateRepository.findByCategory(category, org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据创建者查找模板（限制返回1000条）
     */
    @Transactional(readOnly = true)
    public List<TemplateDTO> getTemplatesByCreatedBy(Long createdBy) {
        log.debug("Fetching templates by creator: {}", createdBy);
        return templateRepository.findByCreatedBy(createdBy, org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TemplateDTO copyTemplate(Long id, TemplateCopyRequest request) {
        Template source = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        ensureInitialVersion(source);

        Template copied = Template.builder()
                .name(request.getName())
                .category(source.getCategory())
                .fileUrl(source.getFileUrl())
                .description(source.getDescription())
                .currentVersion("1.0")
                .fileSize(source.getFileSize())
                .tags(copyTags(source.getTags()))
                .createdBy(request.getCreatedBy())
                .build();

        copied = templateRepository.save(copied);
        createVersionRecord(copied, "1.0", "复制自模板 #" + source.getId(), copied.getName(), copied.getCreatedBy());
        return toDTO(copied);
    }

    @Transactional(readOnly = true)
    public List<TemplateVersionDTO> getTemplateVersions(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        ensureInitialVersion(template);

        return templateVersionRepository.findByTemplateIdOrderByCreatedAtDesc(id).stream()
                .map(version -> TemplateVersionDTO.builder()
                        .id(version.getId())
                        .version(version.getVersion())
                        .description(version.getDescription())
                        .snapshotName(version.getSnapshotName())
                        .createdBy(version.getCreatedBy())
                        .createdAt(version.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public TemplateUseRecordDTO createTemplateUseRecord(Long id, TemplateUseRecordRequest request) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        ensureInitialVersion(template);

        TemplateUseRecord record = templateUseRecordRepository.save(TemplateUseRecord.builder()
                .template(template)
                .documentName(request.getDocumentName())
                .docType(request.getDocType())
                .projectId(request.getProjectId())
                .appliedOptions(joinOptions(request.getApplyOptions()))
                .usedBy(request.getUsedBy())
                .build());

        return TemplateUseRecordDTO.builder()
                .id(record.getId())
                .documentName(record.getDocumentName())
                .docType(record.getDocType())
                .projectId(record.getProjectId())
                .applyOptions(splitOptions(record.getAppliedOptions()))
                .usedBy(record.getUsedBy())
                .usedAt(record.getUsedAt())
                .build();
    }

    @Transactional
    public TemplateDTO createTemplateDownloadRecord(Long id, TemplateDownloadRecordRequest request) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id.toString()));
        ensureInitialVersion(template);

        templateDownloadRecordRepository.save(TemplateDownloadRecord.builder()
                .template(template)
                .downloadedBy(request != null ? request.getDownloadedBy() : null)
                .build());

        return toDTO(template);
    }

    private void ensureInitialVersion(Template template) {
        if (templateVersionRepository.findByTemplateIdOrderByCreatedAtDesc(template.getId()).isEmpty()) {
            if (template.getCurrentVersion() == null || template.getCurrentVersion().isBlank()) {
                template.setCurrentVersion("1.0");
                template.setFileSize(template.getFileSize() != null ? template.getFileSize() : "未知");
                templateRepository.save(template);
            }
            createVersionRecord(template, template.getCurrentVersion(), "初始版本", template.getName(), template.getCreatedBy());
        }
    }

    private void createVersionRecord(Template template, String version, String description, String snapshotName, Long createdBy) {
        templateVersionRepository.save(TemplateVersion.builder()
                .template(template)
                .version(version)
                .description(description)
                .snapshotName(snapshotName)
                .createdBy(createdBy)
                .build());
    }

    private String incrementVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isBlank()) {
            return "1.0";
        }

        try {
            return BigDecimal.valueOf(Double.parseDouble(currentVersion))
                    .add(BigDecimal.valueOf(0.1))
                    .setScale(1, RoundingMode.HALF_UP)
                    .toPlainString();
        } catch (NumberFormatException ignored) {
            return currentVersion + ".1";
        }
    }

    private String joinOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        return options.stream().collect(Collectors.joining(","));
    }

    private List<String> splitOptions(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private List<String> copyTags(List<String> tags) {
        return tags == null ? List.of() : List.copyOf(tags);
    }

    private TemplateDTO toDTO(Template template) {
        long downloads = templateDownloadRecordRepository.countByTemplateId(template.getId());
        long useCount = templateUseRecordRepository.countByTemplateId(template.getId());
        return TemplateDTO.builder()
                .id(template.getId())
                .name(template.getName())
                .category(template.getCategory())
                .fileUrl(template.getFileUrl())
                .description(template.getDescription())
                .currentVersion(template.getCurrentVersion() != null ? template.getCurrentVersion() : "1.0")
                .fileSize(template.getFileSize() != null ? template.getFileSize() : "未知")
                .downloads(downloads)
                .useCount(useCount)
                .tags(copyTags(template.getTags()))
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
