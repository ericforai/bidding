// Input: HTTP 请求、路径参数、认证上下文和 DTO
// Output: 标准化 API 响应和用例入口
// Pos: Controller/接口适配层
// 维护声明: 仅维护协议适配与参数校验；业务规则下沉到 service.
package com.xiyu.bid.qualification.controller;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.qualification.dto.QualificationDTO;
import com.xiyu.bid.qualification.service.QualificationService;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/knowledge/qualifications")
@RequiredArgsConstructor
public class QualificationController {

    private final QualificationService qualificationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "CREATE", entityType = "Qualification", description = "创建资质")
    public ResponseEntity<ApiResponse<QualificationDTO>> createQualification(@Valid @RequestBody QualificationDTO dto) {
        sanitizeQualificationDTO(dto);
        QualificationDTO created = qualificationService.createQualification(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Qualification created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Qualification", description = "获取所有资质")
    public ResponseEntity<ApiResponse<List<QualificationDTO>>> getAllQualifications() {
        List<QualificationDTO> qualifications = qualificationService.getAllQualifications();
        return ResponseEntity.ok(ApiResponse.success("Qualifications retrieved successfully", qualifications));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Qualification", description = "根据ID获取资质")
    public ResponseEntity<ApiResponse<QualificationDTO>> getQualificationById(@PathVariable Long id) {
        QualificationDTO qualification = qualificationService.getQualificationById(id);
        return ResponseEntity.ok(ApiResponse.success("Qualification retrieved successfully", qualification));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "UPDATE", entityType = "Qualification", description = "更新资质")
    public ResponseEntity<ApiResponse<QualificationDTO>> updateQualification(@PathVariable Long id, @Valid @RequestBody QualificationDTO dto) {
        sanitizeQualificationDTO(dto);
        QualificationDTO updated = qualificationService.updateQualification(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Qualification updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "DELETE", entityType = "Qualification", description = "删除资质")
    public ResponseEntity<Void> deleteQualification(@PathVariable Long id) {
        qualificationService.deleteQualification(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Qualification", description = "根据类型获取资质")
    public ResponseEntity<ApiResponse<List<QualificationDTO>>> getQualificationsByType(@PathVariable com.xiyu.bid.entity.Qualification.Type type) {
        List<QualificationDTO> qualifications = qualificationService.getQualificationsByType(type);
        return ResponseEntity.ok(ApiResponse.success("Qualifications retrieved successfully", qualifications));
    }

    @GetMapping("/valid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Qualification", description = "获取有效资质")
    public ResponseEntity<ApiResponse<List<QualificationDTO>>> getValidQualifications() {
        List<QualificationDTO> qualifications = qualificationService.getValidQualifications();
        return ResponseEntity.ok(ApiResponse.success("Valid qualifications retrieved successfully", qualifications));
    }

    private void sanitizeQualificationDTO(QualificationDTO dto) {
        if (dto.getName() != null) dto.setName(InputSanitizer.sanitizeString(dto.getName(), 200));
        if (dto.getFileUrl() != null) dto.setFileUrl(InputSanitizer.sanitizeString(dto.getFileUrl(), 500));
    }
}
