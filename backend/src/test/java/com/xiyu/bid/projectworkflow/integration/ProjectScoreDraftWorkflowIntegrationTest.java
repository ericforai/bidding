package com.xiyu.bid.projectworkflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftGenerateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftUpdateRequest;
import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import com.xiyu.bid.projectworkflow.repository.ProjectScoreDraftRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.UserRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class ProjectScoreDraftWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectScoreDraftRepository projectScoreDraftRepository;

    private Project project;
    private User ownerUser;

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
        projectScoreDraftRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = userRepository.save(User.builder()
                .username("zhangjingli-test")
                .password("XiyuDemo!2026")
                .email("manager@example.com")
                .fullName("张经理")
                .role(User.Role.MANAGER)
                .enabled(true)
                .build());

        project = projectRepository.save(Project.builder()
                .name("评分拆解测试项目")
                .tenderId(8888L)
                .status(Project.Status.PREPARING)
                .managerId(1111L)
                .startDate(LocalDateTime.of(2026, 3, 12, 9, 0))
                .endDate(LocalDateTime.of(2026, 3, 30, 18, 0))
                .build());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void scoreDraftWorkflow_ShouldParseAssignAndGenerateFormalTasks() throws Exception {
        MockMultipartFile scoreFile = new MockMultipartFile(
                "file",
                "score-table.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                buildSampleDocx()
        );

        String parseResponse = mockMvc.perform(multipart("/api/projects/{projectId}/score-drafts/parse", project.getId())
                        .file(scoreFile))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.drafts[0].status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long draftId = objectMapper.readTree(parseResponse).path("data").path("drafts").get(0).path("id").asLong();

        ProjectScoreDraftUpdateRequest updateRequest = ProjectScoreDraftUpdateRequest.builder()
                .assigneeId(ownerUser.getId())
                .assigneeName(ownerUser.getFullName())
                .status(ProjectScoreDraft.Status.READY)
                .dueDate(LocalDateTime.of(2026, 3, 20, 18, 0))
                .build();

        mockMvc.perform(patch("/api/projects/{projectId}/score-drafts/{draftId}", project.getId(), draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeName").value("张经理"))
                .andExpect(jsonPath("$.data.status").value("READY"));

        ProjectScoreDraftGenerateRequest generateRequest = ProjectScoreDraftGenerateRequest.builder()
                .draftIds(List.of(draftId))
                .build();

        mockMvc.perform(post("/api/projects/{projectId}/score-drafts/generate-tasks", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].owner").value("张经理"))
                .andExpect(jsonPath("$.data[0].status").value("todo"));

        mockMvc.perform(get("/api/projects/{projectId}/score-drafts", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("GENERATED"))
                .andExpect(jsonPath("$.data[1].status").value("DRAFT"));

        assertThat(taskRepository.findByProjectId(project.getId())).hasSize(1);
        assertThat(taskRepository.findByProjectId(project.getId()).get(0).getTitle()).contains("项目经理资质");
    }

    private byte[] buildSampleDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(3, 3);
            table.getRow(0).getCell(0).setText("评分项");
            table.getRow(0).getCell(1).setText("评分标准");
            table.getRow(0).getCell(2).setText("分值");

            table.getRow(1).getCell(0).setText("项目经理资质");
            table.getRow(1).getCell(1).setText("提供一级建造师证书得3分");
            table.getRow(1).getCell(2).setText("3分");

            table.getRow(2).getCell(0).setText("同类项目业绩");
            table.getRow(2).getCell(1).setText("每提供1个同类项目业绩得2分，最高6分");
            table.getRow(2).getCell(2).setText("最高6分");

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
