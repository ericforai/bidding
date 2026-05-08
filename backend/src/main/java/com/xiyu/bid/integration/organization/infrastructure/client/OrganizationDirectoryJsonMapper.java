package com.xiyu.bid.integration.organization.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiyu.bid.integration.organization.domain.OrganizationDepartmentSnapshot;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class OrganizationDirectoryJsonMapper {
    OrganizationDepartmentSnapshot department(JsonNode root) {
        JsonNode node = payloadNode(root);
        String externalDeptId = firstText(node, "deptId", "departmentId", "id");
        String departmentCode = firstText(node, "deptCode", "departmentCode", "code", "deptId");
        return new OrganizationDepartmentSnapshot(
                externalDeptId,
                departmentCode,
                firstText(node, "deptName", "departmentName", "name"),
                firstText(node, "parentDeptId", "parentDepartmentId", "parentId"),
                firstText(node, "parentDeptCode", "parentDepartmentCode", "parentCode", "parentDeptId"),
                enabled(node)
        );
    }

    OrganizationUserSnapshot user(JsonNode root) {
        JsonNode node = payloadNode(root);
        String externalUserId = firstText(node, "userId", "id");
        return new OrganizationUserSnapshot(
                externalUserId,
                firstText(node, "userNo", "username", "loginName", "employeeNo", "userId"),
                firstText(node, "userName", "fullName", "name"),
                firstText(node, "email", "mail"),
                firstText(node, "mobile", "phone", "telephone"),
                firstText(node, "deptId", "departmentCode", "deptCode"),
                firstText(node, "deptName", "departmentName"),
                firstText(node, "roleCode", "positionCode", "jobCode", "positionName"),
                enabled(node)
        );
    }

    List<OrganizationDepartmentSnapshot> departments(JsonNode root) {
        return snapshotNodes(root).stream().map(this::department).toList();
    }

    List<OrganizationUserSnapshot> users(JsonNode root) {
        return snapshotNodes(root).stream().map(this::user).toList();
    }

    private List<JsonNode> snapshotNodes(JsonNode root) {
        JsonNode payload = payloadNode(root);
        JsonNode array = payload.isArray()
                ? payload
                : firstArray(payload, "records", "items", "list", "departments", "users");
        if (array == null) {
            return List.of();
        }
        List<JsonNode> nodes = new ArrayList<>();
        array.forEach(nodes::add);
        return nodes;
    }

    private JsonNode payloadNode(JsonNode root) {
        if (root.has("data") && (root.get("data").isObject() || root.get("data").isArray())) {
            return root.get("data");
        }
        if (root.has("result") && (root.get("result").isObject() || root.get("result").isArray())) {
            return root.get("result");
        }
        return root;
    }

    private JsonNode firstArray(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isValueNode() && !value.isNull()) {
                String text = value.asText().trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private boolean enabled(JsonNode node) {
        JsonNode enabled = node.path("enabled");
        if (enabled.isBoolean()) {
            return enabled.asBoolean();
        }
        JsonNode disabled = node.path("disabled");
        if (disabled.isBoolean()) {
            return !disabled.asBoolean();
        }
        String status = firstText(node, "status", "userStatus", "deptStatus").toLowerCase(Locale.ROOT);
        return !status.contains("disabled") && !status.contains("inactive")
                && !status.contains("停用") && !status.contains("离职");
    }
}
