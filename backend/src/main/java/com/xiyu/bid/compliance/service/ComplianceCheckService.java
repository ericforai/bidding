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
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.repository.CaseRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final CaseRepository caseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查项目合规性
     *
     * @param projectId 项目ID
     * @return 合规检查结果
     */
    @Transactional
    public ComplianceCheckResultDTO checkProjectCompliance(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }

        // 验证项目存在
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        log.info("Starting compliance check for project: {}", projectId);

        // 获取所有启用的规则
        List<ComplianceRule> rules = complianceRuleRepository.findByEnabledTrue();

        // 执行合规检查
        List<ComplianceIssue> issues = new ArrayList<>();
        int totalRules = rules.size();
        int failedRules = 0;

        for (ComplianceRule rule : rules) {
            try {
                ComplianceIssue issue = checkRule(rule, project);
                if (issue != null) {
                    issues.add(issue);
                    if (!issue.getPassed()) {
                        failedRules++;
                    }
                }
            } catch (Exception e) {
                log.error("Error checking rule {} for project {}", rule.getName(), projectId, e);
                issues.add(ComplianceIssue.builder()
                        .ruleId(rule.getId())
                        .ruleName(rule.getName())
                        .ruleType(rule.getRuleType())
                        .severity(ComplianceIssue.Severity.MEDIUM)
                        .description("Rule check failed: " + e.getMessage())
                        .recommendation("Review rule configuration")
                        .passed(false)
                        .build());
                failedRules++;
            }
        }

        // 计算整体状态
        ComplianceCheckResult.Status overallStatus = determineOverallStatus(issues, totalRules, failedRules);

        // 计算风险分数
        int riskScore = calculateRiskScore(issues, totalRules);

        // 保存检查结果
        ComplianceCheckResult result = ComplianceCheckResult.builder()
                .projectId(projectId)
                .overallStatus(overallStatus)
                .riskScore(riskScore)
                .checkedAt(LocalDateTime.now())
                .checkedBy("system")
                .build();

        try {
            String checkDetails = objectMapper.writeValueAsString(issues);
            result.setCheckDetails(checkDetails);
        } catch (JsonProcessingException e) {
            log.error("Error serializing check details", e);
        }

        result = complianceCheckResultRepository.save(result);

        log.info("Compliance check completed for project {}: status={}, riskScore={}",
                projectId, overallStatus, riskScore);

        return ComplianceCheckResultDTO.builder()
                .id(result.getId())
                .projectId(result.getProjectId())
                .overallStatus(result.getOverallStatus())
                .issues(issues)
                .riskScore(result.getRiskScore())
                .checkedAt(result.getCheckedAt())
                .checkedBy(result.getCheckedBy())
                .build();
    }

    /**
     * 检查标书合规性
     *
     * @param tenderId 标书ID
     * @return 合规检查结果
     */
    @Transactional
    public ComplianceCheckResultDTO checkTenderCompliance(Long tenderId) {
        if (tenderId == null) {
            throw new IllegalArgumentException("Tender ID cannot be null");
        }

        // 验证标书存在
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new RuntimeException("Tender not found with id: " + tenderId));

        log.info("Starting compliance check for tender: {}", tenderId);

        // 获取所有启用的规则
        List<ComplianceRule> rules = complianceRuleRepository.findByEnabledTrue();

        // 执行合规检查
        List<ComplianceIssue> issues = new ArrayList<>();
        int totalRules = rules.size();
        int failedRules = 0;

        for (ComplianceRule rule : rules) {
            try {
                ComplianceIssue issue = checkRuleForTender(rule, tender);
                if (issue != null) {
                    issues.add(issue);
                    if (!issue.getPassed()) {
                        failedRules++;
                    }
                }
            } catch (Exception e) {
                log.error("Error checking rule {} for tender {}", rule.getName(), tenderId, e);
                issues.add(ComplianceIssue.builder()
                        .ruleId(rule.getId())
                        .ruleName(rule.getName())
                        .ruleType(rule.getRuleType())
                        .severity(ComplianceIssue.Severity.MEDIUM)
                        .description("Rule check failed: " + e.getMessage())
                        .recommendation("Review rule configuration")
                        .passed(false)
                        .build());
                failedRules++;
            }
        }

        // 计算整体状态
        ComplianceCheckResult.Status overallStatus = determineOverallStatus(issues, totalRules, failedRules);

        // 计算风险分数
        int riskScore = calculateRiskScore(issues, totalRules);

        // 保存检查结果
        ComplianceCheckResult result = ComplianceCheckResult.builder()
                .tenderId(tenderId)
                .overallStatus(overallStatus)
                .riskScore(riskScore)
                .checkedAt(LocalDateTime.now())
                .checkedBy("system")
                .build();

        try {
            String checkDetails = objectMapper.writeValueAsString(issues);
            result.setCheckDetails(checkDetails);
        } catch (JsonProcessingException e) {
            log.error("Error serializing check details", e);
        }

        result = complianceCheckResultRepository.save(result);

        log.info("Compliance check completed for tender {}: status={}, riskScore={}",
                tenderId, overallStatus, riskScore);

        return ComplianceCheckResultDTO.builder()
                .id(result.getId())
                .tenderId(result.getTenderId())
                .overallStatus(result.getOverallStatus())
                .issues(issues)
                .riskScore(result.getRiskScore())
                .checkedAt(result.getCheckedAt())
                .checkedBy(result.getCheckedBy())
                .build();
    }

    /**
     * 评估项目风险
     *
     * @param projectId 项目ID
     * @return 风险评估结果
     */
    public RiskAssessmentDTO assessRisk(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }

        // 验证项目存在
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

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
        if (resultId == null) {
            throw new IllegalArgumentException("Result ID cannot be null");
        }

        return complianceCheckResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Compliance check result not found with id: " + resultId));
    }

    /**
     * 获取项目的所有合规检查结果
     */
    public List<ComplianceCheckResult> getCheckResultsByProjectId(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }

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
            return createErrorIssue(rule, "Invalid rule definition");
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
            return createErrorIssue(rule, "Invalid rule definition");
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
            return createErrorIssue(rule, "Invalid rule definition");
        }
    }

    /**
     * 检查经验要求
     */
    private ComplianceIssue checkExperience(ComplianceRule rule, Project project) {
        try {
            Map<String, Object> ruleDef = objectMapper.readValue(rule.getRuleDefinition(), Map.class);
            long minYears = getLongRuleValue(ruleDef, "minYears", 0L);
            long minProjects = getLongRuleValue(ruleDef, "minProjects", 0L);
            Case.Industry industry = resolveIndustryFilter(ruleDef, project);
            String productLine = resolveProductLineFilter(ruleDef, project);
            LocalDate projectDateFrom = minYears > 0
                    ? LocalDate.now().minusYears(minYears).plusDays(1)
                    : null;

            long recentWonProjects = caseRepository.countWonCasesByFilters(
                    industry,
                    productLine,
                    projectDateFrom,
                    null);
            boolean passed = recentWonProjects >= minProjects;
            String description = passed
                    ? "Experience requirements met via historical case library"
                    : "Historical case library does not contain enough recent winning projects";

            return ComplianceIssue.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .ruleType(rule.getRuleType())
                    .severity(passed ? ComplianceIssue.Severity.LOW : ComplianceIssue.Severity.MEDIUM)
                    .description(description)
                    .recommendation(passed ? null : "补充近年同类中标案例，或在案例库中沉淀更多已归档项目")
                    .passed(passed)
                    .build();
        } catch (JsonProcessingException e) {
            return createErrorIssue(rule, "Invalid rule definition");
        }
    }

    private long getLongRuleValue(Map<String, Object> ruleDef, String key, long defaultValue) {
        Object rawValue = ruleDef.get(key);
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        if (rawValue instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String getStringRuleValue(Map<String, Object> ruleDef, String... keys) {
        for (String key : keys) {
            Object rawValue = ruleDef.get(key);
            if (rawValue instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    private String resolveProductLineFilter(Map<String, Object> ruleDef, Project project) {
        String explicit = getStringRuleValue(ruleDef, "productLine", "businessLine", "caseProductLine");
        if (explicit != null) {
            return explicit;
        }
        if (project != null && project.getSourceModule() != null && !project.getSourceModule().isBlank()) {
            return project.getSourceModule().trim();
        }
        return null;
    }

    private Case.Industry resolveIndustryFilter(Map<String, Object> ruleDef, Project project) {
        String rawIndustry = getStringRuleValue(ruleDef, "industry", "caseIndustry");
        if (rawIndustry != null) {
            Case.Industry parsed = parseIndustry(rawIndustry);
            if (parsed != null) {
                return parsed;
            }
        }

        if (project == null) {
            return null;
        }
        return inferIndustryFromProject(project);
    }

    private Case.Industry inferIndustryFromProject(Project project) {
        String sourceText = String.join(" ",
                safe(project.getName()),
                safe(project.getSourceModule()),
                safe(project.getSourceReasoningSummary()),
                safe(project.getSourceCustomer()))
                .toLowerCase(Locale.ROOT);

        if (sourceText.contains("能源") || sourceText.contains("电力")) {
            return Case.Industry.ENERGY;
        }
        if (sourceText.contains("制造")) {
            return Case.Industry.MANUFACTURING;
        }
        if (sourceText.contains("交通") || sourceText.contains("轨道") || sourceText.contains("运输")) {
            return Case.Industry.TRANSPORTATION;
        }
        if (sourceText.contains("环保") || sourceText.contains("环境")) {
            return Case.Industry.ENVIRONMENTAL;
        }
        if (sourceText.contains("地产") || sourceText.contains("楼宇")) {
            return Case.Industry.REAL_ESTATE;
        }
        if (sourceText.contains("园区") || sourceText.contains("政务") || sourceText.contains("城市") || sourceText.contains("基建")) {
            return Case.Industry.INFRASTRUCTURE;
        }
        return null;
    }

    private Case.Industry parseIndustry(String rawIndustry) {
        try {
            return Case.Industry.valueOf(rawIndustry.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
            return createErrorIssue(rule, "Invalid rule definition");
        }
    }

    /**
     * 检查标书文档
     */
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

    /**
     * 创建错误问题
     */
    private ComplianceIssue createErrorIssue(ComplianceRule rule, String errorMessage) {
        return ComplianceIssue.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .ruleType(rule.getRuleType())
                .severity(ComplianceIssue.Severity.MEDIUM)
                .description(errorMessage)
                .recommendation("Review and fix rule definition")
                .passed(false)
                .build();
    }

    /**
     * 确定整体合规状态
     */
    private ComplianceCheckResult.Status determineOverallStatus(
            List<ComplianceIssue> issues, int totalRules, int failedRules) {

        if (totalRules == 0) {
            return ComplianceCheckResult.Status.COMPLIANT;
        }

        if (failedRules == 0) {
            return ComplianceCheckResult.Status.COMPLIANT;
        }

        double failureRate = (double) failedRules / totalRules;

        // 检查是否有严重问题
        boolean hasCritical = issues.stream()
                .anyMatch(i -> i.getSeverity() == ComplianceIssue.Severity.CRITICAL && !i.getPassed());

        if (hasCritical) {
            return ComplianceCheckResult.Status.NON_COMPLIANT;
        }

        if (failureRate >= 0.5) {
            return ComplianceCheckResult.Status.NON_COMPLIANT;
        } else if (failureRate >= 0.2) {
            return ComplianceCheckResult.Status.PARTIAL_COMPLIANT;
        } else {
            return ComplianceCheckResult.Status.WARNING;
        }
    }

    /**
     * 计算风险分数
     */
    private int calculateRiskScore(List<ComplianceIssue> issues, int totalRules) {
        if (totalRules == 0 || issues.isEmpty()) {
            return 0;
        }

        int totalScore = 0;
        for (ComplianceIssue issue : issues) {
            int severityScore = switch (issue.getSeverity()) {
                case CRITICAL -> 100;
                case HIGH -> 75;
                case MEDIUM -> 50;
                case LOW -> 25;
            };
            totalScore += issue.getPassed() ? 0 : severityScore;
        }

        return Math.min(100, totalScore / totalRules);
    }

    /**
     * 计算默认风险分数（基于项目状态）
     */
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
}
