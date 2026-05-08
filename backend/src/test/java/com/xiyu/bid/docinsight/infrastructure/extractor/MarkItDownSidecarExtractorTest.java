package com.xiyu.bid.docinsight.infrastructure.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.docinsight.application.ExtractedDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkItDownSidecarExtractorTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void shouldSendSidecarKeyWhenConfigured() {
        MarkItDownSidecarExtractor extractor = new MarkItDownSidecarExtractor(
                restTemplate,
                new ObjectMapper(),
                "http://localhost:8000",
                "test-sidecar-key"
        );
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

        ExtractedDocument result = extractor.extract("test.pdf", "application/pdf", "dummy".getBytes());

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), requestCaptor.capture(), eq(String.class));
        assertThat(result.extractorKey()).isEqualTo("markitdown-sidecar");
        assertThat(requestCaptor.getValue().getHeaders().getFirst("X-Sidecar-Key"))
                .isEqualTo("test-sidecar-key");
    }
}
