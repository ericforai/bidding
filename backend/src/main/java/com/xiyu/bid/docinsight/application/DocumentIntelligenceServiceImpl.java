package com.xiyu.bid.docinsight.application;

import com.xiyu.bid.docinsight.domain.DocumentChunk;
import com.xiyu.bid.docinsight.domain.StructuralDocumentChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentIntelligenceServiceImpl implements DocumentIntelligenceService {

    private final DocumentStorage storage;
    private final DocumentTextExtractor extractor;
    private final StructuralDocumentChunker chunker;
    private final List<DocumentAnalyzer> analyzers;

    @Override
    public DocumentAnalysisResult process(String profileCode, String entityId, MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            StoredDocument stored = storage.store(profileCode, entityId, file.getOriginalFilename(), file.getContentType(), content);
            return parse(profileCode, stored, file.getOriginalFilename(), file.getContentType(), content);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }

    @Override
    public DocumentAnalysisResult processExisting(String profileCode, String entityId, String storagePath, String fileName, String contentType) {
        byte[] content = storage.load(storagePath)
                .orElseThrow(() -> new IllegalArgumentException("File not found at: " + storagePath));
        
        StoredDocument stored = new StoredDocument("doc-insight://existing", storagePath, "unknown-hash");
        return parse(profileCode, stored, fileName, contentType, content);
    }

    private DocumentAnalysisResult parse(String profileCode, StoredDocument stored, String fileName, String contentType, byte[] content) {
        ExtractedDocument extracted = extractor.extract(fileName, contentType, content);
        List<DocumentChunk> chunks = chunker.chunk(extracted.text(), extracted.structuredMetadata());

        DocumentAnalyzer analyzer = analyzers.stream()
                .filter(a -> a.supports(profileCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No analyzer supported for profile: " + profileCode));

        DocumentAnalysisInput input = new DocumentAnalysisInput(
                stored.fileUrl(),
                fileName,
                extracted.text(),
                extracted.structuredMetadata(),
                chunks,
                profileCode,
                Map.of()
        );

        return analyzer.analyze(input);
    }
}
