package com.xiyu.bid.batch.core;

import com.xiyu.bid.entity.Tender;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class TenderStatusTransitionPolicy {

    public boolean canTransition(Tender.Status currentStatus, Tender.Status targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return true;
        }
        return allowedTargets(currentStatus).contains(targetStatus);
    }

    public void assertTransition(Tender.Status currentStatus, Tender.Status targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            throw new IllegalArgumentException(
                    String.format("Tender status cannot transition from %s to %s", currentStatus, targetStatus)
            );
        }
    }

    private Set<Tender.Status> allowedTargets(Tender.Status currentStatus) {
        return switch (currentStatus) {
            case PENDING -> EnumSet.of(Tender.Status.TRACKING, Tender.Status.ABANDONED);
            case TRACKING -> EnumSet.of(Tender.Status.PENDING, Tender.Status.BIDDED, Tender.Status.ABANDONED);
            case BIDDED -> EnumSet.noneOf(Tender.Status.class);
            case ABANDONED -> EnumSet.of(Tender.Status.PENDING);
        };
    }
}
