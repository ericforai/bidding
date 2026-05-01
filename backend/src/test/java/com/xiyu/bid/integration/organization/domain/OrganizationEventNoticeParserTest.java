package com.xiyu.bid.integration.organization.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizationEventNoticeParser - pure notice parsing")
class OrganizationEventNoticeParserTest {

    @Test
    @DisplayName("parses BaseOssDept notice and keeps only notification identity")
    void parse_acceptsBaseOssDeptNotice() {
        OrganizationEventNoticeParseResult result = OrganizationEventNoticeParser.parse(new OrganizationEventNoticeFields(
                "trace-1", "span-1", "parent-1", "customer-org", "BaseOssDept",
                "2026-04-30T10:15:30+08:00", "evt-1", "D001", ""
        ));

        assertThat(result.valid()).isTrue();
        assertThat(result.notice().topic()).isEqualTo(OrganizationEventType.DEPARTMENT_NOTICE);
        assertThat(result.notice().subjectId()).isEqualTo("D001");
    }

    @Test
    @DisplayName("rejects missing topic-specific data id")
    void parse_rejectsMissingUserId() {
        OrganizationEventNoticeParseResult result = OrganizationEventNoticeParser.parse(new OrganizationEventNoticeFields(
                "trace-1", "span-1", "parent-1", "customer-org", "BaseOssUser",
                "2026-04-30T10:15:30+08:00", "evt-1", "D001", ""
        ));

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("userId");
    }

    @Test
    @DisplayName("rejects unknown event topic")
    void parse_rejectsUnknownTopic() {
        OrganizationEventNoticeParseResult result = OrganizationEventNoticeParser.parse(new OrganizationEventNoticeFields(
                "trace-1", "span-1", "parent-1", "customer-org", "LegacyUser",
                "2026-04-30T10:15:30+08:00", "evt-1", "", "U001"
        ));

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("主题");
    }
}
