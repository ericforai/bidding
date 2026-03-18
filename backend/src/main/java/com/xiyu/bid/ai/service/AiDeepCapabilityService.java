// Input: AI repositories, DTOs, and support services
// Output: AI Deep Capability business service operations
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.ai.dto.*;
import com.xiyu.bid.ai.entity.AiAnalysisJob;
import com.xiyu.bid.ai.entity.AiAnalysisResult;
import com.xiyu.bid.ai.entity.ProjectScorePreview;
import com.xiyu.bid.ai.repository.AiAnalysisJobRepository;
import com.xiyu.bid.ai.repository.AiAnalysisResultRepository;
import com.xiyu.bid.ai.repository.ProjectScorePreviewRepository;
import com.xiyu.bid.competitionintel.dto.CompetitionAnalysisDTO;
import com.xiyu.bid.competitionintel.service.CompetitionIntelService;
import com.xiyu.bid.compliance.entity.ComplianceCheckResult;
import com.xiyu.bid.compliance.service.ComplianceCheckService;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.roi.dto.ROIAnalysisDTO;
import com.xiyu.bid.roi.service.ROIAnalysisService;
import com.xiyu.bid.scoreanalysis.dto.ScoreAnalysisDTO;
import com.xiyu.bid.scoreanalysis.service.ScoreAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiDeepCapabilityService {

    private final AiService aiService;
    private final TenderRepository tenderRepository;
    private final ProjectRepository projectRepository;
    private final AiAnalysisJobRepository aiAnalysisJobRepository;
    private final AiAnalysisResultRepository aiAnalysisResultRepository;
    private final ProjectScorePreviewRepository projectScorePreviewRepository;
    private final ScoreAnalysisService scoreAnalysisService;
    private final CompetitionIntelService competitionIntelService;
    private final ComplianceCheckService complianceCheckService;
    private final ROIAnalysisService roiAnalysisService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<TenderAiAnalysisDTO> getLatestTenderAnalysis(Long tenderId) {
        return aiAnalysisResultRepository
                .findFirstByTenderIdAndAnalysisTypeOrderByCreatedAtDesc(tenderId, AiAnalysisJob.AnalysisType.TENDER_ANALYSIS)
                .map(this::deserializeTenderAnalysis);
    }

    @Transactional
    public TenderAiAnalysisDTO analyzeTender(Long tenderId, Long requestedBy) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", tenderId.toString()));

        AiAnalysisJob job = aiAnalysisJobRepository.save(AiAnalysisJob.builder()
                .analysisType(AiAnalysisJob.AnalysisType.TENDER_ANALYSIS)
                .targetType(AiAnalysisJob.TargetType.TENDER)
                .targetId(tenderId)
                .requestedBy(requestedBy)
                .status(AiAnalysisJob.JobStatus.PENDING)
                .build());

        try {
            AiAnalysisResponse response = aiService.analyzeTenderSync(tenderId, Map.of(
                    "budget", tender.getBudget() == null ? BigDecimal.ZERO : tender.getBudget(),
                    "source", tender.getSource() == null ? "" : tender.getSource(),
                    "deadline", tender.getDeadline() == null ? "" : tender.getDeadline().toString()
            ));

            TenderAiAnalysisDTO dto = buildTenderAnalysisDTO(tenderId, response);
            aiAnalysisResultRepository.save(AiAnalysisResult.builder()
                    .jobId(job.getId())
                    .tenderId(tenderId)
                    .analysisType(AiAnalysisJob.AnalysisType.TENDER_ANALYSIS)
                    .score(dto.getWinScore())
                    .riskLevel(resolveRiskLevelText(response))
                    .suggestion(dto.getSuggestion())
                    .payloadJson(writeValue(dto))
                    .build());

            job.setStatus(AiAnalysisJob.JobStatus.COMPLETED);
            job.setCompletedAt(java.time.LocalDateTime.now());
            aiAnalysisJobRepository.save(job);
            return dto;
        } catch (RuntimeException ex) {
            job.setStatus(AiAnalysisJob.JobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(java.time.LocalDateTime.now());
            aiAnalysisJobRepository.save(job);
            throw ex;
        }
    }

    @Transactional
    public ProjectScorePreviewDTO createScorePreview(ProjectScorePreviewRequestDTO request, Long requestedBy) {
        AiAnalysisJob job = aiAnalysisJobRepository.save(AiAnalysisJob.builder()
                .analysisType(AiAnalysisJob.AnalysisType.PROJECT_SCORE_PREVIEW)
                .targetType(request.getProjectId() != null ? AiAnalysisJob.TargetType.PROJECT : AiAnalysisJob.TargetType.TENDER)
                .targetId(request.getProjectId() != null ? request.getProjectId() : request.getTenderId())
                .requestedBy(requestedBy)
                .status(AiAnalysisJob.JobStatus.PENDING)
                .build());

        try {
            ProjectScorePreviewDTO dto = buildPreview(request);
            ProjectScorePreview entity = projectScorePreviewRepository.save(ProjectScorePreview.builder()
                    .projectId(request.getProjectId())
                    .tenderId(request.getTenderId())
                    .projectName(request.getProjectName())
                    .industry(request.getIndustry())
                    .budget(request.getBudget())
                    .tagsJson(writeValue(request.getTags() == null ? List.of() : request.getTags()))
                    .winScore(dto.getAiSummary().getWinScore())
                    .winLevel(dto.getAiSummary().getWinLevel())
                    .payloadJson(writeValue(dto))
                    .build());
            dto.setId(entity.getId());

            job.setStatus(AiAnalysisJob.JobStatus.COMPLETED);
            job.setCompletedAt(java.time.LocalDateTime.now());
            aiAnalysisJobRepository.save(job);
            return dto;
        } catch (RuntimeException ex) {
            job.setStatus(AiAnalysisJob.JobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(java.time.LocalDateTime.now());
            aiAnalysisJobRepository.save(job);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ProjectAiCardsDTO getProjectAiCards(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));

        ApiResponse<ScoreAnalysisDTO> scoreResponse = scoreAnalysisService.getAnalysisByProject(projectId);
        ScoreAnalysisDTO score = scoreResponse.isSuccess() ? scoreResponse.getData() : null;

        List<CompetitionAnalysisDTO> competition = competitionIntelService.getAnalysisByProject(projectId);
        List<ComplianceCheckResult> compliance = complianceCheckService.getCheckResultsByProjectId(projectId);

        ROIAnalysisDTO roi = null;
        try {
            roi = roiAnalysisService.getAnalysisByProject(projectId);
        } catch (ResourceNotFoundException ignored) {
            log.debug("No ROI analysis for project {}", projectId);
        }

        return ProjectAiCardsDTO.builder()
                .score(score)
                .competition(competition)
                .compliance(compliance)
                .roi(roi)
                .build();
    }

    private TenderAiAnalysisDTO deserializeTenderAnalysis(AiAnalysisResult result) {
        try {
            return objectMapper.readValue(result.getPayloadJson(), TenderAiAnalysisDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to read tender AI analysis result", e);
        }
    }

    private TenderAiAnalysisDTO buildTenderAnalysisDTO(Long tenderId, AiAnalysisResponse response) {
        List<DimensionScoreViewDTO> dimensions = Optional.ofNullable(response.getDimensionScores())
                .orElse(List.of())
                .stream()
                .map(item -> DimensionScoreViewDTO.builder()
                        .name(mapDimensionName(item.getDimension()))
                        .score(item.getScore())
                        .build())
                .toList();

        List<AiRiskItemDTO> risks = new ArrayList<>();
        List<String> weaknesses = Optional.ofNullable(response.getWeaknesses()).orElse(List.of());
        List<String> recommendations = Optional.ofNullable(response.getRecommendations()).orElse(List.of());
        for (int i = 0; i < Math.max(weaknesses.size(), recommendations.size()); i++) {
            String desc = i < weaknesses.size() ? weaknesses.get(i) : "AI 识别到的潜在风险";
            String action = i < recommendations.size() ? recommendations.get(i) : "建议继续补强相关材料";
            risks.add(AiRiskItemDTO.builder()
                    .level(resolveRiskItemLevel(response.getRiskLevel(), i))
                    .desc(desc)
                    .action(action)
                    .build());
        }

        List<AiAutoTaskDTO> autoTasks = recommendations.stream()
                .map(item -> AiAutoTaskDTO.builder()
                        .id("AI-" + Math.abs(item.hashCode()))
                        .title(item)
                        .owner("AI助手")
                        .dueDate(java.time.LocalDate.now().plusDays(3).toString())
                        .priority(response.getRiskLevel() == Tender.RiskLevel.HIGH ? "high" : "medium")
                        .build())
                .toList();

        return TenderAiAnalysisDTO.builder()
                .tenderId(tenderId)
                .winScore(response.getScore())
                .suggestion(recommendations.isEmpty() ? defaultTenderSuggestion(response.getRiskLevel()) : recommendations.get(0))
                .dimensionScores(dimensions)
                .risks(risks)
                .autoTasks(autoTasks)
                .build();
    }

    private ProjectScorePreviewDTO buildPreview(ProjectScorePreviewRequestDTO request) {
        List<String> tags = request.getTags() == null ? List.of() : request.getTags();
        int winScore = 60;
        if ("政府".equals(request.getIndustry())) winScore += 10;
        if ("央国企".equals(request.getIndustry())) winScore += 5;
        if (tags.contains("信创")) winScore += 5;
        if (tags.contains("智慧城市")) winScore += 5;
        if (request.getBudget().compareTo(new BigDecimal("500")) > 0) winScore -= 5;
        winScore = Math.max(0, Math.min(100, winScore));
        String winLevel = winScore >= 80 ? "high" : winScore >= 60 ? "medium" : "low";

        List<ScoreCategoryCoverageDTO> categories = new ArrayList<>();
        categories.add(ScoreCategoryCoverageDTO.builder().name("技术").weight(40).covered(tags.contains("信创") ? 32 : 28).total(40).percentage(tags.contains("信创") ? 80 : 70).gaps(tags.contains("信创") ? List.of("大数据平台") : List.of("物联网架构方案", "大数据平台")).build());
        categories.add(ScoreCategoryCoverageDTO.builder().name("商务").weight(30).covered(25).total(30).percentage(83).gaps(List.of()).build());
        categories.add(ScoreCategoryCoverageDTO.builder().name("案例").weight(20).covered(tags.contains("智慧城市") ? 14 : 8).total(20).percentage(tags.contains("智慧城市") ? 70 : 40).gaps(tags.contains("智慧城市") ? List.of() : List.of("智慧城市案例")).build());
        categories.add(ScoreCategoryCoverageDTO.builder().name("服务").weight(10).covered(7).total(10).percentage(70).gaps(List.of("运维承诺")).build());

        List<GapItemDTO> gapItems = categories.stream()
                .flatMap(category -> category.getGaps().stream().map(gap -> GapItemDTO.builder()
                        .category(category.getName())
                        .scorePoint(gap)
                        .required(resolveGapRequirement(gap))
                        .status("missing")
                        .build()))
                .toList();

        List<GeneratedTaskDTO> tasks = gapItems.stream()
                .sorted(Comparator.comparing(GapItemDTO::getCategory))
                .map(item -> GeneratedTaskDTO.builder()
                        .name("补齐" + item.getScorePoint())
                        .priority("技术".equals(item.getCategory()) ? "high" : "medium")
                        .suggestion(item.getRequired())
                        .selected(true)
                        .build())
                .toList();

        List<AiSummaryRiskDTO> risks = gapItems.stream()
                .limit(3)
                .map(item -> AiSummaryRiskDTO.builder()
                        .level("技术".equals(item.getCategory()) ? "high" : "medium")
                        .content(item.getScorePoint() + " 仍缺失，可能影响评分")
                        .build())
                .toList();

        List<String> suggestions = new ArrayList<>();
        suggestions.add("优先补充关键评分点材料，先处理高权重项");
        if (tags.contains("信创")) {
            suggestions.add("突出国产化兼容和信创生态证明材料");
        }
        if (!tags.contains("智慧城市")) {
            suggestions.add("补充智慧城市类案例，提升案例项覆盖率");
        }

        return ProjectScorePreviewDTO.builder()
                .projectId(request.getProjectId())
                .tenderId(request.getTenderId())
                .aiSummary(AiSummaryViewDTO.builder()
                        .winScore(winScore)
                        .winLevel(winLevel)
                        .risks(risks)
                        .suggestions(suggestions)
                        .build())
                .scoreAnalysis(ScoreAnalysisPreviewDTO.builder()
                        .scoreCategories(categories)
                        .gapItems(gapItems)
                        .build())
                .generatedTasks(tasks)
                .build();
    }

    private String resolveGapRequirement(String gap) {
        return switch (gap) {
            case "物联网架构方案" -> "架构图+技术说明";
            case "大数据平台" -> "平台架构+性能指标";
            case "智慧城市案例" -> "至少1个同类案例";
            case "运维承诺" -> "3年免费运维承诺";
            default -> "补充相关证明材料";
        };
    }

    private String mapDimensionName(String name) {
        return switch (name) {
            case "Technical" -> "需求匹配";
            case "Financial" -> "竞争态势";
            case "Timing" -> "交付能力";
            case "Team" -> "客户关系";
            case "Resources" -> "资质满足";
            case "Risk" -> "竞争态势";
            default -> name;
        };
    }

    private String resolveRiskItemLevel(Tender.RiskLevel riskLevel, int index) {
        if (riskLevel == Tender.RiskLevel.HIGH && index == 0) return "high";
        return index == 0 ? "medium" : "medium";
    }

    private String defaultTenderSuggestion(Tender.RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> "整体匹配度较高，建议积极推进";
            case MEDIUM -> "建议补强关键短板后继续推进";
            case HIGH -> "风险偏高，需先完成重点问题整改";
        };
    }

    private String resolveRiskLevelText(AiAnalysisResponse response) {
        return response.getRiskLevel() == null ? "MEDIUM" : response.getRiskLevel().name();
    }

    private String writeValue(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AI payload", e);
        }
    }
}
