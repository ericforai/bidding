package com.xiyu.bid.casework.infrastructure.client;

import com.xiyu.bid.casework.domain.port.CaseSnapshotPort;
import com.xiyu.bid.documentexport.dto.DocumentCaseSnapshotDTO;
import com.xiyu.bid.documentexport.service.DocumentExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentExportCaseSnapshotClient implements CaseSnapshotPort {

    private final DocumentExportService documentExportService;

    @Override
    public DocumentCaseSnapshotDTO getCaseSnapshot(Long projectId) {
        return documentExportService.getCaseSnapshot(projectId);
    }
}
