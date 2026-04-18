package com.xiyu.bid.approval.service;

import com.xiyu.bid.approval.entity.ApprovalRequest;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalDecisionPolicyTest {

    @Test
    void canSubmit_ShouldRejectWhenPendingRequestAlreadyExists() {
        ApprovalDecisionPolicy.Decision decision = ApprovalDecisionPolicy.canSubmit(List.of(
                ApprovalRequest.builder().status(ApprovalStatus.PENDING).build(),
                ApprovalRequest.builder().status(ApprovalStatus.REJECTED).build()
        ));

        assertThat(decision.permitted()).isFalse();
        assertThat(decision.message()).contains("已有待审批的请求");
    }

    @Test
    void canApprove_ShouldRejectNonPendingRequests() {
        ApprovalDecisionPolicy.Decision decision = ApprovalDecisionPolicy.canApprove(
                ApprovalRequest.builder().status(ApprovalStatus.APPROVED).build()
        );

        assertThat(decision.permitted()).isFalse();
        assertThat(decision.message()).contains("当前状态不允许审批");
    }

    @Test
    void canResubmit_ShouldRequireRejectedRequestOwnedByRequester() {
        ApprovalRequest request = ApprovalRequest.builder()
                .status(ApprovalStatus.REJECTED)
                .requesterId(10L)
                .build();

        assertThat(ApprovalDecisionPolicy.canResubmit(request, 10L).permitted()).isTrue();
        assertThat(ApprovalDecisionPolicy.canResubmit(request, 99L).permitted()).isFalse();
    }
}
