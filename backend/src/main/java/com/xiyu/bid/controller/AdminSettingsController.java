package com.xiyu.bid.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.dto.DataScopeConfigResponse;
import com.xiyu.bid.service.DataScopeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final DataScopeConfigService dataScopeConfigService;

    @GetMapping("/data-scope")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DataScopeConfigResponse>> getDataScopeConfig() {
        log.info("GET /api/admin/settings/data-scope - fetching data scope config");
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved data scope config", dataScopeConfigService.getConfig()));
    }

    @PutMapping("/data-scope")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DataScopeConfigResponse>> saveDataScopeConfig(@RequestBody DataScopeConfigResponse request) {
        log.info("PUT /api/admin/settings/data-scope - saving data scope config");
        return ResponseEntity.ok(ApiResponse.success("Data scope config saved successfully", dataScopeConfigService.saveConfig(request)));
    }
}
