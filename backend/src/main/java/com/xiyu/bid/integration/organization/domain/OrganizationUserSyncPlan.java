package com.xiyu.bid.integration.organization.domain;

public record OrganizationUserSyncPlan(
        String username,
        String fullName,
        String email,
        String phone,
        String departmentCode,
        String departmentName,
        String roleCode,
        boolean enabled
) {
}
