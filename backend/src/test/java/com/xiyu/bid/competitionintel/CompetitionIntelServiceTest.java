package com.xiyu.bid.competitionintel;

import com.xiyu.bid.competitionintel.dto.AnalysisCreateRequest;
import com.xiyu.bid.competitionintel.dto.CompetitorCreateRequest;
import com.xiyu.bid.competitionintel.dto.CompetitionAnalysisDTO;
import com.xiyu.bid.competitionintel.dto.CompetitorDTO;
import com.xiyu.bid.competitionintel.entity.CompetitionAnalysis;
import com.xiyu.bid.competitionintel.entity.Competitor;
import com.xiyu.bid.competitionintel.repository.CompetitionAnalysisRepository;
import com.xiyu.bid.competitionintel.repository.CompetitorRepository;
import com.xiyu.bid.competitionintel.service.CompetitionIntelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 竞争情报服务测试
 * 使用TDD方法测试竞争情报服务的所有业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class CompetitionIntelServiceTest {

    @Mock
    private CompetitorRepository competitorRepository;

    @Mock
    private CompetitionAnalysisRepository analysisRepository;

    private CompetitionIntelService service;

    private Competitor testCompetitor;
    private CompetitionAnalysis testAnalysis;
    private CompetitorCreateRequest competitorRequest;
    private AnalysisCreateRequest analysisRequest;

    @BeforeEach
    void setUp() {
        service = new CompetitionIntelService(competitorRepository, analysisRepository);

        testCompetitor = Competitor.builder()
                .id(1L)
                .name("竞企A")
                .industry("建筑业")
                .strengths("资质齐全，技术实力强")
                .weaknesses("报价偏高，响应速度慢")
                .marketShare(new BigDecimal("25.5"))
                .typicalBidRangeMin(new BigDecimal("1000000"))
                .typicalBidRangeMax(new BigDecimal("1500000"))
                .createdAt(LocalDateTime.now())
                .build();

        testAnalysis = CompetitionAnalysis.builder()
                .id(1L)
                .projectId(100L)
                .competitorId(1L)
                .analysisDate(LocalDateTime.now())
                .winProbability(new BigDecimal("65.5"))
                .competitiveAdvantage("资质齐全，类似项目经验丰富")
                .recommendedStrategy("突出技术优势，适当降低报价")
                .riskFactors("对手可能采取低价策略")
                .build();

        competitorRequest = CompetitorCreateRequest.builder()
                .name("竞企B")
                .industry("建筑业")
                .strengths("成本控制好")
                .weaknesses("技术实力一般")
                .marketShare(new BigDecimal("15.0"))
                .typicalBidRangeMin(new BigDecimal("800000"))
                .typicalBidRangeMax(new BigDecimal("1200000"))
                .build();

        analysisRequest = AnalysisCreateRequest.builder()
                .projectId(100L)
                .competitorId(1L)
                .winProbability(new BigDecimal("70.0"))
                .competitiveAdvantage("技术领先")
                .recommendedStrategy("强调创新")
                .riskFactors("价格竞争")
                .build();
    }

    // ========== createCompetitor Tests ==========

    @Test
    void createCompetitor_WithValidData_ShouldReturnSavedCompetitor() {
        // Given
        Competitor savedCompetitor = Competitor.builder()
                .id(2L)
                .name(competitorRequest.getName())
                .industry(competitorRequest.getIndustry())
                .strengths(competitorRequest.getStrengths())
                .weaknesses(competitorRequest.getWeaknesses())
                .marketShare(competitorRequest.getMarketShare())
                .typicalBidRangeMin(competitorRequest.getTypicalBidRangeMin())
                .typicalBidRangeMax(competitorRequest.getTypicalBidRangeMax())
                .createdAt(LocalDateTime.now())
                .build();

        when(competitorRepository.save(any(Competitor.class))).thenReturn(savedCompetitor);

        // When
        CompetitorDTO result = service.createCompetitor(competitorRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo("竞企B");
        assertThat(result.getIndustry()).isEqualTo("建筑业");
        assertThat(result.getMarketShare()).isEqualByComparingTo("15.0");

        verify(competitorRepository).save(any(Competitor.class));
    }

    @Test
    void createCompetitor_WithNullName_ShouldThrowException() {
        // Given
        CompetitorCreateRequest invalidRequest = CompetitorCreateRequest.builder()
                .name(null)
                .industry("建筑业")
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createCompetitor(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Competitor name is required");

        verify(competitorRepository, never()).save(any(Competitor.class));
    }

    @Test
    void createCompetitor_WithEmptyName_ShouldThrowException() {
        // Given
        CompetitorCreateRequest invalidRequest = CompetitorCreateRequest.builder()
                .name("")
                .industry("建筑业")
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createCompetitor(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Competitor name is required");

        verify(competitorRepository, never()).save(any(Competitor.class));
    }

    @Test
    void createCompetitor_WithNegativeMarketShare_ShouldThrowException() {
        // Given
        CompetitorCreateRequest invalidRequest = CompetitorCreateRequest.builder()
                .name("竞企C")
                .marketShare(new BigDecimal("-10.0"))
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createCompetitor(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Market share cannot be negative");

        verify(competitorRepository, never()).save(any(Competitor.class));
    }

    @Test
    void createCompetitor_WithMarketShareOver100_ShouldThrowException() {
        // Given
        CompetitorCreateRequest invalidRequest = CompetitorCreateRequest.builder()
                .name("竞企C")
                .marketShare(new BigDecimal("150.0"))
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createCompetitor(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Market share cannot exceed 100");

        verify(competitorRepository, never()).save(any(Competitor.class));
    }

    @Test
    void createCompetitor_WithInvalidBidRange_ShouldThrowException() {
        // Given
        CompetitorCreateRequest invalidRequest = CompetitorCreateRequest.builder()
                .name("竞企C")
                .typicalBidRangeMin(new BigDecimal("2000000"))
                .typicalBidRangeMax(new BigDecimal("1000000"))
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createCompetitor(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bid range minimum cannot be greater than maximum");

        verify(competitorRepository, never()).save(any(Competitor.class));
    }

    @Test
    void createCompetitor_WithNegativeBidRange_ShouldThrowException() {
        // Given
        CompetitorCreateRequest invalidRequest = CompetitorCreateRequest.builder()
                .name("竞企C")
                .typicalBidRangeMin(new BigDecimal("-1000"))
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createCompetitor(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bid range values cannot be negative");

        verify(competitorRepository, never()).save(any(Competitor.class));
    }

    // ========== getAllCompetitors Tests ==========

    @Test
    void getAllCompetitors_ShouldReturnListOfCompetitors() {
        // Given
        Competitor competitor2 = Competitor.builder()
                .id(2L)
                .name("竞企B")
                .industry("制造业")
                .build();

        when(competitorRepository.findAll()).thenReturn(Arrays.asList(testCompetitor, competitor2));

        // When
        List<CompetitorDTO> result = service.getAllCompetitors();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("竞企A");
        assertThat(result.get(1).getName()).isEqualTo("竞企B");

        verify(competitorRepository).findAll();
    }

    @Test
    void getAllCompetitors_WithEmptyResult_ShouldReturnEmptyList() {
        // Given
        when(competitorRepository.findAll()).thenReturn(List.of());

        // When
        List<CompetitorDTO> result = service.getAllCompetitors();

        // Then
        assertThat(result).isEmpty();

        verify(competitorRepository).findAll();
    }

    // ========== createAnalysis Tests ==========

    @Test
    void createAnalysis_WithValidData_ShouldReturnSavedAnalysis() {
        // Given
        CompetitionAnalysis savedAnalysis = CompetitionAnalysis.builder()
                .id(2L)
                .projectId(analysisRequest.getProjectId())
                .competitorId(analysisRequest.getCompetitorId())
                .winProbability(analysisRequest.getWinProbability())
                .competitiveAdvantage(analysisRequest.getCompetitiveAdvantage())
                .recommendedStrategy(analysisRequest.getRecommendedStrategy())
                .riskFactors(analysisRequest.getRiskFactors())
                .analysisDate(LocalDateTime.now())
                .build();

        when(analysisRepository.save(any(CompetitionAnalysis.class))).thenReturn(savedAnalysis);

        // When
        CompetitionAnalysisDTO result = service.createAnalysis(analysisRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getWinProbability()).isEqualByComparingTo("70.0");

        verify(analysisRepository).save(any(CompetitionAnalysis.class));
    }

    @Test
    void createAnalysis_WithNullProjectId_ShouldThrowException() {
        // Given
        AnalysisCreateRequest invalidRequest = AnalysisCreateRequest.builder()
                .projectId(null)
                .competitorId(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createAnalysis(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID is required");

        verify(analysisRepository, never()).save(any(CompetitionAnalysis.class));
    }

    @Test
    void createAnalysis_WithInvalidWinProbability_ShouldThrowException() {
        // Given
        AnalysisCreateRequest invalidRequest = AnalysisCreateRequest.builder()
                .projectId(100L)
                .winProbability(new BigDecimal("-10"))
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createAnalysis(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Win probability must be between 0 and 100");

        verify(analysisRepository, never()).save(any(CompetitionAnalysis.class));
    }

    @Test
    void createAnalysis_WithWinProbabilityOver100_ShouldThrowException() {
        // Given
        AnalysisCreateRequest invalidRequest = AnalysisCreateRequest.builder()
                .projectId(100L)
                .winProbability(new BigDecimal("150"))
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createAnalysis(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Win probability must be between 0 and 100");

        verify(analysisRepository, never()).save(any(CompetitionAnalysis.class));
    }

    // ========== getAnalysisByProject Tests ==========

    @Test
    void getAnalysisByProject_WithValidProjectId_ShouldReturnAnalysisList() {
        // Given
        CompetitionAnalysis analysis2 = CompetitionAnalysis.builder()
                .id(2L)
                .projectId(100L)
                .competitorId(2L)
                .winProbability(new BigDecimal("40.0"))
                .build();

        when(analysisRepository.findByProjectId(100L))
                .thenReturn(Arrays.asList(testAnalysis, analysis2));

        // When
        List<CompetitionAnalysisDTO> result = service.getAnalysisByProject(100L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProjectId()).isEqualTo(100L);
        assertThat(result.get(1).getProjectId()).isEqualTo(100L);

        verify(analysisRepository).findByProjectId(100L);
    }

    @Test
    void getAnalysisByProject_WithEmptyResult_ShouldReturnEmptyList() {
        // Given
        when(analysisRepository.findByProjectId(999L)).thenReturn(List.of());

        // When
        List<CompetitionAnalysisDTO> result = service.getAnalysisByProject(999L);

        // Then
        assertThat(result).isEmpty();

        verify(analysisRepository).findByProjectId(999L);
    }

    @Test
    void getAnalysisByProject_WithNullProjectId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> service.getAnalysisByProject(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID is required");

        verify(analysisRepository, never()).findByProjectId(any());
    }

    // ========== getHistoricalPerformance Tests ==========

    @Test
    void getHistoricalPerformance_WithValidCompetitorId_ShouldReturnAnalysisList() {
        // Given
        CompetitionAnalysis oldAnalysis = CompetitionAnalysis.builder()
                .id(2L)
                .projectId(90L)
                .competitorId(1L)
                .winProbability(new BigDecimal("55.0"))
                .analysisDate(LocalDateTime.now().minusMonths(6))
                .build();

        when(analysisRepository.findByCompetitorIdOrderByAnalysisDateDesc(1L))
                .thenReturn(Arrays.asList(testAnalysis, oldAnalysis));

        // When
        List<CompetitionAnalysisDTO> result = service.getHistoricalPerformance(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCompetitorId()).isEqualTo(1L);
        assertThat(result.get(1).getCompetitorId()).isEqualTo(1L);

        verify(analysisRepository).findByCompetitorIdOrderByAnalysisDateDesc(1L);
    }

    @Test
    void getHistoricalPerformance_WithEmptyResult_ShouldReturnEmptyList() {
        // Given
        when(analysisRepository.findByCompetitorIdOrderByAnalysisDateDesc(999L))
                .thenReturn(List.of());

        // When
        List<CompetitionAnalysisDTO> result = service.getHistoricalPerformance(999L);

        // Then
        assertThat(result).isEmpty();

        verify(analysisRepository).findByCompetitorIdOrderByAnalysisDateDesc(999L);
    }

    @Test
    void getHistoricalPerformance_WithNullCompetitorId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> service.getHistoricalPerformance(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Competitor ID is required");

        verify(analysisRepository, never()).findByCompetitorIdOrderByAnalysisDateDesc(any());
    }

    // ========== analyzeCompetition Tests ==========

    @Test
    void analyzeCompetition_WithValidProjectId_ShouldReturnNewAnalysis() {
        // Given
        CompetitionAnalysis newAnalysis = CompetitionAnalysis.builder()
                .id(3L)
                .projectId(100L)
                .analysisDate(LocalDateTime.now())
                .winProbability(new BigDecimal("60.0"))
                .competitiveAdvantage("自动生成的优势分析")
                .recommendedStrategy("自动生成的策略建议")
                .riskFactors("自动生成的风险因素")
                .build();

        when(analysisRepository.save(any(CompetitionAnalysis.class))).thenReturn(newAnalysis);

        // When
        CompetitionAnalysisDTO result = service.analyzeCompetition(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getWinProbability()).isEqualByComparingTo("60.0");
        assertThat(result.getAnalysisDate()).isNotNull();

        verify(analysisRepository).save(any(CompetitionAnalysis.class));
    }

    @Test
    void analyzeCompetition_WithNullProjectId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> service.analyzeCompetition(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID is required");

        verify(analysisRepository, never()).save(any(CompetitionAnalysis.class));
    }

    @Test
    void analyzeCompetition_WithRepositoryError_ShouldPropagateException() {
        // Given
        when(analysisRepository.save(any(CompetitionAnalysis.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> service.analyzeCompetition(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(analysisRepository).save(any(CompetitionAnalysis.class));
    }
}
