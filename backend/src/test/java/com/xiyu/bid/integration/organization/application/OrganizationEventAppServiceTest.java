package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.entity.RoleProfile;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.integration.organization.domain.OrganizationEventStatus;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookRequest;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationEventAppService — orchestration")
class OrganizationEventAppServiceTest {

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
    @DisplayName("processes user upsert into platform user")
    void receiveEvent_userUpsert_savesUser() {
        when(eventLogRepository.existsByEventKey(any())).thenReturn(false);
        when(eventLogRepository.saveAndFlush(any(OrganizationEventLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername("u001")).thenReturn(Optional.empty());
        RoleProfile staff = role("staff");
        when(roleProfileRepository.findByCodeIgnoreCase("staff")).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest(
                "org.user.upsert",
                "{\"userCode\":\"u001\",\"name\":\"张三\",\"departmentCode\":\"SALES\",\"departmentName\":\"销售部\"}"
        );

        OrganizationEventWebhookResponse response = service.receiveWebhook(request, "trace-1", "customer-org");

        assertThat(response.code()).isEqualTo("200");
        assertThat(response.data().accepted()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(eventLogRepository).save(any(OrganizationEventLogEntity.class));
    }

    @Test
    @DisplayName("returns accepted duplicate without rewriting user")
    void receiveEvent_duplicate_skipsProcessing() {
        when(eventLogRepository.existsByEventKey(any())).thenReturn(true);
        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest(
                "org.user.upsert",
                "{\"userCode\":\"u001\"}"
        );

        OrganizationEventWebhookResponse response = service.receiveWebhook(request, "trace-1", "customer-org");

        assertThat(response.code()).isEqualTo("200");
        assertThat(response.data().duplicate()).isTrue();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("duplicate unique-key race returns duplicate without rewriting user")
    void receiveEvent_duplicateRace_skipsProcessing() {
        when(eventLogRepository.existsByEventKey(any())).thenReturn(false);
        when(eventLogRepository.saveAndFlush(any(OrganizationEventLogEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate event"));
        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest(
                "org.user.upsert",
                "{\"userCode\":\"u001\"}"
        );

        OrganizationEventWebhookResponse response = service.receiveWebhook(request, "trace-1", "customer-org");

        assertThat(response.code()).isEqualTo("200");
        assertThat(response.data().duplicate()).isTrue();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("role changed updates user role instead of silent success")
    void receiveEvent_roleChanged_updatesUser() {
        User existing = new User();
        existing.setUsername("u001");
        existing.setPassword("hash");
        existing.setEmail("u001@example.com");
        existing.setFullName("张三");
        existing.setEnabled(true);
        when(eventLogRepository.existsByEventKey(any())).thenReturn(false);
        when(eventLogRepository.saveAndFlush(any(OrganizationEventLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername("u001")).thenReturn(Optional.of(existing));
        when(roleProfileRepository.findByCodeIgnoreCase("manager")).thenReturn(Optional.of(role("manager")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrganizationIntegrationProperties mappedProperties = new OrganizationIntegrationProperties();
        mappedProperties.setAllowedSourceApps(java.util.List.of("customer-org"));
        mappedProperties.setManagerRoleCodes(java.util.List.of("external-manager"));
        service = new OrganizationEventAppService(
                eventLogRepository,
                departmentRepository,
                userRepository,
                roleProfileRepository,
                mappedProperties
        );
        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest(
                "org.user.role.changed",
                "{\"userCode\":\"u001\",\"roleCode\":\"external-manager\"}"
        );

        OrganizationEventWebhookResponse response = service.receiveWebhook(request, "trace-1", "customer-org");

        assertThat(response.code()).isEqualTo("200");
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getFullName()).isEqualTo("张三");
        assertThat(savedUser.getValue().getEmail()).isEqualTo("u001@example.com");
    }

    @Test
    @DisplayName("invalid envelope is logged as rejected and returns 500 contract code")
    void receiveEvent_invalidEnvelope_logsRejected() {
        OrganizationEventWebhookRequest request = new OrganizationEventWebhookRequest("org.unknown", "{}");

        OrganizationEventWebhookResponse response = service.receiveWebhook(request, "trace-1", "customer-org");

        assertThat(response.code()).isEqualTo("500");
        assertThat(response.data().status()).isEqualTo(OrganizationEventStatus.REJECTED.name());
        verify(eventLogRepository).save(any(OrganizationEventLogEntity.class));
    }

    private RoleProfile role(String code) {
        RoleProfile role = new RoleProfile();
        role.setId(1L);
        role.setCode(code);
        role.setName(code);
        role.setEnabled(true);
        return role;
    }
}
