package com.xiyu.bid.biddraftagent.infrastructure.tenderdocument;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.biddraftagent.application.ExtractedTenderDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkItDownSidecarTextExtractorTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PoiPdfTenderDocumentTextExtractor fallbackExtractor;

    private MarkItDownSidecarTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new MarkItDownSidecarTextExtractor(
                restTemplate,
                new ObjectMapper(),
                "http://localhost:8000",
                fallbackExtractor
        );
    }

    @Test
    void shouldExtractTextViaSidecar() {
        String sidecarResponse = """
                {
                  "documentId": "test.pdf",
                  "markdown": "# Header\\nContent",
                  "sections": [],
                  "warnings": [],
                  "converter": "markitdown",
                  "contentHash": "12345"
                }
                """;

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(sidecarResponse);

        byte[] content = "dummy-pdf-content".getBytes();
        ExtractedTenderDocument result = extractor.extract("test.pdf", "application/pdf", content);

        assertThat(result.text()).isEqualTo("# Header\nContent");
        assertThat(result.structuredMetadata()).isEqualTo(sidecarResponse);
        assertThat(result.extractorKey()).isEqualTo("markitdown-sidecar");
    }

    @Test
    void shouldFallbackToLegacyWhenSidecarFails() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        byte[] content = "dummy-pdf-content".getBytes();
        ExtractedTenderDocument legacyResult = new ExtractedTenderDocument("test.pdf", "application/pdf", "Legacy Text", 11, "poi-pdfbox-v1", null);

        when(fallbackExtractor.extract("test.pdf", "application/pdf", content)).thenReturn(legacyResult);

        ExtractedTenderDocument result = extractor.extract("test.pdf", "application/pdf", content);

        assertThat(result.text()).isEqualTo("Legacy Text");
        assertThat(result.extractorKey()).isEqualTo("poi-pdfbox-v1");
        verify(fallbackExtractor).extract("test.pdf", "application/pdf", content);
    }

    @Test
    void shouldFallbackWhenSidecarReturnsEmptyMarkdown() {
        String sidecarResponse = "{\"documentId\":\"test.pdf\",\"markdown\":\"\",\"sections\":[]}";
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(sidecarResponse);

        byte[] content = "dummy".getBytes();
        ExtractedTenderDocument legacyResult = new ExtractedTenderDocument("empty.pdf", "application/pdf", "Legacy", 6, "poi-pdfbox-v1", null);
        when(fallbackExtractor.extract("empty.pdf", "application/pdf", content)).thenReturn(legacyResult);

        ExtractedTenderDocument result = extractor.extract("empty.pdf", "application/pdf", content);

        assertThat(result.extractorKey()).isEqualTo("poi-pdfbox-v1");
        verify(fallbackExtractor).extract("empty.pdf", "application/pdf", content);
    }

    @Test
    void shouldFallbackWhenSidecarReturnsMalformedJson() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{ not-valid-json");

        byte[] content = "dummy".getBytes();
        ExtractedTenderDocument legacyResult = new ExtractedTenderDocument("broken.pdf", "application/pdf", "Legacy", 6, "poi-pdfbox-v1", null);
        when(fallbackExtractor.extract("broken.pdf", "application/pdf", content)).thenReturn(legacyResult);

        ExtractedTenderDocument result = extractor.extract("broken.pdf", "application/pdf", content);

        assertThat(result.extractorKey()).isEqualTo("poi-pdfbox-v1");
        verify(fallbackExtractor).extract("broken.pdf", "application/pdf", content);
    }

    @Test
    void shouldFallbackWhenSidecarReturnsNullBody() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);

        byte[] content = "dummy".getBytes();
        ExtractedTenderDocument legacyResult = new ExtractedTenderDocument("null.pdf", "application/pdf", "Legacy", 6, "poi-pdfbox-v1", null);
        when(fallbackExtractor.extract("null.pdf", "application/pdf", content)).thenReturn(legacyResult);

        ExtractedTenderDocument result = extractor.extract("null.pdf", "application/pdf", content);

        assertThat(result.extractorKey()).isEqualTo("poi-pdfbox-v1");
        verify(fallbackExtractor).extract("null.pdf", "application/pdf", content);
    }

    @Test
    void shouldFallbackOnReadTimeoutFromDedicatedRestTemplate() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("Read timed out"));

        byte[] content = "dummy".getBytes();
        ExtractedTenderDocument legacyResult = new ExtractedTenderDocument("slow.pdf", "application/pdf", "Legacy", 6, "poi-pdfbox-v1", null);
        when(fallbackExtractor.extract("slow.pdf", "application/pdf", content)).thenReturn(legacyResult);

        ExtractedTenderDocument result = extractor.extract("slow.pdf", "application/pdf", content);

        assertThat(result.extractorKey()).isEqualTo("poi-pdfbox-v1");
        verify(fallbackExtractor).extract("slow.pdf", "application/pdf", content);
    }
}
