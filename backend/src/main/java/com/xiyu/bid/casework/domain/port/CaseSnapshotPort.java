package com.xiyu.bid.casework.domain.port;

import com.xiyu.bid.documentexport.dto.DocumentCaseSnapshotDTO;

public interface CaseSnapshotPort {

    DocumentCaseSnapshotDTO getCaseSnapshot(Long projectId);
}
