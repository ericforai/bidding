package com.xiyu.bid.integration.organization.infrastructure.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.integration.organization.application.OrganizationDirectoryGateway;
import com.xiyu.bid.integration.organization.application.OrganizationIntegrationProperties;
import com.xiyu.bid.integration.organization.domain.OrganizationDepartmentSnapshot;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "xiyu.integrations.organization.directory", name = "base-url")
public class OrganizationDirectoryHttpGateway implements OrganizationDirectoryGateway {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OrganizationIntegrationProperties.Directory directory;
    private final OrganizationDirectoryJsonMapper mapper = new OrganizationDirectoryJsonMapper();

    public OrganizationDirectoryHttpGateway(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            OrganizationIntegrationProperties properties
    ) {
        this(restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getDirectory().getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getDirectory().getReadTimeoutMs()))
                .build(), objectMapper, properties);
    }

    OrganizationDirectoryHttpGateway(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            OrganizationIntegrationProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.directory = properties.getDirectory();
    }

    @Override
    public Optional<OrganizationDepartmentSnapshot> fetchDepartmentByDeptId(String deptId) {
        return getJson(url(directory.getDepartmentDetailPath(), Map.of("deptId", deptId)))
                .map(mapper::department);
    }

    @Override
    public Optional<OrganizationUserSnapshot> fetchUserByUserId(String userId) {
        return getJson(url(directory.getUserDetailPath(), Map.of("userId", userId)))
                .map(mapper::user);
    }

    @Override
    public List<OrganizationDepartmentSnapshot> listDepartmentsByWindow(LocalDateTime startAt, LocalDateTime endAt) {
        return getJson(windowUrl(directory.getDepartmentWindowPath(), startAt, endAt))
                .map(mapper::departments)
                .orElse(List.of());
    }

    @Override
    public List<OrganizationUserSnapshot> listUsersByWindow(LocalDateTime startAt, LocalDateTime endAt) {
        return getJson(windowUrl(directory.getUserWindowPath(), startAt, endAt))
                .map(mapper::users)
                .orElse(List.of());
    }

    private Optional<JsonNode> getJson(String url) {
        if (url.isBlank()) {
            return Optional.empty();
        }
        try {
            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(body));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (JsonProcessingException | RestClientException ex) {
            throw new OrganizationDirectoryHttpGatewayException("组织架构主数据接口调用失败", ex);
        }
    }

    private String url(String path, Map<String, ?> variables) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String base = path.startsWith("http") ? path : trimRight(directory.getBaseUrl()) + "/" + trimLeft(path);
        return UriComponentsBuilder.fromUriString(base)
                .buildAndExpand(variables)
                .toUriString();
    }

    private String windowUrl(String path, LocalDateTime startAt, LocalDateTime endAt) {
        if (path == null || path.isBlank()) {
            return "";
        }
        boolean hasStartAt = path.contains("{startAt}");
        boolean hasEndAt = path.contains("{endAt}");
        String expanded = url(path, Map.of("startAt", startAt.toString(), "endAt", endAt.toString()));
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(expanded);
        if (!hasStartAt) {
            builder.queryParam("startAt", startAt);
        }
        if (!hasEndAt) {
            builder.queryParam("endAt", endAt);
        }
        return builder.toUriString();
    }

    private String trimLeft(String value) {
        return value == null ? "" : value.replaceFirst("^/+", "");
    }

    private String trimRight(String value) {
        return value == null ? "" : value.replaceFirst("/+$", "");
    }
}
