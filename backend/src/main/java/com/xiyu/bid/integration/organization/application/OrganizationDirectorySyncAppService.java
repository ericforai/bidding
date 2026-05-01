package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.domain.OrganizationEventNotice;
import com.xiyu.bid.integration.organization.domain.OrganizationEventNoticeParseResult;
import com.xiyu.bid.integration.organization.domain.OrganizationEventStatus;
import com.xiyu.bid.integration.organization.domain.OrganizationEventType;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookRequest;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationDirectorySyncAppService {
    private final OrganizationEventInboxService inboxService;
    private final OrganizationEventNoticeJsonReader noticeJsonReader;
    private final OrganizationDirectoryGateway directoryGateway;
    private final OrganizationDepartmentSyncWriter departmentWriter;
    private final OrganizationUserSyncWriter userWriter;
    private final OrganizationIntegrationSettingsResolver settingsResolver;

    public OrganizationEventWebhookResponse receiveWebhook(OrganizationEventWebhookRequest request) {
        String rawPayload = request == null ? "" : request.eventMessage();
        OrganizationIntegrationSettings settings = settingsResolver.resolve();
        if (!settings.enabled()) {
            String eventKey = OrganizationEventKeyFactory.hash(rawPayload);
            inboxService.markRejected(eventKey, "组织架构事件接入已关闭", rawPayload);
            return response("500", "组织架构事件接入已关闭", eventKey, false, false, OrganizationEventStatus.REJECTED);
        }
        OrganizationEventNoticeParseResult parsed = noticeJsonReader.parse(rawPayload);
        if (!parsed.valid()) {
            String eventKey = OrganizationEventKeyFactory.hash(rawPayload);
            inboxService.markRejected(eventKey, parsed.message(), rawPayload);
            return response("500", parsed.message(), eventKey, false, false, OrganizationEventStatus.REJECTED);
        }
        if (!requestTopicMatchesPayload(request, parsed.notice())) {
            String eventKey = inboxService.eventKey(parsed.notice());
            inboxService.markRejected(eventKey, "HTTP事件Topic与payload不一致", rawPayload);
            return response("500", "HTTP事件Topic与payload不一致", eventKey, false, false, OrganizationEventStatus.REJECTED);
        }
        return processNotice(parsed.notice(), rawPayload, settings);
    }

    private OrganizationEventWebhookResponse processNotice(
            OrganizationEventNotice notice,
            String rawPayload,
            OrganizationIntegrationSettings settings
    ) {
        String eventKey = inboxService.eventKey(notice);
        if (!settings.sourceAllowed(notice.eventSource())) {
            inboxService.markRejected(eventKey, "事件来源不在白名单内", rawPayload);
            return response("500", "事件来源不在白名单内", eventKey, false, false, OrganizationEventStatus.REJECTED);
        }
        if (!inboxService.reserve(notice, rawPayload)) {
            return response("200", "success", eventKey, true, true, OrganizationEventStatus.DUPLICATE);
        }
        try {
            return lookupAndWrite(notice, eventKey);
        } catch (RuntimeException ex) {
            inboxService.markFailed(eventKey, "组织架构同步处理异常", "SYNC_EXCEPTION");
            return response("500", "组织架构同步处理异常", eventKey, false, false, OrganizationEventStatus.FAILED);
        }
    }

    private OrganizationEventWebhookResponse lookupAndWrite(OrganizationEventNotice notice, String eventKey) {
        if (notice.topic() == OrganizationEventType.DEPARTMENT_NOTICE) {
            return directoryGateway.fetchDepartmentByDeptId(notice.subjectId())
                    .map(snapshot -> {
                        departmentWriter.upsert(notice.eventSource(), eventKey, snapshot);
                        inboxService.markProcessed(eventKey);
                        return response("200", "success", eventKey, true, false, OrganizationEventStatus.PROCESSED);
                    })
                    .orElseGet(() -> failLookup(eventKey, "DEPARTMENT_NOT_FOUND"));
        }
        return directoryGateway.fetchUserByUserId(notice.subjectId())
                .map(snapshot -> {
                    userWriter.upsert(notice.eventSource(), eventKey, snapshot);
                    inboxService.markProcessed(eventKey);
                    return response("200", "success", eventKey, true, false, OrganizationEventStatus.PROCESSED);
                })
                .orElseGet(() -> failLookup(eventKey, "USER_NOT_FOUND"));
    }

    private OrganizationEventWebhookResponse failLookup(String eventKey, String errorCode) {
        inboxService.markFailed(eventKey, "组织架构主数据接口未返回数据", errorCode);
        return response("500", "组织架构主数据接口未返回数据", eventKey, false, false, OrganizationEventStatus.FAILED);
    }

    private boolean requestTopicMatchesPayload(OrganizationEventWebhookRequest request, OrganizationEventNotice notice) {
        return request == null
                || request.eventTopic() == null
                || request.eventTopic().isBlank()
                || notice.topic().topic().equals(request.eventTopic());
    }

    private OrganizationEventWebhookResponse response(
            String code,
            String message,
            String eventKey,
            boolean accepted,
            boolean duplicate,
            OrganizationEventStatus status
    ) {
        return OrganizationEventResponseFactory.response(code, message, eventKey, accepted, duplicate, status);
    }
}
