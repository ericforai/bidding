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
            Long projectId,
            Long structureId,
            boolean structureCreated,
            Map<String, DocumentSection> sectionsByStableKey,
            Map<String, DocumentSection> sectionsByTitle,
            Map<Long, DocumentSectionLock> locksBySectionId
    ) {
        this.projectId = projectId;
        this.structureId = structureId;
        this.structureCreated = structureCreated;
        this.sectionsByStableKey = sectionsByStableKey;
        this.sectionsByTitle = sectionsByTitle;
        this.locksBySectionId = locksBySectionId;
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
