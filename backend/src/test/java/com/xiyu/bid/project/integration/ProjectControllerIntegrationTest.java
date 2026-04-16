package com.xiyu.bid.project.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.ProjectGroup;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.repository.ProjectGroupRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.RoleProfileRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.admin.service.DataScopeConfigService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private RoleProfileRepository roleProfileRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User managerUser;
    private User staffUser;
    private User outsiderUser;
    private User departmentViewerUser;
    private User groupViewerUser;
    private com.xiyu.bid.entity.RoleProfile defaultProfile;

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
        roleProfileRepository.deleteAll();

        defaultProfile = roleProfileRepository.save(com.xiyu.bid.entity.RoleProfile.builder()
                .code("test-profile")
                .name("测试权限")
                .dataScope("self")
                .build());

        managerUser = userRepository.save(User.builder()
                .username("manager-user")
                .password("encoded")
                .email("manager@example.com")
                .fullName("项目经理")
                .role(User.Role.MANAGER)
                .roleProfile(defaultProfile)
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
                .roleProfile(defaultProfile)
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
                .roleProfile(defaultProfile)
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
                .roleProfile(defaultProfile)
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
                .roleProfile(defaultProfile)
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
    @WithMockUser(roles = {"ADMIN"})
    void createProject_ShouldPersistAllBusinessFields() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "全字段业务项目",
                                  "tenderId": 301,
                                  "status": "INITIATED",
                                  "managerId": 501,
                                  "teamMembers": [501],
                                  "startDate": "2026-04-01T09:00:00",
                                  "endDate": "2026-05-15T18:00:00",
                                  "customer": "西部某能源集团",
                                  "budget": 12500000.50,
                                  "industry": "能源",
                                  "region": "新疆乌鲁木齐",
                                  "platform": "中国招标投标公共服务平台",
                                  "deadline": "2026-05-10",
                                  "description": "项目背景: 风电场二期建设",
                                  "remark": "甲方要求增项响应",
                                  "tagsJson": "[\\"风电\\",\\"重点\\"]"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.customer").value("西部某能源集团"))
                .andExpect(jsonPath("$.data.budget").value(12500000.50))
                .andExpect(jsonPath("$.data.industry").value("能源"))
                .andExpect(jsonPath("$.data.region").value("新疆乌鲁木齐"))
                .andExpect(jsonPath("$.data.platform").value("中国招标投标公共服务平台"))
                .andExpect(jsonPath("$.data.deadline").value("2026-05-10"))
                .andExpect(jsonPath("$.data.description").value("项目背景: 风电场二期建设"))
                .andExpect(jsonPath("$.data.remark").value("甲方要求增项响应"))
                .andExpect(jsonPath("$.data.tagsJson").value("[\"风电\",\"重点\"]"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateProject_ShouldUpdateBusinessFields() throws Exception {
        Project existing = projectRepository.save(Project.builder()
                .name("待更新项目")
                .tenderId(401L)
                .status(Project.Status.INITIATED)
                .managerId(managerUser.getId())
                .teamMembers(List.of(managerUser.getId()))
                .startDate(LocalDateTime.of(2026, 4, 1, 9, 0))
                .endDate(LocalDateTime.of(2026, 5, 1, 18, 0))
                .customer("初始客户")
                .industry("制造")
                .build());

        mockMvc.perform(put("/api/projects/{id}", existing.getId())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "已更新项目",
                                  "tenderId": 401,
                                  "managerId": %d,
                                  "teamMembers": [%d],
                                  "startDate": "2026-04-01T09:00:00",
                                  "endDate": "2026-05-30T18:00:00",
                                  "customer": "更新后客户",
                                  "budget": 8800000.00,
                                  "industry": "智慧城市",
                                  "region": "北京",
                                  "platform": "央采平台",
                                  "deadline": "2026-05-25",
                                  "description": "更新后描述",
                                  "remark": "更新后备注",
                                  "tagsJson": "[\\"智慧\\",\\"城市\\"]"
                                }
                                """.formatted(managerUser.getId(), managerUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("已更新项目"))
                .andExpect(jsonPath("$.data.customer").value("更新后客户"))
                .andExpect(jsonPath("$.data.budget").value(8800000.00))
                .andExpect(jsonPath("$.data.industry").value("智慧城市"))
                .andExpect(jsonPath("$.data.region").value("北京"))
                .andExpect(jsonPath("$.data.platform").value("央采平台"))
                .andExpect(jsonPath("$.data.deadline").value("2026-05-25"))
                .andExpect(jsonPath("$.data.description").value("更新后描述"))
                .andExpect(jsonPath("$.data.remark").value("更新后备注"))
                .andExpect(jsonPath("$.data.tagsJson").value("[\"智慧\",\"城市\"]"));
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
