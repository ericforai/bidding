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
import com.xiyu.bid.service.AuditLogService;
import com.xiyu.bid.service.IAuditLogService;
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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
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
    private ProjectDocumentRepository projectDocumentRepository;

    @Autowired
    private DocumentExportRepository documentExportRepository;

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

        adminUser = userRepository.save(User.builder()
                .username("audit-admin")
                .password("XiyuDemo!2026")
                .email("audit-admin@example.com")
                .fullName("审计管理员")
                .role(User.Role.ADMIN)
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
        mockMvc.perform(get("/api/analytics/product-lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].name", hasItem("智慧办公")));

        mockMvc.perform(get("/api/analytics/drill-down")
                        .param("type", "trend")
                        .param("key", currentMonthKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.totalParticipation").value(2))
                .andExpect(jsonPath("$.data.projects[0].name").value("智慧办公实施项目"));

        mockMvc.perform(get("/api/analytics/drill-down")
                        .param("type", "competitor")
                        .param("key", "中国政府采购网"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.totalParticipation").value(2))
                .andExpect(jsonPath("$.data.files[0].name").value("智慧办公实施项目_export.json"));

        mockMvc.perform(get("/api/analytics/drilldown/projects")
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metricKey").value("projects"))
                .andExpect(jsonPath("$.data.items[0].title").value("智慧办公实施项目"))
                .andExpect(jsonPath("$.data.summary.activeCount").value(1));

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
