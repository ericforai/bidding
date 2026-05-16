package com.xiyu.bid.organization.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationUpsertPolicyTest {

    @Test
    void shouldInsertOnCreateWhenNotExisting() {
        var result = OrganizationUpsertPolicy.decideDeptUpsert("CREATE", false);
        assertThat(result).isEqualTo(OrganizationUpsertPolicy.UpsertDecision.INSERT_ACTIVE);
    }

    @Test
    void shouldSkipOnCreateWhenAlreadyExists() {
        var result = OrganizationUpsertPolicy.decideDeptUpsert("CREATE", true);
        assertThat(result).isEqualTo(OrganizationUpsertPolicy.UpsertDecision.SKIP_EXISTS);
    }

    @Test
    void shouldUpdateOnUpdateWhenExisting() {
        var result = OrganizationUpsertPolicy.decideDeptUpsert("UPDATE", true);
        assertThat(result).isEqualTo(OrganizationUpsertPolicy.UpsertDecision.UPDATE_EXISTING);
    }

    @Test
    void shouldInsertOnUpdateWhenNotExisting() {
        var result = OrganizationUpsertPolicy.decideDeptUpsert("UPDATE", false);
        assertThat(result).isEqualTo(OrganizationUpsertPolicy.UpsertDecision.INSERT_ACTIVE);
    }

    @Test
    void shouldMarkDeletedWhenExisting() {
        var result = OrganizationUpsertPolicy.decideDeptUpsert("DELETE", true);
        assertThat(result).isEqualTo(OrganizationUpsertPolicy.UpsertDecision.MARK_DELETED);
    }

    @Test
    void shouldSkipDeleteWhenAbsent() {
        var result = OrganizationUpsertPolicy.decideDeptUpsert("DELETE", false);
        assertThat(result).isEqualTo(OrganizationUpsertPolicy.UpsertDecision.SKIP_ABSENT);
    }

    @Test
    void shouldReturnUnknownForUnexpectedEventType() {
        var result = OrganizationUpsertPolicy.decideDeptUpsert("RENAME", false);
        assertThat(result).isEqualTo(OrganizationUpsertPolicy.UpsertDecision.UNKNOWN_EVENT_TYPE);
    }

    @Test
    void shouldDeriveInactiveOnLookbackMissForPreviouslyActive() {
        var result = OrganizationUpsertPolicy.deriveStatusOnLookbackMiss(true);
        assertThat(result).isEqualTo("INACTIVE");
    }

    @Test
    void shouldDeriveDeletedOnLookbackMissForPreviouslyInactive() {
        var result = OrganizationUpsertPolicy.deriveStatusOnLookbackMiss(false);
        assertThat(result).isEqualTo("DELETED");
    }
}
