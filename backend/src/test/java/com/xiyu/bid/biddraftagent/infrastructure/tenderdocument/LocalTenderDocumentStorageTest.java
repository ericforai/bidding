package com.xiyu.bid.biddraftagent.infrastructure.tenderdocument;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalTenderDocumentStorageTest {

    @TempDir
    private Path tempDir;

    @Test
    void loadByFileUrl_shouldReadDocumentPreviouslyStoredByFileUrl() {
        LocalTenderDocumentStorage storage = new LocalTenderDocumentStorage(tempDir.toString());
        byte[] content = "招标正文".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        var stored = storage.store(11L, "招标文件.docx", "application/octet-stream", content);
        var loaded = storage.loadByFileUrl(stored.fileUrl());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().storedDocument().fileUrl()).isEqualTo(stored.fileUrl());
        assertThat(loaded.get().storedDocument().contentSha256()).isEqualTo(stored.contentSha256());
        assertThat(loaded.get().content()).isEqualTo(content);
    }
}
