package com.xiyu.bid.casework.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.casework.dto.CaseReferenceRecordCreateRequest;
import com.xiyu.bid.casework.dto.CaseShareRecordCreateRequest;
import com.xiyu.bid.dto.CaseDTO;
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.casework.repository.CaseReferenceRecordRepository;
import com.xiyu.bid.casework.repository.CaseShareRecordRepository;
import com.xiyu.bid.repository.CaseRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class CaseAdvancedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseShareRecordRepository caseShareRecordRepository;

    @Autowired
    private CaseReferenceRecordRepository caseReferenceRecordRepository;

    private Case caseStudy;
    private User ownerUser;

    @TestConfiguration
    static class TestBeans {
        @Bean(name = "passwordEncryptionUtil")
        @Primary
        PasswordEncryptionUtil passwordEncryptionUtil() {
            return new PasswordEncryptionUtil() {
                @Override
                public void initialize() {}
                @Override
                public String encrypt(String plainPassword) { return plainPassword; }
                @Override
                public String decrypt(String encryptedPassword) { return encryptedPassword; }
                @Override
                public boolean isKeyValid() { return true; }
            };
        }
    }

    @BeforeEach
    void setUp() {
        caseReferenceRecordRepository.deleteAll();
        caseShareRecordRepository.deleteAll();
        caseRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = userRepository.save(User.builder()
                .username("case-admin")
                .password("XiyuDemo!2026")
                .email("case-admin@example.com")
                .fullName("李总")
                .role(User.Role.ADMIN)
                .enabled(true)
                .build());

        caseStudy = caseRepository.save(Case.builder()
                .title("智慧园区平台案例")
                .industry(Case.Industry.INFRASTRUCTURE)
                .outcome(Case.Outcome.WON)
                .amount(new BigDecimal("780.00"))
                .projectDate(LocalDate.of(2025, 5, 20))
                .description("项目初始摘要")
                .customerName("杭州市人民政府")
                .locationName("杭州")
                .projectPeriod("2025-01-01 - 2025-12-31")
                .tags(List.of("智慧园区", "IOC"))
                .highlights(List.of("统一门户", "跨部门联动"))
                .technologies(List.of("Vue", "Spring Boot"))
                .viewCount(12L)
                .useCount(3L)
                .build());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void caseAdvancedEndpoints_ShouldPersistEditsSharesAndReferences() throws Exception {
        CaseDTO updateRequest = CaseDTO.builder()
                .title("智慧园区平台案例（更新）")
                .industry(Case.Industry.INFRASTRUCTURE)
                .outcome(Case.Outcome.WON)
                .amount(new BigDecimal("820.00"))
                .projectDate(LocalDate.of(2025, 5, 20))
                .description("更新后的项目摘要")
                .customerName("杭州市人民政府")
                .locationName("杭州")
                .projectPeriod("2025-01-01 - 2025-12-31")
                .tags(List.of("智慧园区", "政务"))
                .highlights(List.of("统一门户", "一网统管", "跨部门联动"))
                .technologies(List.of("Vue", "Spring Boot", "PostgreSQL"))
                .viewCount(66L)
                .useCount(3L)
                .build();

        mockMvc.perform(put("/api/knowledge/cases/{id}", caseStudy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("智慧园区平台案例（更新）"))
                .andExpect(jsonPath("$.data.customerName").value("杭州市人民政府"))
                .andExpect(jsonPath("$.data.tags", hasSize(2)))
                .andExpect(jsonPath("$.data.highlights", hasSize(3)));

        CaseShareRecordCreateRequest shareRequest = CaseShareRecordCreateRequest.builder()
                .createdBy(ownerUser.getId())
                .createdByName("李总")
                .baseUrl("http://127.0.0.1:14173")
                .build();

        mockMvc.perform(post("/api/knowledge/cases/{id}/share-records", caseStudy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url").value(containsString("/knowledge/case/detail?id=" + caseStudy.getId())))
                .andExpect(jsonPath("$.data.createdByName").value("李总"));

        mockMvc.perform(get("/api/knowledge/cases/{id}/share-records", caseStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        CaseReferenceRecordCreateRequest referenceRequest = CaseReferenceRecordCreateRequest.builder()
                .referencedBy(ownerUser.getId())
                .referencedByName("李总")
                .referenceTarget("杭州 IOC 投标项目")
                .referenceContext("用于项目方案章节中的成功案例引用")
                .build();

        mockMvc.perform(post("/api/knowledge/cases/{id}/references", caseStudy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(referenceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.referenceTarget").value("杭州 IOC 投标项目"))
                .andExpect(jsonPath("$.data.referencedByName").value("李总"));

        mockMvc.perform(get("/api/knowledge/cases/{id}/references", caseStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/knowledge/cases/{id}", caseStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.useCount").value(4))
                .andExpect(jsonPath("$.data.description").value("更新后的项目摘要"))
                .andExpect(jsonPath("$.data.technologies", hasSize(3)));

        assertThat(caseShareRecordRepository.findByCaseIdOrderByCreatedAtDesc(caseStudy.getId())).hasSize(1);
        assertThat(caseReferenceRecordRepository.findByCaseIdOrderByReferencedAtDesc(caseStudy.getId())).hasSize(1);
        assertThat(caseRepository.findById(caseStudy.getId()).orElseThrow().getUseCount()).isEqualTo(4L);
    }
}
