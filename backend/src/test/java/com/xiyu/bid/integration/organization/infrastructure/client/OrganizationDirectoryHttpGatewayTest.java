package com.xiyu.bid.integration.organization.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.integration.organization.application.OrganizationIntegrationProperties;
import com.xiyu.bid.integration.organization.domain.OrganizationDirectoryLookupContext;
import com.xiyu.bid.integration.organization.domain.OrganizationDepartmentSnapshot;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("OrganizationDirectoryHttpGateway - customer org master data client")
class OrganizationDirectoryHttpGatewayTest {

    @Test
    @DisplayName("fetches user master data by immutable userId")
    void fetchUserByUserId_mapsUserSnapshot() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://oss.example.test/users/720518523"))
                .andRespond(withSuccess("""
                        {
                          "code": "200",
                          "data": {
                            "userId": 720518523,
                            "userNo": "wangwu",
                            "userName": "王五",
                            "email": "wangwu@example.com",
                            "mobile": "13900000000",
                            "deptId": 3730158,
                            "deptName": "销售部",
                            "status": "ACTIVE"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<OrganizationUserSnapshot> snapshot = gateway(restTemplate).fetchUserByUserId("720518523");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().externalUserId()).isEqualTo("720518523");
        assertThat(snapshot.get().username()).isEqualTo("wangwu");
        assertThat(snapshot.get().email()).isEqualTo("wangwu@example.com");
        assertThat(snapshot.get().departmentCode()).isEqualTo("3730158");
        assertThat(snapshot.get().enabled()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("sends trace source and auth headers to YAPI gateway")
    void fetchUserByUserId_sendsTraceSourceAndAuthHeaders() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://oss.example.test/users/720518523"))
                .andExpect(header("EHSY-TraceID", "trace-1"))
                .andExpect(header("EHSY-SRCAPP", "BidSystem"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("""
                        {
                          "code": "200",
                          "data": {
                            "userId": 720518523,
                            "userNo": "wangwu",
                            "userName": "王五",
                            "email": "wangwu@example.com",
                            "mobile": "13900000000",
                            "deptId": 3730158
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        OrganizationIntegrationProperties properties = properties();
        properties.getDirectory().setSourceApp("BidSystem");
        properties.getDirectory().setAuthHeaderName("Authorization");
        properties.getDirectory().setAuthToken("Bearer test-token");

        Optional<OrganizationUserSnapshot> snapshot = new OrganizationDirectoryHttpGateway(
                restTemplate,
                new ObjectMapper(),
                properties
        ).fetchUserByUserId("720518523", new OrganizationDirectoryLookupContext("trace-1", "oss"));

        assertThat(snapshot).isPresent();
        server.verify();
    }

    @Test
    @DisplayName("maps code 200 result envelope as successful payload")
    void fetchUserByUserId_code200ResultEnvelope_mapsUserSnapshot() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://oss.example.test/users/720518523"))
                .andRespond(withSuccess("""
                        {
                          "code": "200",
                          "result": {
                            "userId": 720518523,
                            "userNo": "wangwu",
                            "userName": "王五",
                            "deptId": 3730158
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<OrganizationUserSnapshot> snapshot = gateway(restTemplate).fetchUserByUserId("720518523");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().externalUserId()).isEqualTo("720518523");
        server.verify();
    }

    @Test
    @DisplayName("maps 401 to non retryable gateway exception")
    void fetchUserByUserId_unauthorized_nonRetryable() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://oss.example.test/users/720518523"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> gateway(restTemplate).fetchUserByUserId("720518523"))
                .isInstanceOf(OrganizationDirectoryHttpGatewayException.class)
                .satisfies(ex -> assertThat(((OrganizationDirectoryHttpGatewayException) ex).retryable()).isFalse());
        server.verify();
    }

    @Test
    @DisplayName("fetches department master data by immutable deptId")
    void fetchDepartmentByDeptId_mapsDepartmentSnapshot() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://oss.example.test/departments/3730158"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "deptId": 3730158,
                            "deptName": "销售部",
                            "parentDeptId": 1000,
                            "enabled": true
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<OrganizationDepartmentSnapshot> snapshot = gateway(restTemplate).fetchDepartmentByDeptId("3730158");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().externalDeptId()).isEqualTo("3730158");
        assertThat(snapshot.get().departmentCode()).isEqualTo("3730158");
        assertThat(snapshot.get().departmentName()).isEqualTo("销售部");
        assertThat(snapshot.get().parentExternalDeptId()).isEqualTo("1000");
        server.verify();
    }

    @Test
    @DisplayName("maps 404 response to empty so caller can disable local record")
    void fetchUserByUserId_notFound_returnsEmpty() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://oss.example.test/users/720518523"))
                .andRespond(withResourceNotFound());

        assertThat(gateway(restTemplate).fetchUserByUserId("720518523")).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("lists changed users by configured window endpoint")
    void listUsersByWindow_mapsUserRecords() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://oss.example.test/users/window?startAt=2026-05-01T10:00&endAt=2026-05-02T10:30"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "records": [
                              {
                                "userId": 720518523,
                                "userNo": "wangwu",
                                "userName": "王五",
                                "email": "wangwu@example.com",
                                "mobile": "13900000000",
                                "deptId": 3730158
                              },
                              {
                                "userId": 720518524,
                                "userNo": "lisi",
                                "userName": "李四",
                                "email": "lisi@example.com",
                                "mobile": "13900000001",
                                "deptId": 3730158
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<OrganizationUserSnapshot> snapshots = gateway(restTemplate)
                .listUsersByWindow(LocalDateTime.parse("2026-05-01T10:00"), LocalDateTime.parse("2026-05-02T10:30"));

        assertThat(snapshots).extracting(OrganizationUserSnapshot::externalUserId)
                .containsExactly("720518523", "720518524");
        assertThat(snapshots).extracting(OrganizationUserSnapshot::email)
                .containsExactly("wangwu@example.com", "lisi@example.com");
        server.verify();
    }

    @Test
    @DisplayName("lists changed departments by configured window endpoint")
    void listDepartmentsByWindow_mapsDepartmentItems() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://oss.example.test/departments/window?startAt=2026-05-01T10:00&endAt=2026-05-02T10:30"))
                .andRespond(withSuccess("""
                        {
                          "result": {
                            "items": [
                              {
                                "deptId": 3730158,
                                "deptName": "销售部",
                                "parentDeptId": 1000
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<OrganizationDepartmentSnapshot> snapshots = gateway(restTemplate)
                .listDepartmentsByWindow(LocalDateTime.parse("2026-05-01T10:00"), LocalDateTime.parse("2026-05-02T10:30"));

        assertThat(snapshots).extracting(OrganizationDepartmentSnapshot::externalDeptId).containsExactly("3730158");
        assertThat(snapshots).extracting(OrganizationDepartmentSnapshot::parentExternalDeptId).containsExactly("1000");
        server.verify();
    }

    private OrganizationDirectoryHttpGateway gateway(RestTemplate restTemplate) {
        return new OrganizationDirectoryHttpGateway(restTemplate, new ObjectMapper(), properties());
    }

    private OrganizationIntegrationProperties properties() {
        OrganizationIntegrationProperties properties = new OrganizationIntegrationProperties();
        properties.getDirectory().setBaseUrl("https://oss.example.test");
        properties.getDirectory().setUserDetailPath("/users/{userId}");
        properties.getDirectory().setDepartmentDetailPath("/departments/{deptId}");
        properties.getDirectory().setUserWindowPath("/users/window?startAt={startAt}&endAt={endAt}");
        properties.getDirectory().setDepartmentWindowPath("/departments/window?startAt={startAt}&endAt={endAt}");
        return properties;
    }
}
