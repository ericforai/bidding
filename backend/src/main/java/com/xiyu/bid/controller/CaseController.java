package com.xiyu.bid.controller;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.casework.dto.CaseReferenceRecordCreateRequest;
import com.xiyu.bid.casework.dto.CaseReferenceRecordDTO;
import com.xiyu.bid.casework.dto.CaseShareRecordCreateRequest;
import com.xiyu.bid.casework.dto.CaseShareRecordDTO;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.CaseDTO;
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.service.CaseService;
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
 * 案例管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "CREATE", entityType = "Case", description = "创建案例")
    public ResponseEntity<ApiResponse<CaseDTO>> createCase(
            @Valid @RequestBody CaseDTO dto) {
        // Sanitize user input
        sanitizeCaseDTO(dto);
        CaseDTO created = caseService.createCase(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Case", description = "获取所有案例")
    public ResponseEntity<ApiResponse<List<CaseDTO>>> getAllCases() {
        List<CaseDTO> cases = caseService.getAllCases();
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved successfully", cases));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Case", description = "根据ID获取案例")
    public ResponseEntity<ApiResponse<CaseDTO>> getCaseById(@PathVariable Long id) {
        CaseDTO caseStudy = caseService.getCaseById(id);
        return ResponseEntity.ok(ApiResponse.success("Case retrieved successfully", caseStudy));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "UPDATE", entityType = "Case", description = "更新案例")
    public ResponseEntity<ApiResponse<CaseDTO>> updateCase(
            @PathVariable Long id,
            @Valid @RequestBody CaseDTO dto) {
        // Sanitize user input
        sanitizeCaseDTO(dto);
        CaseDTO updated = caseService.updateCase(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Case updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "DELETE", entityType = "Case", description = "删除案例")
    public ResponseEntity<Void> deleteCase(@PathVariable Long id) {
        caseService.deleteCase(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/industry/{industry}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Case", description = "根据行业获取案例")
    public ResponseEntity<ApiResponse<List<CaseDTO>>> getCasesByIndustry(
            @PathVariable Case.Industry industry) {
        List<CaseDTO> cases = caseService.getCasesByIndustry(industry);
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved successfully", cases));
    }

    @GetMapping("/outcome/{outcome}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Case", description = "根据结果获取案例")
    public ResponseEntity<ApiResponse<List<CaseDTO>>> getCasesByOutcome(
            @PathVariable Case.Outcome outcome) {
        List<CaseDTO> cases = caseService.getCasesByOutcome(outcome);
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved successfully", cases));
    }

    @GetMapping("/{id}/share-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Case", description = "获取案例分享记录")
    public ResponseEntity<ApiResponse<List<CaseShareRecordDTO>>> getCaseShareRecords(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Case share records retrieved successfully",
                caseService.getCaseShareRecords(id)));
    }

    @PostMapping("/{id}/share-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "CREATE", entityType = "Case", description = "创建案例分享记录")
    public ResponseEntity<ApiResponse<CaseShareRecordDTO>> createCaseShareRecord(
            @PathVariable Long id,
            @Valid @RequestBody CaseShareRecordCreateRequest request) {
        request.setCreatedByName(InputSanitizer.sanitizeString(request.getCreatedByName(), 100));
        request.setBaseUrl(InputSanitizer.sanitizeString(request.getBaseUrl(), 500));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case share record created successfully",
                        caseService.createCaseShareRecord(id, request)));
    }

    @GetMapping("/{id}/references")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "READ", entityType = "Case", description = "获取案例引用记录")
    public ResponseEntity<ApiResponse<List<CaseReferenceRecordDTO>>> getCaseReferenceRecords(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Case reference records retrieved successfully",
                caseService.getCaseReferenceRecords(id)));
    }

    @PostMapping("/{id}/references")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "CREATE", entityType = "Case", description = "创建案例引用记录")
    public ResponseEntity<ApiResponse<CaseReferenceRecordDTO>> createCaseReferenceRecord(
            @PathVariable Long id,
            @Valid @RequestBody CaseReferenceRecordCreateRequest request) {
        request.setReferencedByName(InputSanitizer.sanitizeString(request.getReferencedByName(), 100));
        request.setReferenceTarget(InputSanitizer.sanitizeString(request.getReferenceTarget(), 255));
        request.setReferenceContext(InputSanitizer.sanitizeString(request.getReferenceContext(), 1000));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case reference record created successfully",
                        caseService.createCaseReferenceRecord(id, request)));
    }

    /**
     * 清洗案例DTO中的用户输入
     */
    private void sanitizeCaseDTO(CaseDTO dto) {
        if (dto.getTitle() != null) {
            dto.setTitle(InputSanitizer.sanitizeString(dto.getTitle(), 200));
        }
        if (dto.getDescription() != null) {
            dto.setDescription(InputSanitizer.sanitizeString(dto.getDescription(), 2000));
        }
        if (dto.getCustomerName() != null) {
            dto.setCustomerName(InputSanitizer.sanitizeString(dto.getCustomerName(), 255));
        }
        if (dto.getLocationName() != null) {
            dto.setLocationName(InputSanitizer.sanitizeString(dto.getLocationName(), 255));
        }
        if (dto.getProjectPeriod() != null) {
            dto.setProjectPeriod(InputSanitizer.sanitizeString(dto.getProjectPeriod(), 255));
        }
        if (dto.getTags() != null) {
            dto.setTags(dto.getTags().stream().map(tag -> InputSanitizer.sanitizeString(tag, 50)).toList());
        }
        if (dto.getHighlights() != null) {
            dto.setHighlights(dto.getHighlights().stream().map(item -> InputSanitizer.sanitizeString(item, 1000)).toList());
        }
        if (dto.getTechnologies() != null) {
            dto.setTechnologies(dto.getTechnologies().stream().map(item -> InputSanitizer.sanitizeString(item, 255)).toList());
        }
    }
}
