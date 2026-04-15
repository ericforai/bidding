package com.xiyu.bid.analytics.integration;

import com.xiyu.bid.documentexport.entity.DocumentExport;
import com.xiyu.bid.documentexport.repository.DocumentExportRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.repository.AuditLogRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.audit.service.AuditLogService;
import com.xiyu.bid.audit.service.IAuditLogService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
class AuditOperationalAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IAuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TenderRepository tenderRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.xiyu.bid.repository.RoleProfileRepository roleProfileRepository;

    @Autowired
    private ProjectDocumentRepository projectDocumentRepository;

    @Autowired
    private DocumentExportRepository documentExportRepository;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    private User adminUser;
    private Project project;
    private String currentMonthKey;

    @TestConfiguration
    static class TestBeans {
        @Bean(name = "passwordEncryptionUtil")
        @Primary
        PasswordEncryptionUtil passwordEncryptionUtil() {
            return new PasswordEncryptionUtil() {
                @Override
                public void initialize() {
                }

                @Override
                public String encrypt(String plainPassword) {
                    return plainPassword;
                }

                @Override
                public String decrypt(String encryptedPassword) {
                    return encryptedPassword;
                }

                @Override
                public boolean isKeyValid() {
                    return true;
                }
            };
        }
    }

    @BeforeEach
    void setUp() {
        documentExportRepository.deleteAll();
        projectDocumentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        tenderRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        roleProfileRepository.deleteAll();

        com.xiyu.bid.entity.RoleProfile defaultProfile = roleProfileRepository.save(com.xiyu.bid.entity.RoleProfile.builder()
                .code("audit-test-profile")
                .name("审计测试权限")
                .dataScope("self")
                .build());

        adminUser = userRepository.save(User.builder()
                .username("audit-admin")
                .password("XiyuDemo!2026")
                .email("audit-admin@example.com")
                .fullName("审计管理员")
                .role(User.Role.ADMIN)
                .roleProfile(defaultProfile)
                .enabled(true)
                .build());

        Tender officeTender = tenderRepository.save(Tender.builder()
                .title("智慧办公平台采购")
                .source("中国政府采购网")
                .budget(new BigDecimal("500000"))
                .status(Tender.Status.BIDDED)
                .aiScore(88)
                .riskLevel(Tender.RiskLevel.LOW)
                .build());

        Tender cloudTender = tenderRepository.save(Tender.builder()
                .title("云服务平台扩容")
                .source("中国政府采购网")
                .budget(new BigDecimal("800000"))
                .status(Tender.Status.TRACKING)
                .aiScore(75)
                .riskLevel(Tender.RiskLevel.MEDIUM)
                .build());

        currentMonthKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        project = projectRepository.save(Project.builder()
                .name("智慧办公实施项目")
                .tenderId(officeTender.getId())
                .status(Project.Status.BIDDING)
                .managerId(adminUser.getId())
                .teamMembers(List.of(adminUser.getId()))
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(10))
                .build());

        taskRepository.save(Task.builder()
                .projectId(project.getId())
                .title("准备商务应答")
                .description("汇总商务偏离表")
                .assigneeId(adminUser.getId())
                .status(Task.Status.IN_PROGRESS)
                .priority(Task.Priority.HIGH)
                .dueDate(LocalDateTime.now().plusDays(2))
                .build());

        projectDocumentRepository.save(ProjectDocument.builder()
                .projectId(project.getId())
                .name("技术应答.docx")
                .size("2MB")
                .fileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .uploaderId(adminUser.getId())
                .uploaderName("审计管理员")
                .build());

        documentExportRepository.save(DocumentExport.builder()
                .projectId(project.getId())
                .structureId(1L)
                .projectName(project.getName())
                .format("json")
                .fileName("智慧办公实施项目_export.json")
                .contentType("application/json")
                .fileSize(5120L)
                .exportedBy(adminUser.getId())
                .exportedByName("审计管理员")
                .build());

        auditLogService.logSync(AuditLogService.AuditLogEntry.builder()
                .userId(String.valueOf(adminUser.getId()))
                .username(adminUser.getUsername())
                .action("EXPORT")
                .entityType("PROJECT")
                .entityId(String.valueOf(project.getId()))
                .description("Exported project package")
                .success(true)
                .build());

        auditLogService.logSync(AuditLogService.AuditLogEntry.builder()
                .userId(String.valueOf(adminUser.getId()))
                .username(adminUser.getUsername())
                .action("ARCHIVE")
                .entityType("PROJECT")
                .entityId(String.valueOf(project.getId()))
                .description("Archived final package")
                .success(true)
                .build());

        auditLogService.logSync(AuditLogService.AuditLogEntry.builder()
                .userId("unknown")
                .username("unknown")
                .action("LOGIN")
                .entityType("SYSTEM")
                .entityId("login")
                .description("Failed login attempt")
                .success(false)
                .errorMessage("Bad credentials")
                .build());

        resetStatistics();
    }

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
        assertQueryCountAtMost(4);
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

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void analyticsQueries_ShouldStayBelowNaiveBaselines() throws Exception {
        long productLineStatements = measureStatements(() -> mockMvc.perform(get("/api/analytics/product-lines"))
                .andExpect(status().isOk()));
        long productLineBaseline = measureStatements(this::runNaiveProductLineBaseline);
        assertThat(productLineStatements)
                .as("product-lines should use fewer SQL statements than the naive baseline")
                .isGreaterThan(0L)
                .isLessThan(productLineBaseline)
                .isLessThanOrEqualTo(4L);

        long drillDownStatements = measureStatements(() -> mockMvc.perform(get("/api/analytics/drill-down")
                .param("type", "trend")
                .param("key", currentMonthKey))
                .andExpect(status().isOk()));
        long drillDownBaseline = measureStatements(() -> runNaiveDrillDownBaseline("trend", currentMonthKey));
        assertThat(drillDownStatements)
                .as("drill-down should use fewer SQL statements than the naive baseline")
                .isGreaterThan(0L)
                .isLessThan(drillDownBaseline)
                .isLessThanOrEqualTo(8L);

        long winRateStatements = measureStatements(() -> mockMvc.perform(get("/api/analytics/drilldown/win-rate")
                .param("outcome", "WON"))
                .andExpect(status().isOk()));
        long winRateBaseline = measureStatements(this::runNaiveWinRateBaseline);
        assertThat(winRateStatements)
                .as("win-rate drill-down should use fewer SQL statements than the naive baseline")
                .isGreaterThan(0L)
                .isLessThan(winRateBaseline)
                .isLessThanOrEqualTo(8L);

        long teamStatements = measureStatements(() -> mockMvc.perform(get("/api/analytics/drilldown/team")
                .param("role", "ADMIN"))
                .andExpect(status().isOk()));
        long teamBaseline = measureStatements(this::runNaiveTeamBaseline);
        assertThat(teamStatements)
                .as("team drill-down should use fewer SQL statements than the naive baseline")
                .isGreaterThan(0L)
                .isLessThan(teamBaseline)
                .isLessThanOrEqualTo(8L);
    }

    private long measureStatements(ThrowingAction action) throws Exception {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        boolean previouslyEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            action.run();
            return statistics.getPrepareStatementCount();
        } finally {
            statistics.clear();
            statistics.setStatisticsEnabled(previouslyEnabled);
        }
    }

    private void resetStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    private void assertQueryCountAtMost(long expectedMax) {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        assertThat(statistics.getPrepareStatementCount())
                .as("SQL statement count should stay within the optimized threshold")
                .isLessThanOrEqualTo(expectedMax);
    }

    private void runNaiveProductLineBaseline() {
        tenderRepository.findAll().forEach(tender -> {
            tenderRepository.findById(tender.getId());
            projectRepository.findByTenderId(tender.getId());
        });
    }

    private void runNaiveDrillDownBaseline(String type, String key) {
        List<Tender> tenders = tenderRepository.findAll();
        List<Project> projects = projectRepository.findAll();
        List<Task> tasks = taskRepository.findAll();
        projectDocumentRepository.findAll();
        documentExportRepository.findAll();

        for (Tender tender : tenders) {
            tenderRepository.findById(tender.getId());
        }
        for (Project project : projects) {
            projectRepository.findById(project.getId());
            if (project.getTenderId() != null) {
                tenderRepository.findById(project.getTenderId());
            }
            projectDocumentRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());
            documentExportRepository.findByProjectIdOrderByExportedAtDesc(project.getId());
        }
        for (Task task : tasks) {
            taskRepository.findById(task.getId());
        }
        if ("trend".equals(type) && key != null) {
            tenderRepository.findAll().stream()
                    .filter(tender -> tender.getCreatedAt() != null)
                    .filter(tender -> tender.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")).equals(key))
                    .toList();
        }
    }

    private void runNaiveWinRateBaseline() {
        List<Tender> tenders = tenderRepository.findAll();
        List<Project> projects = projectRepository.findAll();
        userRepository.findAll();

        for (Tender tender : tenders) {
            tenderRepository.findById(tender.getId());
            projectRepository.findByTenderId(tender.getId());
        }
        for (Project project : projects) {
            if (project.getManagerId() != null) {
                userRepository.findById(project.getManagerId());
            }
            if (project.getTenderId() != null) {
                tenderRepository.findById(project.getTenderId());
            }
        }
    }

    private void runNaiveTeamBaseline() {
        List<Project> projects = projectRepository.findAll();
        List<Tender> tenders = tenderRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Task> tasks = taskRepository.findAll();

        for (Project project : projects) {
            projectRepository.findById(project.getId());
            if (project.getTenderId() != null) {
                tenderRepository.findById(project.getTenderId());
            }
            if (project.getManagerId() != null) {
                userRepository.findById(project.getManagerId());
            }
            taskRepository.findByProjectId(project.getId());
            projectDocumentRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());
            documentExportRepository.findByProjectIdOrderByExportedAtDesc(project.getId());
        }
        tenders.forEach(tender -> tenderRepository.findById(tender.getId()));
        users.forEach(user -> userRepository.findById(user.getId()));
        tasks.forEach(task -> taskRepository.findById(task.getId()));
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
