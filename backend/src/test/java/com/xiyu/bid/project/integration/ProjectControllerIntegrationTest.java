package com.xiyu.bid.project.integration;

import com.xiyu.bid.entity.Project;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.repository.ProjectRepository;
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
        projectRepository.deleteAll();
        projectRepository.save(Project.builder()
                .name("真实项目列表回归")
                .tenderId(101L)
                .status(Project.Status.PREPARING)
                .managerId(501L)
                .teamMembers(List.of(601L, 602L))
                .startDate(LocalDateTime.of(2026, 3, 10, 9, 0))
                .endDate(LocalDateTime.of(2026, 3, 20, 18, 0))
                .build());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAllProjects_ShouldSerializeTeamMembersAndReturnList() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("真实项目列表回归"))
                .andExpect(jsonPath("$.data[0].teamMembers[0]").value(601))
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
}
