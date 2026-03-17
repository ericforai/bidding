package com.xiyu.bid.ai.client;

import com.xiyu.bid.ai.dto.AiAnalysisResponse;
import com.xiyu.bid.ai.dto.DimensionScore;
import com.xiyu.bid.entity.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Mock AI Provider
 * Provides realistic mock AI analysis responses for testing and development
 * Activated when ai.provider property is set to "mock" or not specified (default)
 */
@Service
@ConditionalOnProperty(
    name = "ai.provider",
    havingValue = "mock",
    matchIfMissing = true
)
@Slf4j
public class MockAiProvider implements AiProvider {

    private static final List<String> COMMON_STRENGTHS = Arrays.asList(
        "Strong technical team capabilities",
        "Competitive pricing strategy",
        "Good project management experience",
        "Solid financial health",
        "Relevant industry experience",
        "Excellent track record with similar projects"
    );

    private static final List<String> COMMON_WEAKNESSES = Arrays.asList(
        "Limited experience in very large-scale projects",
        "Tight timeline may require additional resources",
        "Some team members lack specific certifications",
        "Geographic distance from client location",
        "Limited portfolio in this specific sector"
    );

    private static final List<String> COMMON_RECOMMENDATIONS = Arrays.asList(
        "Highlight relevant case studies in the proposal",
        "Prepare detailed competitive pricing strategy",
        "Consider partnering with local firms for geographic coverage",
        "Obtain required certifications before bid submission",
        "Allocate additional resources for tight timeline",
        "Emphasize team qualifications and experience"
    );

    @Override
    public CompletableFuture<AiAnalysisResponse> analyzeTender(String content, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Mock AI analyzing tender content length: {}", content != null ? content.length() : 0);

            // Simulate AI processing delay
            simulateProcessingDelay();

            // Generate score based on content and context
            int score = calculateScore(content, context);
            Tender.RiskLevel riskLevel = calculateRiskLevel(score);

            return AiAnalysisResponse.builder()
                    .score(score)
                    .riskLevel(riskLevel)
                    .strengths(selectRandomStrengths(score))
                    .weaknesses(selectRandomWeaknesses(score))
                    .recommendations(selectRandomRecommendations(score))
                    .dimensionScores(generateDimensionScores(score))
                    .build();
        });
    }

    @Override
    public CompletableFuture<AiAnalysisResponse> analyzeProject(Long projectId, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Mock AI analyzing project id: {}", projectId);

            // Simulate AI processing delay
            simulateProcessingDelay();

            // Generate score based on project context
            int score = calculateProjectScore(projectId, context);
            Tender.RiskLevel riskLevel = calculateRiskLevel(score);

            return AiAnalysisResponse.builder()
                    .score(score)
                    .riskLevel(riskLevel)
                    .strengths(selectRandomStrengths(score))
                    .weaknesses(selectRandomWeaknesses(score))
                    .recommendations(selectRandomRecommendations(score))
                    .dimensionScores(generateProjectDimensionScores(score))
                    .build();
        });
    }

    /**
     * Calculate score based on content and context
     */
    private int calculateScore(String content, Map<String, Object> context) {
        int baseScore = 60;

        // Adjust based on content length
        if (content != null && !content.isEmpty()) {
            if (content.length() > 1000) {
                baseScore += 10;
            } else if (content.length() > 500) {
                baseScore += 5;
            }
        }

        // Adjust based on context
        if (context != null) {
            Object budget = context.get("budget");
            if (budget instanceof Number) {
                double budgetValue = ((Number) budget).doubleValue();
                if (budgetValue > 1000000) {
                    baseScore += 15;
                } else if (budgetValue > 500000) {
                    baseScore += 10;
                }
            }
        }

        return Math.min(100, Math.max(0, baseScore + getRandomVariation()));
    }

    /**
     * Calculate project-specific score
     */
    private int calculateProjectScore(Long projectId, Map<String, Object> context) {
        int baseScore = 55;

        // Adjust based on context
        if (context != null) {
            Object teamSize = context.get("teamSize");
            if (teamSize instanceof Number) {
                int size = ((Number) teamSize).intValue();
                if (size >= 5) {
                    baseScore += 15;
                } else if (size >= 3) {
                    baseScore += 10;
                }
            }
        }

        return Math.min(100, Math.max(0, baseScore + getRandomVariation()));
    }

    /**
     * Calculate risk level based on score
     */
    private Tender.RiskLevel calculateRiskLevel(int score) {
        if (score >= 70) {
            return Tender.RiskLevel.LOW;
        } else if (score >= 50) {
            return Tender.RiskLevel.MEDIUM;
        } else {
            return Tender.RiskLevel.HIGH;
        }
    }

    /**
     * Select appropriate strengths based on score
     */
    private List<String> selectRandomStrengths(int score) {
        int count = score >= 70 ? 3 : (score >= 50 ? 2 : 1);
        return selectRandomItems(COMMON_STRENGTHS, count);
    }

    /**
     * Select appropriate weaknesses based on score
     */
    private List<String> selectRandomWeaknesses(int score) {
        int count = score >= 70 ? 1 : (score >= 50 ? 2 : 3);
        return selectRandomItems(COMMON_WEAKNESSES, count);
    }

    /**
     * Select appropriate recommendations based on score
     */
    private List<String> selectRandomRecommendations(int score) {
        int count = score >= 70 ? 2 : (score >= 50 ? 3 : 4);
        return selectRandomItems(COMMON_RECOMMENDATIONS, count);
    }

    /**
     * Generate dimension scores for tender analysis
     */
    private List<DimensionScore> generateDimensionScores(int overallScore) {
        int technicalScore = Math.min(100, overallScore + getRandomVariation());
        int financialScore = Math.min(100, overallScore + getRandomVariation());
        int timingScore = Math.min(100, overallScore + getRandomVariation());

        return Arrays.asList(
            DimensionScore.builder()
                    .dimension("Technical")
                    .score(normalizeScore(technicalScore))
                    .details("Technical capabilities and expertise assessment")
                    .build(),
            DimensionScore.builder()
                    .dimension("Financial")
                    .score(normalizeScore(financialScore))
                    .details("Financial health and pricing competitiveness")
                    .build(),
            DimensionScore.builder()
                    .dimension("Timing")
                    .score(normalizeScore(timingScore))
                    .details("Project timeline and delivery feasibility")
                    .build()
        );
    }

    /**
     * Generate dimension scores for project analysis
     */
    private List<DimensionScore> generateProjectDimensionScores(int overallScore) {
        int teamScore = Math.min(100, overallScore + getRandomVariation());
        int resourceScore = Math.min(100, overallScore + getRandomVariation());
        int riskScore = Math.min(100, overallScore + getRandomVariation());

        return Arrays.asList(
            DimensionScore.builder()
                    .dimension("Team")
                    .score(normalizeScore(teamScore))
                    .details("Team composition and capabilities")
                    .build(),
            DimensionScore.builder()
                    .dimension("Resources")
                    .score(normalizeScore(resourceScore))
                    .details("Resource allocation and availability")
                    .build(),
            DimensionScore.builder()
                    .dimension("Risk")
                    .score(normalizeScore(riskScore))
                    .details("Risk assessment and mitigation strategies")
                    .build()
        );
    }

    /**
     * Select random items from a list
     */
    private List<String> selectRandomItems(List<String> items, int count) {
        if (items.isEmpty() || count <= 0) {
            return List.of();
        }

        int actualCount = Math.min(count, items.size());
        java.util.Collections.shuffle(items);
        return items.subList(0, actualCount);
    }

    /**
     * Get random score variation (-10 to +10)
     */
    private int getRandomVariation() {
        return (int) (Math.random() * 21) - 10;
    }

    /**
     * Normalize score to 0-100 range
     */
    private int normalizeScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Simulate AI processing delay (100-500ms)
     */
    private void simulateProcessingDelay() {
        try {
            int delay = 100 + (int) (Math.random() * 400);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Mock AI processing delay interrupted", e);
        }
    }
}
