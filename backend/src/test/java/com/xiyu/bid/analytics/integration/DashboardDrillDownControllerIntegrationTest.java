package com.xiyu.bid.analytics.integration;

import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class DashboardDrillDownControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenderRepository tenderRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

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
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        tenderRepository.deleteAll();
        userRepository.deleteAll();

        User manager = userRepository.save(User.builder()
                .username("manager")
                .password("demo123")
                .email("manager@xiyu.test")
                .fullName("张经理")
                .role(User.Role.MANAGER)
                .enabled(true)
                .build());

        User staff = userRepository.save(User.builder()
                .username("staff")
                .password("demo123")
                .email("staff@xiyu.test")
                .fullName("李工")
                .role(User.Role.STAFF)
                .enabled(true)
                .build());

        Tender tenderA = tenderRepository.save(Tender.builder()
                .title("年度收入回归项目")
                .source("华北")
                .budget(new BigDecimal("800"))
                .status(Tender.Status.BIDDED)
                .aiScore(90)
                .deadline(LocalDateTime.of(2026, 3, 20, 18, 0))
                .build());

        Tender tenderB = tenderRepository.save(Tender.builder()
                .title("团队筛选回归项目")
                .source("华南")
                .budget(new BigDecimal("500"))
                .status(Tender.Status.TRACKING)
                .aiScore(82)
                .deadline(LocalDateTime.of(2026, 3, 25, 18, 0))
                .build());

        Project projectA = projectRepository.save(Project.builder()
                .name("收入下钻项目")
                .tenderId(tenderA.getId())
                .status(Project.Status.BIDDING)
                .managerId(manager.getId())
                .teamMembers(List.of(staff.getId()))
                .startDate(LocalDateTime.of(2026, 3, 11, 9, 0))
                .endDate(LocalDateTime.of(2026, 3, 18, 18, 0))
                .build());

        Project projectB = projectRepository.save(Project.builder()
                .name("团队下钻项目")
                .tenderId(tenderB.getId())
                .status(Project.Status.PREPARING)
                .managerId(manager.getId())
                .teamMembers(List.of(staff.getId()))
                .startDate(LocalDateTime.of(2026, 3, 12, 9, 0))
                .endDate(LocalDateTime.of(2026, 3, 24, 18, 0))
                .build());

        taskRepository.save(Task.builder()
                .projectId(projectA.getId())
                .title("编写技术方案")
                .assigneeId(staff.getId())
                .status(Task.Status.COMPLETED)
                .priority(Task.Priority.HIGH)
                .dueDate(LocalDateTime.of(2026, 3, 16, 18, 0))
                .build());

        taskRepository.save(Task.builder()
                .projectId(projectB.getId())
                .title("整理资质文件")
                .assigneeId(staff.getId())
                .status(Task.Status.TODO)
                .priority(Task.Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 3, 20, 18, 0))
                .build());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getRevenueDrillDown_ShouldReturnPaginatedRows() throws Exception {
        mockMvc.perform(get("/api/analytics/drilldown/revenue")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metricKey").value("revenue"))
                .andExpect(jsonPath("$.data.items[0].title").value("年度收入回归项目"))
                .andExpect(jsonPath("$.data.items[0].ownerName").value("收入下钻项目"))
                .andExpect(jsonPath("$.data.summary.totalAmount").value(1300));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getTeamDrillDown_ShouldSupportRoleFilter() throws Exception {
        mockMvc.perform(get("/api/analytics/drilldown/team")
                        .param("role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metricKey").value("team"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("李工"))
                .andExpect(jsonPath("$.data.items[0].completedTaskCount").value(1))
                .andExpect(jsonPath("$.data.items[0].overdueTaskCount").value(0))
                .andExpect(jsonPath("$.data.items[0].taskCompletionRate").value(50.0))
                .andExpect(jsonPath("$.data.items[0].score").isNumber())
                .andExpect(jsonPath("$.data.summary.totalCompletedTasks").value(1))
                .andExpect(jsonPath("$.data.summary.averageTaskCompletionRate").value(50.0))
                .andExpect(jsonPath("$.data.filters.dimensions[0].selectedValue").value("STAFF"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getProjectsDrillDown_ShouldSupportInProgressAlias() throws Exception {
        mockMvc.perform(get("/api/analytics/drilldown/projects")
                        .param("status", "in_progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metricKey").value("projects"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.summary.activeCount").value(2));
    }
}
