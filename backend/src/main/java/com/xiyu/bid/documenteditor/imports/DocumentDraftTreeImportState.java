package com.xiyu.bid.documenteditor.imports;

import com.xiyu.bid.documenteditor.dto.DraftTreeSkippedSectionDTO;
import com.xiyu.bid.documenteditor.dto.DraftTreeUpsertResultDTO;
import com.xiyu.bid.documenteditor.entity.DocumentSection;
import com.xiyu.bid.documenteditor.entity.DocumentSectionLock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DocumentDraftTreeImportState {

    public final Long projectId;
    public final Long structureId;
    public final boolean structureCreated;
    public final Map<String, DocumentSection> sectionsByStableKey;
    public final Map<String, DocumentSection> sectionsByTitle;
    public final Map<Long, DocumentSectionLock> locksBySectionId;
    public final ImportStats stats = new ImportStats();

    public DocumentDraftTreeImportState(
            Long pProjectId,
            Long pStructureId,
            boolean pStructureCreated,
            Map<String, DocumentSection> pSectionsByStableKey,
            Map<String, DocumentSection> pSectionsByTitle,
            Map<Long, DocumentSectionLock> pLocksBySectionId
    ) {
        this.projectId = pProjectId;
        this.structureId = pStructureId;
        this.structureCreated = pStructureCreated;
        this.sectionsByStableKey = pSectionsByStableKey;
        this.sectionsByTitle = pSectionsByTitle;
        this.locksBySectionId = pLocksBySectionId;
    }

    public DraftTreeUpsertResultDTO toResult() {
        return DraftTreeUpsertResultDTO.builder()
                .projectId(projectId)
                .structureId(structureId)
                .structureCreated(structureCreated)
                .totalSections(stats.total)
                .createdSections(stats.created)
                .updatedSections(stats.updated)
                .skippedSectionsCount(stats.skipped)
                .skippedSections(stats.skippedSections)
                .build();
    }

    public static final class ImportStats {
        public int total;
        public int created;
        public int updated;
        public int skipped;
        public final List<DraftTreeSkippedSectionDTO> skippedSections = new ArrayList<>();
    }
}
