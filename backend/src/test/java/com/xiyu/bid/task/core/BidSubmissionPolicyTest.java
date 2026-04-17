package com.xiyu.bid.task.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BidSubmissionPolicyTest {

    @Test
    void validateSubmission_ShouldAccept_AllTasksCompleteWithDeliverables() {
        var result = BidSubmissionPolicy.validateSubmission(5, 5, 3, 1);
        assertThat(result.submittable()).isTrue();
        assertThat(result.reason()).isEmpty();
        assertThat(result.gaps()).isEmpty();
    }

    @Test
    void validateSubmission_ShouldReject_IncompleteTasks() {
        var result = BidSubmissionPolicy.validateSubmission(5, 3, 2, 1);
        assertThat(result.submittable()).isFalse();
        assertThat(result.gaps())
                .anyMatch(g -> g.description().contains("未完成"));
    }

    @Test
    void validateSubmission_ShouldReject_NoDeliverablesAtAll() {
        var result = BidSubmissionPolicy.validateSubmission(4, 4, 0, 1);
        assertThat(result.submittable()).isFalse();
        assertThat(result.gaps())
                .anyMatch(g -> g.description().contains("交付物"));
    }

    @Test
    void validateSubmission_ShouldReject_MultipleGaps() {
        var result = BidSubmissionPolicy.validateSubmission(6, 4, 0, 1);
        assertThat(result.submittable()).isFalse();
        assertThat(result.gaps()).hasSize(2);
    }

    @Test
    void validateSubmission_ZeroTasks_ShouldBeSubmittable() {
        var result = BidSubmissionPolicy.validateSubmission(0, 0, 0, 1);
        assertThat(result.submittable()).isTrue();
    }

    @Test
    void validateSubmission_DefaultMinPerTask_ShouldBeOne() {
        var result = BidSubmissionPolicy.validateSubmission(3, 3, 3, 0);
        assertThat(result.submittable()).isTrue();

        var result2 = BidSubmissionPolicy.validateSubmission(3, 3, 3, -1);
        assertThat(result2.submittable()).isTrue();
    }
}
