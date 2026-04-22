package com.xiyu.bid.biddraftagent.infrastructure.tenderdocument;

import com.xiyu.bid.biddraftagent.application.StoredTenderDocument;
import com.xiyu.bid.biddraftagent.application.TenderDocumentStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class LocalTenderDocumentStorage implements TenderDocumentStorage {

    private final Path uploadRoot;

    public LocalTenderDocumentStorage(@Value("${app.bid-agent.upload-dir:}") String configuredUploadDir) {
        this.uploadRoot = configuredUploadDir == null || configuredUploadDir.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "xiyu-bid-agent-uploads")
                : Path.of(configuredUploadDir);
    }

    @Override
    public StoredTenderDocument store(Long projectId, String fileName, String contentType, byte[] content) {
        String hash = sha256(content);
        String safeName = safeFileName(fileName);
        Path projectDir = uploadRoot.resolve(String.valueOf(projectId));
        Path targetPath = projectDir.resolve(hash.substring(0, 12) + "-" + safeName);
        try {
            Files.createDirectories(projectDir);
            Files.write(targetPath, content);
        } catch (IOException ex) {
            throw new IllegalStateException("保存招标文件失败", ex);
        }
        return new StoredTenderDocument(
                "bid-agent://tender-documents/" + projectId + "/" + targetPath.getFileName(),
                targetPath.toAbsolutePath().toString(),
                hash
        );
    }

    private String safeFileName(String fileName) {
        String candidate = fileName == null || fileName.isBlank() ? "tender-document" : fileName.trim();
        return candidate.replaceAll("[\\\\/:*?\"<>|]+", "_");
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
