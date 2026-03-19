package com.xiyu.bid.project.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.ProjectGroup;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.repository.ProjectGroupRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.DataScopeConfigService;
import com.xiyu.bid.settings.entity.SystemSetting;
import com.xiyu.bid.settings.repository.SystemSettingRepository;
import com.xiyu.bid.support.TestPasswordEncryptionUtil;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User managerUser;
    private User staffUser;
    private User outsiderUser;
    private User departmentViewerUser;
    private User groupViewerUser;

    @TestConfiguration
    static class TestBeans {
        @Bean(name = "passwordEncryptionUtil")
        @Primary
        PasswordEncryptionUtil passwordEncryptionUtil() {
            return new TestPasswordEncryptionUtil();
        }
    }

    @BeforeEach
    void setUp() {
        projectGroupRepository.deleteAll();
        projectRepository.deleteAll();
        systemSettingRepository.deleteAll();
        userRepository.deleteAll();

        managerUser = userRepository.save(User.builder()
                .username("manager-user")
                .password("encoded")
                .email("manager@example.com")
                .fullName("项目经理")
                .role(User.Role.MANAGER)
                .departmentCode("BID")
                .departmentName("投标管理部")
                .enabled(true)
                .build());
        staffUser = userRepository.save(User.builder()
                .username("staff-user")
                .password("encoded")
                .email("staff@example.com")
                .fullName("项目成员")
                .role(User.Role.STAFF)
                .departmentCode("TECH")
                .departmentName("技术部")
                .enabled(true)
                .build());
        outsiderUser = userRepository.save(User.builder()
                .username("outsider-user")
                .password("encoded")
                .email("outsider@example.com")
                .fullName("外部人员")
                .role(User.Role.STAFF)
                .departmentCode("SALES")
                .departmentName("销售部")
                .enabled(true)
                .build());
        departmentViewerUser = userRepository.save(User.builder()
                .username("dept-viewer-user")
                .password("encoded")
                .email("dept-viewer@example.com")
                .fullName("同部门查看人")
                .role(User.Role.STAFF)
                .departmentCode("BID")
                .departmentName("投标管理部")
                .enabled(true)
                .build());
        groupViewerUser = userRepository.save(User.builder()
                .username("group-viewer-user")
                .password("encoded")
                .email("group-viewer@example.com")
                .fullName("项目组查看人")
                .role(User.Role.STAFF)
                .departmentCode("FINANCE")
                .departmentName("财务部")
                .enabled(true)
                .build());

        projectRepository.save(Project.builder()
                .name("真实项目列表回归")
                .tenderId(101L)
                .status(Project.Status.PREPARING)
                .managerId(managerUser.getId())
                .teamMembers(List.of(staffUser.getId(), 602L))
                .startDate(LocalDateTime.of(2026, 3, 10, 9, 0))
                .endDate(LocalDateTime.of(2026, 3, 20, 18, 0))
                .build());
        projectRepository.save(Project.builder()
                .name("无权限项目")
                .tenderId(102L)
                .status(Project.Status.REVIEWING)
                .managerId(888L)
                .teamMembers(List.of(889L))
                .startDate(LocalDateTime.of(2026, 3, 12, 9, 0))
                .endDate(LocalDateTime.of(2026, 3, 22, 18, 0))
                .build());

        Long visibleProjectId = projectRepository.findByNameContainingIgnoreCase("真实项目列表回归").get(0).getId();
        projectGroupRepository.saveAndFlush(ProjectGroup.builder()
                .groupCode("G1")
                .groupName("重点项目组")
                .managerUserId(managerUser.getId())
                .visibility(ProjectGroup.Visibility.MEMBERS)
                .memberUserIds(List.of(groupViewerUser.getId()))
                .projectIds(List.of(visibleProjectId))
                .build());

        systemSettingRepository.save(SystemSetting.builder()
                .configKey("data_scope_config")
                .payloadJson(writeDataScopePayload())
                .build());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAllProjects_ShouldSerializeTeamMembersAndReturnList() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("真实项目列表回归"))
                .andExpect(jsonPath("$.data[0].teamMembers[0]").value(staffUser.getId()))
                .andExpect(jsonPath("$.data[0].teamMembers[1]").value(602));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void createProject_ShouldPersistSourceMetadata() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "来源线索项目",
                                  "tenderId": 201,
                                  "status": "INITIATED",
                                  "managerId": 501,
                                  "teamMembers": [501],
                                  "startDate": "2026-03-18T09:00:00",
                                  "endDate": "2026-03-28T18:00:00",
                                  "sourceModule": "customer-opportunity-center",
                                  "sourceCustomerId": "CUST-001",
                                  "sourceCustomer": "华东某集团",
                                  "sourceOpportunityId": "OPP-001",
                                  "sourceReasoningSummary": "根据客户采购节奏建议提前立项"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sourceModule").value("customer-opportunity-center"))
                .andExpect(jsonPath("$.data.sourceCustomerId").value("CUST-001"))
                .andExpect(jsonPath("$.data.sourceCustomer").value("华东某集团"))
                .andExpect(jsonPath("$.data.sourceOpportunityId").value("OPP-001"))
                .andExpect(jsonPath("$.data.sourceReasoningSummary").value("根据客户采购节奏建议提前立项"));
    }

    @Test
    @WithMockUser(username = "staff-user", roles = {"STAFF"})
    void getAllProjects_ShouldFilterProjectsByCurrentMembership() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("真实项目列表回归"));
    }

    @Test
    @WithMockUser(username = "outsider-user", roles = {"STAFF"})
    void getProjectById_ShouldReturnForbiddenForUnauthorizedProject() throws Exception {
        Long restrictedProjectId = projectRepository.findByNameContainingIgnoreCase("真实项目列表回归").get(0).getId();

        mockMvc.perform(get("/api/projects/{id}", restrictedProjectId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("权限不足，无法访问该资源"));
    }

    @Test
    @WithMockUser(username = "dept-viewer-user", roles = {"STAFF"})
    void getAllProjects_ShouldIncludeProjectsGrantedByDepartmentScope() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("真实项目列表回归"));
    }

    private String writeDataScopePayload() {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "departmentTree", List.of(
                            java.util.Map.of(
                                    "departmentCode", "BID",
                                    "departmentName", "投标管理部",
                                    "sortOrder", 1
                            ),
                            java.util.Map.of(
                                    "departmentCode", "BID_SUB",
                                    "departmentName", "投标一部",
                                    "parentDepartmentCode", "BID",
                                    "sortOrder", 2
                            ),
                            java.util.Map.of(
                                    "departmentCode", "FINANCE",
                                    "departmentName", "财务部",
                                    "sortOrder", 3
                            )
                    ),
                    "userRules", List.of(),
                    "departmentRules", List.of(
                            java.util.Map.of(
                                    "departmentCode", "BID",
                                    "dataScope", "deptAndSub",
                                    "canViewOtherDepts", false,
                                    "allowedDeptCodes", List.of()
                            )
                    )
            ));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    @WithMockUser(username = "group-viewer-user", roles = {"STAFF"})
    void getAllProjects_ShouldIncludeProjectsGrantedByProjectGroupRule() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("真实项目列表回归"));
    }

    @Test
    @WithMockUser(username = "group-viewer-user", roles = {"STAFF"})
    void getAllProjects_ShouldExcludeProjectsAfterProjectGroupIsDeleted() throws Exception {
        projectGroupRepository.deleteAll();

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
