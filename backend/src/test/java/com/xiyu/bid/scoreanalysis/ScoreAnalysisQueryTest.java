package com.xiyu.bid.scoreanalysis;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.scoreanalysis.dto.ScoreAnalysisDTO;
import com.xiyu.bid.scoreanalysis.entity.ScoreAnalysis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@DisplayName("ScoreAnalysis query tests")
class ScoreAnalysisQueryTest extends AbstractScoreAnalysisServiceTest {

    @Test
    @DisplayName("应该获取项目的评分分析")
    void shouldGetAnalysisByProjectSuccessfully() {
        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L)).thenReturn(Optional.of(testAnalysis));
        when(dimensionScoreRepository.findByAnalysisId(1L)).thenReturn(List.of(testDimension));

        ApiResponse<ScoreAnalysisDTO> response = scoreAnalysisService.getAnalysisByProject(100L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(100L, response.getData().getProjectId());
        assertEquals(85, response.getData().getOverallScore());
        assertFalse(response.getData().getDimensions().isEmpty());
        verify(projectAccessScopeService).assertCurrentUserCanAccessProject(100L);
    }

    @Test
    @DisplayName("应该获取项目不存在的分析时返回错误")
    void shouldReturnErrorWhenAnalysisNotFound() {
        when(scoreAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(999L)).thenReturn(Optional.empty());

        ApiResponse<ScoreAnalysisDTO> response = scoreAnalysisService.getAnalysisByProject(999L);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("未找到项目的评分分析", response.getMessage());
    }

    @Test
    @DisplayName("应该获取项目历史分析")
    void shouldGetAnalysisHistorySuccessfully() {
        ScoreAnalysis oldAnalysis = ScoreAnalysis.builder()
                .id(2L)
                .projectId(100L)
                .analysisDate(LocalDateTime.now().minusDays(10))
                .overallScore(75)
                .riskLevel(RiskLevel.MEDIUM)
                .build();
        when(scoreAnalysisRepository.findByProjectIdOrderByAnalysisDateDesc(100L))
                .thenReturn(List.of(testAnalysis, oldAnalysis));

        ApiResponse<List<ScoreAnalysisDTO>> response = scoreAnalysisService.getAnalysisHistory(100L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(85, response.getData().get(0).getOverallScore());
        assertEquals(75, response.getData().get(1).getOverallScore());
        verify(projectAccessScopeService).assertCurrentUserCanAccessProject(100L);
    }

    @Test
    @DisplayName("应该比较两个项目的评分")
    void shouldCompareProjectsSuccessfully() {
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

        ApiResponse<List<ScoreAnalysisDTO>> response = scoreAnalysisService.compareProjects(100L, 200L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(85, response.getData().get(0).getOverallScore());
        assertEquals(72, response.getData().get(1).getOverallScore());
        verify(projectAccessScopeService).assertCurrentUserCanAccessProject(100L);
        verify(projectAccessScopeService).assertCurrentUserCanAccessProject(200L);
    }
}
