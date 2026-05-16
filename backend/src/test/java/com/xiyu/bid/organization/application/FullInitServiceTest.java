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
class FullInitServiceTest {

    @Mock
    private XiyuOrganizationApiClient apiClient;

    private OrganizationSyncProperties properties;
    private FullInitService service;

    @BeforeEach
    void setUp() {
        properties = new OrganizationSyncProperties();
        properties.setFullInitPageSize(2);
        service = new FullInitService(apiClient, properties);
    }

    @Test
    void shouldReturnZeroWhenApiReturnsEmpty() {
        when(apiClient.fetchAllDepartments(anyInt(), anyInt())).thenReturn(List.of());
        when(apiClient.fetchAllUsers(anyInt(), anyInt())).thenReturn(List.of());

        FullInitService.InitResult result = service.executeFullInit();

        assertThat(result.departmentsImported()).isZero();
        assertThat(result.usersImported()).isZero();
        assertThat(result.skipped()).isZero();
    }

    @Test
    void shouldCountImportedEntities() {
        var dept = new XiyuOrganizationApiClient.DepartmentLookupResult(
                "D001", "Engineering", null, null, "ACTIVE", true);
        var user = new XiyuOrganizationApiClient.UserLookupResult(
                "U001", "Alice", null, null, "D001", null, "ACTIVE", true);

        when(apiClient.fetchAllDepartments(0, 2)).thenReturn(List.of(dept));
        when(apiClient.fetchAllDepartments(1, 2)).thenReturn(List.of());
        when(apiClient.fetchAllUsers(0, 2)).thenReturn(List.of(user));
        when(apiClient.fetchAllUsers(1, 2)).thenReturn(List.of());

        FullInitService.InitResult result = service.executeFullInit();

        assertThat(result.departmentsImported()).isEqualTo(1);
        assertThat(result.usersImported()).isEqualTo(1);
    }

    @Test
    void shouldPaginateThroughMultiplePages() {
        var dept1 = new XiyuOrganizationApiClient.DepartmentLookupResult(
                "D001", "Eng", null, null, "ACTIVE", true);
        var dept2 = new XiyuOrganizationApiClient.DepartmentLookupResult(
                "D002", "Sales", null, null, "ACTIVE", true);
        var dept3 = new XiyuOrganizationApiClient.DepartmentLookupResult(
                "D003", "HR", null, null, "ACTIVE", true);

        when(apiClient.fetchAllDepartments(0, 2)).thenReturn(List.of(dept1, dept2));
        when(apiClient.fetchAllDepartments(1, 2)).thenReturn(List.of(dept3));
        when(apiClient.fetchAllDepartments(2, 2)).thenReturn(List.of());
        when(apiClient.fetchAllUsers(anyInt(), anyInt())).thenReturn(List.of());

        FullInitService.InitResult result = service.executeFullInit();

        assertThat(result.departmentsImported()).isEqualTo(3);
    }
}
