package com.xiyu.bid.integration.organization.infrastructure.client;

import com.xiyu.bid.integration.organization.application.OrganizationIntegrationProperties;
import com.xiyu.bid.integration.organization.domain.OrganizationDirectoryLookupContext;
import org.springframework.http.HttpHeaders;

public class OrganizationDirectoryAuthHeaders {
    private final OrganizationIntegrationProperties.Directory directory;

    OrganizationDirectoryAuthHeaders(OrganizationIntegrationProperties.Directory directory) {
        this.directory = directory;
    }

    HttpHeaders headers(OrganizationDirectoryLookupContext context) {
        HttpHeaders headers = new HttpHeaders();
        set(headers, directory.getTraceHeaderName(), value(context.traceId()));
        set(headers, directory.getSourceHeaderName(), firstPresent(directory.getSourceApp(), context.sourceApp()));
        set(headers, directory.getAuthHeaderName(), directory.getAuthToken());
        return headers;
    }

    private void set(HttpHeaders headers, String name, String value) {
        if (!isBlank(name) && !isBlank(value)) {
            headers.set(name.trim(), value.trim());
        }
    }

    private String firstPresent(String preferred, String fallback) {
        return isBlank(preferred) ? value(fallback) : preferred.trim();
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
