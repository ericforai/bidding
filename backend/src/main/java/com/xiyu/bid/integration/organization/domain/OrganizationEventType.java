package com.xiyu.bid.integration.organization.domain;

public enum OrganizationEventType {
    DEPARTMENT_UPSERT("org.department.upsert"),
    DEPARTMENT_DISABLE("org.department.disable"),
    USER_UPSERT("org.user.upsert"),
    USER_DISABLE("org.user.disable"),
    USER_ROLE_CHANGED("org.user.role.changed");

    private final String topic;

    OrganizationEventType(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }
}
