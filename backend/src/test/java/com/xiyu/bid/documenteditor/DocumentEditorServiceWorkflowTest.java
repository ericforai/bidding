package com.xiyu.bid.documenteditor;

import com.xiyu.bid.documenteditor.dto.DocumentReminderDTO;
import com.xiyu.bid.documenteditor.dto.DocumentSectionDTO;
import com.xiyu.bid.documenteditor.dto.SectionAssignmentRequest;
import com.xiyu.bid.documenteditor.dto.SectionLockRequest;
import com.xiyu.bid.documenteditor.dto.SectionReminderRequest;
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
import com.xiyu.bid.documenteditor.service.DocumentEditorService;
import com.xiyu.bid.service.IAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentEditorServiceWorkflowTest {

    @Mock
    private DocumentStructureRepository structureRepository;
    @Mock
    private DocumentSectionRepository sectionRepository;
    @Mock
    private DocumentSectionAssignmentRepository assignmentRepository;
    @Mock
    private DocumentSectionLockRepository lockRepository;
    @Mock
    private DocumentReminderRepository reminderRepository;
    @Mock
    private IAuditLogService auditLogService;

    @InjectMocks
    private DocumentEditorService documentEditorService;

    private DocumentStructure structure;
    private DocumentSection section;

    @BeforeEach
    void setUp() {
        structure = DocumentStructure.builder()
                .id(10L)
                .projectId(100L)
                .name("Test Structure")
                .build();

        section = DocumentSection.builder()
                .id(20L)
                .structureId(10L)
                .sectionType(SectionType.SECTION)
                .title("技术方案")
                .content("Initial content")
                .orderIndex(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void assignSection_shouldPersistAssignmentAndReturnEnrichedSection() {
        SectionAssignmentRequest request = SectionAssignmentRequest.builder()
                .sectionId(20L)
                .owner("张经理")
                .assignedBy(1L)
                .dueDate(LocalDate.of(2026, 3, 31))
                .build();

        when(structureRepository.findByProjectId(100L)).thenReturn(Optional.of(structure));
        when(sectionRepository.findById(20L)).thenReturn(Optional.of(section));
        when(assignmentRepository.findBySectionId(20L)).thenReturn(Optional.empty());
        when(lockRepository.findBySectionId(20L)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(DocumentSectionAssignment.class))).thenAnswer(invocation -> {
            DocumentSectionAssignment assignment = invocation.getArgument(0);
            assignment.setId(30L);
            return assignment;
        });

        DocumentSectionDTO result = documentEditorService.assignSection(100L, request);

        assertEquals("张经理", result.getOwner());
        assertEquals(LocalDate.of(2026, 3, 31), result.getDueDate());
        assertEquals(1L, result.getAssignedBy());
        assertFalse(Boolean.TRUE.equals(result.getLocked()));
        verify(assignmentRepository).save(any(DocumentSectionAssignment.class));
    }

    @Test
    void updateLock_shouldPersistLockAndReturnEnrichedSection() {
        SectionLockRequest request = SectionLockRequest.builder()
                .sectionId(20L)
                .locked(true)
                .userId(2L)
                .build();

        when(structureRepository.findByProjectId(100L)).thenReturn(Optional.of(structure));
        when(sectionRepository.findById(20L)).thenReturn(Optional.of(section));
        when(lockRepository.findBySectionId(20L)).thenReturn(Optional.empty());
        when(assignmentRepository.findBySectionId(20L)).thenReturn(Optional.empty());
        when(lockRepository.save(any(DocumentSectionLock.class))).thenAnswer(invocation -> {
            DocumentSectionLock lock = invocation.getArgument(0);
            lock.setId(31L);
            return lock;
        });

        DocumentSectionDTO result = documentEditorService.updateLock(100L, request);

        assertTrue(result.getLocked());
        assertEquals(2L, result.getLockedBy());
        assertNotNull(result.getLockedAt());
        verify(lockRepository).save(any(DocumentSectionLock.class));
    }

    @Test
    void createReminder_shouldPersistReminder() {
        SectionReminderRequest request = SectionReminderRequest.builder()
                .sectionId(20L)
                .recipient("李总")
                .message("请确认章节进度")
                .remindedBy(3L)
                .build();

        when(structureRepository.findByProjectId(100L)).thenReturn(Optional.of(structure));
        when(sectionRepository.findById(20L)).thenReturn(Optional.of(section));
        when(reminderRepository.save(any())).thenAnswer(invocation -> {
            var reminder = invocation.getArgument(0, com.xiyu.bid.documenteditor.entity.DocumentReminder.class);
            reminder.setId(40L);
            return reminder;
        });

        DocumentReminderDTO result = documentEditorService.createReminder(100L, request);

        assertEquals(40L, result.getId());
        assertEquals("李总", result.getRecipient());
        assertEquals("请确认章节进度", result.getMessage());
        assertEquals(3L, result.getRemindedBy());
        assertNotNull(result.getRemindedAt());
        verify(reminderRepository).save(any());
    }

    @Test
    void getSectionTree_shouldIncludeAssignmentAndLockState() {
        DocumentSectionAssignment assignment = DocumentSectionAssignment.builder()
                .sectionId(20L)
                .projectId(100L)
                .owner("王工")
                .assignedBy(9L)
                .dueDate(LocalDate.of(2026, 4, 1))
                .build();
        DocumentSectionLock lock = DocumentSectionLock.builder()
                .sectionId(20L)
                .projectId(100L)
                .locked(true)
                .lockedBy(8L)
                .lockedAt(LocalDateTime.now())
                .build();

        when(structureRepository.findByProjectId(100L)).thenReturn(Optional.of(structure));
        when(sectionRepository.findByStructureId(10L)).thenReturn(List.of(section));
        when(assignmentRepository.findBySectionIdIn(List.of(20L))).thenReturn(List.of(assignment));
        when(lockRepository.findBySectionIdIn(List.of(20L))).thenReturn(List.of(lock));

        List<DocumentSectionDTO> tree = documentEditorService.getSectionTree(100L);

        assertEquals(1, tree.size());
        assertEquals("王工", tree.get(0).getOwner());
        assertTrue(tree.get(0).getLocked());
        assertEquals(LocalDate.of(2026, 4, 1), tree.get(0).getDueDate());
    }
}
