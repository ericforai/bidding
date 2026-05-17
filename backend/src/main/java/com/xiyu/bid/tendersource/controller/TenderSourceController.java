package com.xiyu.bid.tendersource.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.tendersource.application.TenderSourceConnectionTestService;
import com.xiyu.bid.tendersource.dto.TenderSourceTestRequest;
import com.xiyu.bid.tendersource.dto.TenderSourceTestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标讯源配置控制器。
 */
@RestController
@RequestMapping("/api/tender-sources")
@RequiredArgsConstructor
@Slf4j
public class TenderSourceController {

    /**
     * 连接测试服务
     */
    private final TenderSourceConnectionTestService tenderSourceConnectionTestService;

    /**
     * 测试标讯源连接
     */
    @PostMapping("/test-connection")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TenderSourceTestResponse>> testConnection(
            @Valid @RequestBody TenderSourceTestRequest request) {
        log.info("POST /api/tender-sources/test-connection - Testing connection for platform: {}",
                request.getPlatform());
        TenderSourceTestResponse response = tenderSourceConnectionTestService.testConnection(request);
        return ResponseEntity.ok(ApiResponse.success("Connection test completed", response));
    }
}
