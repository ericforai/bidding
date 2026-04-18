package com.xiyu.bid.template.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.dto.TemplateCopyRequest;
import com.xiyu.bid.dto.TemplateDTO;
import com.xiyu.bid.dto.TemplateDownloadRecordRequest;
import com.xiyu.bid.dto.TemplateUseRecordRequest;
import com.xiyu.bid.entity.Template;
import com.xiyu.bid.repository.TemplateDownloadRecordRepository;
import com.xiyu.bid.repository.TemplateRepository;
import com.xiyu.bid.repository.TemplateUseRecordRepository;
import com.xiyu.bid.repository.TemplateVersionRepository;
import com.xiyu.bid.support.NoOpPasswordEncryptionTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@Import(NoOpPasswordEncryptionTestConfig.class)
class TemplateAdvancedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Autowired
    private TemplateUseRecordRepository templateUseRecordRepository;

    @Autowired
    private TemplateDownloadRecordRepository templateDownloadRecordRepository;

    private Template template;

    @BeforeEach
    void setUp() {
        templateDownloadRecordRepository.deleteAll();
        templateUseRecordRepository.deleteAll();
        templateVersionRepository.deleteAll();
        templateRepository.deleteAll();

        template = templateRepository.save(Template.builder()
                .name("智慧园区技术方案模板")
                .category(Template.Category.TECHNICAL)
                .description("园区项目模板")
                .currentVersion("1.0")
                .fileSize("2.4MB")
                .tags(List.of("园区", "技术"))
                .createdBy(1L)
                .build());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void copyAndVersionHistory_ShouldPersist() throws Exception {
        TemplateCopyRequest copyRequest = new TemplateCopyRequest();
        copyRequest.setName("智慧园区技术方案模板（副本）");
        copyRequest.setCreatedBy(2L);

        mockMvc.perform(post("/api/knowledge/templates/{id}/copy", template.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(copyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("智慧园区技术方案模板（副本）"))
                .andExpect(jsonPath("$.data.currentVersion").value("1.0"));

        TemplateDTO updatePayload = TemplateDTO.builder()
                .name("智慧园区技术方案模板")
                .category(Template.Category.TECHNICAL)
                .description("更新后的模板描述")
                .fileSize("3.1MB")
                .tags(List.of("园区", "技术", "更新"))
                .createdBy(1L)
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/knowledge/templates/{id}", template.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentVersion").value("1.1"));

        mockMvc.perform(get("/api/knowledge/templates/{id}/versions", template.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].version").value("1.1"))
                .andExpect(jsonPath("$.data[1].version").value("1.0"));

        assertThat(templateVersionRepository.findByTemplateIdOrderByCreatedAtDesc(template.getId())).hasSize(2);
        assertThat(templateRepository.findById(template.getId()).orElseThrow().getCurrentVersion()).isEqualTo("1.1");
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void useAndDownload_ShouldRecordAndUpdateCounters() throws Exception {
        TemplateUseRecordRequest useRequest = new TemplateUseRecordRequest();
        useRequest.setDocumentName("IOC项目技术方案");
        useRequest.setDocType("tech");
        useRequest.setProjectId(11L);
        useRequest.setApplyOptions(List.of("content", "styles"));
        useRequest.setUsedBy(3L);

        mockMvc.perform(post("/api/knowledge/templates/{id}/use-records", template.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentName").value("IOC项目技术方案"))
                .andExpect(jsonPath("$.data.applyOptions", hasSize(2)));

        TemplateDownloadRecordRequest downloadRequest = new TemplateDownloadRecordRequest();
        downloadRequest.setDownloadedBy(9L);

        mockMvc.perform(post("/api/knowledge/templates/{id}/downloads", template.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(downloadRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.downloads").value(1))
                .andExpect(jsonPath("$.data.useCount").value(1));

        mockMvc.perform(get("/api/knowledge/templates/{id}", template.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloads").value(1))
                .andExpect(jsonPath("$.data.useCount").value(1));

        assertThat(templateUseRecordRepository.countByTemplateId(template.getId())).isEqualTo(1);
        assertThat(templateDownloadRecordRepository.countByTemplateId(template.getId())).isEqualTo(1);
    }
}
