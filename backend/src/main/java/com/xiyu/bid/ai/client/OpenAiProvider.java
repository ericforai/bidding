package com.xiyu.bid.ai.client;

import com.xiyu.bid.ai.dto.AiAnalysisResponse;
import com.xiyu.bid.ai.dto.DimensionScore;
import com.xiyu.bid.entity.Tender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

/**
 * OpenAI Provider
 * Integrates with OpenAI API for real AI analysis
 * Activated when ai.provider property is set to "openai"
 */
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
@Slf4j
public class OpenAiProvider implements AiProvider {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-3.5-turbo";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public OpenAiProvider(RestTemplateBuilder restTemplateBuilder) {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            log.warn("OPENAI_API_KEY environment variable not set. OpenAI provider will not function correctly.");
        }

        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(TIMEOUT)
                .setReadTimeout(TIMEOUT)
                .build();

        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AiAnalysisResponse analyzeTender(String content, Map<String, Object> context) {
        try {
            log.debug("OpenAI analyzing tender content length: {}", content != null ? content.length() : 0);

            String prompt = buildTenderAnalysisPrompt(content, context);
            String response = callOpenAI(prompt);

            return parseAnalysisResponse(response);
        } catch (Exception e) {
            log.error("Error calling OpenAI API for tender analysis", e);
            throw new RuntimeException("Failed to analyze tender with OpenAI", e);
        }
    }

    @Override
    public AiAnalysisResponse analyzeProject(Long projectId, Map<String, Object> context) {
        try {
            log.debug("OpenAI analyzing project id: {}", projectId);

            String prompt = buildProjectAnalysisPrompt(projectId, context);
            String response = callOpenAI(prompt);

            return parseAnalysisResponse(response);
        } catch (Exception e) {
            log.error("Error calling OpenAI API for project analysis", e);
            throw new RuntimeException("Failed to analyze project with OpenAI", e);
        }
    }

    /**
     * Build prompt for tender analysis
     */
    private String buildTenderAnalysisPrompt(String content, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following tender opportunity and provide a comprehensive assessment.\n\n");

        if (content != null && !content.isEmpty()) {
            prompt.append("TENDER CONTENT:\n").append(content).append("\n\n");
        }

        if (context != null && !context.isEmpty()) {
            prompt.append("ADDITIONAL CONTEXT:\n");
            context.forEach((key, value) -> prompt.append("- ").append(key).append(": ").append(value).append("\n"));
            prompt.append("\n");
        }

        prompt.append("""
                Please provide your analysis in JSON format with the following structure:
                {
                  "score": <integer 0-100>,
                  "riskLevel": <"LOW", "MEDIUM", or "HIGH">,
                  "strengths": ["<strength 1>", "<strength 2>", ...],
                  "weaknesses": ["<weakness 1>", "<weakness 2>", ...],
                  "recommendations": ["<recommendation 1>", "<recommendation 2>", ...],
                  "dimensionScores": [
                    {"dimension": "Technical", "score": <0-100>, "details": "<explanation>"},
                    {"dimension": "Financial", "score": <0-100>, "details": "<explanation>"},
                    {"dimension": "Timing", "score": <0-100>, "details": "<explanation>"}
                  ]
                }
                """);

        return prompt.toString();
    }

    /**
     * Build prompt for project analysis
     */
    private String buildProjectAnalysisPrompt(Long projectId, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following project and provide a comprehensive assessment.\n\n");
        prompt.append("PROJECT ID: ").append(projectId).append("\n\n");

        if (context != null && !context.isEmpty()) {
            prompt.append("PROJECT CONTEXT:\n");
            context.forEach((key, value) -> prompt.append("- ").append(key).append(": ").append(value).append("\n"));
            prompt.append("\n");
        }

        prompt.append("""
                Please provide your analysis in JSON format with the following structure:
                {
                  "score": <integer 0-100>,
                  "riskLevel": <"LOW", "MEDIUM", or "HIGH">,
                  "strengths": ["<strength 1>", "<strength 2>", ...],
                  "weaknesses": ["<weakness 1>", "<weakness 2>", ...],
                  "recommendations": ["<recommendation 1>", "<recommendation 2>", ...],
                  "dimensionScores": [
                    {"dimension": "Team", "score": <0-100>, "details": "<explanation>"},
                    {"dimension": "Resources", "score": <0-100>, "details": "<explanation>"},
                    {"dimension": "Risk", "score": <0-100>, "details": "<explanation>"}
                  ]
                }
                """);

        return prompt.toString();
    }

    /**
     * Call OpenAI API
     */
    private String callOpenAI(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured. Please set OPENAI_API_KEY environment variable.");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are an expert bidding consultant analyzing tender opportunities and projects."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractContentFromResponse(response.getBody());
            } else {
                throw new RuntimeException("OpenAI API request failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }

    /**
     * Extract content from OpenAI response
     */
    private String extractContentFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new RuntimeException("Invalid OpenAI response format");
        } catch (Exception e) {
            log.error("Error parsing OpenAI response", e);
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    /**
     * Parse analysis response from OpenAI
     */
    private AiAnalysisResponse parseAnalysisResponse(String response) {
        try {
            // Try to extract JSON from the response (in case there's extra text)
            String jsonContent = extractJson(response);

            JsonNode root = objectMapper.readTree(jsonContent);

            int score = root.path("score").asInt();
            String riskLevelStr = root.path("riskLevel").asText();
            Tender.RiskLevel riskLevel = Tender.RiskLevel.valueOf(riskLevelStr);

            List<String> strengths = parseStringList(root.path("strengths"));
            List<String> weaknesses = parseStringList(root.path("weaknesses"));
            List<String> recommendations = parseStringList(root.path("recommendations"));

            List<DimensionScore> dimensionScores = parseDimensionScores(root.path("dimensionScores"));

            return AiAnalysisResponse.builder()
                    .score(score)
                    .riskLevel(riskLevel)
                    .strengths(strengths)
                    .weaknesses(weaknesses)
                    .recommendations(recommendations)
                    .dimensionScores(dimensionScores)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing analysis response", e);
            throw new RuntimeException("Failed to parse analysis response", e);
        }
    }

    /**
     * Extract JSON from response text
     */
    private String extractJson(String response) {
        int startIndex = response.indexOf("{");
        int endIndex = response.lastIndexOf("}");

        if (startIndex >= 0 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }

        return response;
    }

    /**
     * Parse string list from JSON node
     */
    private List<String> parseStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                result.add(item.asText());
            }
        }
        return result;
    }

    /**
     * Parse dimension scores from JSON node
     */
    private List<DimensionScore> parseDimensionScores(JsonNode node) {
        List<DimensionScore> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                result.add(DimensionScore.builder()
                        .dimension(item.path("dimension").asText())
                        .score(item.path("score").asInt())
                        .details(item.path("details").asText())
                        .build());
            }
        }
        return result;
    }
}
