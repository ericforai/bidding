package com.xiyu.bid.integration.organization.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizationEventPolicy — pure event decisions")
class OrganizationEventPolicyTest {

    @Test
    @DisplayName("validates supported user upsert envelope")
    void validateEnvelope_acceptsSupportedUserUpsert() {
        OrganizationEventEnvelope envelope = new OrganizationEventEnvelope(
                "org.user.upsert",
                "customer-org",
                "trace-1",
                "{\"userCode\":\"u001\"}"
        );

        OrganizationEventValidation result = OrganizationEventPolicy.validateEnvelope(envelope, Set.of("customer-org"));

        assertThat(result.valid()).isTrue();
        assertThat(result.type()).isEqualTo(OrganizationEventType.USER_UPSERT);
    }

    @Test
    @DisplayName("rejects unknown source app before processing payload")
    void validateEnvelope_rejectsUnknownSourceApp() {
        OrganizationEventEnvelope envelope = new OrganizationEventEnvelope(
                "org.user.upsert",
                "unknown",
                "trace-1",
                "{\"userCode\":\"u001\"}"
        );

        OrganizationEventValidation result = OrganizationEventPolicy.validateEnvelope(envelope, Set.of("customer-org"));

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("来源");
    }

    @Test
    @DisplayName("role mapping never promotes unknown role to admin")
    void mapRole_unknownRoleFallsBackToStaff() {
        String roleCode = OrganizationEventPolicy.mapRoleCode(
                "finance-director",
                Set.of("customer-admin"),
                Set.of("customer-manager")
        );

        assertThat(roleCode).isEqualTo("staff");
    }

    @Test
    @DisplayName("user sync plan disables users without deleting them")
    void planUserSync_disableEventKeepsUserIdentity() {
        OrganizationUserSnapshot incoming = new OrganizationUserSnapshot(
                "u001",
                "zhangsan",
                "张三",
                "zhangsan@example.com",
                "13800000000",
                "SALES",
                "销售部",
                "sales",
                false
        );

        OrganizationUserSyncPlan plan = OrganizationEventPolicy.planUserSync(
                incoming,
                Set.of("customer-admin"),
                Set.of("sales-manager")
        );

        assertThat(plan.enabled()).isFalse();
        assertThat(plan.username()).isEqualTo("zhangsan");
        assertThat(plan.roleCode()).isEqualTo("staff");
    }
}
