package com.xiyu.bid.controller;

import com.xiyu.bid.annotation.Auditable;
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
    }
}
