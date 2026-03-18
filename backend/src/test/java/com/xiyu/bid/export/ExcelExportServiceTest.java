package com.xiyu.bid.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.config.ExportConfig;
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Qualification;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.Template;
import com.xiyu.bid.export.service.ExcelExportService;
import com.xiyu.bid.repository.CaseRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.QualificationRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.TemplateRepository;
import com.xiyu.bid.service.IAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private QualificationRepository qualificationRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private IAuditLogService auditLogService;

    private ExcelExportService excelExportService;
    private ExportConfig exportConfig;

    private Tender testTender;
    private Project testProject;
    private Qualification testQualification;
    private Case testCase;
    private Template testTemplate;

    @BeforeEach
    void setUp() {
        exportConfig = new ExportConfig();
        exportConfig.setMaxRecords(10000);
        exportConfig.setMaxFileSizeBytes(52_428_800L);
        exportConfig.setAuditEnabled(true);
        exportConfig.setMaxExportsPerHour(10);

        excelExportService = new ExcelExportService(
                tenderRepository,
                projectRepository,
                qualificationRepository,
                caseRepository,
                templateRepository,
                exportConfig,
                auditLogService
        );

        testTender = Tender.builder()
                .id(1L)
                .title("测试标讯项目")
                .source("政府采购网")
                .budget(new BigDecimal("1000000.00"))
                .deadline(LocalDateTime.of(2024, 6, 30, 17, 0))
                .status(Tender.Status.PENDING)
                .aiScore(85)
                .riskLevel(Tender.RiskLevel.LOW)
                .createdAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .build();

        testProject = Project.builder()
                .id(1L)
                .name("测试投标项目")
                .tenderId(1L)
                .status(Project.Status.PREPARING)
                .managerId(1L)
                .startDate(LocalDateTime.of(2024, 3, 1, 10, 0))
                .endDate(LocalDateTime.of(2024, 6, 30, 17, 0))
                .createdAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .build();

        testQualification = Qualification.builder()
                .id(1L)
                .name("建筑工程施工总承包资质")
                .type(Qualification.Type.CONSTRUCTION)
                .level(Qualification.Level.FIRST)
                .issueDate(LocalDate.of(2020, 1, 1))
                .expiryDate(LocalDate.of(2025, 12, 31))
                .fileUrl("/files/qualification.pdf")
                .createdAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .build();

        testCase = Case.builder()
                .id(1L)
                .title("智慧城市建设项目")
                .industry(Case.Industry.INFRASTRUCTURE)
                .outcome(Case.Outcome.WON)
                .amount(new BigDecimal("5000000.00"))
                .projectDate(LocalDate.of(2023, 6, 15))
                .description("智慧城市综合管理平台建设")
                .customerName("某市人民政府")
                .locationName("北京市")
                .projectPeriod("12个月")
                .viewCount(100L)
                .useCount(5L)
                .createdAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .build();

        testTemplate = Template.builder()
                .id(1L)
                .name("技术标模板")
                .category(Template.Category.TECHNICAL)
                .fileUrl("/files/template.docx")
                .description("技术标书标准模板")
                .currentVersion("V1.0")
                .fileSize("2.5MB")
                .createdBy(1L)
                .createdAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .build();
    }

    @Test
    void exportToExcel_WithTendersType_ShouldExportTenders() {
        // Setup pagination mock
        Page<Tender> page = new PageImpl<>(Arrays.asList(testTender), PageRequest.of(0, 1000), 1);
        when(tenderRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page)
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 1000), 1)); // Empty page for second call

        Path outputPath = Path.of("/tmp/test_tenders.xlsx");

        long fileSize = excelExportService.exportToExcel("tenders", outputPath, null, 1L);

        assertThat(fileSize).isGreaterThan(0);
        verify(tenderRepository, atLeastOnce()).findAll(any(org.springframework.data.domain.Pageable.class));

        java.io.File file = outputPath.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void exportToExcel_WithProjectsType_ShouldExportProjects() {
        Page<Project> page = new PageImpl<>(Arrays.asList(testProject), PageRequest.of(0, 1000), 1);
        when(projectRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page)
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 1000), 1));

        Path outputPath = Path.of("/tmp/test_projects.xlsx");

        long fileSize = excelExportService.exportToExcel("projects", outputPath, null, 1L);

        assertThat(fileSize).isGreaterThan(0);
        verify(projectRepository, atLeastOnce()).findAll(any(org.springframework.data.domain.Pageable.class));

        java.io.File file = outputPath.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void exportToExcel_WithQualificationsType_ShouldExportQualifications() {
        Page<Qualification> page = new PageImpl<>(Arrays.asList(testQualification), PageRequest.of(0, 1000), 1);
        when(qualificationRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page)
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 1000), 1));

        Path outputPath = Path.of("/tmp/test_qualifications.xlsx");

        long fileSize = excelExportService.exportToExcel("qualifications", outputPath, null, 1L);

        assertThat(fileSize).isGreaterThan(0);
        verify(qualificationRepository, atLeastOnce()).findAll(any(org.springframework.data.domain.Pageable.class));

        java.io.File file = outputPath.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void exportToExcel_WithCasesType_ShouldExportCases() {
        Page<Case> page = new PageImpl<>(Arrays.asList(testCase), PageRequest.of(0, 1000), 1);
        when(caseRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page)
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 1000), 1));

        Path outputPath = Path.of("/tmp/test_cases.xlsx");

        long fileSize = excelExportService.exportToExcel("cases", outputPath, null, 1L);

        assertThat(fileSize).isGreaterThan(0);
        verify(caseRepository, atLeastOnce()).findAll(any(org.springframework.data.domain.Pageable.class));

        java.io.File file = outputPath.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void exportToExcel_WithTemplatesType_ShouldExportTemplates() {
        Page<Template> page = new PageImpl<>(Arrays.asList(testTemplate), PageRequest.of(0, 1000), 1);
        when(templateRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page)
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 1000), 1));

        Path outputPath = Path.of("/tmp/test_templates.xlsx");

        long fileSize = excelExportService.exportToExcel("templates", outputPath, null, 1L);

        assertThat(fileSize).isGreaterThan(0);
        verify(templateRepository, atLeastOnce()).findAll(any(org.springframework.data.domain.Pageable.class));

        java.io.File file = outputPath.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void exportToExcel_WithEmptyList_ShouldCreateEmptyExcelWithHeaders() {
        Page<Tender> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 1000), 0);
        when(tenderRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(emptyPage);

        Path outputPath = Path.of("/tmp/test_empty_tenders.xlsx");

        long fileSize = excelExportService.exportToExcel("tenders", outputPath, null, 1L);

        assertThat(fileSize).isGreaterThan(0);
        verify(tenderRepository).findAll(any(org.springframework.data.domain.Pageable.class));

        java.io.File file = outputPath.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void exportToExcel_WithInvalidType_ShouldThrowException() {
        Path outputPath = Path.of("/tmp/test_invalid.xlsx");

        assertThatThrownBy(() -> excelExportService.exportToExcel("invalid_type", outputPath, null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported export type");
    }

    @Test
    void exportToExcel_WithNullType_ShouldThrowException() {
        Path outputPath = Path.of("/tmp/test_null.xlsx");

        assertThatThrownBy(() -> excelExportService.exportToExcel(null, outputPath, null, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exportToExcel_WithPathTraversal_ShouldThrowException() {
        Path outputPath = Path.of("/tmp/test/../etc/passwd");

        assertThatThrownBy(() -> excelExportService.exportToExcel("tenders", outputPath, null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path traversal");
    }

    @Test
    void exportToExcel_WithRecordLimit_ShouldEnforceLimit() {
        // Create 20001 tenders (more than the default limit of 10000)
        List<Tender> largeList = java.util.stream.IntStream.range(0, 20001)
                .mapToObj(i -> Tender.builder()
                        .id((long) i)
                        .title("标讯 " + i)
                        .source("测试来源")
                        .budget(new BigDecimal("1000000.00"))
                        .status(Tender.Status.PENDING)
                        .build())
                .toList();

        // Setup pagination to return all tenders
        when(tenderRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenAnswer(invocation -> {
                    org.springframework.data.domain.Pageable pageable = invocation.getArgument(0);
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), largeList.size());
                    List<Tender> subList = largeList.subList(start, end);
                    return new PageImpl<>(subList, pageable, largeList.size());
                });

        Path outputPath = Path.of("/tmp/test_large_tenders.xlsx");

        long fileSize = excelExportService.exportToExcel("tenders", outputPath, null, 1L);

        // Should succeed but only export up to the limit
        assertThat(fileSize).isGreaterThan(0);

        java.io.File file = outputPath.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void exportToExcel_ShouldLogAuditEntry() {
        Page<Tender> page = new PageImpl<>(Arrays.asList(testTender), PageRequest.of(0, 1000), 1);
        when(tenderRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page)
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 1000), 1));

        Path outputPath = Path.of("/tmp/test_audit_tenders.xlsx");

        excelExportService.exportToExcel("tenders", outputPath, null, 1L);

        verify(auditLogService).log(any(com.xiyu.bid.service.AuditLogService.AuditLogEntry.class));

        java.io.File file = outputPath.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void getExportFileName_WithTendersType_ShouldReturnCorrectFileName() {
        String result = excelExportService.getExportFileName("tenders");

        assertThat(result).contains("标讯列表");
        assertThat(result).endsWith(".xlsx");
    }

    @Test
    void getExportFileName_WithProjectsType_ShouldReturnCorrectFileName() {
        String result = excelExportService.getExportFileName("projects");

        assertThat(result).contains("项目列表");
        assertThat(result).endsWith(".xlsx");
    }

    @Test
    void getExportFileName_WithQualificationsType_ShouldReturnCorrectFileName() {
        String result = excelExportService.getExportFileName("qualifications");

        assertThat(result).contains("资质列表");
        assertThat(result).endsWith(".xlsx");
    }

    @Test
    void getExportFileName_WithCasesType_ShouldReturnCorrectFileName() {
        String result = excelExportService.getExportFileName("cases");

        assertThat(result).contains("案例列表");
        assertThat(result).endsWith(".xlsx");
    }

    @Test
    void getExportFileName_WithTemplatesType_ShouldReturnCorrectFileName() {
        String result = excelExportService.getExportFileName("templates");

        assertThat(result).contains("模板列表");
        assertThat(result).endsWith(".xlsx");
    }

    @Test
    void getExportFileName_WithInvalidType_ShouldReturnGenericFileName() {
        String result = excelExportService.getExportFileName("invalid");

        assertThat(result).contains("导出数据");
        assertThat(result).endsWith(".xlsx");
    }

    @Test
    void getExportFileName_ShouldIncludeTimestamp() {
        String result = excelExportService.getExportFileName("tenders");

        assertThat(result).containsPattern("\\d{8}_\\d{6}");
    }

    @Test
    void exportToExcel_LegacyMethod_ShouldStillWork() {
        Page<Tender> page = new PageImpl<>(Arrays.asList(testTender), PageRequest.of(0, 1000), 1);
        when(tenderRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page)
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 1000), 1));

        Path outputPath = Path.of("/tmp/test_legacy_tenders.xlsx");

        // Call legacy method (without userId parameter)
        long fileSize = excelExportService.exportToExcel("tenders", outputPath, null);

        assertThat(fileSize).isGreaterThan(0);

        java.io.File file = outputPath.toFile();
        if (file.exists()) {
            file.delete();
        }
    }
}
