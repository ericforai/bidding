package com.xiyu.bid.organization.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventDeduplicationPolicyTest {

    @Test
    void shouldCreateSameDedupKeyForSameTriple() {
        var key1 = EventDeduplicationPolicy.dedupKey("trace1", "span1", "BaseOssDept");
        var key2 = EventDeduplicationPolicy.dedupKey("trace1", "span1", "BaseOssDept");

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }

    @Test
    void shouldCreateDifferentKeyForDifferentTriples() {
        var key1 = EventDeduplicationPolicy.dedupKey("trace1", "span1", "BaseOssDept");
        var key2 = EventDeduplicationPolicy.dedupKey("trace2", "span1", "BaseOssDept");
        var key3 = EventDeduplicationPolicy.dedupKey("trace1", "span2", "BaseOssDept");
        var key4 = EventDeduplicationPolicy.dedupKey("trace1", "span1", "BaseOssUser");

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).isNotEqualTo(key3);
        assertThat(key1).isNotEqualTo(key4);
    }

    @Test
    void shouldRejectNullArguments() {
        assertThatThrownBy(() -> EventDeduplicationPolicy.dedupKey(null, "s", "t"))
                .isInstanceOf(EventValidationException.class);
        assertThatThrownBy(() -> EventDeduplicationPolicy.dedupKey("t", null, "t"))
                .isInstanceOf(EventValidationException.class);
        assertThatThrownBy(() -> EventDeduplicationPolicy.dedupKey("t", "s", null))
                .isInstanceOf(EventValidationException.class);
    }
}
