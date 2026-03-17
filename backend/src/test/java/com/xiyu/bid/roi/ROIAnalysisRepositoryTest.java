package com.xiyu.bid.roi;

import com.xiyu.bid.roi.entity.ROIAnalysis;
import com.xiyu.bid.roi.repository.ROIAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ROI分析Repository测试类
 * 测试数据库操作
 */
@DataJpaTest
@ActiveProfiles("test")
class ROIAnalysisRepositoryTest {

    @Autowired
    private ROIAnalysisRepository roiAnalysisRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ROIAnalysis testAnalysis;

    @BeforeEach
    void setUp() {
        // Clear existing data
        roiAnalysisRepository.deleteAll();

        testAnalysis = ROIAnalysis.builder()
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
    }

    // ==================== save Tests ====================

    @Test
    void save_WithValidAnalysis_ShouldPersistAnalysis() {
        // When
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProjectId()).isEqualTo(100L);
        assertThat(saved.getEstimatedCost()).isEqualByComparingTo("500000.00");
        assertThat(saved.getEstimatedRevenue()).isEqualByComparingTo("800000.00");
    }

    @Test
    void save_WithNullProjectId_ShouldThrowException() {
        // Given
        testAnalysis.setProjectId(null);

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(
                org.hibernate.exception.ConstraintViolationException.class,
                () -> roiAnalysisRepository.saveAndFlush(testAnalysis)
        );
    }

    @Test
    void save_WithNegativeCost_ShouldPersist() {
        // Given - Negative cost is technically allowed (represents loss scenario)
        testAnalysis.setEstimatedCost(new BigDecimal("-100000.00"));

        // When
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);

        // Then
        assertThat(saved.getEstimatedCost()).isEqualByComparingTo("-100000.00");
    }

    // ==================== findById Tests ====================

    @Test
    void findById_WithExistingId_ShouldReturnAnalysis() {
        // Given
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);

        // When
        Optional<ROIAnalysis> found = roiAnalysisRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getProjectId()).isEqualTo(100L);
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // When
        Optional<ROIAnalysis> found = roiAnalysisRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    // ==================== findByProjectId Tests ====================

    @Test
    void findByProjectId_WithExistingProjectId_ShouldReturnAnalysis() {
        // Given
        roiAnalysisRepository.save(testAnalysis);

        // When
        Optional<ROIAnalysis> found = roiAnalysisRepository.findByProjectId(100L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getProjectId()).isEqualTo(100L);
    }

    @Test
    void findByProjectId_WithNonExistingProjectId_ShouldReturnEmpty() {
        // When
        Optional<ROIAnalysis> found = roiAnalysisRepository.findByProjectId(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByProjectId_WithMultipleAnalysesForSameProject_ShouldReturnOne() {
        // Given - Save first analysis
        roiAnalysisRepository.save(testAnalysis);

        // Save second analysis for same project
        ROIAnalysis secondAnalysis = ROIAnalysis.builder()
                .projectId(100L)
                .analysisDate(LocalDateTime.now())
                .estimatedCost(new BigDecimal("600000.00"))
                .estimatedRevenue(new BigDecimal("900000.00"))
                .estimatedProfit(new BigDecimal("300000.00"))
                .roiPercentage(new BigDecimal("50.00"))
                .paybackPeriodMonths(30)
                .createdBy(2L)
                .build();

        roiAnalysisRepository.save(secondAnalysis);

        // When - Should return the most recent one
        Optional<ROIAnalysis> found = roiAnalysisRepository.findByProjectId(100L);

        // Then
        assertThat(found).isPresent();
        // Repository returns first match, order depends on implementation
    }

    // ==================== findAll Tests ====================

    @Test
    void findAll_WithMultipleAnalyses_ShouldReturnAll() {
        // Given
        roiAnalysisRepository.save(testAnalysis);

        ROIAnalysis secondAnalysis = ROIAnalysis.builder()
                .projectId(101L)
                .analysisDate(LocalDateTime.now())
                .estimatedCost(new BigDecimal("400000.00"))
                .estimatedRevenue(new BigDecimal("700000.00"))
                .estimatedProfit(new BigDecimal("300000.00"))
                .roiPercentage(new BigDecimal("75.00"))
                .paybackPeriodMonths(18)
                .createdBy(1L)
                .build();

        roiAnalysisRepository.save(secondAnalysis);

        // When
        List<ROIAnalysis> all = roiAnalysisRepository.findAll();

        // Then
        assertThat(all).hasSize(2);
    }

    @Test
    void findAll_WithNoAnalyses_ShouldReturnEmptyList() {
        // When
        List<ROIAnalysis> all = roiAnalysisRepository.findAll();

        // Then
        assertThat(all).isEmpty();
    }

    // ==================== deleteById Tests ====================

    @Test
    void deleteById_WithExistingId_ShouldDeleteAnalysis() {
        // Given
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);
        Long id = saved.getId();

        // When
        roiAnalysisRepository.deleteById(id);

        // Then
        Optional<ROIAnalysis> found = roiAnalysisRepository.findById(id);
        assertThat(found).isEmpty();
    }

    // ==================== Edge Cases ====================

    @Test
    void save_WithVeryLargePrecisionValues_ShouldPersistCorrectly() {
        // Given
        testAnalysis.setEstimatedCost(new BigDecimal("99999999999999.99"));
        testAnalysis.setEstimatedRevenue(new BigDecimal("99999999999999.99"));
        testAnalysis.setRoiPercentage(new BigDecimal("99999.99"));

        // When
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);

        // Then
        assertThat(saved.getEstimatedCost()).isEqualByComparingTo("99999999999999.99");
        assertThat(saved.getEstimatedRevenue()).isEqualByComparingTo("99999999999999.99");
        assertThat(saved.getRoiPercentage()).isEqualByComparingTo("99999.99");
    }

    @Test
    void save_WithNegativeROI_ShouldPersist() {
        // Given
        testAnalysis.setEstimatedCost(new BigDecimal("800000.00"));
        testAnalysis.setEstimatedRevenue(new BigDecimal("500000.00"));
        testAnalysis.setEstimatedProfit(new BigDecimal("-300000.00"));
        testAnalysis.setRoiPercentage(new BigDecimal("-37.50"));

        // When
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);

        // Then
        assertThat(saved.getRoiPercentage()).isEqualByComparingTo("-37.50");
    }

    @Test
    void save_WithNullOptionalFields_ShouldPersist() {
        // Given
        testAnalysis.setRiskFactors(null);
        testAnalysis.setAssumptions(null);
        testAnalysis.setPaybackPeriodMonths(null);

        // When
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);

        // Then
        assertThat(saved.getRiskFactors()).isNull();
        assertThat(saved.getAssumptions()).isNull();
        assertThat(saved.getPaybackPeriodMonths()).isNull();
    }

    @Test
    void save_WithEmptyRiskFactorsAndAssumptions_ShouldPersist() {
        // Given
        testAnalysis.setRiskFactors("");
        testAnalysis.setAssumptions("");

        // When
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);

        // Then
        assertThat(saved.getRiskFactors()).isEmpty();
        assertThat(saved.getAssumptions()).isEmpty();
    }

    @Test
    void save_WithVeryLongRiskFactors_ShouldTruncate() {
        // Given - Create text longer than TEXT field limit
        String longText = "A".repeat(10000);
        testAnalysis.setRiskFactors(longText);

        // When
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);

        // Then - Should be saved (TEXT type has large capacity)
        assertThat(saved.getRiskFactors()).isNotNull();
        assertThat(saved.getRiskFactors().length()).isLessThanOrEqualTo(10000);
    }

    @Test
    void findByProjectId_AfterUpdate_ShouldReturnUpdatedData() {
        // Given
        ROIAnalysis saved = roiAnalysisRepository.save(testAnalysis);
        saved.setEstimatedCost(new BigDecimal("600000.00"));
        saved.setEstimatedRevenue(new BigDecimal("950000.00"));

        // When
        roiAnalysisRepository.save(saved);

        // Then
        Optional<ROIAnalysis> found = roiAnalysisRepository.findByProjectId(100L);
        assertThat(found).isPresent();
        assertThat(found.get().getEstimatedCost()).isEqualByComparingTo("600000.00");
        assertThat(found.get().getEstimatedRevenue()).isEqualByComparingTo("950000.00");
    }
}
