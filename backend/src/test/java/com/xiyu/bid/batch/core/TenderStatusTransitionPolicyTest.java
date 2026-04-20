package com.xiyu.bid.batch.core;

import com.xiyu.bid.entity.Tender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenderStatusTransitionPolicyTest {

    private final TenderStatusTransitionPolicy policy = new TenderStatusTransitionPolicy();

    @Test
    void shouldAllowPendingToTracking() {
        assertTrue(policy.canTransition(Tender.Status.PENDING, Tender.Status.TRACKING));
        assertDoesNotThrow(() -> policy.assertTransition(Tender.Status.PENDING, Tender.Status.TRACKING));
    }

    @Test
    void shouldAllowTrackingToBidded() {
        assertTrue(policy.canTransition(Tender.Status.TRACKING, Tender.Status.BIDDED));
    }

    @Test
    void shouldRejectBiddedRollbackToTracking() {
        assertFalse(policy.canTransition(Tender.Status.BIDDED, Tender.Status.TRACKING));
        assertThrows(IllegalArgumentException.class,
                () -> policy.assertTransition(Tender.Status.BIDDED, Tender.Status.TRACKING));
    }

    @Test
    void shouldAllowAbandonedRecoveryToPending() {
        assertTrue(policy.canTransition(Tender.Status.ABANDONED, Tender.Status.PENDING));
    }
}
