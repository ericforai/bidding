package com.xiyu.bid.marketinsight.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionTransitionPolicyTest {

    @Test
    void validateTransition_WatchToRecommend_ShouldAllow() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.WATCH,
                PredictionTransitionPolicy.PredictionStatus.RECOMMEND);
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void validateTransition_WatchToCancelled_ShouldAllow() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.WATCH,
                PredictionTransitionPolicy.PredictionStatus.CANCELLED);
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void validateTransition_WatchToConverted_ShouldDeny() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.WATCH,
                PredictionTransitionPolicy.PredictionStatus.CONVERTED);
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void validateTransition_RecommendToConverted_ShouldAllow() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.RECOMMEND,
                PredictionTransitionPolicy.PredictionStatus.CONVERTED);
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void validateTransition_RecommendToWatch_ShouldAllow() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.RECOMMEND,
                PredictionTransitionPolicy.PredictionStatus.WATCH);
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void validateTransition_RecommendToCancelled_ShouldAllow() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.RECOMMEND,
                PredictionTransitionPolicy.PredictionStatus.CANCELLED);
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void validateTransition_ConvertedToWatch_ShouldDeny() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.CONVERTED,
                PredictionTransitionPolicy.PredictionStatus.WATCH);
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void validateTransition_ConvertedToAny_ShouldDeny() {
        for (var target : PredictionTransitionPolicy.PredictionStatus.values()) {
            if (target == PredictionTransitionPolicy.PredictionStatus.CONVERTED) continue;
            var result = PredictionTransitionPolicy.validateTransition(
                    PredictionTransitionPolicy.PredictionStatus.CONVERTED, target);
            assertThat(result.allowed())
                    .as("CONVERTED -> %s should be denied", target)
                    .isFalse();
        }
    }

    @Test
    void validateTransition_CancelledToWatch_ShouldAllow() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.CANCELLED,
                PredictionTransitionPolicy.PredictionStatus.WATCH);
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void validateTransition_CancelledToRecommend_ShouldDeny() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.CANCELLED,
                PredictionTransitionPolicy.PredictionStatus.RECOMMEND);
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void validateTransition_SameStatus_ShouldBeOk() {
        for (var status : PredictionTransitionPolicy.PredictionStatus.values()) {
            var result = PredictionTransitionPolicy.validateTransition(status, status);
            assertThat(result.allowed())
                    .as("%s -> %s should be allowed", status, status)
                    .isTrue();
        }
    }

    @Test
    void validateTransition_NullCurrent_ShouldDeny() {
        var result = PredictionTransitionPolicy.validateTransition(
                null, PredictionTransitionPolicy.PredictionStatus.WATCH);
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void validateTransition_NullTarget_ShouldDeny() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.WATCH, null);
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void validateTransition_BothNull_ShouldDeny() {
        var result = PredictionTransitionPolicy.validateTransition(null, null);
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void transitionResult_DeniedShouldContainReason() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.WATCH,
                PredictionTransitionPolicy.PredictionStatus.CONVERTED);
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isNotBlank();
    }

    @Test
    void transitionResult_OkShouldHaveEmptyReason() {
        var result = PredictionTransitionPolicy.validateTransition(
                PredictionTransitionPolicy.PredictionStatus.WATCH,
                PredictionTransitionPolicy.PredictionStatus.RECOMMEND);
        assertThat(result.allowed()).isTrue();
        assertThat(result.reason()).isEmpty();
    }
}
