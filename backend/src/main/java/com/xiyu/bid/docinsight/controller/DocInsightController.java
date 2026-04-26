package com.xiyu.bid.docinsight.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.docinsight.application.DocumentAnalysisResult;
import com.xiyu.bid.docinsight.application.DocumentIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/doc-insight")
@RequiredArgsConstructor
public class DocInsightController {

    private final DocumentIntelligenceService docInsightService;

    @PostMapping("/parse")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<DocumentAnalysisResult>> parse(
            @RequestParam("profile") String profileCode,
            @RequestParam("entityId") String entityId,
            @RequestParam("file") MultipartFile file) {
        
        DocumentAnalysisResult result = docInsightService.process(profileCode, entityId, file);
        return ResponseEntity.ok(ApiResponse.success("Document analyzed successfully", result));
    }
}
