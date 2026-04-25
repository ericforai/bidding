package com.xiyu.bid.biddraftagent.infrastructure.openai;

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
                    if (!currentChunk.isEmpty()) {
                        chunks.add(currentChunk.toString());
                        currentChunk.setLength(0);
                    }
                    
                    if (sectionText.length() > MAX_CHARS) {
                        List<String> subChunks = TenderDocumentTextChunker.split(sectionText, MAX_CHARS, OVERLAP_CHARS);
                        chunks.addAll(subChunks);
                    } else {
                        currentChunk.append(sectionText);
                    }
                } else {
                    currentChunk.append(sectionText);
                }
            }

            if (!currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString());
            }

            return chunks;

        } catch (Exception e) {
            log.warn("Failed to parse structured metadata for chunking, falling back to legacy chunker: {}", e.getMessage());
            return TenderDocumentTextChunker.split(text, MAX_CHARS, OVERLAP_CHARS);
        }
    }
}
