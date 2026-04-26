package com.xiyu.bid.docinsight.application;

import java.util.Optional;

public interface DocumentStorage {
    StoredDocument store(String category, String entityId, String fileName, String contentType, byte[] content);
    Optional<byte[]> load(String storagePath);
}
