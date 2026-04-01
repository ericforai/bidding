// Input: HTTP 请求、路径参数、认证上下文和 DTO
// Output: 标准化 API 响应和用例入口
// Pos: Controller/接口适配层
// 维护声明: 仅维护协议适配与参数校验；业务规则下沉到 service.
package com.xiyu.bid.tender.controller;

import com.xiyu.bid.ai.dto.TenderAiAnalysisDTO;
import com.xiyu.bid.ai.service.AiDeepCapabilityService;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.tender.dto.TenderRequest;
import com.xiyu.bid.tender.dto.TenderDTO;
import com.xiyu.bid.tender.service.TenderService;
import com.xiyu.bid.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tenders")
@RequiredArgsConstructor
@Slf4j
public class TenderController {

    private final TenderService tenderService;
    private final AiDeepCapabilityService aiDeepCapabilityService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TenderDTO>>> getAllTenders() {
        log.info("GET /api/tenders - Fetching all tenders");
        List<TenderDTO> tenders = tenderService.getAllTenders();
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved tenders", tenders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TenderDTO>> getTenderById(@PathVariable Long id) {
        log.info("GET /api/tenders/{} - Fetching tender", id);
        TenderDTO tender = tenderService.getTenderById(id);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved tender", tender));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TenderDTO>> createTender(@Valid @RequestBody TenderRequest tenderRequest) {
        log.info("POST /api/tenders - Creating new tender: {}", tenderRequest.getTitle());
        sanitizeTenderRequest(tenderRequest);
        TenderDTO tenderDTO = convertRequestToDTO(tenderRequest);
        TenderDTO createdTender = tenderService.createTender(tenderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tender created successfully", createdTender));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TenderDTO>> updateTender(@PathVariable Long id, @Valid @RequestBody TenderRequest tenderRequest) {
        log.info("PUT /api/tenders/{} - Updating tender", id);
        sanitizeTenderRequest(tenderRequest);
        TenderDTO tenderDTO = convertRequestToDTO(tenderRequest);
        TenderDTO updatedTender = tenderService.updateTender(id, tenderDTO);
        return ResponseEntity.ok(ApiResponse.success("Tender updated successfully", updatedTender));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTender(@PathVariable Long id) {
        log.info("DELETE /api/tenders/{} - Deleting tender", id);
        tenderService.deleteTender(id);
        return ResponseEntity.ok(ApiResponse.success("Tender deleted successfully", null));
    }

    @PostMapping("/{id}/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TenderDTO>> analyzeTender(@PathVariable Long id) {
        log.info("POST /api/tenders/{}/analyze - Analyzing tender", id);
        TenderDTO analyzedTender = tenderService.analyzeTender(id);
        return ResponseEntity.ok(ApiResponse.success("Tender analyzed successfully", analyzedTender));
    }

    @GetMapping("/{id}/ai-analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TenderAiAnalysisDTO>> getTenderAiAnalysis(@PathVariable Long id) {
        log.info("GET /api/tenders/{}/ai-analysis - Fetching tender AI analysis", id);
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
        TenderAiAnalysisDTO analysis = aiDeepCapabilityService.analyzeTender(id, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tender AI analysis generated successfully", analysis));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TenderDTO>>> getTendersByStatus(@PathVariable com.xiyu.bid.entity.Tender.Status status) {
        log.info("GET /api/tenders/status/{} - Fetching tenders by status", status);
        List<TenderDTO> tenders = tenderService.getTendersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved tenders", tenders));
    }

    @GetMapping("/source/{source}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TenderDTO>>> getTendersBySource(@PathVariable String source) {
        log.info("GET /api/tenders/source/{} - Fetching tenders by source", source);
        String sanitizedSource = InputSanitizer.sanitizeString(source, 100);
        List<TenderDTO> tenders = tenderService.getTendersBySource(sanitizedSource);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved tenders", tenders));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<com.xiyu.bid.entity.Tender.Status, Long>>> getStatistics() {
        log.info("GET /api/tenders/statistics - Fetching tender statistics");
        Map<com.xiyu.bid.entity.Tender.Status, Long> statistics = tenderService.getTenderStatistics();
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved statistics", statistics));
    }

    private TenderDTO convertRequestToDTO(TenderRequest request) {
        return TenderDTO.builder().title(request.getTitle()).source(request.getSource()).budget(request.getBudget())
                .deadline(request.getDeadline()).status(request.getStatus()).aiScore(request.getAiScore()).riskLevel(request.getRiskLevel()).build();
    }

    private void sanitizeTenderRequest(TenderRequest request) {
        if (request.getTitle() != null) request.setTitle(InputSanitizer.sanitizeString(request.getTitle(), 200));
        if (request.getSource() != null) request.setSource(InputSanitizer.sanitizeString(request.getSource(), 100));
    }
}
