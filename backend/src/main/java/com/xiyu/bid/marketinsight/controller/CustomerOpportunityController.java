// Input: CustomerOpportunityService
// Output: Customer Opportunity REST API endpoints
// Pos: Controller/控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.marketinsight.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.marketinsight.dto.CustomerInsightDTO;
import com.xiyu.bid.marketinsight.dto.CustomerPredictionDTO;
import com.xiyu.bid.marketinsight.dto.CustomerPurchaseDTO;
import com.xiyu.bid.marketinsight.service.CustomerOpportunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 客户商机控制器
 * 处理客户商机洞察、采购记录、预测和状态转换的HTTP请求
 */
@RestController
@RequestMapping("/api/customer-opportunities")
@RequiredArgsConstructor
@Slf4j
public class CustomerOpportunityController {

    private final CustomerOpportunityService customerOpportunityService;

    /**
     * 获取所有客户洞察列表
     */
    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<CustomerInsightDTO>>> getCustomerInsights() {
        log.info("GET /api/customer-opportunities/insights - Fetching customer insights");
        List<CustomerInsightDTO> insights = customerOpportunityService.getCustomerInsights();
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved customer insights", insights));
    }

    /**
     * 获取指定采购人的采购记录
     */
    @GetMapping("/{purchaserHash}/purchases")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<CustomerPurchaseDTO>>> getCustomerPurchases(
            @PathVariable String purchaserHash) {
        log.info("GET /api/customer-opportunities/{}/purchases - Fetching customer purchases", purchaserHash);
        List<CustomerPurchaseDTO> purchases = customerOpportunityService.getCustomerPurchases(purchaserHash);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved customer purchases", purchases));
    }

    /**
     * 获取指定采购人的商机预测
     */
    @GetMapping("/{purchaserHash}/predictions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<CustomerPredictionDTO>>> getCustomerPredictions(
            @PathVariable String purchaserHash) {
        log.info("GET /api/customer-opportunities/{}/predictions - Fetching customer predictions", purchaserHash);
        List<CustomerPredictionDTO> predictions = customerOpportunityService.getCustomerPredictions(purchaserHash);
        return ResponseEntity.ok(ApiResponse.success("Successfully retrieved customer predictions", predictions));
    }

    /**
     * 重新计算客户商机预测（仅管理员/经理）
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> refreshInsights() {
        log.info("POST /api/customer-opportunities/refresh - Refreshing customer insights");
        customerOpportunityService.refreshInsights();
        return ResponseEntity.ok(ApiResponse.success("Customer insights refreshed successfully", null));
    }

    /**
     * 转换预测状态（仅管理员/经理）
     */
    @PutMapping("/predictions/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CustomerPredictionDTO>> transitionPrediction(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        log.info("PUT /api/customer-opportunities/predictions/{}/status - Transitioning to {}", id, status);
        CustomerPredictionDTO prediction = customerOpportunityService.transitionPrediction(id, status);
        return ResponseEntity.ok(ApiResponse.success("Prediction status updated successfully", prediction));
    }

    /**
     * 将预测转化为项目（仅管理员/经理）
     */
    @PutMapping("/predictions/{id}/convert")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CustomerPredictionDTO>> convertPrediction(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long projectId = body.get("projectId");
        log.info("PUT /api/customer-opportunities/predictions/{}/convert - Converting to project {}", id, projectId);
        CustomerPredictionDTO prediction = customerOpportunityService.convertPrediction(id, projectId);
        return ResponseEntity.ok(ApiResponse.success("Prediction converted successfully", prediction));
    }
}
