package com.xiyu.bid.scoreanalysis;

import com.xiyu.bid.scoreanalysis.entity.ScoreAnalysis;
import com.xiyu.bid.scoreanalysis.entity.DimensionScore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScoreAnalysis实体测试
 * 测试评分分析实体的创建、验证和业务逻辑
 */
@DisplayName("ScoreAnalysis实体测试")
class ScoreAnalysisEntityTest {

    @Test
    @DisplayName("应该成功创建ScoreAnalysis实体")
    void shouldCreateScoreAnalysisSuccessfully() {
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
    void shouldCreateDimensionScoreSuccessfully() {
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

    @Test
    @DisplayName("应该允许ScoreAnalysis字段为空")
    void shouldAllowNullFieldsInScoreAnalysis() {
        // Given & When
        ScoreAnalysis analysis = ScoreAnalysis.builder()
                .projectId(100L)
                .build();

        // Then
        assertNotNull(analysis);
        assertEquals(100L, analysis.getProjectId());
        assertNull(analysis.getOverallScore());
        assertNull(analysis.getRiskLevel());
        assertNull(analysis.getSummary());
    }
}
