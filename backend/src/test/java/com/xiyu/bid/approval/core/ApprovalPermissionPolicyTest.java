package com.xiyu.bid.approval.core;

import com.xiyu.bid.approval.entity.ApprovalRequest;
import com.xiyu.bid.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalPermissionPolicyTest {

    private final ApprovalPermissionPolicy policy = new ApprovalPermissionPolicy();

    @Test
    void canApprove_RequiresCurrentApproverWhenAssigned() {
        ApprovalRequest request = ApprovalRequest.builder()
                .currentApproverId(20L)
                .requesterId(10L)
                .build();

        assertThat(policy.canApprove(request, 20L).allowed()).isTrue();
        assertThat(policy.canApprove(request, 30L).reason()).contains("没有权限");
    }

    @Test
    void canViewPendingQueue_RestrictsNonPrivilegedCrossUserReads() {
        assertThat(policy.canViewPendingQueue(20L, User.Role.STAFF, 20L).allowed()).isTrue();
        assertThat(policy.canViewPendingQueue(20L, User.Role.STAFF, 30L).reason()).contains("只能查看自己的");
        assertThat(policy.canViewPendingQueue(20L, User.Role.MANAGER, 30L).allowed()).isTrue();
    }

    @Test
    void canViewApprovalRequest_AllowsRequesterApproverAndPrivilegedRoles() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requesterId(10L)
                .currentApproverId(20L)
                .build();

        assertThat(policy.canViewApprovalRequest(request, 10L, User.Role.STAFF).allowed()).isTrue();
        assertThat(policy.canViewApprovalRequest(request, 20L, User.Role.STAFF).allowed()).isTrue();
        assertThat(policy.canViewApprovalRequest(request, 99L, User.Role.MANAGER).allowed()).isTrue();
        assertThat(policy.canViewApprovalRequest(request, 99L, User.Role.STAFF).reason()).contains("无权查看");
    }
}
