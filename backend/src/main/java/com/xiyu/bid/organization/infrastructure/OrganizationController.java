package com.xiyu.bid.organization.infrastructure;

import com.xiyu.bid.organization.application.FullInitService;
import com.xiyu.bid.organization.application.ReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/xiyu/org-events")
public class OrganizationController {

    private final FullInitService fullInitService;
    private final ReconciliationService reconciliationService;

    public OrganizationController(FullInitService fullInitService,
                                  ReconciliationService reconciliationService) {
        this.fullInitService = fullInitService;
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> triggerFullInit() {
        FullInitService.InitResult result = fullInitService.executeFullInit();
        return ResponseEntity.ok(Map.of(
                "result", "ok",
                "departmentsImported", result.departmentsImported(),
                "usersImported", result.usersImported(),
                "skipped", result.skipped()));
    }

    @PostMapping("/reconcile")
    public ResponseEntity<Map<String, Object>> triggerReconciliation() {
        ReconciliationService.ReconciliationReport report = reconciliationService.reconcileDepartments();
        return ResponseEntity.ok(Map.of(
                "result", "ok",
                "totalDiffs", report.totalDiffs(),
                "alertTriggered", report.alertTriggered()));
    }
}
