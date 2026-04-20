package com.xiyu.bid.scoreanalysis;

import com.xiyu.bid.scoreanalysis.dto.DimensionScoreDTO;
import com.xiyu.bid.scoreanalysis.dto.ScoreAnalysisCreateRequest;
import com.xiyu.bid.scoreanalysis.entity.DimensionScore;
import com.xiyu.bid.scoreanalysis.entity.ScoreAnalysis;
import com.xiyu.bid.scoreanalysis.repository.DimensionScoreRepository;
import com.xiyu.bid.scoreanalysis.repository.ScoreAnalysisRepository;
import com.xiyu.bid.scoreanalysis.service.ScoreAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
abstract class AbstractScoreAnalysisServiceTest {

    @Mock
    protected ScoreAnalysisRepository scoreAnalysisRepository;

    @Mock
    protected DimensionScoreRepository dimensionScoreRepository;

    protected ScoreAnalysisService scoreAnalysisService;
    protected ScoreAnalysis testAnalysis;
    protected DimensionScore testDimension;
    protected ScoreAnalysisCreateRequest createRequest;

    @BeforeEach
    void setUpScoreAnalysisFixture() {
        scoreAnalysisService = new ScoreAnalysisService(scoreAnalysisRepository, dimensionScoreRepository);

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
                DimensionScoreDTO.builder().dimensionName("技术能力").score(90).weight(new BigDecimal("0.30")).build(),
                DimensionScoreDTO.builder().dimensionName("财务实力").score(85).weight(new BigDecimal("0.25")).build(),
                DimensionScoreDTO.builder().dimensionName("团队经验").score(80).weight(new BigDecimal("0.20")).build(),
                DimensionScoreDTO.builder().dimensionName("历史业绩").score(88).weight(new BigDecimal("0.15")).build(),
                DimensionScoreDTO.builder().dimensionName("合规性").score(95).weight(new BigDecimal("0.10")).build()
        );

        createRequest = ScoreAnalysisCreateRequest.builder()
                .projectId(100L)
                .analystId(10L)
                .isAiGenerated(true)
                .summary("综合评估优秀")
                .dimensions(dimensions)
                .build();
    }
}
