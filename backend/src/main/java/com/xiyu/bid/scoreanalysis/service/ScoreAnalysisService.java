// Input: scoreanalysis repositories, DTOs, and support services
// Output: Score Analysis business service operations
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.scoreanalysis.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.scoreanalysis.RiskLevel;
import com.xiyu.bid.scoreanalysis.dto.DimensionScoreDTO;
import com.xiyu.bid.scoreanalysis.dto.ScoreAnalysisCreateRequest;
import com.xiyu.bid.scoreanalysis.dto.ScoreAnalysisDTO;
import com.xiyu.bid.scoreanalysis.entity.DimensionScore;
import com.xiyu.bid.scoreanalysis.entity.ScoreAnalysis;
import com.xiyu.bid.scoreanalysis.repository.DimensionScoreRepository;
import com.xiyu.bid.scoreanalysis.repository.ScoreAnalysisRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 评分分析服务
 * 提供评分分析的业务逻辑处理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreAnalysisService {

    private final ScoreAnalysisRepository scoreAnalysisRepository;
    private final DimensionScoreRepository dimensionScoreRepository;
    private final ProjectAccessScopeService projectAccessScopeService;
    /**
     * 创建评分分析
     * @param request 创建请求
     * @return 创建的评分分析
     */
    @Auditable(
            action = "CREATE",
            entityType = "ScoreAnalysis",
            description = "创建评分分析"
    )
    @Transactional
    public ApiResponse<ScoreAnalysisDTO> createAnalysis(ScoreAnalysisCreateRequest request) {
        try {
            projectAccessScopeService.assertCurrentUserCanAccessProject(request.getProjectId());
            // 创建评分分析主记录
            ScoreAnalysis analysis = ScoreAnalysis.builder()
                    .projectId(request.getProjectId())
                    .analysisDate(LocalDateTime.now())
                    .analystId(request.getAnalystId())
                    .isAiGenerated(request.getIsAiGenerated() != null ? request.getIsAiGenerated() : false)
                    .summary(request.getSummary())
                    .build();

            // 如果提供了维度数据，计算综合评分
            if (request.getDimensions() != null && !request.getDimensions().isEmpty()) {
                BigDecimal totalScore = calculateWeightedScore(request.getDimensions());
                analysis.setOverallScore(totalScore.intValue());
                analysis.setRiskLevel(determineRiskLevel(totalScore.intValue()));
            }

            ScoreAnalysis savedAnalysis = scoreAnalysisRepository.save(analysis);

            // 保存维度分数
            if (request.getDimensions() != null && !request.getDimensions().isEmpty()) {
                List<DimensionScore> dimensions = request.getDimensions().stream()
                        .map(dto -> DimensionScore.builder()
                                .analysisId(savedAnalysis.getId())
                                .dimensionName(dto.getDimensionName())
                                .score(dto.getScore())
                                .weight(dto.getWeight())
                                .comments(dto.getComments())
                                .build())
                        .collect(Collectors.toList());

                dimensionScoreRepository.saveAll(dimensions);
            }

            ScoreAnalysisDTO responseDTO = convertToDTO(savedAnalysis);
            return ApiResponse.success("评分分析创建成功", responseDTO);

        } catch (RuntimeException e) {
            log.error("创建评分分析失败: {}", e.getMessage(), e);
            return ApiResponse.error("创建评分分析失败: " + e.getMessage());
        }
    }

    /**
     * 计算项目的综合评分
     * @param projectId 项目ID
     * @return 综合评分
     */
    public ApiResponse<Integer> calculateOverallScore(Long projectId) {
        try {
            projectAccessScopeService.assertCurrentUserCanAccessProject(projectId);
            Optional<ScoreAnalysis> analysisOpt =
                    scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(projectId);

            if (analysisOpt.isEmpty()) {
                return ApiResponse.error("未找到项目的评分分析");
            }

            ScoreAnalysis analysis = analysisOpt.get();
            List<DimensionScore> dimensions = dimensionScoreRepository.findByAnalysisId(analysis.getId());

            if (dimensions.isEmpty()) {
                return ApiResponse.success("综合评分计算成功", analysis.getOverallScore());
            }

            BigDecimal totalScore = calculateWeightedScoreFromEntities(dimensions);

            // 更新分析记录
            analysis.setOverallScore(totalScore.intValue());
            analysis.setRiskLevel(determineRiskLevel(totalScore.intValue()));
            scoreAnalysisRepository.save(analysis);

            return ApiResponse.success(totalScore.intValue());

        } catch (RuntimeException e) {
            log.error("计算综合评分失败: {}", e.getMessage(), e);
            return ApiResponse.error("计算综合评分失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目的评分分析
     * @param projectId 项目ID
     * @return 评分分析
     */
    public ApiResponse<ScoreAnalysisDTO> getAnalysisByProject(Long projectId) {
        try {
            projectAccessScopeService.assertCurrentUserCanAccessProject(projectId);
            Optional<ScoreAnalysis> analysisOpt =
                    scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(projectId);

            if (analysisOpt.isEmpty()) {
                return ApiResponse.error("未找到项目的评分分析");
            }

            ScoreAnalysis analysis = analysisOpt.get();
            ScoreAnalysisDTO dto = convertToDTO(analysis);

            return ApiResponse.success("获取评分分析成功", dto);

        } catch (RuntimeException e) {
            log.error("获取评分分析失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取评分分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目的历史分析记录
     * @param projectId 项目ID
     * @return 历史分析列表
     */
    public ApiResponse<List<ScoreAnalysisDTO>> getAnalysisHistory(Long projectId) {
        try {
            projectAccessScopeService.assertCurrentUserCanAccessProject(projectId);
            List<ScoreAnalysis> analyses =
                    scoreAnalysisRepository.findByProjectIdOrderByAnalysisDateDesc(projectId);

            List<ScoreAnalysisDTO> dtos = analyses.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ApiResponse.success("历史分析记录", dtos);

        } catch (RuntimeException e) {
            log.error("获取历史分析失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取历史分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取最新的评分分析
     * @param projectId 项目ID
     * @return 最新的评分分析
     */
    public ApiResponse<ScoreAnalysisDTO> getLatestAnalysis(Long projectId) {
        try {
            projectAccessScopeService.assertCurrentUserCanAccessProject(projectId);
            Optional<ScoreAnalysis> analysisOpt =
                    scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(projectId);

            if (analysisOpt.isEmpty()) {
                return ApiResponse.error("未找到项目的评分分析");
            }

            ScoreAnalysis analysis = analysisOpt.get();
            ScoreAnalysisDTO dto = convertToDTO(analysis);

            return ApiResponse.success("获取最新分析成功", dto);

        } catch (RuntimeException e) {
            log.error("获取最新分析失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取最新分析失败: " + e.getMessage());
        }
    }

    /**
     * 比较两个项目的评分
     * @param projectId1 项目1 ID
     * @param projectId2 项目2 ID
     * @return 两个项目的评分对比
     */
    public ApiResponse<List<ScoreAnalysisDTO>> compareProjects(Long projectId1, Long projectId2) {
        try {
            projectAccessScopeService.assertCurrentUserCanAccessProject(projectId1);
            projectAccessScopeService.assertCurrentUserCanAccessProject(projectId2);
            Optional<ScoreAnalysis> analysis1Opt =
                    scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(projectId1);
            Optional<ScoreAnalysis> analysis2Opt =
                    scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(projectId2);

            if (analysis1Opt.isEmpty()) {
                return ApiResponse.error("无法找到项目" + projectId1 + "的评分分析");
            }
            if (analysis2Opt.isEmpty()) {
                return ApiResponse.error("无法找到项目" + projectId2 + "的评分分析");
            }

            ScoreAnalysisDTO dto1 = convertToDTO(analysis1Opt.get());
            ScoreAnalysisDTO dto2 = convertToDTO(analysis2Opt.get());

            return ApiResponse.success("项目比较结果", List.of(dto1, dto2));

        } catch (RuntimeException e) {
            log.error("比较项目失败: {}", e.getMessage(), e);
            return ApiResponse.error("比较项目失败: " + e.getMessage());
        }
    }

    /**
     * 计算加权分数
     * @param dimensions 维度分数DTO列表
     * @return 加权总分
     */
    private BigDecimal calculateWeightedScore(List<DimensionScoreDTO> dimensions) {
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (DimensionScoreDTO dimension : dimensions) {
            if (dimension.getScore() != null && dimension.getWeight() != null) {
                BigDecimal score = BigDecimal.valueOf(dimension.getScore());
                BigDecimal weight = dimension.getWeight();
                totalScore = totalScore.add(score.multiply(weight));
                totalWeight = totalWeight.add(weight);
            }
        }

        // 如果权重总和不为1，进行归一化
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0 &&
            totalWeight.compareTo(BigDecimal.ONE) != 0) {
            totalScore = totalScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
        }

        return totalScore;
    }

    /**
     * 从实体列表计算加权分数
     * @param dimensions 维度分数实体列表
     * @return 加权总分
     */
    private BigDecimal calculateWeightedScoreFromEntities(List<DimensionScore> dimensions) {
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (DimensionScore dimension : dimensions) {
            if (dimension.getScore() != null && dimension.getWeight() != null) {
                BigDecimal score = BigDecimal.valueOf(dimension.getScore());
                BigDecimal weight = dimension.getWeight();
                totalScore = totalScore.add(score.multiply(weight));
                totalWeight = totalWeight.add(weight);
            }
        }

        // 如果权重总和不为1，进行归一化
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0 &&
            totalWeight.compareTo(BigDecimal.ONE) != 0) {
            totalScore = totalScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
        }

        return totalScore;
    }

    /**
     * 根据分数确定风险等级
     * @param score 分数
     * @return 风险等级
     */
    private RiskLevel determineRiskLevel(Integer score) {
        if (score == null) {
            return RiskLevel.MEDIUM;
        }
        if (score >= 80) {
            return RiskLevel.LOW;
        } else if (score >= 60) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }

    /**
     * 将实体转换为DTO
     * @param analysis 评分分析实体
     * @return DTO
     */
    private ScoreAnalysisDTO convertToDTO(ScoreAnalysis analysis) {
        List<DimensionScore> dimensions =
                dimensionScoreRepository.findByAnalysisId(analysis.getId());

        List<DimensionScoreDTO> dimensionDTOs = dimensions.stream()
                .map(d -> DimensionScoreDTO.builder()
                        .id(d.getId())
                        .analysisId(d.getAnalysisId())
                        .dimensionName(d.getDimensionName())
                        .score(d.getScore())
                        .weight(d.getWeight())
                        .comments(d.getComments())
                        .build())
                .collect(Collectors.toList());

        return ScoreAnalysisDTO.builder()
                .id(analysis.getId())
                .projectId(analysis.getProjectId())
                .analysisDate(analysis.getAnalysisDate())
                .overallScore(analysis.getOverallScore())
                .riskLevel(analysis.getRiskLevel())
                .analystId(analysis.getAnalystId())
                .isAiGenerated(analysis.getIsAiGenerated())
                .summary(analysis.getSummary())
                .dimensions(dimensionDTOs)
                .build();
    }
}
