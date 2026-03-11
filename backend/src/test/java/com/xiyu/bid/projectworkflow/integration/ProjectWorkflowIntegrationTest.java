package com.xiyu.bid.projectworkflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.projectworkflow.dto.ProjectDocumentCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectReminderCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectShareLinkCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskStatusUpdateRequest;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.projectworkflow.repository.ProjectReminderRepository;
import com.xiyu.bid.projectworkflow.repository.ProjectShareLinkRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class ProjectWorkflowIntegrationTest {

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
    private ProjectDocumentRepository projectDocumentRepository;

    @Autowired
    private ProjectReminderRepository projectReminderRepository;

    @Autowired
    private ProjectShareLinkRepository projectShareLinkRepository;

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
        projectShareLinkRepository.deleteAll();
        projectReminderRepository.deleteAll();
        projectDocumentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = userRepository.save(User.builder()
                .username("lizong-test")
                .password("XiyuDemo!2026")
                .email("lizong-test@example.com")
                .fullName("李总")
                .role(User.Role.ADMIN)
                .enabled(true)
                .build());

        project = projectRepository.save(Project.builder()
                .name("项目详情工作流回归")
                .tenderId(5001L)
                .status(Project.Status.PREPARING)
                .managerId(9001L)
                .teamMembers(List.of(9101L, 9102L))
                .startDate(LocalDateTime.of(2026, 3, 11, 9, 0))
                .endDate(LocalDateTime.of(2026, 3, 20, 18, 0))
                .build());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void projectWorkflowEndpoints_ShouldPersistTasksDocumentsRemindersAndShareLinks() throws Exception {
        ProjectTaskCreateRequest taskRequest = ProjectTaskCreateRequest.builder()
                .title("准备商务应答")
                .description("整理商务偏离表")
                .assigneeId(ownerUser.getId())
                .assigneeName("李总")
                .priority(Task.Priority.HIGH)
                .dueDate(LocalDateTime.of(2026, 3, 15, 18, 0))
                .build();

        String taskResponse = mockMvc.perform(post("/api/projects/{projectId}/tasks", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("准备商务应答"))
                .andExpect(jsonPath("$.data.owner").value("李总"))
                .andExpect(jsonPath("$.data.status").value("todo"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long taskId = objectMapper.readTree(taskResponse).path("data").path("id").asLong();

        ProjectTaskStatusUpdateRequest statusRequest = ProjectTaskStatusUpdateRequest.builder()
                .status(Task.Status.IN_PROGRESS)
                .build();

        mockMvc.perform(patch("/api/projects/{projectId}/tasks/{taskId}/status", project.getId(), taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("doing"));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("准备商务应答"))
                .andExpect(jsonPath("$.data[0].owner").value("李总"));

        ProjectDocumentCreateRequest documentRequest = ProjectDocumentCreateRequest.builder()
                .name("商务应答.docx")
                .size("2MB")
                .fileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .uploaderId(ownerUser.getId())
                .uploaderName("李总")
                .build();

        String documentResponse = mockMvc.perform(post("/api/projects/{projectId}/documents", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(documentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("商务应答.docx"))
                .andExpect(jsonPath("$.data.uploader").value("李总"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long documentId = objectMapper.readTree(documentResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/projects/{projectId}/documents", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("商务应答.docx"));

        ProjectReminderCreateRequest reminderRequest = ProjectReminderCreateRequest.builder()
                .title("跟进商务稿")
                .message("明天 09:00 前确认商务应答版本")
                .remindAt(LocalDateTime.of(2026, 3, 12, 9, 0))
                .createdBy(ownerUser.getId())
                .createdByName("李总")
                .recipient("项目负责人")
                .build();

        mockMvc.perform(post("/api/projects/{projectId}/reminders", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reminderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("跟进商务稿"))
                .andExpect(jsonPath("$.data.createdByName").value("李总"));

        ProjectShareLinkCreateRequest shareRequest = ProjectShareLinkCreateRequest.builder()
                .createdBy(ownerUser.getId())
                .createdByName("李总")
                .baseUrl("http://127.0.0.1:14173")
                .build();

        mockMvc.perform(post("/api/projects/{projectId}/share-links", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.containsString("/project/" + project.getId())))
                .andExpect(jsonPath("$.data.createdByName").value("李总"));

        mockMvc.perform(delete("/api/projects/{projectId}/documents/{documentId}", project.getId(), documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(taskRepository.findByProjectId(project.getId())).hasSize(1);
        assertThat(taskRepository.findByProjectId(project.getId()).get(0).getStatus()).isEqualTo(Task.Status.IN_PROGRESS);
        assertThat(projectDocumentRepository.findByProjectIdOrderByCreatedAtDesc(project.getId())).isEmpty();
        assertThat(projectReminderRepository.findByProjectIdOrderByRemindAtDesc(project.getId())).hasSize(1);
        assertThat(projectShareLinkRepository.findByProjectIdOrderByCreatedAtDesc(project.getId())).hasSize(1);
    }
}
