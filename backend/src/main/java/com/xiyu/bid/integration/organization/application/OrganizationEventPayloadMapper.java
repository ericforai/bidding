package com.xiyu.bid.integration.organization.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;

final class OrganizationEventPayloadMapper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OrganizationEventPayloadMapper() {
    }

    static OrganizationUserSnapshot toUserSnapshot(String payload, boolean enabled) {
        JsonNode node = readPayload(payload);
        String externalUserId = requiredText(node, "userCode");
        return new OrganizationUserSnapshot(
                externalUserId,
                text(node, "username", externalUserId),
                text(node, "name", externalUserId),
                text(node, "email", externalUserId + "@external-org.local"),
                text(node, "phone", ""),
                text(node, "departmentCode", ""),
                text(node, "departmentName", ""),
                text(node, "roleCode", ""),
                enabled
        );
    }

    static DepartmentPayload toDepartmentPayload(String payload) {
        JsonNode node = readPayload(payload);
        String code = requiredText(node, "departmentCode");
        return new DepartmentPayload(
                code,
                text(node, "departmentName", code),
                text(node, "parentDepartmentCode", "")
        );
    }

    private static JsonNode readPayload(String payload) {
        try {
            return OBJECT_MAPPER.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("事件消息不是合法 JSON");
        }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = text(node, field, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("事件消息缺少字段: " + field);
        }
        return value;
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText(fallback).trim();
    }

    record DepartmentPayload(String code, String name, String parentCode) {
    }
}
