package com.xiyu.bid.organization.application;

import com.xiyu.bid.organization.config.OrganizationSyncProperties;
import com.xiyu.bid.organization.domain.ReconciliationPolicy;
import com.xiyu.bid.organization.infrastructure.XiyuOrganizationApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs daily reconciliation between local organization data and the
 * Xiyu source-of-truth API. Differences exceeding the configurable threshold
 * trigger an alert instead of automatic repair.
 */
@Service
public class ReconciliationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationService.class);

    private final XiyuOrganizationApiClient apiClient;
    private final OrganizationSyncProperties properties;

    public ReconciliationService(XiyuOrganizationApiClient apiClient,
                                 OrganizationSyncProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
    }

    public ReconciliationReport reconcileDepartments() {
        List<ReconDiff> diffs = new ArrayList<>();
        int page = 0;

        while (true) {
            var remoteDepts = apiClient.fetchAllDepartments(page, properties.getFullInitPageSize());
            if (remoteDepts.isEmpty()) {
                break;
            }
            for (var remote : remoteDepts) {
                if (!remote.found()) {
                    diffs.add(new ReconDiff(remote.deptId(), "DEPARTMENT",
                            ReconciliationPolicy.DiffType.MISSING_LOCALLY, "Not found locally"));
                }
            }
            page++;
        }

        boolean shouldAlert = diffs.size() > properties.getReconciliationMaxDiffThreshold();
        ReconciliationReport report = new ReconciliationReport(diffs.size(), diffs, shouldAlert);
        LOG.info("Reconciliation complete: {} diffs, alert={}", diffs.size(), shouldAlert);
        return report;
    }

    public record ReconDiff(String entityId, String entityType,
                            ReconciliationPolicy.DiffType diffType, String detail) {
    }

    public record ReconciliationReport(int totalDiffs, List<ReconDiff> diffs, boolean alertTriggered) {
    }
}
