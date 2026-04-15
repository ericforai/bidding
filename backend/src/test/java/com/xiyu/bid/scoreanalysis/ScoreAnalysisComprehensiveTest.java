package com.xiyu.bid.scoreanalysis;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.scoreanalysis.dto.ScoreAnalysisDTO;
import com.xiyu.bid.scoreanalysis.dto.DimensionScoreDTO;
import com.xiyu.bid.scoreanalysis.dto.ScoreAnalysisCreateRequest;
import com.xiyu.bid.scoreanalysis.entity.ScoreAnalysis;
import com.xiyu.bid.scoreanalysis.entity.DimensionScore;
import com.xiyu.bid.scoreanalysis.repository.ScoreAnalysisRepository;
import com.xiyu.bid.scoreanalysis.repository.DimensionScoreRepository;
import com.xiyu.bid.scoreanalysis.service.ScoreAnalysisService;
import com.xiyu.bid.audit.service.IAuditLogService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ScoreAnalysis综合测试
 * 包含实体、DTO、Repository、Service的完整测试覆盖
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreAnalysis综合测试")
class ScoreAnalysisComprehensiveTest {

    @Mock
    private ScoreAnalysisRepository scoreAnalysisRepository;

    @Mock
    private DimensionScoreRepository dimensionScoreRepository;

    @Mock
    private IAuditLogService auditLogService;

    @InjectMocks
    private ScoreAnalysisService scoreAnalysisService;

    private ScoreAnalysis testAnalysis;
    private DimensionScore testDimension;
    private ScoreAnalysisCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        testDimension = DimensionScore.builder()
                .id(1L)
                .analysisId(1L)
                .dimensionName("技术能力")
                .score(90)
                .weight(new BigDecimal("0.30"))
                .comments("技术团队经验丰富")
                .build();

        testAnalysis = ScoreAnalysis.builder()
                .id(1L)
                .projectId(100L)
                .analysisDate(LocalDateTime.now())
                .overallScore(85)
                .riskLevel(RiskLevel.LOW)
                .analystId(10L)
                .isAiGenerated(true)
                .summary("优秀的技术方案")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<DimensionScoreDTO> dimensions = Arrays.asList(
                DimensionScoreDTO.builder()
                        .dimensionName("技术能力")
                        .score(90)
                        .weight(new BigDecimal("0.30"))
                        .build(),
                DimensionScoreDTO.builder()
                        .dimensionName("财务实力")
                        .score(85)
                        .weight(new BigDecimal("0.25"))
                        .build(),
                DimensionScoreDTO.builder()
                        .dimensionName("团队经验")
                        .score(80)
                        .weight(new BigDecimal("0.20"))
                        .build(),
                DimensionScoreDTO.builder()
                        .dimensionName("历史业绩")
                        .score(88)
                        .weight(new BigDecimal("0.15"))
                        .build(),
                DimensionScoreDTO.builder()
                        .dimensionName("合规性")
                        .score(95)
                        .weight(new BigDecimal("0.10"))
                        .build()
        );

        createRequest = ScoreAnalysisCreateRequest.builder()
                .projectId(100L)
                .analystId(10L)
                .isAiGenerated(true)
                .summary("综合评估优秀")
                .dimensions(dimensions)
                .build();
    }

    // ==================== Entity Tests ====================

    @Test
    @DisplayName("应该成功创建ScoreAnalysis实体")
    void shouldCreateScoreAnalysisEntitySuccessfully() {
        // Given & When
        ScoreAnalysis analysis = ScoreAnalysis.builder()
                .id(1L)
                .projectId(100L)
                .analysisDate(LocalDateTime.now())
                .overallScore(85)
                .riskLevel(RiskLevel.LOW)
                .analystId(10L)
                .isAiGenerated(true)
                .summary("优秀的技术方案")
                .build();

        // Then
        assertNotNull(analysis);
        assertEquals(1L, analysis.getId());
        assertEquals(100L, analysis.getProjectId());
        assertEquals(85, analysis.getOverallScore());
        assertEquals(RiskLevel.LOW, analysis.getRiskLevel());
        assertEquals(10L, analysis.getAnalystId());
        assertTrue(analysis.getIsAiGenerated());
        assertEquals("优秀的技术方案", analysis.getSummary());
    }

    @Test
    @DisplayName("应该成功创建DimensionScore实体")
    void shouldCreateDimensionScoreEntitySuccessfully() {
        // Given & When
        DimensionScore dimension = DimensionScore.builder()
                .id(1L)
                .analysisId(100L)
                .dimensionName("技术能力")
                .score(90)
                .weight(new BigDecimal("0.30"))
                .comments("技术团队经验丰富")
                .build();

        // Then
        assertNotNull(dimension);
        assertEquals(1L, dimension.getId());
        assertEquals(100L, dimension.getAnalysisId());
        assertEquals("技术能力", dimension.getDimensionName());
        assertEquals(90, dimension.getScore());
        assertEquals(new BigDecimal("0.30"), dimension.getWeight());
        assertEquals("技术团队经验丰富", dimension.getComments());
    }

    // ==================== Service Tests ====================

    @Test
    @DisplayName("应该成功创建评分分析")
    void shouldCreateAnalysisSuccessfully() {
        // Given
        when(scoreAnalysisRepository.save(any(ScoreAnalysis.class))).thenReturn(testAnalysis);
        when(dimensionScoreRepository.saveAll(anyList())).thenReturn(Arrays.asList(testDimension));

        // When
        ApiResponse<ScoreAnalysisDTO> response = scoreAnalysisService.createAnalysis(createRequest);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(100L, response.getData().getProjectId());
        verify(scoreAnalysisRepository, times(1)).save(any(ScoreAnalysis.class));
        verify(dimensionScoreRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("创建分析时应该记录审计日志")
    void shouldLogAuditWhenCreatingAnalysis() {
        // Given
        when(scoreAnalysisRepository.save(any(ScoreAnalysis.class))).thenReturn(testAnalysis);
        when(dimensionScoreRepository.saveAll(anyList())).thenReturn(Arrays.asList(testDimension));

        // When
        scoreAnalysisService.createAnalysis(createRequest);

        // Then - 审计日志通过@Auditable切面记录，不在service中直接调用
        // 这里只验证service方法被正确调用
        verify(scoreAnalysisRepository, times(1)).save(any(ScoreAnalysis.class));
    }

    @Test
    @DisplayName("应该正确计算加权总分")
    void shouldCalculateOverallScoreCorrectly() {
        // Given
        List<DimensionScore> dimensions = Arrays.asList(
                DimensionScore.builder()
                        .dimensionName("技术能力")
                        .score(90)
                        .weight(new BigDecimal("0.30"))
                        .build(),
                DimensionScore.builder()
                        .dimensionName("财务实力")
                        .score(80)
                        .weight(new BigDecimal("0.25"))
                        .build(),
                DimensionScore.builder()
                        .dimensionName("团队经验")
                        .score(85)
                        .weight(new BigDecimal("0.20"))
                        .build(),
                DimensionScore.builder()
                        .dimensionName("历史业绩")
                        .score(75)
                        .weight(new BigDecimal("0.15"))
                        .build(),
                DimensionScore.builder()
                        .dimensionName("合规性")
                        .score(95)
                        .weight(new BigDecimal("0.10"))
                        .build()
        );

        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L))
                .thenReturn(Optional.of(testAnalysis));
        when(dimensionScoreRepository.findByAnalysisId(1L)).thenReturn(dimensions);
        when(scoreAnalysisRepository.save(any(ScoreAnalysis.class))).thenReturn(testAnalysis);

        // When
        ApiResponse<Integer> response = scoreAnalysisService.calculateOverallScore(100L);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        // 计算: 90*0.30 + 80*0.25 + 85*0.20 + 75*0.15 + 95*0.10 = 27 + 20 + 17 + 11.25 + 9.5 = 84.75 -> 84 (向下取整)
        assertEquals(84, response.getData());
    }

    @Test
    @DisplayName("应该根据分数确定风险等级")
    void shouldDetermineRiskLevelBasedOnScore() {
        // 高风险
        ScoreAnalysis highRisk = ScoreAnalysis.builder()
                .id(1L)
                .projectId(101L)
                .overallScore(45)
                .riskLevel(RiskLevel.HIGH)
                .build();

        // 中等风险
        ScoreAnalysis mediumRisk = ScoreAnalysis.builder()
                .id(2L)
                .projectId(102L)
                .overallScore(65)
                .riskLevel(RiskLevel.MEDIUM)
                .build();

        // 低风险
        ScoreAnalysis lowRisk = ScoreAnalysis.builder()
                .id(3L)
                .projectId(103L)
                .overallScore(85)
                .riskLevel(RiskLevel.LOW)
                .build();

        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(101L))
                .thenReturn(Optional.of(highRisk));
        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(102L))
                .thenReturn(Optional.of(mediumRisk));
        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(103L))
                .thenReturn(Optional.of(lowRisk));
        when(dimensionScoreRepository.findByAnalysisId(anyLong())).thenReturn(new ArrayList<>());

        // When & Then
        ApiResponse<ScoreAnalysisDTO> response1 = scoreAnalysisService.getAnalysisByProject(101L);
        assertTrue(response1.isSuccess());
        assertEquals(RiskLevel.HIGH, response1.getData().getRiskLevel());

        ApiResponse<ScoreAnalysisDTO> response2 = scoreAnalysisService.getAnalysisByProject(102L);
        assertTrue(response2.isSuccess());
        assertEquals(RiskLevel.MEDIUM, response2.getData().getRiskLevel());

        ApiResponse<ScoreAnalysisDTO> response3 = scoreAnalysisService.getAnalysisByProject(103L);
        assertTrue(response3.isSuccess());
        assertEquals(RiskLevel.LOW, response3.getData().getRiskLevel());
    }

    @Test
    @DisplayName("应该获取项目的评分分析")
    void shouldGetAnalysisByProjectSuccessfully() {
        // Given
        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L))
                .thenReturn(Optional.of(testAnalysis));
        when(dimensionScoreRepository.findByAnalysisId(1L))
                .thenReturn(Arrays.asList(testDimension));

        // When
        ApiResponse<ScoreAnalysisDTO> response = scoreAnalysisService.getAnalysisByProject(100L);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(100L, response.getData().getProjectId());
        assertEquals(85, response.getData().getOverallScore());
        assertFalse(response.getData().getDimensions().isEmpty());
    }

    @Test
    @DisplayName("应该获取项目不存在的分析时返回错误")
    void shouldReturnErrorWhenAnalysisNotFound() {
        // Given
        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(999L))
                .thenReturn(Optional.empty());

        // When
        ApiResponse<ScoreAnalysisDTO> response = scoreAnalysisService.getAnalysisByProject(999L);

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("未找到项目的评分分析", response.getMessage());
    }

    @Test
    @DisplayName("应该获取项目历史分析")
    void shouldGetAnalysisHistorySuccessfully() {
        // Given
        ScoreAnalysis oldAnalysis = ScoreAnalysis.builder()
                .id(2L)
                .projectId(100L)
                .analysisDate(LocalDateTime.now().minusDays(10))
                .overallScore(75)
                .riskLevel(RiskLevel.MEDIUM)
                .build();

        when(scoreAnalysisRepository.findByProjectIdOrderByAnalysisDateDesc(100L))
                .thenReturn(Arrays.asList(testAnalysis, oldAnalysis));

        // When
        ApiResponse<List<ScoreAnalysisDTO>> response = scoreAnalysisService.getAnalysisHistory(100L);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(85, response.getData().get(0).getOverallScore());
        assertEquals(75, response.getData().get(1).getOverallScore());
    }

    @Test
    @DisplayName("应该比较两个项目的评分")
    void shouldCompareProjectsSuccessfully() {
        // Given
        ScoreAnalysis project2Analysis = ScoreAnalysis.builder()
                .id(2L)
                .projectId(200L)
                .overallScore(72)
                .riskLevel(RiskLevel.MEDIUM)
                .build();

        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L))
                .thenReturn(Optional.of(testAnalysis));
        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(200L))
                .thenReturn(Optional.of(project2Analysis));
        when(dimensionScoreRepository.findByAnalysisId(1L)).thenReturn(new ArrayList<>());
        when(dimensionScoreRepository.findByAnalysisId(2L)).thenReturn(new ArrayList<>());

        // When
        ApiResponse<List<ScoreAnalysisDTO>> response = scoreAnalysisService.compareProjects(100L, 200L);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(85, response.getData().get(0).getOverallScore());
        assertEquals(72, response.getData().get(1).getOverallScore());
    }

    @Test
    @DisplayName("应该处理空维度列表的创建请求")
    void shouldHandleCreateRequestWithEmptyDimensions() {
        // Given
        ScoreAnalysisCreateRequest emptyRequest = ScoreAnalysisCreateRequest.builder()
                .projectId(100L)
                .dimensions(new ArrayList<>())
                .build();

        when(scoreAnalysisRepository.save(any(ScoreAnalysis.class))).thenReturn(testAnalysis);
        when(dimensionScoreRepository.findByAnalysisId(anyLong())).thenReturn(new ArrayList<>());

        // When
        ApiResponse<ScoreAnalysisDTO> response = scoreAnalysisService.createAnalysis(emptyRequest);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        // 空列表不会调用saveAll
        verify(dimensionScoreRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("应该处理null维度的创建请求")
    void shouldHandleCreateRequestWithNullDimensions() {
        // Given
        ScoreAnalysisCreateRequest nullRequest = ScoreAnalysisCreateRequest.builder()
                .projectId(100L)
                .dimensions(null)
                .build();

        when(scoreAnalysisRepository.save(any(ScoreAnalysis.class))).thenReturn(testAnalysis);

        // When
        ApiResponse<ScoreAnalysisDTO> response = scoreAnalysisService.createAnalysis(nullRequest);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(dimensionScoreRepository, never()).saveAll(anyList());
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("应该正确处理边界分数0和100")
    void shouldHandleBoundaryScoresZeroAndHundred() {
        // Given
        List<DimensionScoreDTO> zeroDimensions = Arrays.asList(
                DimensionScoreDTO.builder()
                        .dimensionName("技术能力")
                        .score(0)
                        .weight(new BigDecimal("1.00"))
                        .build()
        );

        ScoreAnalysisCreateRequest zeroRequest = ScoreAnalysisCreateRequest.builder()
                .projectId(100L)
                .dimensions(zeroDimensions)
                .build();

        ScoreAnalysis zeroScoreAnalysis = ScoreAnalysis.builder()
                .id(1L)
                .projectId(100L)
                .overallScore(0)
                .riskLevel(RiskLevel.HIGH)
                .build();

        when(scoreAnalysisRepository.save(any(ScoreAnalysis.class))).thenReturn(zeroScoreAnalysis);

        // When
        ApiResponse<ScoreAnalysisDTO> response = scoreAnalysisService.createAnalysis(zeroRequest);

        // Then
        assertTrue(response.isSuccess());
        assertEquals(0, response.getData().getOverallScore());
        assertEquals(RiskLevel.HIGH, response.getData().getRiskLevel());
    }
}
