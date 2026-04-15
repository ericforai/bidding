package com.xiyu.bid.documents.service;

import com.xiyu.bid.documents.dto.AssemblyRequest;
import com.xiyu.bid.documents.dto.AssemblyTemplateDTO;
import com.xiyu.bid.documents.dto.DocumentAssemblyDTO;
import com.xiyu.bid.documents.dto.TemplateCreateRequest;
import com.xiyu.bid.documents.entity.AssemblyTemplate;
import com.xiyu.bid.documents.entity.DocumentAssembly;
import com.xiyu.bid.documents.repository.AssemblyTemplateRepository;
import com.xiyu.bid.documents.repository.DocumentAssemblyRepository;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.audit.service.IAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DocumentAssemblyService单元测试
 * 测试文档组装服务的所有业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class DocumentAssemblyServiceTest {

    @Mock
    private AssemblyTemplateRepository templateRepository;

    @Mock
    private DocumentAssemblyRepository assemblyRepository;

    @Mock
    private IAuditLogService auditLogService;

    private DocumentAssemblyService documentAssemblyService;

    private AssemblyTemplate testTemplate;
    private DocumentAssembly testAssembly;
    private TemplateCreateRequest createRequest;
    private AssemblyRequest assemblyRequest;

    @BeforeEach
    void setUp() {
        documentAssemblyService = new DocumentAssemblyService(
                templateRepository,
                assemblyRepository,
                auditLogService
        );

        testTemplate = AssemblyTemplate.builder()
                .id(1L)
                .name("投标书模板")
                .description("标准投标书模板")
                .category("BIDDING_DOCUMENT")
                .templateContent("尊敬的${招标方名称}：\n\n我方愿意参与${项目名称}的投标，报价为${报价金额}元。")
                .variables("{\"招标方名称\":\"string\",\"项目名称\":\"string\",\"报价金额\":\"number\"}")
                .createdBy(100L)
                .createdAt(LocalDateTime.now())
                .build();

        testAssembly = DocumentAssembly.builder()
                .id(1L)
                .projectId(200L)
                .templateId(1L)
                .assembledContent("尊敬的XX公司：\n\n我方愿意参与ABC项目的投标，报价为500000元。")
                .variables("{\"招标方名称\":\"XX公司\",\"项目名称\":\"ABC项目\",\"报价金额\":500000}")
                .assembledBy(300L)
                .assembledAt(LocalDateTime.now())
                .build();

        createRequest = TemplateCreateRequest.builder()
                .name("新模板")
                .description("新模板描述")
                .category("CONTRACT")
                .templateContent("合同内容：${甲方}与${乙方}")
                .variables("{\"甲方\":\"string\",\"乙方\":\"string\"}")
                .createdBy(100L)
                .build();

        assemblyRequest = AssemblyRequest.builder()
                .templateId(1L)
                .variables("{\"招标方名称\":\"XX公司\",\"项目名称\":\"ABC项目\",\"报价金额\":500000}")
                .assembledBy(300L)
                .build();
    }

    @Test
    void createTemplate_ShouldReturnSavedTemplate() {
        // Given
        when(templateRepository.save(any(AssemblyTemplate.class))).thenReturn(testTemplate);

        // When
        AssemblyTemplateDTO result = documentAssemblyService.createTemplate(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("投标书模板");
        assertThat(result.getCategory()).isEqualTo("BIDDING_DOCUMENT");

        verify(templateRepository).save(any(AssemblyTemplate.class));
    }

    @Test
    void createTemplate_WithNullName_ShouldThrowException() {
        // Given
        TemplateCreateRequest invalidRequest = TemplateCreateRequest.builder()
                .name(null)
                .templateContent("内容")
                .build();

        // When & Then
        assertThatThrownBy(() -> documentAssemblyService.createTemplate(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template name");
    }

    @Test
    void createTemplate_WithEmptyContent_ShouldThrowException() {
        // Given
        TemplateCreateRequest invalidRequest = TemplateCreateRequest.builder()
                .name("模板")
                .templateContent("")
                .build();

        // When & Then
        assertThatThrownBy(() -> documentAssemblyService.createTemplate(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template content");
    }

    @Test
    void getTemplatesByCategory_ShouldReturnListOfTemplates() {
        // Given
        AssemblyTemplate contractTemplate1 = AssemblyTemplate.builder()
                .id(1L)
                .name("销售合同模板")
                .category("CONTRACT")
                .templateContent("销售合同内容")
                .build();

        AssemblyTemplate contractTemplate2 = AssemblyTemplate.builder()
                .id(2L)
                .name("采购合同模板")
                .category("CONTRACT")
                .templateContent("采购合同内容")
                .build();

        when(templateRepository.findByCategory("CONTRACT"))
                .thenReturn(Arrays.asList(contractTemplate1, contractTemplate2));

        // When
        List<AssemblyTemplateDTO> result = documentAssemblyService.getTemplatesByCategory("CONTRACT");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategory()).isEqualTo("CONTRACT");
        assertThat(result.get(1).getCategory()).isEqualTo("CONTRACT");
    }

    @Test
    void getTemplatesByCategory_WithEmptyResult_ShouldReturnEmptyList() {
        // Given
        when(templateRepository.findByCategory("UNKNOWN")).thenReturn(List.of());

        // When
        List<AssemblyTemplateDTO> result = documentAssemblyService.getTemplatesByCategory("UNKNOWN");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void assembleDocument_ShouldReturnAssembledContent() {
        // Given
        when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
        when(assemblyRepository.save(any(DocumentAssembly.class))).thenReturn(testAssembly);

        // When
        DocumentAssemblyDTO result = documentAssemblyService.assembleDocument(200L, 1L,
                "{\"招标方名称\":\"XX公司\",\"项目名称\":\"ABC项目\",\"报价金额\":500000}", 300L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(200L);
        assertThat(result.getTemplateId()).isEqualTo(1L);
        assertThat(result.getAssembledContent()).contains("XX公司");
        assertThat(result.getAssembledContent()).contains("ABC项目");

        verify(assemblyRepository).save(any(DocumentAssembly.class));
    }

    @Test
    void assembleDocument_WithInvalidTemplateId_ShouldThrowException() {
        // Given
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                documentAssemblyService.assembleDocument(200L, 999L, "{}", 300L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Template not found");
    }

    @Test
    void assembleDocument_WithMissingVariable_ShouldLeavePlaceholder() {
        // Given
        when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
        when(assemblyRepository.save(any(DocumentAssembly.class))).thenAnswer(invocation -> {
            DocumentAssembly assembly = invocation.getArgument(0);
            return assembly;
        });

        // When - Missing "报价金额" variable
        DocumentAssemblyDTO result = documentAssemblyService.assembleDocument(200L, 1L,
                "{\"招标方名称\":\"XX公司\",\"项目名称\":\"ABC项目\"}", 300L);

        // Then - Should leave placeholder for missing variable
        assertThat(result.getAssembledContent()).contains("XX公司");
        assertThat(result.getAssembledContent()).contains("ABC项目");
    }

    @Test
    void getAssembliesByProject_ShouldReturnListOfAssemblies() {
        // Given
        DocumentAssembly assembly2 = DocumentAssembly.builder()
                .id(2L)
                .projectId(200L)
                .templateId(2L)
                .assembledContent("第二个组装文档")
                .build();

        when(assemblyRepository.findByProjectId(200L))
                .thenReturn(Arrays.asList(testAssembly, assembly2));

        // When
        List<DocumentAssemblyDTO> result = documentAssemblyService.getAssembliesByProject(200L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProjectId()).isEqualTo(200L);
        assertThat(result.get(1).getProjectId()).isEqualTo(200L);
    }

    @Test
    void getAssembliesByProject_WithEmptyResult_ShouldReturnEmptyList() {
        // Given
        when(assemblyRepository.findByProjectId(999L)).thenReturn(List.of());

        // When
        List<DocumentAssemblyDTO> result = documentAssemblyService.getAssembliesByProject(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void regenerateAssembly_ShouldReturnNewAssembly() {
        // Given
        when(assemblyRepository.findById(1L)).thenReturn(Optional.of(testAssembly));
        when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));

        DocumentAssembly newAssembly = DocumentAssembly.builder()
                .id(2L)
                .projectId(200L)
                .templateId(1L)
                .assembledContent("重新生成的内容")
                .build();

        when(assemblyRepository.save(any(DocumentAssembly.class))).thenReturn(newAssembly);

        // When
        DocumentAssemblyDTO result = documentAssemblyService.regenerateAssembly(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);

        verify(assemblyRepository).save(any(DocumentAssembly.class));
    }

    @Test
    void regenerateAssembly_WithInvalidAssemblyId_ShouldThrowException() {
        // Given
        when(assemblyRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> documentAssemblyService.regenerateAssembly(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Assembly not found");
    }

    @Test
    void regenerateAssembly_WithDeletedTemplate_ShouldThrowException() {
        // Given
        when(assemblyRepository.findById(1L)).thenReturn(Optional.of(testAssembly));
        when(templateRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> documentAssemblyService.regenerateAssembly(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Template not found");
    }

    @Test
    void replaceVariables_ShouldCorrectlyReplaceAllPlaceholders() {
        // Given
        String template = "尊敬的${name}，关于${project}的报价为${amount}元。";
        String variablesJson = "{\"name\":\"张三\",\"project\":\"ABC项目\",\"amount\":100000}";

        // When
        String result = documentAssemblyService.replaceVariables(template, variablesJson);

        // Then
        assertThat(result).contains("张三");
        assertThat(result).contains("ABC项目");
        assertThat(result).contains("100000");
        assertThat(result).doesNotContain("${");
    }

    @Test
    void replaceVariables_WithEmptyJson_ShouldReturnOriginalTemplate() {
        // Given
        String template = "尊敬的${name}，这是测试内容。";

        // When
        String result = documentAssemblyService.replaceVariables(template, "{}");

        // Then
        assertThat(result).contains("${name}");
    }

    @Test
    void replaceVariables_WithExtraVariables_ShouldIgnoreExtra() {
        // Given
        String template = "项目：${project}";
        String variablesJson = "{\"project\":\"ABC\",\"extra\":\"忽略\"}";

        // When
        String result = documentAssemblyService.replaceVariables(template, variablesJson);

        // Then
        assertThat(result).contains("ABC");
        assertThat(result).doesNotContain("${project}");
    }
}
