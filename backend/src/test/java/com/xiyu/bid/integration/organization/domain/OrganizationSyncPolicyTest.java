package com.xiyu.bid.integration.organization.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizationSyncPolicy - pure sync decisions")
class OrganizationSyncPolicyTest {

    @Test
    @DisplayName("builds idempotency key from source topic key and time")
    void idempotencyKey_usesCustomerNoticeIdentity() {
        OrganizationEventNotice notice = new OrganizationEventNotice(
                "trace-1",
                "span-1",
                "parent-1",
                "customer-org",
                OrganizationEventType.USER_NOTICE,
                "2026-04-30T10:15:30+08:00",
                "user-10001",
                "10001"
        );

        assertThat(OrganizationSyncPolicy.idempotencyKey(notice))
                .isEqualTo("customer-org|BaseOssUser|user-10001|2026-04-30T10:15:30+08:00");
    }

    @Test
    @DisplayName("unknown external role is downgraded to staff")
    void planUserSync_unknownRole_downgradesToStaff() {
        OrganizationUserSnapshot snapshot = new OrganizationUserSnapshot(
                "10001", "zhangsan", "张三", "zhangsan@example.com",
                "13800000000", "sales", "销售部", "boss", true
        );

        OrganizationUserSyncPlan plan = OrganizationSyncPolicy.planUserSync(
                snapshot,
                "staff",
                Set.of("boss"),
                Set.of()
        );

        assertThat(plan.roleCode()).isEqualTo("staff");
    }

    @Test
    @DisplayName("existing manager is not automatically promoted to admin")
    void planUserSync_adminRole_doesNotAutoElevate() {
        OrganizationUserSnapshot snapshot = new OrganizationUserSnapshot(
                "10001", "zhangsan", "张三", "zhangsan@example.com",
                "13800000000", "sales", "销售部", "external-admin", true
        );

        OrganizationUserSyncPlan plan = OrganizationSyncPolicy.planUserSync(
                snapshot,
                "manager",
                Set.of("external-admin"),
                Set.of()
        );

        assertThat(plan.roleCode()).isEqualTo("manager");
    }
}
