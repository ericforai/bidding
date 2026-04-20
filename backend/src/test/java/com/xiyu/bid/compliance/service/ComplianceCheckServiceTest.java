package com.xiyu.bid.compliance.service;

import com.xiyu.bid.compliance.entity.ComplianceCheckResult;
import com.xiyu.bid.compliance.entity.ComplianceRule;
import com.xiyu.bid.compliance.repository.ComplianceCheckResultRepository;
import com.xiyu.bid.compliance.repository.ComplianceRuleRepository;
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.repository.CaseRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceCheckServiceTest {

    @Mock
    private ComplianceRuleRepository complianceRuleRepository;
    @Mock
    private ComplianceCheckResultRepository complianceCheckResultRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TenderRepository tenderRepository;
    @Mock
    private CaseRepository caseRepository;

    private ComplianceCheckService complianceCheckService;

    @BeforeEach
    void setUp() {
        complianceCheckService = new ComplianceCheckService(
                complianceRuleRepository,
                complianceCheckResultRepository,
                projectRepository,
                tenderRepository,
                caseRepository);
    }

    @Test
    void checkProjectCompliance_ShouldPassWhenRelevantCaseLibraryCountMeetsThreshold() {
        Project project = Project.builder()
                .id(100L)
                .name("智慧园区综合治理项目")
                .tenderId(1L)
                .managerId(2L)
                .sourceModule("智慧园区")
                .sourceReasoningSummary("本次项目聚焦园区一体化治理")
                .build();
        ComplianceRule rule = experienceRule("""
                {"minYears":2,"minProjects":2}
                """);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(complianceRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(caseRepository.countWonCasesByFilters(
                Case.Industry.INFRASTRUCTURE,
                "智慧园区",
                LocalDate.now().minusYears(2).plusDays(1),
                null)).thenReturn(2L);
        when(complianceCheckResultRepository.save(any(ComplianceCheckResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = complianceCheckService.checkProjectCompliance(100L);

        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getPassed()).isTrue();
        ArgumentCaptor<ComplianceCheckResult> savedResult = ArgumentCaptor.forClass(ComplianceCheckResult.class);
        verify(complianceCheckResultRepository).save(savedResult.capture());
        assertThat(savedResult.getValue().getOverallStatus()).isEqualTo(ComplianceCheckResult.Status.COMPLIANT);
    }

    @Test
    void checkProjectCompliance_ShouldFailWhenOnlyUnrelatedCasesExist() {
        Project project = Project.builder()
                .id(101L)
                .name("能源调度分析项目")
                .tenderId(1L)
                .managerId(2L)
                .sourceModule("能源中台")
                .sourceReasoningSummary("建设电力与能源调度分析平台")
                .build();
        ComplianceRule rule = experienceRule("""
                {"minYears":3,"minProjects":1}
                """);

        when(projectRepository.findById(101L)).thenReturn(Optional.of(project));
        when(complianceRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(caseRepository.countWonCasesByFilters(
                Case.Industry.ENERGY,
                "能源中台",
                LocalDate.now().minusYears(3).plusDays(1),
                null)).thenReturn(0L);
        when(complianceCheckResultRepository.save(any(ComplianceCheckResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = complianceCheckService.checkProjectCompliance(101L);

        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getPassed()).isFalse();
        assertThat(result.getIssues().get(0).getRecommendation()).contains("案例库");
    }

    private ComplianceRule experienceRule(String definition) {
        return ComplianceRule.builder()
                .id(1L)
                .name("类似案例经验")
                .ruleType(ComplianceRule.RuleType.EXPERIENCE)
                .ruleDefinition(definition)
                .enabled(true)
                .build();
    }
}
