package com.xiyu.bid.roi;

import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.roi.dto.ROIAnalysisCreateRequest;
import com.xiyu.bid.roi.dto.ROIAnalysisDTO;
import com.xiyu.bid.roi.dto.SensitivityAnalysisRequest;
import com.xiyu.bid.roi.dto.SensitivityAnalysisResult;
import com.xiyu.bid.roi.entity.ROIAnalysis;
import com.xiyu.bid.roi.repository.ROIAnalysisRepository;
import com.xiyu.bid.roi.service.ROIAnalysisService;
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
 * ROI分析服务测试类
 * 测试ROI计算、敏感性分析等核心功能
 */
@ExtendWith(MockitoExtension.class)
class ROIAnalysisServiceTest {

    @Mock
    private ROIAnalysisRepository roiAnalysisRepository;

    private ROIAnalysisService roiAnalysisService;

    private ROIAnalysis testROIAnalysis;
    private ROIAnalysisCreateRequest createRequest;
    private SensitivityAnalysisRequest sensitivityRequest;

    @BeforeEach
    void setUp() {
        roiAnalysisService = new ROIAnalysisService(roiAnalysisRepository);

        testROIAnalysis = ROIAnalysis.builder()
                .id(1L)
                .projectId(100L)
                .analysisDate(LocalDateTime.of(2024, 3, 1, 10, 0))
                .estimatedCost(new BigDecimal("500000.00"))
                .estimatedRevenue(new BigDecimal("800000.00"))
                .estimatedProfit(new BigDecimal("300000.00"))
                .roiPercentage(new BigDecimal("60.00"))
                .paybackPeriodMonths(24)
                .riskFactors("Market volatility, regulatory changes")
                .assumptions("Project completion on time, no cost overruns")
                .createdBy(1L)
                .build();

        createRequest = ROIAnalysisCreateRequest.builder()
                .projectId(100L)
                .estimatedCost(new BigDecimal("500000.00"))
                .estimatedRevenue(new BigDecimal("800000.00"))
                .paybackPeriodMonths(24)
                .riskFactors("Market volatility, regulatory changes")
                .assumptions("Project completion on time, no cost overruns")
                .createdBy(1L)
                .build();

        sensitivityRequest = SensitivityAnalysisRequest.builder()
                .projectId(100L)
                .costVariations(Arrays.asList(-10.0, 0.0, 10.0))
                .revenueVariations(Arrays.asList(-10.0, 0.0, 10.0))
                .build();
    }

    // ==================== createAnalysis Tests ====================

    @Test
    void createAnalysis_WithValidData_ShouldReturnSavedAnalysis() {
        // Given
        when(roiAnalysisRepository.save(any(ROIAnalysis.class))).thenReturn(testROIAnalysis);

        // When
        ROIAnalysisDTO result = roiAnalysisService.createAnalysis(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getEstimatedCost()).isEqualByComparingTo("500000.00");
        assertThat(result.getEstimatedRevenue()).isEqualByComparingTo("800000.00");
        assertThat(result.getEstimatedProfit()).isEqualByComparingTo("300000.00");
        assertThat(result.getRoiPercentage()).isEqualByComparingTo("60.00");
        assertThat(result.getPaybackPeriodMonths()).isEqualTo(24);

        verify(roiAnalysisRepository).save(any(ROIAnalysis.class));
    }

    @Test
    void createAnalysis_WithNullProjectId_ShouldThrowException() {
        // Given
        ROIAnalysisCreateRequest invalidRequest = ROIAnalysisCreateRequest.builder()
                .projectId(null)
                .estimatedCost(new BigDecimal("500000.00"))
                .estimatedRevenue(new BigDecimal("800000.00"))
                .createdBy(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.createAnalysis(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID");
    }

    @Test
    void createAnalysis_WithNullCost_ShouldThrowException() {
        // Given
        ROIAnalysisCreateRequest invalidRequest = ROIAnalysisCreateRequest.builder()
                .projectId(100L)
                .estimatedCost(null)
                .estimatedRevenue(new BigDecimal("800000.00"))
                .createdBy(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.createAnalysis(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estimated cost");
    }

    @Test
    void createAnalysis_WithNegativeCost_ShouldThrowException() {
        // Given
        ROIAnalysisCreateRequest invalidRequest = ROIAnalysisCreateRequest.builder()
                .projectId(100L)
                .estimatedCost(new BigDecimal("-100000.00"))
                .estimatedRevenue(new BigDecimal("800000.00"))
                .createdBy(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.createAnalysis(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estimated cost");
    }

    @Test
    void createAnalysis_WithNullRevenue_ShouldThrowException() {
        // Given
        ROIAnalysisCreateRequest invalidRequest = ROIAnalysisCreateRequest.builder()
                .projectId(100L)
                .estimatedCost(new BigDecimal("500000.00"))
                .estimatedRevenue(null)
                .createdBy(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.createAnalysis(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estimated revenue");
    }

    @Test
    void createAnalysis_WithNegativeRevenue_ShouldThrowException() {
        // Given
        ROIAnalysisCreateRequest invalidRequest = ROIAnalysisCreateRequest.builder()
                .projectId(100L)
                .estimatedCost(new BigDecimal("500000.00"))
                .estimatedRevenue(new BigDecimal("-100000.00"))
                .createdBy(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.createAnalysis(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estimated revenue");
    }

    @Test
    void createAnalysis_ShouldAutoCalculateProfitAndROI() {
        // Given
        ROIAnalysisCreateRequest request = ROIAnalysisCreateRequest.builder()
                .projectId(100L)
                .estimatedCost(new BigDecimal("500000.00"))
                .estimatedRevenue(new BigDecimal("850000.00"))
                .paybackPeriodMonths(24)
                .createdBy(1L)
                .build();

        ROIAnalysis calculatedAnalysis = ROIAnalysis.builder()
                .id(1L)
                .projectId(100L)
                .estimatedCost(new BigDecimal("500000.00"))
                .estimatedRevenue(new BigDecimal("850000.00"))
                .estimatedProfit(new BigDecimal("350000.00"))
                .roiPercentage(new BigDecimal("70.00"))
                .paybackPeriodMonths(24)
                .build();

        when(roiAnalysisRepository.save(any(ROIAnalysis.class))).thenReturn(calculatedAnalysis);

        // When
        ROIAnalysisDTO result = roiAnalysisService.createAnalysis(request);

        // Then
        assertThat(result.getEstimatedProfit()).isEqualByComparingTo("350000.00");
        assertThat(result.getRoiPercentage()).isEqualByComparingTo("70.00");
    }

    @Test
    void createAnalysis_WithZeroCost_ShouldThrowException() {
        // Given
        ROIAnalysisCreateRequest invalidRequest = ROIAnalysisCreateRequest.builder()
                .projectId(100L)
                .estimatedCost(BigDecimal.ZERO)
                .estimatedRevenue(new BigDecimal("800000.00"))
                .createdBy(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.createAnalysis(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estimated cost must be greater than zero");
    }

    // ==================== getAnalysisByProject Tests ====================

    @Test
    void getAnalysisByProject_WithValidProjectId_ShouldReturnAnalysis() {
        // Given
        when(roiAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L)).thenReturn(Optional.of(testROIAnalysis));

        // When
        ROIAnalysisDTO result = roiAnalysisService.getAnalysisByProject(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getEstimatedCost()).isEqualByComparingTo("500000.00");
    }

    @Test
    void getAnalysisByProject_WithInvalidProjectId_ShouldThrowException() {
        // Given
        when(roiAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.getAnalysisByProject(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ROI analysis not found");
    }

    @Test
    void getAnalysisByProject_WithNullProjectId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.getAnalysisByProject(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID");
    }

    // ==================== calculateROI Tests ====================

    @Test
    void calculateROI_WithValidProjectId_ShouldReturnCalculatedROI() {
        // Given
        when(roiAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L)).thenReturn(Optional.empty());
        when(roiAnalysisRepository.save(any(ROIAnalysis.class))).thenReturn(testROIAnalysis);

        // When
        ROIAnalysisDTO result = roiAnalysisService.calculateROI(100L, createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getEstimatedProfit()).isEqualByComparingTo("300000.00");
        assertThat(result.getRoiPercentage()).isEqualByComparingTo("60.00");

        verify(roiAnalysisRepository).save(any(ROIAnalysis.class));
    }

    @Test
    void calculateROI_WithExistingAnalysis_ShouldUpdateExisting() {
        // Given
        when(roiAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L)).thenReturn(Optional.of(testROIAnalysis));
        when(roiAnalysisRepository.save(any(ROIAnalysis.class))).thenReturn(testROIAnalysis);

        // When
        ROIAnalysisDTO result = roiAnalysisService.calculateROI(100L, createRequest);

        // Then
        assertThat(result).isNotNull();
        verify(roiAnalysisRepository).save(any(ROIAnalysis.class));
    }

    @Test
    void calculateROI_WithNullProjectId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.calculateROI(null, createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID");
    }

    @Test
    void calculateROI_WithNullRequest_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.calculateROI(100L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request");
    }

    // ==================== performSensitivityAnalysis Tests ====================

    @Test
    void performSensitivityAnalysis_WithValidData_ShouldReturnResults() {
        // Given
        when(roiAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L)).thenReturn(Optional.of(testROIAnalysis));

        // When
        SensitivityAnalysisResult result = roiAnalysisService.performSensitivityAnalysis(100L, sensitivityRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getScenarios()).hasSize(9); // 3 cost variations * 3 revenue variations
    }

    @Test
    void performSensitivityAnalysis_WithNoExistingAnalysis_ShouldThrowException() {
        // Given
        when(roiAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.performSensitivityAnalysis(999L, sensitivityRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ROI analysis not found");
    }

    @Test
    void performSensitivityAnalysis_WithNullProjectId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.performSensitivityAnalysis(null, sensitivityRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID");
    }

    @Test
    void performSensitivityAnalysis_WithNullRequest_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.performSensitivityAnalysis(100L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sensitivity analysis request");
    }

    @Test
    void performSensitivityAnalysis_WithEmptyVariations_ShouldThrowException() {
        // Given
        SensitivityAnalysisRequest invalidRequest = SensitivityAnalysisRequest.builder()
                .projectId(100L)
                .costVariations(Arrays.asList())
                .revenueVariations(Arrays.asList())
                .build();

        // When & Then
        assertThatThrownBy(() -> roiAnalysisService.performSensitivityAnalysis(100L, invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("variations");
    }

    @Test
    void performSensitivityAnalysis_ShouldCalculateCorrectROIForScenarios() {
        // Given
        when(roiAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L)).thenReturn(Optional.of(testROIAnalysis));

        // When
        SensitivityAnalysisResult result = roiAnalysisService.performSensitivityAnalysis(100L, sensitivityRequest);

        // Then
        assertThat(result.getScenarios()).isNotEmpty();

        // Verify best case scenario
        var bestCase = result.getScenarios().stream()
                .filter(s -> s.getDescription().contains("Best case"))
                .findFirst();
        assertThat(bestCase).isPresent();

        // Verify worst case scenario
        var worstCase = result.getScenarios().stream()
                .filter(s -> s.getDescription().contains("Worst case"))
                .findFirst();
        assertThat(worstCase).isPresent();
    }

    // ==================== Edge Cases ====================

    @Test
    void createAnalysis_WithVeryLargeNumbers_ShouldHandleCorrectly() {
        // Given
        ROIAnalysisCreateRequest request = ROIAnalysisCreateRequest.builder()
                .projectId(100L)
                .estimatedCost(new BigDecimal("999999999999.99"))
                .estimatedRevenue(new BigDecimal("9999999999999.99"))
                .createdBy(1L)
                .build();

        ROIAnalysis largeAnalysis = ROIAnalysis.builder()
                .id(1L)
                .projectId(100L)
                .estimatedCost(new BigDecimal("999999999999.99"))
                .estimatedRevenue(new BigDecimal("9999999999999.99"))
                .estimatedProfit(new BigDecimal("9000000000000.00"))
                .roiPercentage(new BigDecimal("900.00"))
                .build();

        when(roiAnalysisRepository.save(any(ROIAnalysis.class))).thenReturn(largeAnalysis);

        // When
        ROIAnalysisDTO result = roiAnalysisService.createAnalysis(request);

        // Then
        assertThat(result.getEstimatedCost()).isEqualByComparingTo("999999999999.99");
        assertThat(result.getRoiPercentage()).isEqualByComparingTo("900.00");
    }

    @Test
    void createAnalysis_WithRevenueLowerThanCost_ShouldCalculateNegativeROI() {
        // Given
        ROIAnalysisCreateRequest request = ROIAnalysisCreateRequest.builder()
                .projectId(100L)
                .estimatedCost(new BigDecimal("800000.00"))
                .estimatedRevenue(new BigDecimal("500000.00"))
                .createdBy(1L)
                .build();

        ROIAnalysis negativeROI = ROIAnalysis.builder()
                .id(1L)
                .projectId(100L)
                .estimatedCost(new BigDecimal("800000.00"))
                .estimatedRevenue(new BigDecimal("500000.00"))
                .estimatedProfit(new BigDecimal("-300000.00"))
                .roiPercentage(new BigDecimal("-37.50"))
                .build();

        when(roiAnalysisRepository.save(any(ROIAnalysis.class))).thenReturn(negativeROI);

        // When
        ROIAnalysisDTO result = roiAnalysisService.createAnalysis(request);

        // Then
        assertThat(result.getEstimatedProfit()).isEqualByComparingTo("-300000.00");
        assertThat(result.getRoiPercentage()).isEqualByComparingTo("-37.50");
    }

    @Test
    void performSensitivityAnalysis_WithSingleVariation_ShouldReturnOneScenario() {
        // Given
        SensitivityAnalysisRequest singleVariation = SensitivityAnalysisRequest.builder()
                .projectId(100L)
                .costVariations(Arrays.asList(0.0))
                .revenueVariations(Arrays.asList(0.0))
                .build();

        when(roiAnalysisRepository.findFirstByProjectIdOrderByAnalysisDateDesc(100L)).thenReturn(Optional.of(testROIAnalysis));

        // When
        SensitivityAnalysisResult result = roiAnalysisService.performSensitivityAnalysis(100L, singleVariation);

        // Then
        assertThat(result.getScenarios()).hasSize(1);
    }
}
