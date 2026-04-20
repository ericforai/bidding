// Input: compliance repositories, DTOs, and support services
// Output: Compliance Check orchestration over pure compliance policies
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.compliance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.compliance.dto.ComplianceCheckResultDTO;
import com.xiyu.bid.compliance.dto.ComplianceIssue;
import com.xiyu.bid.compliance.dto.RiskAssessmentDTO;
import com.xiyu.bid.compliance.entity.ComplianceCheckResult;
import com.xiyu.bid.compliance.entity.ComplianceRule;
import com.xiyu.bid.compliance.repository.ComplianceCheckResultRepository;
import com.xiyu.bid.compliance.repository.ComplianceRuleRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 合规检查服务
 * 只负责加载聚合、调用纯规则核、持久化结果和组装 DTO。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceCheckService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ComplianceRuleRepository complianceRuleRepository;
    private final ComplianceCheckResultRepository complianceCheckResultRepository;
    private final ProjectRepository projectRepository;
    private final TenderRepository tenderRepository;
    private final ExperienceComplianceEvaluator experienceComplianceEvaluator;

    @Transactional
    public ComplianceCheckResultDTO checkProjectCompliance(Long projectId) {
        requireId(projectId, "Project ID");
        Project project = requireProject(projectId);
        log.info("Starting compliance check for project: {}", projectId);

        ComplianceRun run = evaluateRules(
                complianceRuleRepository.findByEnabledTrue(),
                rule -> evaluateProjectRule(rule, project),
                "project " + projectId
        );
        ComplianceCheckResult result = persistResult(
                ComplianceCheckResult.builder()
                        .projectId(projectId)
                        .overallStatus(run.evaluation().overallStatus())
                        .riskScore(run.evaluation().riskScore())
                        .checkedAt(LocalDateTime.now())
                        .checkedBy("system")
                        .build(),
                run.issues()
        );

        log.info("Compliance check completed for project {}: status={}, riskScore={}",
                projectId, run.evaluation().overallStatus(), run.evaluation().riskScore());
        return toCheckResultDTO(result, run.issues());
    }

    @Transactional
    public ComplianceCheckResultDTO checkTenderCompliance(Long tenderId) {
        requireId(tenderId, "Tender ID");
        Tender tender = requireTender(tenderId);
        log.info("Starting compliance check for tender: {}", tenderId);

        ComplianceRun run = evaluateRules(
                complianceRuleRepository.findByEnabledTrue(),
                rule -> ComplianceRuleEvaluator.evaluateTenderRule(rule, tender, OBJECT_MAPPER),
                "tender " + tenderId
        );
        ComplianceCheckResult result = persistResult(
                ComplianceCheckResult.builder()
                        .tenderId(tenderId)
                        .overallStatus(run.evaluation().overallStatus())
                        .riskScore(run.evaluation().riskScore())
                        .checkedAt(LocalDateTime.now())
                        .checkedBy("system")
                        .build(),
                run.issues()
        );

        log.info("Compliance check completed for tender {}: status={}, riskScore={}",
                tenderId, run.evaluation().overallStatus(), run.evaluation().riskScore());
        return toCheckResultDTO(result, run.issues());
    }

    public RiskAssessmentDTO assessRisk(Long projectId) {
        requireId(projectId, "Project ID");
        Project project = requireProject(projectId);
        ComplianceCheckResult latestResult = complianceCheckResultRepository
                .findTopByProjectIdOrderByCheckedAtDesc(projectId)
                .orElse(null);

        int riskScore = latestResult != null
                ? latestResult.getRiskScore()
                : ComplianceCheckPolicy.defaultRiskScore(project.getStatus());
        RiskAssessmentDTO.RiskLevel riskLevel = RiskAssessmentDTO.RiskLevel.fromScore(riskScore);

        return RiskAssessmentDTO.builder()
                .projectId(projectId)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .description(riskLevel.getDescription())
                .recommendation(ComplianceCheckPolicy.recommendationFor(riskLevel))
                .assessedAt(LocalDateTime.now())
                .build();
    }

    public ComplianceCheckResult getCheckResultById(Long resultId) {
        requireId(resultId, "Result ID");
        return complianceCheckResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Compliance check result not found with id: " + resultId));
    }

    public List<ComplianceCheckResult> getCheckResultsByProjectId(Long projectId) {
        requireId(projectId, "Project ID");
        return complianceCheckResultRepository.findByProjectId(projectId);
    }

    private ComplianceRun evaluateRules(
            List<ComplianceRule> rules,
            RuleEvaluator evaluator,
            String targetLabel
    ) {
        List<ComplianceIssue> issues = rules.stream()
                .map(rule -> safeEvaluate(rule, evaluator, targetLabel))
                .toList();
        return new ComplianceRun(issues, ComplianceCheckPolicy.summarize(issues, rules.size()));
    }

    private ComplianceIssue safeEvaluate(ComplianceRule rule, RuleEvaluator evaluator, String targetLabel) {
        try {
            return evaluator.evaluate(rule);
        } catch (RuntimeException exception) {
            log.error("Error checking rule {} for {}", rule.getName(), targetLabel, exception);
            return ComplianceIssueFactory.executionFailure(rule, exception);
        }
    }

    private ComplianceIssue evaluateProjectRule(ComplianceRule rule, Project project) {
        if (rule.getRuleType() == ComplianceRule.RuleType.EXPERIENCE) {
            return experienceComplianceEvaluator.evaluate(rule, project, OBJECT_MAPPER);
        }
        return ComplianceRuleEvaluator.evaluateProjectRule(rule, project, OBJECT_MAPPER);
    }

    private ComplianceCheckResult persistResult(ComplianceCheckResult result, List<ComplianceIssue> issues) {
        try {
            result.setCheckDetails(OBJECT_MAPPER.writeValueAsString(issues));
        } catch (JsonProcessingException exception) {
            log.error("Error serializing check details", exception);
        }
        return complianceCheckResultRepository.save(result);
    }

    private ComplianceCheckResultDTO toCheckResultDTO(ComplianceCheckResult result, List<ComplianceIssue> issues) {
        return ComplianceCheckResultDTO.builder()
                .id(result.getId())
                .projectId(result.getProjectId())
                .tenderId(result.getTenderId())
                .overallStatus(result.getOverallStatus())
                .issues(issues)
                .riskScore(result.getRiskScore())
                .checkedAt(result.getCheckedAt())
                .checkedBy(result.getCheckedBy())
                .build();
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

    private Tender requireTender(Long tenderId) {
        return tenderRepository.findById(tenderId)
                .orElseThrow(() -> new RuntimeException("Tender not found with id: " + tenderId));
    }

    private void requireId(Long id, String label) {
        if (id == null) {
            throw new IllegalArgumentException(label + " cannot be null");
        }
    }

    private record ComplianceRun(List<ComplianceIssue> issues, ComplianceCheckPolicy.Evaluation evaluation) {
    }

    @FunctionalInterface
    private interface RuleEvaluator {
        ComplianceIssue evaluate(ComplianceRule rule);
    }
}
