package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.domain.OrganizationDepartmentSnapshot;
import com.xiyu.bid.integration.organization.domain.OrganizationEventStatus;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookRequest;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizationDirectorySyncAppService - notice orchestration")
class OrganizationDirectorySyncAppServiceTest {
    private FakeGateway gateway;
    private FakeInbox inbox;
    private FakeDepartmentWriter departmentWriter;
    private FakeUserWriter userWriter;
    private OrganizationDirectorySyncAppService service;

    @BeforeEach
    void setUp() {
        gateway = new FakeGateway();
        inbox = new FakeInbox();
        departmentWriter = new FakeDepartmentWriter();
        userWriter = new FakeUserWriter();
        service = new OrganizationDirectorySyncAppService(
                inbox,
                new OrganizationEventNoticeJsonReader(new ObjectMapper()),
                gateway,
                departmentWriter,
                userWriter,
                fixedSettings(true)
        );
    }

    @Test
    @DisplayName("BaseOssUser notice fetches master data before writing user")
    void receiveWebhook_userNotice_fetchesThenWrites() {
        gateway.user = Optional.of(new OrganizationUserSnapshot(
                "10001", "u10001", "张三", "u10001@example.com", "",
                "sales", "销售部", "", true
        ));

        OrganizationEventWebhookResponse response = service.receiveWebhook(request("BaseOssUser", "userId", "10001"));

        assertThat(response.code()).isEqualTo("200");
        assertThat(gateway.fetchedUserId).isEqualTo("10001");
        assertThat(userWriter.snapshot.externalUserId()).isEqualTo("10001");
        assertThat(inbox.status).isEqualTo(OrganizationEventStatus.PROCESSED);
    }

    @Test
    @DisplayName("BaseOssDept notice fetches master data before writing department")
    void receiveWebhook_deptNotice_fetchesThenWrites() {
        gateway.department = Optional.of(new OrganizationDepartmentSnapshot("D001", "sales", "销售部", "", "", true));

        OrganizationEventWebhookResponse response = service.receiveWebhook(request("BaseOssDept", "deptId", "D001"));

        assertThat(response.code()).isEqualTo("200");
        assertThat(gateway.fetchedDeptId).isEqualTo("D001");
        assertThat(departmentWriter.snapshot.externalDeptId()).isEqualTo("D001");
    }

    @Test
    @DisplayName("missing master data keeps event failed for retry")
    void receiveWebhook_missingMasterData_returns500() {
        OrganizationEventWebhookResponse response = service.receiveWebhook(request("BaseOssUser", "userId", "10001"));

        assertThat(response.code()).isEqualTo("500");
        assertThat(inbox.status).isEqualTo(OrganizationEventStatus.FAILED);
    }

    @Test
    @DisplayName("duplicate notice skips master data lookup")
    void receiveWebhook_duplicate_skipsLookup() {
        inbox.reserved = false;

        OrganizationEventWebhookResponse response = service.receiveWebhook(request("BaseOssUser", "userId", "10001"));

        assertThat(response.code()).isEqualTo("200");
        assertThat(response.data().duplicate()).isTrue();
        assertThat(gateway.fetchedUserId).isNull();
    }

    private OrganizationEventWebhookRequest request(String topic, String idField, String id) {
        return new OrganizationEventWebhookRequest(topic, """
                {
                  "traceId": "trace-1",
                  "spanId": "span-1",
                  "parentId": "parent-1",
                  "eventSource": "customer-org",
                  "eventTopic": "%s",
                  "time": "2026-04-30T10:15:30+08:00",
                  "key": "event-1",
                  "data": {"%s": "%s"}
                }
                """.formatted(topic, idField, id));
    }

    static OrganizationIntegrationSettingsResolver fixedSettings(boolean enabled) {
        OrganizationIntegrationProperties properties = new OrganizationIntegrationProperties();
        properties.setEnabled(enabled);
        properties.setAllowedSourceApps(List.of("customer-org"));
        return new OrganizationIntegrationSettingsResolver(null, properties);
    }

    static class FakeGateway implements OrganizationDirectoryGateway {
        Optional<OrganizationUserSnapshot> user = Optional.empty();
        Optional<OrganizationDepartmentSnapshot> department = Optional.empty();
        String fetchedUserId;
        String fetchedDeptId;

        public Optional<OrganizationDepartmentSnapshot> fetchDepartmentByDeptId(String deptId) {
            fetchedDeptId = deptId;
            return department;
        }

        public Optional<OrganizationUserSnapshot> fetchUserByUserId(String userId) {
            fetchedUserId = userId;
            return user;
        }

        public List<OrganizationDepartmentSnapshot> listDepartmentsByWindow(LocalDateTime startAt, LocalDateTime endAt) {
            return List.of();
        }

        public List<OrganizationUserSnapshot> listUsersByWindow(LocalDateTime startAt, LocalDateTime endAt) {
            return List.of();
        }
    }

    private static class FakeInbox extends OrganizationEventInboxService {
        boolean reserved = true;
        OrganizationEventStatus status;
        String errorCode;

        FakeInbox() {
            super(null);
        }

        public String eventKey(com.xiyu.bid.integration.organization.domain.OrganizationEventNotice notice) {
            return "event-key";
        }

        public boolean reserve(com.xiyu.bid.integration.organization.domain.OrganizationEventNotice notice, String rawPayload) {
            return reserved;
        }

        public void markProcessed(String eventKey) {
            status = OrganizationEventStatus.PROCESSED;
        }

        public void markFailed(String eventKey, String message, String errorCode) {
            status = OrganizationEventStatus.FAILED;
            this.errorCode = errorCode;
        }

        public void markRejected(String eventKey, String message, String rawPayload) {
            status = OrganizationEventStatus.REJECTED;
        }
    }

    static class FakeDepartmentWriter extends OrganizationDepartmentSyncWriter {
        OrganizationDepartmentSnapshot snapshot;

        FakeDepartmentWriter() {
            super(null);
        }

        public com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationDepartmentEntity upsert(
                String sourceApp,
                String eventKey,
                OrganizationDepartmentSnapshot snapshot
        ) {
            this.snapshot = snapshot;
            return new com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationDepartmentEntity();
        }
    }

    static class FakeUserWriter extends OrganizationUserSyncWriter {
        OrganizationUserSnapshot snapshot;

        FakeUserWriter() {
            super(null, null, new OrganizationIntegrationProperties());
        }

        public com.xiyu.bid.entity.User upsert(String sourceApp, String eventKey, OrganizationUserSnapshot snapshot) {
            this.snapshot = snapshot;
            return new com.xiyu.bid.entity.User();
        }
    }
}
