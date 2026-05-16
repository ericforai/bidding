package com.xiyu.bid.organization.domain;

import java.util.Optional;

public final class OrganizationUpsertPolicy {

    private OrganizationUpsertPolicy() {
    }

    /**
     * Determines whether an incoming department event should create or update
     * the local record, and what the resulting status should be.
     */
    public static UpsertDecision decideDeptUpsert(String eventType, boolean existingRecord) {
        return switch (eventType) {
            case "CREATE" -> existingRecord
                    ? UpsertDecision.SKIP_EXISTS
                    : UpsertDecision.INSERT_ACTIVE;
            case "UPDATE" -> existingRecord
                    ? UpsertDecision.UPDATE_EXISTING
                    : UpsertDecision.INSERT_ACTIVE;
            case "DELETE" -> existingRecord
                    ? UpsertDecision.MARK_DELETED
                    : UpsertDecision.SKIP_ABSENT;
            default -> UpsertDecision.UNKNOWN_EVENT_TYPE;
        };
    }

    /**
     * Determines the new status for an organization entity when its upstream
     * data is no longer returned by the lookback API.
     */
    public static String deriveStatusOnLookbackMiss(boolean previouslyActive) {
        return previouslyActive ? "INACTIVE" : "DELETED";
    }

    public enum UpsertDecision {
        INSERT_ACTIVE,
        UPDATE_EXISTING,
        SKIP_EXISTS,
        SKIP_ABSENT,
        MARK_DELETED,
        UNKNOWN_EVENT_TYPE
    }
}
