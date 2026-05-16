package com.xiyu.bid.organization.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationPolicyTest {

    @Test
    void shouldClassifyAllDiffTypes() {
        assertThat(ReconciliationPolicy.DiffType.values())
                .containsExactlyInAnyOrder(
                        ReconciliationPolicy.DiffType.MISSING_IN_XIYU,
                        ReconciliationPolicy.DiffType.MISSING_LOCALLY,
                        ReconciliationPolicy.DiffType.FIELD_MISMATCH,
                        ReconciliationPolicy.DiffType.MATCH);
    }
}
