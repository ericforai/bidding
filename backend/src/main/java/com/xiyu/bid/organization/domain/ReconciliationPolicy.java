package com.xiyu.bid.organization.domain;

/**
 * Classifies differences found during daily reconciliation between local
 * organization data and the Xiyu source-of-truth API.
 */
public final class ReconciliationPolicy {

    private ReconciliationPolicy() {
    }

    public enum DiffType {
        /** Entity exists locally but not in the Xiyu API. */
        MISSING_IN_XIYU,
        /** Entity exists in Xiyu but not locally. */
        MISSING_LOCALLY,
        /** Entity exists in both but fields differ. */
        FIELD_MISMATCH,
        /** No difference. */
        MATCH
    }
}
