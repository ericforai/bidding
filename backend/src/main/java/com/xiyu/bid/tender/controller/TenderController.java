// Input: HTTP 请求、路径参数、认证上下文和 DTO
// Output: 标准化 API 响应和用例入口
// Pos: Controller/接口适配层
// 维护声明: 仅维护协议适配与参数校验；业务规则下沉到 service.
package com.xiyu.bid.tender.controller;

import com.xiyu.bid.ai.dto.TenderAiAnalysisDTO;
import com.xiyu.bid.ai.service.AiDeepCapabilityService;
import com.xiyu.bid.demo.service.DemoDataProvider;
import com.xiyu.bid.demo.service.DemoFusionService;
import com.xiyu.bid.demo.service.DemoModeService;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.tender.dto.TenderRequest;
import com.xiyu.bid.tender.dto.TenderDTO;
import com.xiyu.bid.tender.service.TenderCommandService;
import com.xiyu.bid.tender.service.TenderMapper;
import com.xiyu.bid.tender.service.TenderQueryService;
import com.xiyu.bid.tender.service.TenderSearchCriteria;
import com.xiyu.bid.util.InputSanitizer;
import com.xiyu.bid.annotation.DataScope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tenders")
@RequiredArgsConstructor
@Slf4j
public class TenderController {

    private final TenderQueryService tenderQueryService;
    private final TenderCommandService tenderCommandService;
    private final TenderMapper tenderMapper;
    private final AiDeepCapabilityService aiDeepCapabilityService;
    private final DemoModeService demoModeService;
    private final DemoDataProvider demoDataProvider;
    private final DemoFusionService demoFusionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @DataScope
    public ResponseEntity<ApiResponse<List<TenderDTO>>> getAllTenders(@ModelAttribute TenderSearchCriteria criteria) {
        log.info("GET /api/tenders - Searching tenders");
        sanitizeTenderSearchCriteria(criteria);
        List<TenderDTO> tenders = tenderQueryService.searchTenders(criteria);
        if (demoModeService.isEnabled()) {
            tenders = demoFusionService.mergeByKey(tenders, demoDataProvider.getDemoTenders(), TenderDTO::getId);
        }
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved tenders", tenders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TenderDTO>> getTenderById(@PathVariable Long id) {
        log.info("GET /api/tenders/{} - Fetching tender", id);
        if (isDemoEntityId(id)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Successfully retrieved tender",
                    demoDataProvider.findDemoTenderById(id).orElseThrow(() -> new com.xiyu.bid.exception.ResourceNotFoundException("Tender", id.toString()))
            ));
        }
        TenderDTO tender = tenderQueryService.getTenderById(id);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved tender", tender));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TenderDTO>> createTender(@Valid @RequestBody TenderRequest tenderRequest) {
        log.info("POST /api/tenders - Creating new tender: {}", tenderRequest.getTitle());
        sanitizeTenderRequest(tenderRequest);
        TenderDTO tenderDTO = tenderMapper.toDTO(tenderRequest);
        TenderDTO createdTender = tenderCommandService.createTender(tenderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tender created successfully", createdTender));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TenderDTO>> updateTender(@PathVariable Long id, @Valid @RequestBody TenderRequest tenderRequest) {
        log.info("PUT /api/tenders/{} - Updating tender", id);
        rejectDemoMutation(id);
        sanitizeTenderRequest(tenderRequest);
        TenderDTO tenderDTO = tenderMapper.toDTO(tenderRequest);
        TenderDTO updatedTender = tenderCommandService.updateTender(id, tenderDTO);
        return ResponseEntity.ok(ApiResponse.success("Tender updated successfully", updatedTender));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTender(@PathVariable Long id) {
        log.info("DELETE /api/tenders/{} - Deleting tender", id);
        rejectDemoMutation(id);
        tenderCommandService.deleteTender(id);
        return ResponseEntity.ok(ApiResponse.success("Tender deleted successfully", null));
    }

    @PostMapping("/{id}/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TenderDTO>> analyzeTender(@PathVariable Long id) {
        log.info("POST /api/tenders/{}/analyze - Analyzing tender", id);
        rejectDemoMutation(id);
        TenderDTO analyzedTender = tenderCommandService.analyzeTender(id);
        return ResponseEntity.ok(ApiResponse.success("Tender analyzed successfully", analyzedTender));
    }

    @GetMapping("/{id}/ai-analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TenderAiAnalysisDTO>> getTenderAiAnalysis(@PathVariable Long id) {
        log.info("GET /api/tenders/{}/ai-analysis - Fetching tender AI analysis", id);
        if (isDemoEntityId(id)) {
            TenderDTO tender = demoDataProvider.findDemoTenderById(id)
                    .orElseThrow(() -> new com.xiyu.bid.exception.ResourceNotFoundException("Tender", id.toString()));
            TenderAiAnalysisDTO dto = TenderAiAnalysisDTO.builder()
                    .tenderId(tender.getId())
                    .winScore(tender.getAiScore() == null ? 80 : tender.getAiScore())
                    .suggestion("Demo 标讯分析：重点关注资质响应、交付周期和付款条件。")
                    .dimensionScores(List.of())
                    .risks(List.of())
                    .autoTasks(List.of())
                    .build();
            return ResponseEntity.ok(ApiResponse.success("Tender AI analysis retrieved successfully", dto));
        }
        Optional<TenderAiAnalysisDTO> analysis = aiDeepCapabilityService.getLatestTenderAnalysis(id);
        if (analysis.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "Tender AI analysis not found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Tender AI analysis retrieved successfully", analysis.get()));
    }

    @PostMapping("/{id}/ai-analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TenderAiAnalysisDTO>> createTenderAiAnalysis(@PathVariable Long id) {
        log.info("POST /api/tenders/{}/ai-analysis - Generating tender AI analysis", id);
        rejectDemoMutation(id);
        TenderAiAnalysisDTO analysis = aiDeepCapabilityService.analyzeTender(id, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tender AI analysis generated successfully", analysis));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TenderDTO>>> getTendersByStatus(@PathVariable com.xiyu.bid.entity.Tender.Status status) {
        log.info("GET /api/tenders/status/{} - Fetching tenders by status", status);
        List<TenderDTO> tenders = tenderQueryService.getTendersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved tenders", tenders));
    }

    @GetMapping("/source/{source}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TenderDTO>>> getTendersBySource(@PathVariable String source) {
        log.info("GET /api/tenders/source/{} - Fetching tenders by source", source);
        String sanitizedSource = InputSanitizer.sanitizeString(source, 100);
        List<TenderDTO> tenders = tenderQueryService.getTendersBySource(sanitizedSource);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved tenders", tenders));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<com.xiyu.bid.entity.Tender.Status, Long>>> getStatistics() {
        log.info("GET /api/tenders/statistics - Fetching tender statistics");
        Map<com.xiyu.bid.entity.Tender.Status, Long> statistics = tenderQueryService.getTenderStatistics();
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved statistics", statistics));
    }

    private void sanitizeTenderRequest(TenderRequest request) {
        if (request.getTitle() != null) request.setTitle(InputSanitizer.sanitizeString(request.getTitle(), 500));
        if (request.getSource() != null) request.setSource(InputSanitizer.sanitizeString(request.getSource(), 200));
        if (request.getRegion() != null) request.setRegion(InputSanitizer.sanitizeString(request.getRegion(), 100));
        if (request.getIndustry() != null) request.setIndustry(InputSanitizer.sanitizeString(request.getIndustry(), 100));
        if (request.getPurchaserName() != null) request.setPurchaserName(InputSanitizer.sanitizeString(request.getPurchaserName(), 255));
        if (request.getPurchaserHash() != null) request.setPurchaserHash(InputSanitizer.sanitizeString(request.getPurchaserHash(), 64));
        if (request.getContactName() != null) request.setContactName(InputSanitizer.sanitizeString(request.getContactName(), 100));
        if (request.getContactPhone() != null) request.setContactPhone(InputSanitizer.sanitizeString(request.getContactPhone(), 50));
        if (request.getDescription() != null) request.setDescription(InputSanitizer.sanitizeString(request.getDescription(), 5000));
        if (request.getTags() != null) {
            request.setTags(request.getTags().stream()
                    .map(tag -> InputSanitizer.sanitizeString(tag, 100))
                    .filter(tag -> !tag.isBlank())
                    .toList());
        }
    }

    private void sanitizeTenderSearchCriteria(TenderSearchCriteria criteria) {
        if (criteria == null) return;
        if (criteria.getKeyword() != null) criteria.setKeyword(InputSanitizer.sanitizeString(criteria.getKeyword(), 200));
        if (criteria.getSource() != null) criteria.setSource(InputSanitizer.sanitizeString(criteria.getSource(), 200));
        if (criteria.getRegion() != null) criteria.setRegion(InputSanitizer.sanitizeString(criteria.getRegion(), 100));
        if (criteria.getIndustry() != null) criteria.setIndustry(InputSanitizer.sanitizeString(criteria.getIndustry(), 100));
        if (criteria.getPurchaserName() != null) criteria.setPurchaserName(InputSanitizer.sanitizeString(criteria.getPurchaserName(), 255));
        if (criteria.getPurchaserHash() != null) criteria.setPurchaserHash(InputSanitizer.sanitizeString(criteria.getPurchaserHash(), 64));
    }

    private boolean isDemoEntityId(Long id) {
        return demoModeService.isEnabled() && id != null && id < 0;
    }

    private void rejectDemoMutation(Long id) {
        if (isDemoEntityId(id)) {
            throw new IllegalArgumentException("Demo records are read-only in e2e mode");
        }
    }
}
