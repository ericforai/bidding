package com.xiyu.bid.marketinsight.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Pure core policy for prediction status transition validation.
 * No state, no dependencies, no side effects.
 */
public final class PredictionTransitionPolicy {

    /** Allowed transitions from each status. */
    private static final Map<PredictionStatus, Set<PredictionStatus>>
            ALLOWED_TRANSITIONS;

    static {
        EnumMap<PredictionStatus, Set<PredictionStatus>> map =
                new EnumMap<>(PredictionStatus.class);
        map.put(PredictionStatus.WATCH,
                Set.of(PredictionStatus.RECOMMEND,
                        PredictionStatus.CANCELLED));
        map.put(PredictionStatus.RECOMMEND,
                Set.of(PredictionStatus.CONVERTED,
                        PredictionStatus.WATCH,
                        PredictionStatus.CANCELLED));
        map.put(PredictionStatus.CONVERTED, Set.of());
        map.put(PredictionStatus.CANCELLED,
                Set.of(PredictionStatus.WATCH));
        ALLOWED_TRANSITIONS = Map.copyOf(map);
    }

    private PredictionTransitionPolicy() {
    }

    /**
     * Validate whether a prediction status transition is legal.
     *
     * @param current current prediction status
     * @param target  target prediction status
     * @return TransitionResult with allowed flag and reason
     */
    public static TransitionResult validateTransition(
            final PredictionStatus current,
            final PredictionStatus target) {
        CustomerOpportunityLifecyclePolicy.LifecycleDecision decision =
                CustomerOpportunityLifecyclePolicy.transition(
                        current != null
                                ? CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.valueOf(current.name())
                                : null,
                        target != null
                                ? CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.valueOf(target.name())
                                : null);
        return decision.allowed() ? TransitionResult.ok() : TransitionResult.denied(decision.reason());
    }

    /**
     * Result of a transition validation.
     *
     * @param allowed whether transition is legal
     * @param reason  human-readable explanation if denied
     */
    public record TransitionResult(boolean allowed, String reason) {

        /** Create an accepted result.
         *
         * @return accepted transition result
         */
        public static TransitionResult ok() {
            return new TransitionResult(true, "");
        }

        /** Create a denied result with reason.
         *
         * @param reason the denial reason
         * @return denied transition result
         */
        public static TransitionResult denied(final String reason) {
            return new TransitionResult(false, reason);
        }
    }

    /**
     * Prediction status values.
     * Core policy uses its own enum to avoid coupling to JPA entity.
     */
    public enum PredictionStatus {
        /** Under observation. */
        WATCH,
        /** Recommended for action. */
        RECOMMEND,
        /** Converted to opportunity. */
        CONVERTED,
        /** Cancelled observation. */
        CANCELLED
    }
}
