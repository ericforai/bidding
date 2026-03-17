package com.xiyu.bid.documentexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.documenteditor.entity.DocumentSection;
import com.xiyu.bid.documenteditor.entity.DocumentStructure;
import com.xiyu.bid.documenteditor.repository.DocumentSectionRepository;
import com.xiyu.bid.documenteditor.repository.DocumentStructureRepository;
import com.xiyu.bid.documentexport.dto.DocumentArchiveRecordCreateRequest;
import com.xiyu.bid.documentexport.dto.DocumentArchiveRecordDTO;
import com.xiyu.bid.documentexport.dto.DocumentExportCreateRequest;
import com.xiyu.bid.documentexport.dto.DocumentExportDTO;
import com.xiyu.bid.documentexport.entity.DocumentArchiveRecord;
import com.xiyu.bid.documentexport.entity.DocumentExport;
import com.xiyu.bid.documentexport.entity.DocumentExportFile;
import com.xiyu.bid.documentexport.repository.DocumentArchiveRecordRepository;
import com.xiyu.bid.documentexport.repository.DocumentExportFileRepository;
import com.xiyu.bid.documentexport.repository.DocumentExportRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentExportService {

    private final ProjectRepository projectRepository;
    private final DocumentStructureRepository structureRepository;
    private final DocumentSectionRepository sectionRepository;
    private final DocumentExportRepository exportRepository;
    private final DocumentExportFileRepository exportFileRepository;
    private final DocumentArchiveRecordRepository archiveRecordRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<DocumentExportDTO> getExports(Long projectId) {
        return exportRepository.findByProjectIdOrderByExportedAtDesc(projectId).stream()
                .map(this::toExportDTO)
                .toList();
    }

    @Transactional
    public DocumentExportDTO createExport(Long projectId, DocumentExportCreateRequest request) {
        Project project = getProject(projectId);
        DocumentStructure structure = getStructure(projectId);
        List<DocumentSection> sections = sectionRepository.findByStructureId(structure.getId());

        String normalizedFormat = normalizeFormat(request.getFormat());
        String content = buildExportContent(project, structure, sections);
        String fileName = buildFileName(project.getName(), normalizedFormat);
        String contentType = resolveContentType(normalizedFormat);

        DocumentExport savedExport = exportRepository.save(DocumentExport.builder()
                .projectId(projectId)
                .structureId(structure.getId())
                .projectName(project.getName())
                .format(normalizedFormat)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize((long) content.length())
                .exportedBy(request.getExportedBy())
                .exportedByName(request.getExportedByName().trim())
                .build());

        exportFileRepository.save(DocumentExportFile.builder()
                .exportId(savedExport.getId())
                .content(content)
                .build());

        return toExportDTO(savedExport);
    }

    @Transactional(readOnly = true)
    public List<DocumentArchiveRecordDTO> getArchiveRecords(Long projectId) {
        return archiveRecordRepository.findByProjectIdOrderByArchivedAtDesc(projectId).stream()
                .map(this::toArchiveDTO)
                .toList();
    }

    @Transactional
    public DocumentArchiveRecordDTO createArchiveRecord(Long projectId, DocumentArchiveRecordCreateRequest request) {
        Project project = getProject(projectId);
        DocumentStructure structure = getStructure(projectId);

        DocumentExportDTO latestExport = createExport(projectId, buildAutoExportRequest(request));

        DocumentArchiveRecord record = archiveRecordRepository.save(DocumentArchiveRecord.builder()
                .projectId(projectId)
                .structureId(structure.getId())
                .archivedBy(request.getArchivedBy())
                .archivedByName(request.getArchivedByName().trim())
                .archiveReason(request.getArchiveReason().trim())
                .exportId(latestExport.getId())
                .build());

        project.setStatus(Project.Status.ARCHIVED);
        projectRepository.save(project);

        return toArchiveDTO(record);
    }

    private DocumentExportCreateRequest buildAutoExportRequest(DocumentArchiveRecordCreateRequest request) {
        DocumentExportCreateRequest exportRequest = new DocumentExportCreateRequest();
        exportRequest.setFormat("json");
        exportRequest.setExportedBy(request.getArchivedBy());
        exportRequest.setExportedByName(request.getArchivedByName());
        return exportRequest;
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private DocumentStructure getStructure(Long projectId) {
        return structureRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Document structure not found for project: " + projectId));
    }

    private String buildExportContent(Project project, DocumentStructure structure, List<DocumentSection> sections) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", project.getId());
        payload.put("projectName", project.getName());
        payload.put("projectStatus", project.getStatus());
        payload.put("structureId", structure.getId());
        payload.put("structureName", structure.getName());
        payload.put("sections", sections.stream()
                .sorted(Comparator.comparing(DocumentSection::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DocumentSection::getId))
                .map(section -> {
                    Map<String, Object> sectionPayload = new LinkedHashMap<>();
                    sectionPayload.put("id", section.getId());
                    sectionPayload.put("parentId", section.getParentId());
                    sectionPayload.put("sectionType", String.valueOf(section.getSectionType()));
                    sectionPayload.put("title", section.getTitle());
                    sectionPayload.put("content", Optional.ofNullable(section.getContent()).orElse(""));
                    sectionPayload.put("orderIndex", section.getOrderIndex());
                    sectionPayload.put("metadata", Optional.ofNullable(section.getMetadata()).orElse(""));
                    return sectionPayload;
                })
                .toList());
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to generate document export content", exception);
        }
    }

    private String normalizeFormat(String format) {
        String normalized = Optional.ofNullable(format).orElse("json").trim().toLowerCase(Locale.ROOT);
        if (!Set.of("json", "txt").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
        return normalized;
    }

    private String buildFileName(String projectName, String format) {
        String safeProjectName = Optional.ofNullable(projectName).orElse("document").replaceAll("\\s+", "_");
        return safeProjectName + "_document_export." + format;
    }

    private String resolveContentType(String format) {
        return "txt".equals(format) ? "text/plain;charset=utf-8" : "application/json;charset=utf-8";
    }

    private DocumentExportDTO toExportDTO(DocumentExport export) {
        String content = exportFileRepository.findByExportId(export.getId())
                .map(DocumentExportFile::getContent)
                .orElse("");
        return DocumentExportDTO.builder()
                .id(export.getId())
                .projectId(export.getProjectId())
                .structureId(export.getStructureId())
                .projectName(export.getProjectName())
                .format(export.getFormat())
                .fileName(export.getFileName())
                .contentType(export.getContentType())
                .fileSize(export.getFileSize())
                .exportedBy(export.getExportedBy())
                .exportedByName(export.getExportedByName())
                .exportedAt(export.getExportedAt())
                .content(content)
                .build();
    }

    private DocumentArchiveRecordDTO toArchiveDTO(DocumentArchiveRecord record) {
        DocumentExport export = record.getExportId() == null
                ? null
                : exportRepository.findById(record.getExportId()).orElse(null);
        Project project = projectRepository.findById(record.getProjectId()).orElse(null);

        return DocumentArchiveRecordDTO.builder()
                .id(record.getId())
                .projectId(record.getProjectId())
                .structureId(record.getStructureId())
                .archivedBy(record.getArchivedBy())
                .archivedByName(record.getArchivedByName())
                .archiveReason(record.getArchiveReason())
                .exportId(record.getExportId())
                .exportFileName(export != null ? export.getFileName() : null)
                .projectName(project != null ? project.getName() : null)
                .archivedAt(record.getArchivedAt())
                .build();
    }
}
