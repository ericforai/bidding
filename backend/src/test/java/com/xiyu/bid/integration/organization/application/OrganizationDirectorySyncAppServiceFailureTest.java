package com.xiyu.bid.integration.organization.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.integration.organization.domain.OrganizationEventNotice;
import com.xiyu.bid.integration.organization.domain.OrganizationEventStatus;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookRequest;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizationDirectorySyncAppService - failure paths")
class OrganizationDirectorySyncAppServiceFailureTest {
    private FakeGateway gateway;
    private FakeInbox inbox;
    private FakeUserWriter userWriter;
    private OrganizationDirectorySyncAppService service;

    @BeforeEach
    void setUp() {
        gateway = new FakeGateway();
        inbox = new FakeInbox();
        userWriter = new FakeUserWriter();
        service = new OrganizationDirectorySyncAppService(
                inbox,
                new OrganizationEventNoticeJsonReader(new ObjectMapper()),
                gateway,
                new FakeDepartmentWriter(),
                userWriter,
                OrganizationDirectorySyncAppServiceTest.fixedSettings(true)
        );
    }

    @Test
    @DisplayName("HTTP wrapper topic must match payload topic")
    void receiveWebhook_topicMismatch_rejects() {
        OrganizationEventWebhookResponse response = service.receiveWebhook(new OrganizationEventWebhookRequest(
                "BaseOssDept",
                request("BaseOssUser", "userId", "10001").eventMessage()
        ));

        assertThat(response.code()).isEqualTo("500");
        assertThat(inbox.status).isEqualTo(OrganizationEventStatus.REJECTED);
        assertThat(gateway.fetchedUserId).isNull();
    }

    @Test
    @DisplayName("gateway exception keeps event failed for retry")
    void receiveWebhook_gatewayException_marksFailed() {
        gateway.throwOnUser = true;

        OrganizationEventWebhookResponse response = service.receiveWebhook(request("BaseOssUser", "userId", "10001"));

        assertThat(response.code()).isEqualTo("500");
        assertThat(inbox.status).isEqualTo(OrganizationEventStatus.FAILED);
        assertThat(inbox.errorCode).isEqualTo("SYNC_EXCEPTION");
    }

    @Test
    @DisplayName("writer exception keeps reserved inbox failed")
    void receiveWebhook_writerException_marksFailed() {
        gateway.user = Optional.of(new OrganizationUserSnapshot(
                "10001", "u10001", "张三", "u10001@example.com", "",
                "sales", "销售部", "", true
        ));
        userWriter.throwOnWrite = true;

        OrganizationEventWebhookResponse response = service.receiveWebhook(request("BaseOssUser", "userId", "10001"));

        assertThat(response.code()).isEqualTo("500");
        assertThat(inbox.status).isEqualTo(OrganizationEventStatus.FAILED);
        assertThat(inbox.errorCode).isEqualTo("SYNC_EXCEPTION");
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

    private static class FakeGateway extends OrganizationDirectorySyncAppServiceTest.FakeGateway {
        boolean throwOnUser;

        public Optional<OrganizationUserSnapshot> fetchUserByUserId(String userId) {
            if (throwOnUser) {
                throw new IllegalStateException("gateway timeout");
            }
            return super.fetchUserByUserId(userId);
        }
    }

    private static class FakeInbox extends OrganizationEventInboxService {
        OrganizationEventStatus status;
        String errorCode;

        FakeInbox() {
            super(null);
        }

        public String eventKey(OrganizationEventNotice notice) {
            return "event-key";
        }

        public boolean reserve(OrganizationEventNotice notice, String rawPayload) {
            return true;
        }

        public void markRejected(String eventKey, String message, String rawPayload) {
            status = OrganizationEventStatus.REJECTED;
        }

        public void markFailed(String eventKey, String message, String errorCode) {
            status = OrganizationEventStatus.FAILED;
            this.errorCode = errorCode;
        }
    }

    private static class FakeDepartmentWriter extends OrganizationDirectorySyncAppServiceTest.FakeDepartmentWriter {
    }

    private static class FakeUserWriter extends OrganizationDirectorySyncAppServiceTest.FakeUserWriter {
        boolean throwOnWrite;

        public com.xiyu.bid.entity.User upsert(String sourceApp, String eventKey, OrganizationUserSnapshot snapshot) {
            if (throwOnWrite) {
                throw new IllegalStateException("write failed");
            }
            return super.upsert(sourceApp, eventKey, snapshot);
        }
    }
}
