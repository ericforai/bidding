package com.xiyu.bid.ai.client;

import com.xiyu.bid.ai.dto.AiAnalysisResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI Provider Interface
 * Defines the contract for AI analysis providers (Mock, OpenAI, etc.)
 */
public interface AiProvider {

    /**
     * Analyze tender content
     *
     * @param content The tender content to analyze
     * @param context Additional context information (budget, deadline, etc.)
     * @return CompletableFuture containing analysis results
     */
    CompletableFuture<AiAnalysisResponse> analyzeTender(String content, Map<String, Object> context);

    /**
     * Analyze project
     *
     * @param projectId The project ID to analyze
     * @param context Additional context information
     * @return CompletableFuture containing analysis results
     */
    CompletableFuture<AiAnalysisResponse> analyzeProject(Long projectId, Map<String, Object> context);
}
