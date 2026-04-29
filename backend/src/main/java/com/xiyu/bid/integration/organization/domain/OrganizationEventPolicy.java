package com.xiyu.bid.integration.organization.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public final class OrganizationEventPolicy {

    private static final String STAFF = "staff";
    private static final String MANAGER = "manager";
    private static final String ADMIN = "admin";

    private OrganizationEventPolicy() {
    }

    public static OrganizationEventValidation validateEnvelope(
            OrganizationEventEnvelope envelope,
            Set<String> allowedSourceApps
    ) {
        if (isBlank(envelope.sourceApp()) || !allowedSourceApps.contains(envelope.sourceApp().trim())) {
            return OrganizationEventValidation.invalid("事件来源不在白名单内");
        }
        if (isBlank(envelope.traceId())) {
            return OrganizationEventValidation.invalid("事件缺少链路追踪编号");
        }
        if (isBlank(envelope.message())) {
            return OrganizationEventValidation.invalid("事件消息内容不能为空");
        }
        return typeFromTopic(envelope.topic())
                .map(OrganizationEventValidation::ok)
                .orElseGet(() -> OrganizationEventValidation.invalid("不支持的组织事件主题"));
    }

    public static String mapRoleCode(String externalRoleCode, Set<String> adminRoleCodes, Set<String> managerRoleCodes) {
        String normalized = normalize(externalRoleCode);
        if (adminRoleCodes.contains(normalized)) {
            return ADMIN;
        }
        if (managerRoleCodes.contains(normalized)) {
            return MANAGER;
        }
        return STAFF;
    }

    public static OrganizationUserSyncPlan planUserSync(
            OrganizationUserSnapshot incoming,
            Set<String> adminRoleCodes,
            Set<String> managerRoleCodes
    ) {
        String username = firstPresent(incoming.username(), incoming.externalUserId());
        String fullName = firstPresent(incoming.fullName(), username);
        String email = firstPresent(incoming.email(), username + "@external-org.local");
        String roleCode = mapRoleCode(incoming.externalRoleCode(), adminRoleCodes, managerRoleCodes);
        return new OrganizationUserSyncPlan(
                normalize(username),
                fullName.trim(),
                email.trim(),
                blankToEmpty(incoming.phone()),
                normalize(incoming.departmentCode()),
                blankToEmpty(incoming.departmentName()),
                roleCode,
                incoming.enabled()
        );
    }

    private static java.util.Optional<OrganizationEventType> typeFromTopic(String topic) {
        String normalized = normalize(topic);
        return Arrays.stream(OrganizationEventType.values())
                .filter(type -> type.topic().equals(normalized))
                .findFirst();
    }

    private static String firstPresent(String preferred, String fallback) {
        return isBlank(preferred) ? blankToEmpty(fallback) : preferred;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        return blankToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
