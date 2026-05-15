package com.xiyu.bid.integration.organization.infrastructure.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.integration.organization.application.OrganizationDirectoryGateway;
import com.xiyu.bid.integration.organization.application.OrganizationIntegrationProperties;
import com.xiyu.bid.integration.organization.domain.OrganizationDepartmentSnapshot;
import com.xiyu.bid.integration.organization.domain.OrganizationDirectoryLookupContext;
import com.xiyu.bid.integration.organization.domain.OrganizationDirectoryResponseDecision;
import com.xiyu.bid.integration.organization.domain.OrganizationDirectoryResponseOutcome;
import com.xiyu.bid.integration.organization.domain.OrganizationDirectoryResponsePolicy;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Conditional(OrganizationDirectoryBaseUrlConfiguredCondition.class)
public class OrganizationDirectoryHttpGateway implements OrganizationDirectoryGateway {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OrganizationIntegrationProperties.Directory directory;
    private final OrganizationDirectoryJsonMapper mapper = new OrganizationDirectoryJsonMapper();
    private final OrganizationDirectoryAuthHeaders authHeaders;

    @Autowired
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
        this.authHeaders = new OrganizationDirectoryAuthHeaders(directory);
    }

    @Override
    public Optional<OrganizationDepartmentSnapshot> fetchDepartmentByDeptId(String deptId) {
        return fetchDepartmentByDeptId(deptId, OrganizationDirectoryLookupContext.empty());
    }

    @Override
    public Optional<OrganizationDepartmentSnapshot> fetchDepartmentByDeptId(
            String deptId,
            OrganizationDirectoryLookupContext context
    ) {
        return getJson(url(directory.getDepartmentDetailPath(), Map.of("deptId", deptId)), context)
                .map(mapper::department);
    }

    @Override
    public Optional<OrganizationUserSnapshot> fetchUserByUserId(String userId) {
        return fetchUserByUserId(userId, OrganizationDirectoryLookupContext.empty());
    }

    @Override
    public Optional<OrganizationUserSnapshot> fetchUserByUserId(String userId, OrganizationDirectoryLookupContext context) {
        return getJson(url(directory.getUserDetailPath(), Map.of("userId", userId)), context)
                .map(mapper::user);
    }

    @Override
    public List<OrganizationDepartmentSnapshot> listDepartmentsByWindow(LocalDateTime startAt, LocalDateTime endAt) {
        return listDepartmentsByWindow(startAt, endAt, OrganizationDirectoryLookupContext.empty());
    }

    @Override
    public List<OrganizationDepartmentSnapshot> listDepartmentsByWindow(
            LocalDateTime startAt,
            LocalDateTime endAt,
            OrganizationDirectoryLookupContext context
    ) {
        return getJson(windowUrl(directory.getDepartmentWindowPath(), startAt, endAt), context)
                .map(mapper::departments)
                .orElse(List.of());
    }

    @Override
    public List<OrganizationUserSnapshot> listUsersByWindow(LocalDateTime startAt, LocalDateTime endAt) {
        return listUsersByWindow(startAt, endAt, OrganizationDirectoryLookupContext.empty());
    }

    @Override
    public List<OrganizationUserSnapshot> listUsersByWindow(
            LocalDateTime startAt,
            LocalDateTime endAt,
            OrganizationDirectoryLookupContext context
    ) {
        return getJson(windowUrl(directory.getUserWindowPath(), startAt, endAt), context)
                .map(mapper::users)
                .orElse(List.of());
    }

    private Optional<JsonNode> getJson(String url, OrganizationDirectoryLookupContext context) {
        if (url.isBlank()) {
            return Optional.empty();
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders.headers(context == null ? OrganizationDirectoryLookupContext.empty() : context)),
                    String.class
            );
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            return classify(root).outcome() == OrganizationDirectoryResponseOutcome.SUCCESS
                    ? Optional.of(root)
                    : Optional.empty();
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw OrganizationDirectoryHttpGatewayException.nonRetryable("组织架构主数据接口拒绝请求", ex);
            }
            throw OrganizationDirectoryHttpGatewayException.retryable("组织架构主数据接口调用失败", ex);
        } catch (JsonProcessingException | RestClientException ex) {
            throw OrganizationDirectoryHttpGatewayException.retryable("组织架构主数据接口调用失败", ex);
        }
    }

    private OrganizationDirectoryResponseDecision classify(JsonNode root) {
        JsonNode code = root.path("code");
        if (!code.isValueNode() || code.isNull()) {
            return new OrganizationDirectoryResponseDecision(OrganizationDirectoryResponseOutcome.SUCCESS, false, "success");
        }
        OrganizationDirectoryResponseDecision decision = OrganizationDirectoryResponsePolicy.classify(code.asText(), hasData(root));
        if (decision.retryable()) {
            throw OrganizationDirectoryHttpGatewayException.retryable(decision.message(), null);
        }
        if (decision.outcome() == OrganizationDirectoryResponseOutcome.NON_RETRYABLE_FAILURE) {
            throw OrganizationDirectoryHttpGatewayException.nonRetryable(decision.message(), null);
        }
        return decision;
    }

    private boolean hasData(JsonNode root) {
        return hasPayload(root.path("data")) || hasPayload(root.path("result"));
    }

    private boolean hasPayload(JsonNode data) {
        return !data.isMissingNode() && !data.isNull() && (!data.isContainerNode() || data.size() > 0);
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
