package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.biddraftagent.application.TenderDocumentAnalysisInput;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import com.xiyu.bid.docinsight.domain.StructuralDocumentChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiTenderDocumentAnalyzerTest {

    @Mock
    private OpenAiBidAgentConfigurationResolver configurationResolver;

    @Mock
    private OpenAiStructuredOutputService structuredOutputService;

    private OpenAiTenderDocumentAnalyzer analyzer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        StructuralDocumentChunker chunker = new StructuralDocumentChunker(objectMapper);
        analyzer = new OpenAiTenderDocumentAnalyzer(configurationResolver, structuredOutputService, chunker);
        
        when(configurationResolver.resolve(anyString())).thenReturn(new OpenAiBidAgentRequestConfig(
                "key", "http://api.openai.com", "gpt-4", java.time.Duration.ofSeconds(30), OpenAiBidAgentApiStyle.CHAT_COMPLETIONS
        ));
    }

    @Test
    void analyze_shouldUseStructuralChunksAndPreserveSectionPath() {
        String text = "# Chapter 1\\nQualification Content\\n# Chapter 2\\nTechnical Content";
        String metadata = """
                {
                  "sections": [
                    {"heading": "Chapter 1", "charStart": 0, "charEnd": 34, "path": ["Chapter 1"]},
                    {"heading": "Chapter 2", "charStart": 35, "charEnd": 68, "path": ["Chapter 2"]}
                  ]
                }
                """;
        TenderDocumentAnalysisInput input = new TenderDocumentAnalysisInput(1L, 100L, "test.docx", text, metadata);

        // Mock AI responses
        OpenAiTenderDocumentAnalyzer.TenderRequirementOutput output1 = new OpenAiTenderDocumentAnalyzer.TenderRequirementOutput();
        OpenAiTenderDocumentAnalyzer.TenderRequirementItemOutput item1 = new OpenAiTenderDocumentAnalyzer.TenderRequirementItemOutput();
        item1.category = "qualification";
        item1.title = "Cert 1";
        item1.sectionPath = ""; // AI didn't return path, should use default
        output1.requirementItems = List.of(item1);

        OpenAiTenderDocumentAnalyzer.TenderRequirementOutput output2 = new OpenAiTenderDocumentAnalyzer.TenderRequirementOutput();
        OpenAiTenderDocumentAnalyzer.TenderRequirementItemOutput item2 = new OpenAiTenderDocumentAnalyzer.TenderRequirementItemOutput();
        item2.category = "technical";
        item2.title = "Spec 1";
        item2.sectionPath = "Chapter 2 > 2.1 Fine Grained"; // AI returned more specific path
        output2.requirementItems = List.of(item2);

        when(structuredOutputService.request(anyString(), eq(OpenAiTenderDocumentAnalyzer.TenderRequirementOutput.class), any(), anyString()))
                .thenReturn(output1)
                .thenReturn(output2);

        TenderRequirementProfile result = analyzer.analyze(input);

        assertThat(result.items()).hasSize(2);
        
        // Item 1: path inferred from chunk
        assertThat(result.items().get(0).title()).isEqualTo("Cert 1");
        assertThat(result.items().get(0).sectionPath()).isEqualTo("Chapter 1");

        // Item 2: path taken from AI response
        assertThat(result.items().get(1).title()).isEqualTo("Spec 1");
        assertThat(result.items().get(1).sectionPath()).isEqualTo("Chapter 2 > 2.1 Fine Grained");

        // Verify prompts contained section info
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(structuredOutputService, times(2)).request(promptCaptor.capture(), any(), any(), anyString());
        
        assertThat(promptCaptor.getAllValues().get(0)).contains("当前正文所属章节路径: Chapter 1");
        assertThat(promptCaptor.getAllValues().get(1)).contains("当前正文所属章节路径: Chapter 2");
    }
}
