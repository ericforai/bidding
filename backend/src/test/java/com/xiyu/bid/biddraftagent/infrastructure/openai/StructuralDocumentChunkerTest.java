// Input: StructuralDocumentChunker 的分块行为（结构化元数据、边界裁剪、溢出拆分、overlap）
// Output: 分块行为的单元测试覆盖（含 MED-2 overlap 回归）
// Pos: Test/biddraftagent/infrastructure/openai
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StructuralDocumentChunkerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StructuralDocumentChunker chunker = new StructuralDocumentChunker(objectMapper);

    @Test
    void fallsBackToLegacyWhenMetadataIsNull() {
        String text = "abc".repeat(100);
        List<String> chunks = chunker.chunk(text, null);
        assertThat(chunks).isNotEmpty();
        assertThat(String.join("", chunks)).contains(text.substring(0, 50));
    }

    @Test
    void fallsBackToLegacyWhenMetadataIsBlank() {
        String text = "short text";
        List<String> chunks = chunker.chunk(text, "   ");
        assertThat(chunks).containsExactly(text);
    }

    @Test
    void fallsBackToLegacyWhenMetadataIsMalformedJson() {
        String text = "hello world";
        List<String> chunks = chunker.chunk(text, "{ not valid json ");
        assertThat(chunks).containsExactly(text);
    }

    @Test
    void fallsBackToLegacyWhenSectionsArrayIsEmpty() {
        String text = "only the fallback path";
        List<String> chunks = chunker.chunk(text, "{\"sections\":[]}");
        assertThat(chunks).containsExactly(text);
    }

    @Test
    void wellFormedSectionsCombineIntoSingleChunkWhenWithinBudget() {
        String text = "AAA BBB CCC";
        String metadata = """
                {"sections":[
                  {"charStart":0, "charEnd":3},
                  {"charStart":4, "charEnd":7},
                  {"charStart":8, "charEnd":11}
                ]}
                """;
        List<String> chunks = chunker.chunk(text, metadata);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("AAABBBCCC");
    }

    @Test
    void sectionsWithOutOfRangeOffsetsAreClampedWithoutStringIndexOutOfBounds() {
        String text = "hello";
        String metadata = """
                {"sections":[
                  {"charStart":-5, "charEnd":2},
                  {"charStart":3, "charEnd":9999},
                  {"charStart":10, "charEnd":20}
                ]}
                """;
        // Should not throw; out-of-range entries clamped; fully invalid entries (start>=end) skipped.
        List<String> chunks = chunker.chunk(text, metadata);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("he" + "lo");
    }

    @Test
    void sectionExceedingMaxCharsIsSubChunked() {
        String oversized = "x".repeat(4500);
        String text = "prefix" + oversized;
        String metadata = String.format("{\"sections\":[{\"charStart\":0, \"charEnd\":%d}]}", text.length());
        List<String> chunks = chunker.chunk(text, metadata);
        // 4506 chars > 4000 MAX → produces at least 2 sub-chunks
        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
        assertThat(String.join("", chunks)).contains(oversized.substring(0, 1000));
    }

    @Test
    void chunkOverlapPreservesTailOfPreviousChunkAtSectionBoundary() {
        // Build two sections that each fit but together exceed 4000 chars.
        String tailMarker = "LAST_MARKER_OF_FIRST_CHUNK";
        String firstSection = "A".repeat(3500 - tailMarker.length()) + tailMarker;
        String secondSection = "B".repeat(3500);
        String text = firstSection + secondSection;
        int boundary = firstSection.length();
        String metadata = String.format(
                "{\"sections\":[{\"charStart\":0, \"charEnd\":%d}, {\"charStart\":%d, \"charEnd\":%d}]}",
                boundary, boundary, text.length());

        List<String> chunks = chunker.chunk(text, metadata);
        // First chunk is firstSection only (3500 chars); second chunk carries overlap from first.
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isEqualTo(firstSection);
        // MED-2 regression: the second chunk must carry the trailing context of the first,
        // so LAST_MARKER_OF_FIRST_CHUNK is visible to the model when it analyzes chunk 2.
        assertThat(chunks.get(1)).contains(tailMarker);
        assertThat(chunks.get(1)).endsWith(secondSection);
        // Overlap guarantees the marker appears BEFORE the second section's content.
        assertThat(chunks.get(1).indexOf(tailMarker)).isLessThan(chunks.get(1).indexOf("B"));
    }
}
