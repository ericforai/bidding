package com.xiyu.bid.docinsight.infrastructure.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.docinsight.application.DocumentTextExtractor;
import com.xiyu.bid.docinsight.application.ExtractedDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class MarkItDownSidecarExtractor implements DocumentTextExtractor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String sidecarUrl;

    public MarkItDownSidecarExtractor(
            @Qualifier("markItDownSidecarRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.doc-insight.sidecar-url:http://localhost:8000}") String sidecarUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.sidecarUrl = sidecarUrl;
    }

    @Override
    public ExtractedDocument extract(String fileName, String contentType, byte[] content) {
        log.info("Sending document {} to MarkItDown sidecar...", fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        try {
            String responseStr = restTemplate.postForObject(sidecarUrl + "/convert", requestEntity, String.class);
            JsonNode root = objectMapper.readTree(responseStr);
            String markdown = root.path("markdown").asText("");
            
            if (markdown.isBlank()) {
                throw new IllegalStateException("Sidecar returned empty markdown");
            }

            return new ExtractedDocument(
                    markdown,
                    markdown.length(),
                    responseStr,
                    "markitdown-sidecar",
                    Map.of()
            );
        } catch (org.springframework.web.client.RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to extract text using Sidecar: {}", e.getMessage());
            throw new IllegalStateException("Failed to parse sidecar response or network error", e);
        }
    }
}
