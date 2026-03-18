// Input: HTTP 请求、路径参数、认证上下文和 DTO
// Output: 标准化 API 响应和用例入口
// Pos: Controller/接口适配层
// 维护声明: 仅维护协议适配与参数校验；业务规则下沉到 service.
package com.xiyu.bid.controller;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.TemplateCopyRequest;
import com.xiyu.bid.dto.TemplateDownloadRecordRequest;
import com.xiyu.bid.dto.TemplateDownloadRecordDTO;
import com.xiyu.bid.dto.TemplateDTO;
import com.xiyu.bid.dto.TemplateUseRecordDTO;
import com.xiyu.bid.dto.TemplateUseRecordRequest;
import com.xiyu.bid.dto.TemplateVersionDTO;
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

    @PostMapping("/{id}/copy")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "CREATE", entityType = "Template", description = "复制模板")
    public ResponseEntity<ApiResponse<TemplateDTO>> copyTemplate(
            @PathVariable Long id,
            @Valid @RequestBody TemplateCopyRequest request) {
        request.setName(InputSanitizer.sanitizeString(request.getName(), 200));
        TemplateDTO copied = templateService.copyTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.success("Template copied successfully", copied));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Template", description = "获取模板版本历史")
    public ResponseEntity<ApiResponse<List<TemplateVersionDTO>>> getTemplateVersions(@PathVariable Long id) {
        List<TemplateVersionDTO> versions = templateService.getTemplateVersions(id);
        return ResponseEntity.ok(ApiResponse.success("Template versions retrieved successfully", versions));
    }

    @PostMapping("/{id}/use-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "CREATE", entityType = "Template", description = "记录模板使用")
    public ResponseEntity<ApiResponse<TemplateUseRecordDTO>> createTemplateUseRecord(
            @PathVariable Long id,
            @Valid @RequestBody TemplateUseRecordRequest request) {
        request.setDocumentName(InputSanitizer.sanitizeString(request.getDocumentName(), 255));
        request.setDocType(InputSanitizer.sanitizeString(request.getDocType(), 100));
        TemplateUseRecordDTO created = templateService.createTemplateUseRecord(id, request);
        return ResponseEntity.ok(ApiResponse.success("Template use recorded successfully", created));
    }

    @PostMapping("/{id}/downloads")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "CREATE", entityType = "Template", description = "记录模板下载")
    public ResponseEntity<ApiResponse<TemplateDTO>> createTemplateDownloadRecord(
            @PathVariable Long id,
            @RequestBody(required = false) TemplateDownloadRecordRequest request) {
        TemplateDTO updated = templateService.createTemplateDownloadRecord(id, request);
        return ResponseEntity.ok(ApiResponse.success("Template download recorded successfully", updated));
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
        if (dto.getDescription() != null) {
            dto.setDescription(InputSanitizer.sanitizeString(dto.getDescription(), 2000));
        }
        if (dto.getFileSize() != null) {
            dto.setFileSize(InputSanitizer.sanitizeString(dto.getFileSize(), 100));
        }
        if (dto.getTags() != null) {
            dto.setTags(dto.getTags().stream()
                    .map(tag -> InputSanitizer.sanitizeString(tag, 50))
                    .toList());
        }
    }
}
