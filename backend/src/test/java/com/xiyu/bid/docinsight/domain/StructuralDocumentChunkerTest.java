package com.xiyu.bid.docinsight.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StructuralDocumentChunkerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StructuralDocumentChunker chunker = new StructuralDocumentChunker(objectMapper);

    @Test
    void chunk_withMetadata_shouldSplitBySectionsAndPreservePath() {
        String text = "Title\nSection 1 Content\nSection 2 Content";
        String metadata = """
                {
                  "sections": [
                    {"heading": "Section 1", "charStart": 6, "charEnd": 23, "path": ["Chapter 1", "Section 1"]},
                    {"heading": "Section 2", "charStart": 24, "charEnd": 41, "path": ["Chapter 1", "Section 2"]}
                  ]
                }
                """;

        List<DocumentChunk> chunks = chunker.chunk(text, metadata);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).text()).contains("Section 1 Content");
        assertThat(chunks.get(0).sectionPath()).containsExactly("Chapter 1", "Section 1");
        assertThat(chunks.get(1).text()).contains("Section 2 Content");
        assertThat(chunks.get(1).sectionPath()).containsExactly("Chapter 1", "Section 2");
    }

    @Test
    void chunk_noMetadata_shouldFallbackToLegacy() {
        String text = "A".repeat(5000);
        List<DocumentChunk> chunks = chunker.chunk(text, null);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).sectionPath()).isEmpty();
    }
}
