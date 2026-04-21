package com.xiyu.bid.marketinsight.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Pure core lifecycle policy for refresh defaults and status transitions.
 */
public final class CustomerOpportunityLifecyclePolicy {

    private static final Map<PredictionLifecycleStatus, Set<PredictionLifecycleStatus>>
            ALLOWED_TRANSITIONS;

    static {
        EnumMap<PredictionLifecycleStatus, Set<PredictionLifecycleStatus>> map =
                new EnumMap<>(PredictionLifecycleStatus.class);
        map.put(PredictionLifecycleStatus.WATCH,
                Set.of(PredictionLifecycleStatus.RECOMMEND, PredictionLifecycleStatus.CANCELLED));
        map.put(PredictionLifecycleStatus.RECOMMEND,
                Set.of(PredictionLifecycleStatus.WATCH,
                        PredictionLifecycleStatus.CONVERTED,
                        PredictionLifecycleStatus.CANCELLED));
        map.put(PredictionLifecycleStatus.CONVERTED, Set.of());
        map.put(PredictionLifecycleStatus.CANCELLED, Set.of(PredictionLifecycleStatus.WATCH));
        ALLOWED_TRANSITIONS = Map.copyOf(map);
    }

    private CustomerOpportunityLifecyclePolicy() {
    }

    public static PredictionLifecycleStatus resolveRefreshStatus(
            final PredictionLifecycleStatus currentStatus) {
        if (currentStatus == null) {
            return PredictionLifecycleStatus.WATCH;
        }
        return currentStatus;
    }

    public static LifecycleDecision transition(
            final PredictionLifecycleStatus currentStatus,
            final PredictionLifecycleStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return LifecycleDecision.denied(null, "状态不能为空");
        }
        if (currentStatus == targetStatus) {
            return LifecycleDecision.allowed(targetStatus);
        }
        Set<PredictionLifecycleStatus> allowedTargets =
                ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            return LifecycleDecision.denied(
                    currentStatus,
                    "不允许从 " + currentStatus + " 切换到 " + targetStatus + ", 合法目标: " + allowedTargets);
        }
        return LifecycleDecision.allowed(targetStatus);
    }

    public static LifecycleDecision convert(
            final PredictionLifecycleStatus currentStatus,
            final Long projectId) {
        if (projectId == null || projectId <= 0L) {
            return LifecycleDecision.denied(currentStatus, "projectId 必须为正数");
        }
        return transition(currentStatus, PredictionLifecycleStatus.CONVERTED);
    }

    public record LifecycleDecision(
            boolean allowed,
            PredictionLifecycleStatus nextStatus,
            String reason) {

        public static LifecycleDecision allowed(final PredictionLifecycleStatus nextStatus) {
            return new LifecycleDecision(true, nextStatus, "");
        }

        public static LifecycleDecision denied(
                final PredictionLifecycleStatus nextStatus,
                final String reason) {
            return new LifecycleDecision(false, nextStatus, reason);
        }
    }

    public enum PredictionLifecycleStatus {
        WATCH,
        RECOMMEND,
        CONVERTED,
        CANCELLED
    }
}
