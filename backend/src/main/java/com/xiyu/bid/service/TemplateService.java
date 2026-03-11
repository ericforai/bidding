// Input: Repository, 相关依赖
// Output: 业务服务、数据操作
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.service;

import com.xiyu.bid.dto.TemplateDTO;
import com.xiyu.bid.entity.Template;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .tags(dto.getTags())
                .createdBy(dto.getCreatedBy())
                .build();

        Template saved = templateRepository.save(template);
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

        // 使用Builder模式创建新对象而非直接修改
        Template updated = Template.builder()
                .id(existing.getId())
                .name(dto.getName() != null ? dto.getName() : existing.getName())
                .category(dto.getCategory() != null ? dto.getCategory() : existing.getCategory())
                .fileUrl(dto.getFileUrl() != null ? dto.getFileUrl() : existing.getFileUrl())
                .tags(dto.getTags() != null ? dto.getTags() : existing.getTags())
                .createdBy(existing.getCreatedBy())
                .createdAt(existing.getCreatedAt())
                .updatedAt(existing.getUpdatedAt())
                .build();

        updated = templateRepository.save(updated);
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

    private TemplateDTO toDTO(Template template) {
        return TemplateDTO.builder()
                .id(template.getId())
                .name(template.getName())
                .category(template.getCategory())
                .fileUrl(template.getFileUrl())
                .tags(template.getTags())
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
