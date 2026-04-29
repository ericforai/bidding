package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.domain.OrganizationEventStatus;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookRequest;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationDepartmentEntity;
import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationEventLogEntity;
import com.xiyu.bid.integration.organization.infrastructure.persistence.repository.OrganizationDepartmentRepository;
import com.xiyu.bid.integration.organization.infrastructure.persistence.repository.OrganizationEventLogRepository;
import com.xiyu.bid.repository.RoleProfileRepository;
import com.xiyu.bid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationEventAppService — edge orchestration")
class OrganizationEventAppServiceEdgeTest {

    @Mock
    private OrganizationEventLogRepository eventLogRepository;
    @Mock
    private OrganizationDepartmentRepository departmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleProfileRepository roleProfileRepository;

    private OrganizationEventAppService service;

    @BeforeEach
    void setUp() {
        OrganizationIntegrationProperties properties = new OrganizationIntegrationProperties();
        properties.setAllowedSourceApps(java.util.List.of("customer-org"));
        service = new OrganizationEventAppService(
                eventLogRepository,
                departmentRepository,
                userRepository,
                roleProfileRepository,
                properties
        );
    }

    @Test
    @DisplayName("department code is normalized before lookup and save")
    void receiveEvent_departmentUpsert_normalizesCodeBeforeLookup() {
        OrganizationDepartmentEntity existing = new OrganizationDepartmentEntity();
        existing.setDepartmentCode("sales");
        existing.setDepartmentName("Old Sales");
        when(eventLogRepository.existsByEventKey(any())).thenReturn(false);
        when(eventLogRepository.saveAndFlush(any(OrganizationEventLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(departmentRepository.findById("sales")).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(OrganizationDepartmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest(
                "org.department.upsert",
                "{\"departmentCode\":\"SALES\",\"departmentName\":\"销售部\"}"
        );

        OrganizationEventWebhookResponse response = service.receiveWebhook(request, "trace-1", "customer-org");

        assertThat(response.code()).isEqualTo("200");
        ArgumentCaptor<OrganizationDepartmentEntity> savedDepartment = ArgumentCaptor.forClass(OrganizationDepartmentEntity.class);
        verify(departmentRepository).findById(eq("sales"));
        verify(departmentRepository).save(savedDepartment.capture());
        assertThat(savedDepartment.getValue().getDepartmentCode()).isEqualTo("sales");
        assertThat(savedDepartment.getValue().getDepartmentName()).isEqualTo("销售部");
    }

    @Test
    @DisplayName("disabled integration rejects valid events before side effects")
    void receiveEvent_disabledIntegration_rejectsWithoutProcessing() {
        OrganizationIntegrationProperties disabledProperties = new OrganizationIntegrationProperties();
        disabledProperties.setEnabled(false);
        disabledProperties.setAllowedSourceApps(java.util.List.of("customer-org"));
        service = new OrganizationEventAppService(
                eventLogRepository,
                departmentRepository,
                userRepository,
                roleProfileRepository,
                disabledProperties
        );
        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest(
                "org.user.upsert",
                "{\"userCode\":\"u001\"}"
        );

        OrganizationEventWebhookResponse response = service.receiveWebhook(request, "trace-1", "customer-org");

        assertThat(response.code()).isEqualTo("500");
        assertThat(response.data().status()).isEqualTo(OrganizationEventStatus.REJECTED.name());
        verify(userRepository, never()).save(any());
        verify(eventLogRepository).save(any(OrganizationEventLogEntity.class));
    }

    @Test
    @DisplayName("malformed JSON is logged as failed after reservation")
    void receiveEvent_malformedPayload_logsFailed() {
        when(eventLogRepository.existsByEventKey(any())).thenReturn(false);
        when(eventLogRepository.saveAndFlush(any(OrganizationEventLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest("org.user.upsert", "{");

        OrganizationEventWebhookResponse response = service.receiveWebhook(request, "trace-1", "customer-org");

        assertThat(response.code()).isEqualTo("500");
        assertThat(response.data().status()).isEqualTo(OrganizationEventStatus.FAILED.name());
        verify(userRepository, never()).save(any());
        verify(eventLogRepository).save(any(OrganizationEventLogEntity.class));
    }
}
