package com.xiyu.bid.organization.application;

import com.xiyu.bid.organization.config.OrganizationSyncProperties;
import com.xiyu.bid.organization.infrastructure.XiyuOrganizationApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private XiyuOrganizationApiClient apiClient;

    private OrganizationSyncProperties properties;
    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        properties = new OrganizationSyncProperties();
        properties.setReconciliationMaxDiffThreshold(5);
        service = new ReconciliationService(apiClient, properties);
    }

    @Test
    void shouldReturnZeroDiffsWhenApiReturnsEmpty() {
        when(apiClient.fetchAllDepartments(anyInt(), anyInt())).thenReturn(List.of());

        ReconciliationService.ReconciliationReport report = service.reconcileDepartments();

        assertThat(report.totalDiffs()).isZero();
        assertThat(report.alertTriggered()).isFalse();
    }

    @Test
    void shouldDetectMissingLocalEntities() {
        var missing = new XiyuOrganizationApiClient.DepartmentLookupResult(
                "D001", "Eng", null, null, "ACTIVE", false);
        when(apiClient.fetchAllDepartments(0, 500)).thenReturn(List.of(missing));
        when(apiClient.fetchAllDepartments(1, 500)).thenReturn(List.of());

        ReconciliationService.ReconciliationReport report = service.reconcileDepartments();

        assertThat(report.totalDiffs()).isEqualTo(1);
        assertThat(report.diffs()).hasSize(1);
        assertThat(report.diffs().get(0).entityId()).isEqualTo("D001");
        assertThat(report.diffs().get(0).diffType().name()).contains("MISSING");
    }

    @Test
    void shouldTriggerAlertWhenDiffsExceedThreshold() {
        properties.setReconciliationMaxDiffThreshold(2);

        when(apiClient.fetchAllDepartments(0, 500)).thenReturn(List.of(
                new XiyuOrganizationApiClient.DepartmentLookupResult("D01", "A", null, null, "ACTIVE", false),
                new XiyuOrganizationApiClient.DepartmentLookupResult("D02", "B", null, null, "ACTIVE", false),
                new XiyuOrganizationApiClient.DepartmentLookupResult("D03", "C", null, null, "ACTIVE", false)));
        when(apiClient.fetchAllDepartments(1, 500)).thenReturn(List.of());

        ReconciliationService.ReconciliationReport report = service.reconcileDepartments();

        assertThat(report.totalDiffs()).isEqualTo(3);
        assertThat(report.alertTriggered()).isTrue();
    }

    @Test
    void shouldNotAlertWhenDiffsWithinThreshold() {
        properties.setReconciliationMaxDiffThreshold(10);

        when(apiClient.fetchAllDepartments(0, 500)).thenReturn(List.of(
                new XiyuOrganizationApiClient.DepartmentLookupResult("D01", "A", null, null, "ACTIVE", false)));
        when(apiClient.fetchAllDepartments(1, 500)).thenReturn(List.of());

        ReconciliationService.ReconciliationReport report = service.reconcileDepartments();

        assertThat(report.alertTriggered()).isFalse();
    }
}
