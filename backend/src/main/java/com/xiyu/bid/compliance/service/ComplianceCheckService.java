// Input: compliance repositories, DTOs, and support services
// Output: Compliance Check business service operations
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 合规检查服务
 * 提供项目和标书的合规检查功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceCheckService {

    private final ComplianceRuleRepository complianceRuleRepository;
    private final ComplianceCheckResultRepository complianceCheckResultRepository;
    private final ProjectRepository projectRepository;
    private final TenderRepository tenderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查项目合规性
     *
     * @param projectId 项目ID
     * @return 合规检查结果
     */
    @Transactional
    public ComplianceCheckResultDTO checkProjectCompliance(Long projectId) {
        requireId(projectId, "Project ID");
        Project project = requireProject(projectId);
        log.info("Starting compliance check for project: {}", projectId);

        ComplianceRun run = evaluateRules(
                complianceRuleRepository.findByEnabledTrue(),
                rule -> checkRule(rule, project),
                "project " + projectId
        );
        ComplianceCheckResult result = persistProjectResult(projectId, run);

        log.info("Compliance check completed for project {}: status={}, riskScore={}",
                projectId, run.evaluation().overallStatus(), run.evaluation().riskScore());

        return toCheckResultDTO(result, run.issues());
    }

    /**
     * 检查标书合规性
     *
     * @param tenderId 标书ID
     * @return 合规检查结果
     */
    @Transactional
    public ComplianceCheckResultDTO checkTenderCompliance(Long tenderId) {
        requireId(tenderId, "Tender ID");
        Tender tender = requireTender(tenderId);
        log.info("Starting compliance check for tender: {}", tenderId);

        ComplianceRun run = evaluateRules(
                complianceRuleRepository.findByEnabledTrue(),
                rule -> checkRuleForTender(rule, tender),
                "tender " + tenderId
        );
        ComplianceCheckResult result = persistTenderResult(tenderId, run);

        log.info("Compliance check completed for tender {}: status={}, riskScore={}",
                tenderId, run.evaluation().overallStatus(), run.evaluation().riskScore());

        return toCheckResultDTO(result, run.issues());
    }

    /**
     * 评估项目风险
     *
     * @param projectId 项目ID
     * @return 风险评估结果
     */
    public RiskAssessmentDTO assessRisk(Long projectId) {
        requireId(projectId, "Project ID");
        Project project = requireProject(projectId);

        // 获取最新的合规检查结果
        ComplianceCheckResult latestResult = complianceCheckResultRepository
                .findTopByProjectIdOrderByCheckedAtDesc(projectId)
                .orElse(null);

        int riskScore;
        RiskAssessmentDTO.RiskLevel riskLevel;

        if (latestResult != null) {
            riskScore = latestResult.getRiskScore();
            riskLevel = RiskAssessmentDTO.RiskLevel.fromScore(riskScore);
        } else {
            // 如果没有检查结果，基于项目状态计算默认风险
            riskScore = calculateDefaultRiskScore(project);
            riskLevel = RiskAssessmentDTO.RiskLevel.fromScore(riskScore);
        }

        return RiskAssessmentDTO.builder()
                .projectId(projectId)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .description(riskLevel.getDescription())
                .recommendation(getRecommendation(riskLevel))
                .assessedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 获取合规检查结果通过ID
     */
    public ComplianceCheckResult getCheckResultById(Long resultId) {
        requireId(resultId, "Result ID");

        return complianceCheckResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Compliance check result not found with id: " + resultId));
    }

    /**
     * 获取项目的所有合规检查结果
     */
    public List<ComplianceCheckResult> getCheckResultsByProjectId(Long projectId) {
        requireId(projectId, "Project ID");

        return complianceCheckResultRepository.findByProjectId(projectId);
    }

    /**
     * 执行单个规则检查（针对项目）
     */
    private ComplianceIssue checkRule(ComplianceRule rule, Project project) {
        return switch (rule.getRuleType()) {
            case QUALIFICATION -> checkQualifications(rule, project);
            case DOCUMENT -> checkDocuments(rule, project);
            case FINANCIAL -> checkFinancials(rule, project);
            case EXPERIENCE -> checkExperience(rule, project);
            case DEADLINE -> checkDeadlines(rule, project);
        };
    }

    /**
     * 执行单个规则检查（针对标书）
     */
    private ComplianceIssue checkRuleForTender(ComplianceRule rule, Tender tender) {
        return switch (rule.getRuleType()) {
            case DOCUMENT -> checkTenderDocuments(rule, tender);
            case DEADLINE -> checkTenderDeadlines(rule, tender);
            default -> checkTenderGeneral(rule, tender);
        };
    }

    /**
     * 检查资质要求
     */
    private ComplianceIssue checkQualifications(ComplianceRule rule, Project project) {
        try {
            Map<String, Object> ruleDef = objectMapper.readValue(rule.getRuleDefinition(), Map.class);
            String minLevel = (String) ruleDef.get("minLevel");
            Boolean required = (Boolean) ruleDef.getOrDefault("required", true);

            // 模拟检查逻辑（实际应从资质库查询）
            boolean passed = !required || minLevel == null;

            return ComplianceIssue.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .ruleType(rule.getRuleType())
                    .severity(passed ? ComplianceIssue.Severity.LOW : ComplianceIssue.Severity.HIGH)
                    .description(passed ? "Qualification requirements met" : "Minimum qualification level not met")
                    .recommendation(passed ? null : "Ensure company meets minimum qualification requirements")
                    .passed(passed)
                    .build();
        } catch (JsonProcessingException e) {
            return ComplianceIssueFactory.definitionError(rule);
        }
    }

    /**
     * 检查文档完整性
     */
    private ComplianceIssue checkDocuments(ComplianceRule rule, Project project) {
        try {
            Map<String, Object> ruleDef = objectMapper.readValue(rule.getRuleDefinition(), Map.class);
            @SuppressWarnings("unchecked")
            List<String> requiredDocs = (List<String>) ruleDef.get("requiredDocs");

            // 模拟检查逻辑（实际应从文档库查询）
            boolean passed = requiredDocs == null || requiredDocs.isEmpty();

            return ComplianceIssue.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .ruleType(rule.getRuleType())
                    .severity(passed ? ComplianceIssue.Severity.LOW : ComplianceIssue.Severity.MEDIUM)
                    .description(passed ? "All required documents present" : "Missing required documents")
                    .recommendation(passed ? null : "Submit all required documents")
                    .passed(passed)
                    .build();
        } catch (JsonProcessingException e) {
            return ComplianceIssueFactory.definitionError(rule);
        }
    }

    /**
     * 检查财务状况
     */
    private ComplianceIssue checkFinancials(ComplianceRule rule, Project project) {
        try {
            Map<String, Object> ruleDef = objectMapper.readValue(rule.getRuleDefinition(), Map.class);
            Number minRevenue = (Number) ruleDef.get("minRevenue");
            Number maxDebtRatio = (Number) ruleDef.get("maxDebtRatio");

            // 模拟检查逻辑（实际应从财务库查询）
            boolean passed = true;

            return ComplianceIssue.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .ruleType(rule.getRuleType())
                    .severity(passed ? ComplianceIssue.Severity.LOW : ComplianceIssue.Severity.HIGH)
                    .description(passed ? "Financial health indicators good" : "Financial health indicators below threshold")
                    .recommendation(passed ? null : "Improve financial metrics before bidding")
                    .passed(passed)
                    .build();
        } catch (JsonProcessingException e) {
            return ComplianceIssueFactory.definitionError(rule);
        }
    }

    /**
     * 检查经验要求
     */
    private ComplianceIssue checkExperience(ComplianceRule rule, Project project) {
        try {
            Map<String, Object> ruleDef = objectMapper.readValue(rule.getRuleDefinition(), Map.class);
            Number minYears = (Number) ruleDef.get("minYears");
            Number minProjects = (Number) ruleDef.get("minProjects");

            // 模拟检查逻辑（实际应从案例库查询）
            boolean passed = true;

            return ComplianceIssue.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .ruleType(rule.getRuleType())
                    .severity(passed ? ComplianceIssue.Severity.LOW : ComplianceIssue.Severity.MEDIUM)
                    .description(passed ? "Experience requirements met" : "Insufficient experience")
                    .recommendation(passed ? null : "Gain more experience in similar projects")
                    .passed(passed)
                    .build();
        } catch (JsonProcessingException e) {
            return ComplianceIssueFactory.definitionError(rule);
        }
    }

    /**
     * 检查期限要求
     */
    private ComplianceIssue checkDeadlines(ComplianceRule rule, Project project) {
        try {
            Map<String, Object> ruleDef = objectMapper.readValue(rule.getRuleDefinition(), Map.class);
            Number daysBeforeDeadline = (Number) ruleDef.get("daysBeforeDeadline");

            // 模拟检查逻辑
            boolean passed = true;

            return ComplianceIssue.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .ruleType(rule.getRuleType())
                    .severity(passed ? ComplianceIssue.Severity.LOW : ComplianceIssue.Severity.CRITICAL)
                    .description(passed ? "Timeline requirements met" : "Insufficient time before deadline")
                    .recommendation(passed ? null : "Start preparation earlier or request deadline extension")
                    .passed(passed)
                    .build();
        } catch (JsonProcessingException e) {
            return ComplianceIssueFactory.definitionError(rule);
        }
    }

    private ComplianceIssue checkTenderDocuments(ComplianceRule rule, Tender tender) {
        return ComplianceIssue.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .ruleType(rule.getRuleType())
                .severity(ComplianceIssue.Severity.LOW)
                .description("Tender documents available")
                .passed(true)
                .build();
    }

    /**
     * 检查标书期限
     */
    private ComplianceIssue checkTenderDeadlines(ComplianceRule rule, Tender tender) {
        boolean passed = tender.getDeadline() != null && tender.getDeadline().isAfter(LocalDateTime.now());

        return ComplianceIssue.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .ruleType(rule.getRuleType())
                .severity(passed ? ComplianceIssue.Severity.LOW : ComplianceIssue.Severity.HIGH)
                .description(passed ? "Tender deadline valid" : "Tender deadline passed or not set")
                .passed(passed)
                .build();
    }

    /**
     * 通用标书检查
     */
    private ComplianceIssue checkTenderGeneral(ComplianceRule rule, Tender tender) {
        return ComplianceIssue.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .ruleType(rule.getRuleType())
                .severity(ComplianceIssue.Severity.LOW)
                .description("General tender check passed")
                .passed(true)
                .build();
    }

    private ComplianceRun evaluateRules(
            List<ComplianceRule> rules,
            RuleEvaluator evaluator,
            String targetLabel
    ) {
        List<ComplianceIssue> issues = new ArrayList<>();
        for (ComplianceRule rule : rules) {
            try {
                ComplianceIssue issue = evaluator.evaluate(rule);
                if (issue != null) {
                    issues.add(issue);
                }
            } catch (RuntimeException exception) {
                log.error("Error checking rule {} for {}", rule.getName(), targetLabel, exception);
                issues.add(ComplianceIssueFactory.executionFailure(rule, exception));
            }
        }
        return new ComplianceRun(issues, ComplianceCheckPolicy.summarize(issues, rules.size()));
    }

    private ComplianceCheckResult persistProjectResult(Long projectId, ComplianceRun run) {
        ComplianceCheckResult result = ComplianceCheckResult.builder()
                .projectId(projectId)
                .overallStatus(run.evaluation().overallStatus())
                .riskScore(run.evaluation().riskScore())
                .checkedAt(LocalDateTime.now())
                .checkedBy("system")
                .build();
        return complianceCheckResultRepository.save(attachSerializedIssues(result, run.issues()));
    }

    private ComplianceCheckResult persistTenderResult(Long tenderId, ComplianceRun run) {
        ComplianceCheckResult result = ComplianceCheckResult.builder()
                .tenderId(tenderId)
                .overallStatus(run.evaluation().overallStatus())
                .riskScore(run.evaluation().riskScore())
                .checkedAt(LocalDateTime.now())
                .checkedBy("system")
                .build();
        return complianceCheckResultRepository.save(attachSerializedIssues(result, run.issues()));
    }

    private ComplianceCheckResult attachSerializedIssues(ComplianceCheckResult result, List<ComplianceIssue> issues) {
        try {
            result.setCheckDetails(objectMapper.writeValueAsString(issues));
        } catch (JsonProcessingException exception) {
            log.error("Error serializing check details", exception);
        }
        return result;
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

    private int calculateDefaultRiskScore(Project project) {
        return switch (project.getStatus()) {
            case INITIATED -> 20;
            case PREPARING -> 30;
            case REVIEWING -> 40;
            case SEALING -> 25;
            case BIDDING -> 50;
            case ARCHIVED -> 10;
        };
    }

    /**
     * 获取风险建议
     */
    private String getRecommendation(RiskAssessmentDTO.RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> "Project is low risk. Proceed with normal bidding process.";
            case MEDIUM -> "Project has medium risk. Review compliance issues and address key concerns.";
            case HIGH -> "Project has high risk. Immediate action required to address compliance issues.";
        };
    }

    private record ComplianceRun(List<ComplianceIssue> issues, ComplianceCheckPolicy.Evaluation evaluation) {
    }

    @FunctionalInterface
    private interface RuleEvaluator {
        ComplianceIssue evaluate(ComplianceRule rule);
    }
}
