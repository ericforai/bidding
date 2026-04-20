package com.xiyu.bid.documenteditor.service;

import com.xiyu.bid.documenteditor.dto.DocumentStructureDTO;
import com.xiyu.bid.documenteditor.dto.StructureCreateRequest;
import com.xiyu.bid.documenteditor.entity.DocumentStructure;
import com.xiyu.bid.documenteditor.repository.DocumentStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DocumentStructureService {

    private final DocumentStructureRepository structureRepository;

    DocumentStructureDTO createStructure(StructureCreateRequest request) {
        if (request.getProjectId() == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }

        DocumentStructure structure = DocumentStructure.builder()
                .projectId(request.getProjectId())
                .name(request.getName().trim())
                .build();
        return DocumentEditorMapper.toStructureDTO(structureRepository.save(structure));
    }

    DocumentStructureDTO getStructure(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        return DocumentEditorMapper.toStructureDTO(
                structureRepository.findByProjectId(projectId)
                        .orElseThrow(() -> new com.xiyu.bid.exception.ResourceNotFoundException(
                                "Document structure not found for project: " + projectId))
        );
    }
}
