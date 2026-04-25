package com.xiyu.bid.biddraftagent.infrastructure.tenderdocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.biddraftagent.application.ExtractedTenderDocument;
import com.xiyu.bid.biddraftagent.application.TenderDocumentTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@Primary
@Slf4j
public class MarkItDownSidecarTextExtractor implements TenderDocumentTextExtractor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String sidecarUrl;
    private final PoiPdfTenderDocumentTextExtractor fallbackExtractor;

    public MarkItDownSidecarTextExtractor(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.converter.sidecar-url:http://localhost:8000}") String sidecarUrl,
            PoiPdfTenderDocumentTextExtractor fallbackExtractor) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.sidecarUrl = sidecarUrl;
        this.fallbackExtractor = fallbackExtractor;
    }

    @Override
    public ExtractedTenderDocument extract(String fileName, String contentType, byte[] content) {
        try {
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
            String responseStr = restTemplate.postForObject(sidecarUrl + "/convert", requestEntity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            String markdown = root.path("markdown").asText("");
            
            if (markdown.isBlank()) {
                throw new IllegalStateException("Sidecar returned empty markdown");
            }

            return new ExtractedTenderDocument(
                    fileName,
                    contentType,
                    markdown,
                    markdown.length(),
                    "markitdown-sidecar",
                    responseStr
            );
        } catch (Exception e) {
            log.error("Failed to extract text using Sidecar, falling back to legacy extractor: {}", e.getMessage());
            return fallbackExtractor.extract(fileName, contentType, content);
        }
    }
}
