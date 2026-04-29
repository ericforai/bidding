package com.xiyu.bid.security;

import com.xiyu.bid.matrixcollaboration.infrastructure.persistence.entity.CrmCustomerPermission;
import com.xiyu.bid.matrixcollaboration.infrastructure.persistence.entity.ProjectMember;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeamingEntityTest {

    @Test
    void crmCustomerPermissionBuilderCreatesValidEntity() {
        CrmCustomerPermission permission = CrmCustomerPermission.builder()
                .id(1L)
                .customerId("CRM_CUST_001")
                .userId(100L)
                .permissionType("OWNER")
                .build();

        assertThat(permission.getId()).isEqualTo(1L);
        assertThat(permission.getCustomerId()).isEqualTo("CRM_CUST_001");
        assertThat(permission.getUserId()).isEqualTo(100L);
        assertThat(permission.getPermissionType()).isEqualTo("OWNER");
    }

    @Test
    void projectMemberBuilderCreatesValidEntity() {
        ProjectMember member = ProjectMember.builder()
                .id(1L)
                .projectId(500L)
                .userId(101L)
                .memberRole("TECHNICAL_EXPERT")
                .permissionLevel("EDITOR")
                .inherited(false)
                .build();

        assertThat(member.getId()).isEqualTo(1L);
        assertThat(member.getProjectId()).isEqualTo(500L);
        assertThat(member.getUserId()).isEqualTo(101L);
        assertThat(member.getMemberRole()).isEqualTo("TECHNICAL_EXPERT");
        assertThat(member.getPermissionLevel()).isEqualTo("EDITOR");
        assertThat(member.isInherited()).isFalse();
    }
}
