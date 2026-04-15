// Input: documenteditor repositories, DTOs, and support services
// Output: Document Editor business service operations
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.documenteditor.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.documenteditor.dto.DocumentReminderDTO;
import com.xiyu.bid.documenteditor.dto.DocumentSectionDTO;
import com.xiyu.bid.documenteditor.dto.DocumentStructureDTO;
import com.xiyu.bid.documenteditor.dto.SectionAssignmentRequest;
import com.xiyu.bid.documenteditor.dto.SectionCreateRequest;
import com.xiyu.bid.documenteditor.dto.SectionLockRequest;
import com.xiyu.bid.documenteditor.dto.SectionReminderRequest;
import com.xiyu.bid.documenteditor.dto.SectionReorderRequest;
import com.xiyu.bid.documenteditor.dto.SectionUpdateRequest;
import com.xiyu.bid.documenteditor.dto.StructureCreateRequest;
import com.xiyu.bid.documenteditor.entity.DocumentReminder;
import com.xiyu.bid.documenteditor.entity.DocumentSection;
import com.xiyu.bid.documenteditor.entity.DocumentSectionAssignment;
import com.xiyu.bid.documenteditor.entity.DocumentSectionLock;
import com.xiyu.bid.documenteditor.entity.DocumentStructure;
import com.xiyu.bid.documenteditor.entity.SectionType;
import com.xiyu.bid.documenteditor.repository.DocumentReminderRepository;
import com.xiyu.bid.documenteditor.repository.DocumentSectionAssignmentRepository;
import com.xiyu.bid.documenteditor.repository.DocumentSectionLockRepository;
import com.xiyu.bid.documenteditor.repository.DocumentSectionRepository;
import com.xiyu.bid.documenteditor.repository.DocumentStructureRepository;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.audit.service.IAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文档编辑器服务
 * 提供文档结构和章节的管理功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEditorService {

    private final DocumentStructureRepository structureRepository;
    private final DocumentSectionRepository sectionRepository;
    private final DocumentSectionAssignmentRepository assignmentRepository;
    private final DocumentSectionLockRepository lockRepository;
    private final DocumentReminderRepository reminderRepository;
    private final IAuditLogService auditLogService;

    /**
     * 创建文档结构
     *
     * @param request 创建请求
     * @return 创建的文档结构
     */
    @Auditable(action = "CREATE", entityType = "DocumentStructure", description = "Create document structure")
    @Transactional
    public DocumentStructureDTO createStructure(StructureCreateRequest request) {
        // 验证输入
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

        DocumentStructure saved = structureRepository.save(structure);
        return toDTO(saved);
    }

    /**
     * 获取文档结构
     *
     * @param projectId 项目ID
     * @return 文档结构
     */
    public DocumentStructureDTO getStructure(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        DocumentStructure structure = structureRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Document structure not found for project: " + projectId));

        return toDTO(structure);
    }

    /**
     * 添加文档章节
     *
     * @param request 创建请求
     * @return 创建的章节
     */
    @Auditable(action = "CREATE", entityType = "DocumentSection", description = "Add document section")
    @Transactional
    public DocumentSectionDTO addSection(SectionCreateRequest request) {
        // 验证输入
        if (request.getStructureId() == null) {
            throw new IllegalArgumentException("Structure ID is required");
        }
        if (request.getSectionType() == null) {
            throw new IllegalArgumentException("Section type is required");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        // 验证文档结构存在
        if (!structureRepository.existsById(request.getStructureId())) {
            throw new ResourceNotFoundException("Document structure not found with id: " + request.getStructureId());
        }

        // 验证父章节存在（如果提供了父章节ID）
        if (request.getParentId() != null) {
            DocumentSection parentSection = sectionRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent section not found with id: " + request.getParentId()));

            // 验证父章节属于同一个文档结构
            if (!parentSection.getStructureId().equals(request.getStructureId())) {
                throw new IllegalArgumentException("Parent section does not belong to the specified structure");
            }
        }

        DocumentSection section = DocumentSection.builder()
                .structureId(request.getStructureId())
                .parentId(request.getParentId())
                .sectionType(request.getSectionType())
                .title(request.getTitle().trim())
                .content(request.getContent())
                .orderIndex(request.getOrderIndex())
                .metadata(request.getMetadata())
                .build();

        DocumentSection saved = sectionRepository.save(section);
        return toDTO(saved, null, null);
    }

    /**
     * 更新文档章节
     *
     * @param sectionId 章节ID
     * @param request 更新请求
     * @return 更新后的章节
     */
    @Auditable(action = "UPDATE", entityType = "DocumentSection", description = "Update document section")
    @Transactional
    public DocumentSectionDTO updateSection(Long sectionId, SectionUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Update request is required");
        }

        DocumentSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));

        // 更新字段
        if (request.getTitle() != null) {
            if (request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Title cannot be empty");
            }
            section.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            section.setContent(request.getContent());
        }
        if (request.getMetadata() != null) {
            section.setMetadata(request.getMetadata());
        }
        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }

        DocumentSection saved = sectionRepository.save(section);
        return buildSectionDTO(saved);
    }

    @Transactional
    public DocumentSectionDTO assignSection(Long projectId, SectionAssignmentRequest request) {
        DocumentSection section = getProjectSection(projectId, request.getSectionId());
        DocumentSectionAssignment assignment = assignmentRepository.findBySectionId(section.getId())
                .orElseGet(() -> DocumentSectionAssignment.builder()
                        .projectId(projectId)
                        .sectionId(section.getId())
                        .build());

        assignment.setOwner(request.getOwner().trim());
        assignment.setAssignedBy(request.getAssignedBy());
        assignment.setDueDate(request.getDueDate());

        DocumentSectionAssignment savedAssignment = assignmentRepository.save(assignment);
        DocumentSectionLock lock = lockRepository.findBySectionId(section.getId()).orElse(null);
        return toDTO(section, savedAssignment, lock);
    }

    @Transactional
    public DocumentSectionDTO updateLock(Long projectId, SectionLockRequest request) {
        DocumentSection section = getProjectSection(projectId, request.getSectionId());
        DocumentSectionLock lock = lockRepository.findBySectionId(section.getId())
                .orElseGet(() -> DocumentSectionLock.builder()
                        .projectId(projectId)
                        .sectionId(section.getId())
                        .build());

        lock.setLocked(Boolean.TRUE.equals(request.getLocked()));
        lock.setLockedBy(request.getUserId());
        lock.setLockedAt(Boolean.TRUE.equals(request.getLocked()) ? LocalDateTime.now() : null);

        DocumentSectionLock savedLock = lockRepository.save(lock);
        DocumentSectionAssignment assignment = assignmentRepository.findBySectionId(section.getId()).orElse(null);
        return toDTO(section, assignment, savedLock);
    }

    @Transactional
    public DocumentReminderDTO createReminder(Long projectId, SectionReminderRequest request) {
        DocumentSection section = getProjectSection(projectId, request.getSectionId());
        DocumentReminder reminder = DocumentReminder.builder()
                .projectId(projectId)
                .sectionId(section.getId())
                .recipient(request.getRecipient().trim())
                .message(request.getMessage())
                .remindedBy(request.getRemindedBy())
                .remindedAt(LocalDateTime.now())
                .build();

        DocumentReminder savedReminder = reminderRepository.save(reminder);
        return DocumentReminderDTO.builder()
                .id(savedReminder.getId())
                .projectId(savedReminder.getProjectId())
                .sectionId(savedReminder.getSectionId())
                .recipient(savedReminder.getRecipient())
                .message(savedReminder.getMessage())
                .remindedBy(savedReminder.getRemindedBy())
                .remindedAt(savedReminder.getRemindedAt())
                .build();
    }

    /**
     * 删除文档章节
     *
     * @param sectionId 章节ID
     */
    @Auditable(action = "DELETE", entityType = "DocumentSection", description = "Delete document section")
    @Transactional
    public void deleteSection(Long sectionId) {
        if (sectionId == null) {
            throw new IllegalArgumentException("Section ID is required");
        }

        if (!sectionRepository.existsById(sectionId)) {
            throw new ResourceNotFoundException("Section not found with id: " + sectionId);
        }

        // 检查是否有子章节
        List<DocumentSection> children = sectionRepository.findByParentId(sectionId);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete section with child sections. Please delete child sections first.");
        }

        sectionRepository.deleteById(sectionId);
    }

    /**
     * 重新排序章节
     *
     * @param request 重新排序请求
     */
    @Transactional
    public void reorderSections(SectionReorderRequest request) {
        if (request.getStructureId() == null) {
            throw new IllegalArgumentException("Structure ID is required");
        }
        if (request.getSectionOrders() == null || request.getSectionOrders().isEmpty()) {
            throw new IllegalArgumentException("Section orders map is required");
        }

        // 验证文档结构存在
        if (!structureRepository.existsById(request.getStructureId())) {
            throw new ResourceNotFoundException("Document structure not found with id: " + request.getStructureId());
        }

        // 更新每个章节的顺序
        List<DocumentSection> sectionsToUpdate = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : request.getSectionOrders().entrySet()) {
            Long sectionId = entry.getKey();
            Integer orderIndex = entry.getValue();

            DocumentSection section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));

            // 验证章节属于指定的文档结构
            if (!section.getStructureId().equals(request.getStructureId())) {
                throw new IllegalArgumentException("Section with id " + sectionId + " does not belong to the specified structure");
            }

            section.setOrderIndex(orderIndex);
            sectionsToUpdate.add(section);
        }

        sectionRepository.saveAll(sectionsToUpdate);
    }

    /**
     * 获取章节树形结构
     *
     * @param projectId 项目ID
     * @return 章节树
     */
    public List<DocumentSectionDTO> getSectionTree(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        DocumentStructure structure = structureRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Document structure not found for project: " + projectId));

        List<DocumentSection> allSections = sectionRepository.findByStructureId(structure.getId());
        Map<Long, DocumentSectionAssignment> assignments = assignmentRepository.findBySectionIdIn(
                allSections.stream().map(DocumentSection::getId).toList()
        ).stream().collect(Collectors.toMap(DocumentSectionAssignment::getSectionId, item -> item));
        Map<Long, DocumentSectionLock> locks = lockRepository.findBySectionIdIn(
                allSections.stream().map(DocumentSection::getId).toList()
        ).stream().collect(Collectors.toMap(DocumentSectionLock::getSectionId, item -> item));

        // 构建章节树
        return buildTree(allSections, null, assignments, locks);
    }

    /**
     * 递归构建章节树
     *
     * @param allSections 所有章节
     * @param parentId 父章节ID
     * @return 章节树
     */
    private List<DocumentSectionDTO> buildTree(List<DocumentSection> allSections, Long parentId,
                                              Map<Long, DocumentSectionAssignment> assignments,
                                              Map<Long, DocumentSectionLock> locks) {
        if (allSections == null) {
            return List.of();
        }

        return allSections.stream()
                .filter(section -> section != null && Objects.equals(section.getParentId(), parentId))
                .map(section -> {
                    DocumentSectionDTO dto = toDTO(section, assignments.get(section.getId()), locks.get(section.getId()));
                    // Use Optional to safely handle potential null ID
                    Long sectionId = section.getId();
                    if (sectionId != null) {
                        List<DocumentSectionDTO> children = buildTree(allSections, sectionId, assignments, locks);
                        if (!children.isEmpty()) {
                            dto.setChildren(children);
                        }
                    }
                    return dto;
                })
                .sorted(Comparator.comparing(DocumentSectionDTO::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    /**
     * 将文档结构实体转换为DTO
     *
     * @param entity 实体
     * @return DTO
     */
    private DocumentStructureDTO toDTO(DocumentStructure entity) {
        return DocumentStructureDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .name(entity.getName())
                .rootSectionId(entity.getRootSectionId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将文档章节实体转换为DTO
     *
     * @param entity 实体
     * @return DTO
     */
    private DocumentSectionDTO toDTO(DocumentSection entity, DocumentSectionAssignment assignment, DocumentSectionLock lock) {
        return DocumentSectionDTO.builder()
                .id(entity.getId())
                .structureId(entity.getStructureId())
                .parentId(entity.getParentId())
                .sectionType(entity.getSectionType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .orderIndex(entity.getOrderIndex())
                .metadata(entity.getMetadata())
                .owner(assignment != null ? assignment.getOwner() : null)
                .dueDate(assignment != null ? assignment.getDueDate() : null)
                .locked(lock != null ? Boolean.TRUE.equals(lock.getLocked()) : Boolean.FALSE)
                .assignedBy(assignment != null ? assignment.getAssignedBy() : null)
                .lockedBy(lock != null ? lock.getLockedBy() : null)
                .lockedAt(lock != null ? lock.getLockedAt() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .children(List.of())
                .build();
    }

    private DocumentSectionDTO buildSectionDTO(DocumentSection section) {
        DocumentSectionAssignment assignment = assignmentRepository.findBySectionId(section.getId()).orElse(null);
        DocumentSectionLock lock = lockRepository.findBySectionId(section.getId()).orElse(null);
        return toDTO(section, assignment, lock);
    }

    private DocumentSection getProjectSection(Long projectId, Long sectionId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        if (sectionId == null) {
            throw new IllegalArgumentException("Section ID is required");
        }

        DocumentStructure structure = structureRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Document structure not found for project: " + projectId));
        DocumentSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));
        if (!Objects.equals(section.getStructureId(), structure.getId())) {
            throw new IllegalArgumentException("Section does not belong to the specified project");
        }
        return section;
    }
}
