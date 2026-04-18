package com.xiyu.bid.approval.core;

import com.xiyu.bid.approval.entity.ApprovalRequest;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalDecisionPolicyTest {

    private final ApprovalDecisionPolicy policy = new ApprovalDecisionPolicy();

    @Test
    void canSubmit_DeniesWhenPendingRequestExists() {
        ApprovalRequest pending = ApprovalRequest.builder()
                .status(ApprovalStatus.PENDING)
                .build();

        ApprovalRuleResult result = policy.canSubmit(List.of(pending));

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).contains("已有待审批");
    }

    @Test
    void canApprove_DeniesNonPendingRequest() {
        ApprovalRequest request = ApprovalRequest.builder()
                .status(ApprovalStatus.APPROVED)
                .build();

        ApprovalRuleResult result = policy.canApprove(request);

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).contains("当前状态不允许审批");
    }

    @Test
    void canCancel_UsesRequesterBoundary() {
        ApprovalRequest request = ApprovalRequest.builder()
                .status(ApprovalStatus.PENDING)
                .requesterId(10L)
                .build();

        assertThat(policy.canCancel(request, 10L).allowed()).isTrue();
        assertThat(policy.canCancel(request, 20L).allowed()).isFalse();
    }

    @Test
    void canResubmit_RequiresRejectedAndRequester() {
        ApprovalRequest rejected = ApprovalRequest.builder()
                .status(ApprovalStatus.REJECTED)
                .requesterId(10L)
                .build();

        assertThat(policy.canResubmit(rejected, 10L).allowed()).isTrue();
        assertThat(policy.canResubmit(rejected, 11L).reason()).contains("只有申请人");
        assertThat(policy.canResubmit(ApprovalRequest.builder()
                .status(ApprovalStatus.PENDING)
                .requesterId(10L)
                .build(), 10L).reason()).contains("只有被驳回");
    }
}
