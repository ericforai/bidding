package com.xiyu.bid.organization.domain;

public final class EventDeduplicationPolicy {

    private EventDeduplicationPolicy() {
    }

    /**
     * Constructs the deduplication key from traceId, spanId, and eventTopic.
     * Duplicate events with the same triple are skipped regardless of the
     * event payload differences.
     */
    public static DedupKey dedupKey(String traceId, String spanId, String eventTopic) {
        if (traceId == null || spanId == null || eventTopic == null) {
            throw new EventValidationException("traceId, spanId, eventTopic must not be null");
        }
        return new DedupKey(traceId, spanId, eventTopic);
    }

    public record DedupKey(String traceId, String spanId, String eventTopic) {
    }
}
