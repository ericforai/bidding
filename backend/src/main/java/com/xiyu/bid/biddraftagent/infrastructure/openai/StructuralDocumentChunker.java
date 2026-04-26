package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class StructuralDocumentChunker {

    private final ObjectMapper objectMapper;
    private static final int MAX_CHARS = 4000;
    private static final int OVERLAP_CHARS = 200;

    public StructuralDocumentChunker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<String> chunk(String text, String structuredMetadata) {
        if (structuredMetadata == null || structuredMetadata.isBlank()) {
            return TenderDocumentTextChunker.split(text, MAX_CHARS, OVERLAP_CHARS);
        }

        try {
            JsonNode root = objectMapper.readTree(structuredMetadata);
            JsonNode sectionsNode = root.path("sections");
            
            if (sectionsNode.isMissingNode() || !sectionsNode.isArray() || sectionsNode.isEmpty()) {
                return TenderDocumentTextChunker.split(text, MAX_CHARS, OVERLAP_CHARS);
            }

            List<String> chunks = new ArrayList<>();
            StringBuilder currentChunk = new StringBuilder();

            for (JsonNode section : sectionsNode) {
                int start = section.path("charStart").asInt(0);
                int end = section.path("charEnd").asInt(text.length());

                start = Math.max(0, Math.min(start, text.length()));
                end = Math.max(0, Math.min(end, text.length()));

                if (start >= end) continue;

                String sectionText = text.substring(start, end);

                if (currentChunk.length() + sectionText.length() > MAX_CHARS) {
                    String tail = flushWithOverlap(chunks, currentChunk);

                    if (sectionText.length() > MAX_CHARS) {
                        // Prepend the overlap tail so context at the boundary is preserved
                        // before handing off to the sub-chunker for the oversized section.
                        String withContext = tail + sectionText;
                        List<String> subChunks = TenderDocumentTextChunker.split(withContext, MAX_CHARS, OVERLAP_CHARS);
                        chunks.addAll(subChunks);
                    } else {
                        currentChunk.append(tail).append(sectionText);
                    }
                } else {
                    currentChunk.append(sectionText);
                }
            }

            if (!currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString());
            }

            return chunks;

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse structured metadata for chunking, falling back to legacy chunker: {}", e.getMessage());
            return TenderDocumentTextChunker.split(text, MAX_CHARS, OVERLAP_CHARS);
        }
    }

    /**
     * Flushes the in-progress chunk into {@code chunks} and returns its trailing
     * {@link #OVERLAP_CHARS} characters so the caller can prepend them to the next
     * chunk, keeping context continuous across section boundaries.
     */
    private static String flushWithOverlap(List<String> chunks, StringBuilder currentChunk) {
        if (currentChunk.isEmpty()) {
            return "";
        }
        String flushed = currentChunk.toString();
        chunks.add(flushed);
        currentChunk.setLength(0);
        return flushed.length() > OVERLAP_CHARS
                ? flushed.substring(flushed.length() - OVERLAP_CHARS)
                : flushed;
    }
}
