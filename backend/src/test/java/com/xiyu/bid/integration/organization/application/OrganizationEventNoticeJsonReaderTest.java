package com.xiyu.bid.integration.organization.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.integration.organization.domain.OrganizationEventNoticeParseResult;
import com.xiyu.bid.integration.organization.domain.OrganizationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizationEventNoticeJsonReader - JSON shell parsing")
class OrganizationEventNoticeJsonReaderTest {
    private final OrganizationEventNoticeJsonReader reader = new OrganizationEventNoticeJsonReader(new ObjectMapper());

    @Test
    @DisplayName("parses escaped JSON values through Jackson before pure validation")
    void parse_escapedJsonValue() {
        OrganizationEventNoticeParseResult result = reader.parse("""
                {
                  "traceId": "trace-1",
                  "spanId": "span-1",
                  "parentId": "parent-1",
                  "eventSource": "customer-org",
                  "eventTopic": "BaseOssUser",
                  "time": "2026-04-30T10:15:30+08:00",
                  "key": "evt-1",
                  "data": {"userId": "U\\\\u0030001"}
                }
                """);

        assertThat(result.valid()).isTrue();
        assertThat(result.notice().topic()).isEqualTo(OrganizationEventType.USER_NOTICE);
        assertThat(result.notice().subjectId()).isEqualTo("U\\u0030001");
    }

    @Test
    @DisplayName("rejects malformed JSON without throwing")
    void parse_malformedJson_returnsInvalid() {
        OrganizationEventNoticeParseResult result = reader.parse("{\"eventTopic\":\"BaseOssUser\"");

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("JSON");
    }
}
