package com.xiyu.bid.resources.controller;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.config.PaginationConstants;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.resources.dto.BarAssetCreateRequest;
import com.xiyu.bid.resources.dto.BarAssetUpdateRequest;
import com.xiyu.bid.resources.entity.BarAsset;
import com.xiyu.bid.resources.service.BarAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/resources/bar-assets")
@RequiredArgsConstructor
public class BarAssetController {

    private final BarAssetService barAssetService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "CREATE", entityType = "BarAsset", description = "Create bar asset record")
    public ResponseEntity<ApiResponse<BarAsset>> createBarAsset(@Valid @RequestBody BarAssetCreateRequest request) {
        BarAsset asset = barAssetService.createBarAsset(request);
        return ResponseEntity.ok(ApiResponse.success("Bar asset created successfully", asset));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<BarAsset>> getBarAssetById(@PathVariable Long id) {
        BarAsset asset = barAssetService.getBarAssetById(id);
        return ResponseEntity.ok(ApiResponse.success(asset));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<BarAsset>>> getAllBarAssets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BarAsset> assets = barAssetService.getAllBarAssets(pageable);
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<BarAsset>>> getBarAssetsByType(
            @PathVariable BarAsset.AssetType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<BarAsset> assets = barAssetService.getBarAssetsByType(type, pageable);
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<BarAsset>>> getBarAssetsByStatus(
            @PathVariable BarAsset.AssetStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<BarAsset> assets = barAssetService.getBarAssetsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/value-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<BarAsset>>> getBarAssetsByValueRange(
            @RequestParam BigDecimal minValue,
            @RequestParam BigDecimal maxValue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("value").descending());
        Page<BarAsset> assets = barAssetService.getBarAssetsByValueRange(minValue, maxValue, pageable);
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/acquire-date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<BarAsset>>> getBarAssetsByAcquireDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("acquireDate").descending());
        Page<BarAsset> assets = barAssetService.getBarAssetsByAcquireDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<BarAsset>>> searchBarAssets(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<BarAsset> assets = barAssetService.searchBarAssets(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "UPDATE", entityType = "BarAsset", description = "Update bar asset record")
    public ResponseEntity<ApiResponse<BarAsset>> updateBarAsset(
            @PathVariable Long id,
            @Valid @RequestBody BarAssetUpdateRequest request) {

        BarAsset asset = barAssetService.updateBarAsset(id, request);
        return ResponseEntity.ok(ApiResponse.success("Bar asset updated successfully", asset));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "DELETE", entityType = "BarAsset", description = "Delete bar asset record")
    public ResponseEntity<ApiResponse<Void>> deleteBarAsset(@PathVariable Long id) {
        barAssetService.deleteBarAsset(id);
        return ResponseEntity.ok(ApiResponse.success("Bar asset deleted successfully", null));
    }

    @GetMapping("/total-value")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalAssetValue() {
        BigDecimal total = barAssetService.getTotalAssetValue();
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAssetStatistics() {
        Map<String, Object> statistics = barAssetService.getAssetStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
