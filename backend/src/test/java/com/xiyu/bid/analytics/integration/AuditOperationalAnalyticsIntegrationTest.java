package com.xiyu.bid.analytics.integration;

import com.xiyu.bid.support.NoOpPasswordEncryptionTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@Import(NoOpPasswordEncryptionTestConfig.class)
class AuditOperationalAnalyticsIntegrationTest extends AbstractAuditOperationalAnalyticsIntegrationTest {

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void auditEndpoint_ShouldReturnFilteredLogsAndSummary() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .param("module", "project")
                        .param("action", "export")
                        .param("keyword", "package"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].actionType").value("export"))
                .andExpect(jsonPath("$.data.items[0].module").value("project"))
                .andExpect(jsonPath("$.data.items[0].target").value(project.getId().toString()))
                .andExpect(jsonPath("$.data.summary.failedCount").value(0))
                .andExpect(jsonPath("$.data.summary.totalCount").value(1));

        mockMvc.perform(get("/api/audit")
                        .param("status", "failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("failed"))
                .andExpect(jsonPath("$.data.summary.failedCount").value(1));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void analyticsEndpoints_ShouldReturnRealProductLinesAndDrillDownData() throws Exception {
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summaryStats.totalTenders").value(2))
                .andExpect(jsonPath("$.data.summaryStats.activeProjects").value(1))
                .andExpect(jsonPath("$.data.summaryStats.pendingTasks").value(0))
                .andExpect(jsonPath("$.data.statusDistribution.BIDDED").value(1))
                .andExpect(jsonPath("$.data.topCompetitors[0].name").value("中国政府采购网"));

        resetStatistics();
        mockMvc.perform(get("/api/analytics/product-lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].name", hasItem("智慧办公")));
        assertQueryCountAtMost(8);

        resetStatistics();
        mockMvc.perform(get("/api/analytics/drilldown/revenue")
                        .param("status", "BIDDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metricKey").value("revenue"))
                .andExpect(jsonPath("$.data.items[0].title").value("智慧办公平台采购"))
                .andExpect(jsonPath("$.data.summary.totalCount").value(1));
        assertQueryCountAtMost(2);

        resetStatistics();
        mockMvc.perform(get("/api/analytics/drill-down")
                        .param("type", "trend")
                        .param("key", currentMonthKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.totalParticipation").value(2))
                .andExpect(jsonPath("$.data.projects[0].name").value("智慧办公实施项目"));
        assertQueryCountAtMost(8);

        resetStatistics();
        mockMvc.perform(get("/api/analytics/drill-down")
                        .param("type", "competitor")
                        .param("key", "中国政府采购网"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.totalParticipation").value(2))
                .andExpect(jsonPath("$.data.files[0].name").value("智慧办公实施项目_export.json"));
        assertQueryCountAtMost(8);

        resetStatistics();
        mockMvc.perform(get("/api/analytics/drilldown/projects")
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metricKey").value("projects"))
                .andExpect(jsonPath("$.data.items[0].title").value("智慧办公实施项目"))
                .andExpect(jsonPath("$.data.summary.activeCount").value(1));
        assertQueryCountAtMost(2);

        resetStatistics();
        mockMvc.perform(get("/api/analytics/drilldown/team")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metricKey").value("team"))
                .andExpect(jsonPath("$.data.items[0].title").value("审计管理员"))
                .andExpect(jsonPath("$.data.items[0].count").value(1))
                .andExpect(jsonPath("$.data.items[0].managedProjectCount").value(1))
                .andExpect(jsonPath("$.data.summary.totalCompletedTasks").value(0));
        assertQueryCountAtMost(5);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void analyticsEndpoints_ShouldReturnWinRateAndTeamDrillDownData() throws Exception {
        mockMvc.perform(get("/api/analytics/drilldown/win-rate")
                        .param("outcome", "WON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metricKey").value("win-rate"))
                .andExpect(jsonPath("$.data.items[0].title").value("智慧办公平台采购"))
                .andExpect(jsonPath("$.data.summary.totalCount").value(1))
                .andExpect(jsonPath("$.data.summary.wonCount").value(1));

        mockMvc.perform(get("/api/analytics/drilldown/team")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metricKey").value("team"))
                .andExpect(jsonPath("$.data.items[0].title").value("审计管理员"))
                .andExpect(jsonPath("$.data.items[0].count").value(1))
                .andExpect(jsonPath("$.data.items[0].managedProjectCount").value(1))
                .andExpect(jsonPath("$.data.summary.totalCompletedTasks").value(0));
    }
}
