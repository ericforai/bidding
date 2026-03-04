package com.xiyu.bid.controller;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.TemplateDTO;
import com.xiyu.bid.entity.Template;
import com.xiyu.bid.service.TemplateService;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "CREATE", entityType = "Template", description = "创建模板")
    public ResponseEntity<ApiResponse<TemplateDTO>> createTemplate(
            @Valid @RequestBody TemplateDTO dto) {
        // Sanitize user input
        sanitizeTemplateDTO(dto);
        TemplateDTO created = templateService.createTemplate(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Template", description = "获取所有模板")
    public ResponseEntity<ApiResponse<List<TemplateDTO>>> getAllTemplates() {
        List<TemplateDTO> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved successfully", templates));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Template", description = "根据ID获取模板")
    public ResponseEntity<ApiResponse<TemplateDTO>> getTemplateById(@PathVariable Long id) {
        TemplateDTO template = templateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.success("Template retrieved successfully", template));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "UPDATE", entityType = "Template", description = "更新模板")
    public ResponseEntity<ApiResponse<TemplateDTO>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody TemplateDTO dto) {
        // Sanitize user input
        sanitizeTemplateDTO(dto);
        TemplateDTO updated = templateService.updateTemplate(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Template updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "DELETE", entityType = "Template", description = "删除模板")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Template", description = "根据类别获取模板")
    public ResponseEntity<ApiResponse<List<TemplateDTO>>> getTemplatesByCategory(
            @PathVariable Template.Category category) {
        List<TemplateDTO> templates = templateService.getTemplatesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved successfully", templates));
    }

    @GetMapping("/creator/{createdBy}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Template", description = "根据创建者获取模板")
    public ResponseEntity<ApiResponse<List<TemplateDTO>>> getTemplatesByCreator(
            @PathVariable Long createdBy) {
        List<TemplateDTO> templates = templateService.getTemplatesByCreatedBy(createdBy);
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved successfully", templates));
    }

    /**
     * 清洗模板DTO中的用户输入
     */
    private void sanitizeTemplateDTO(TemplateDTO dto) {
        if (dto.getName() != null) {
            dto.setName(InputSanitizer.sanitizeString(dto.getName(), 200));
        }
        if (dto.getFileUrl() != null) {
            dto.setFileUrl(InputSanitizer.sanitizeString(dto.getFileUrl(), 500));
        }
        if (dto.getTags() != null) {
            dto.setTags(dto.getTags().stream()
                    .map(tag -> InputSanitizer.sanitizeString(tag, 50))
                    .toList());
        }
    }
}
