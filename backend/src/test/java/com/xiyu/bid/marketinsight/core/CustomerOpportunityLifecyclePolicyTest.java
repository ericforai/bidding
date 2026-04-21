package com.xiyu.bid.marketinsight.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerOpportunityLifecyclePolicyTest {

    @Test
    void resolveRefreshStatus_WithNull_ShouldDefaultToWatch() {
        assertThat(CustomerOpportunityLifecyclePolicy.resolveRefreshStatus(null))
                .isEqualTo(CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.WATCH);
    }

    @Test
    void transition_WatchToRecommend_ShouldAllow() {
        CustomerOpportunityLifecyclePolicy.LifecycleDecision decision =
                CustomerOpportunityLifecyclePolicy.transition(
                        CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.WATCH,
                        CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.RECOMMEND);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.nextStatus())
                .isEqualTo(CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.RECOMMEND);
    }

    @Test
    void convert_WithoutProjectId_ShouldDeny() {
        CustomerOpportunityLifecyclePolicy.LifecycleDecision decision =
                CustomerOpportunityLifecyclePolicy.convert(
                        CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.RECOMMEND,
                        null);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("projectId");
    }

    @Test
    void convert_FromWatch_ShouldDeny() {
        CustomerOpportunityLifecyclePolicy.LifecycleDecision decision =
                CustomerOpportunityLifecyclePolicy.convert(
                        CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.WATCH,
                        100L);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("WATCH");
    }
}
