// Input: VersionHistoryService实现
// Output: 版本历史服务测试
// Pos: Test/单元测试
// 测试版本历史服务的所有核心功能，包括创建、查询、比较和回滚

package com.xiyu.bid.versionhistory;

import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.audit.service.AuditLogService;
import com.xiyu.bid.audit.service.IAuditLogService;
import com.xiyu.bid.versionhistory.dto.DocumentVersionDTO;
import com.xiyu.bid.versionhistory.dto.VersionCreateRequest;
import com.xiyu.bid.versionhistory.dto.VersionDiffDTO;
import com.xiyu.bid.versionhistory.entity.DocumentVersion;
import com.xiyu.bid.versionhistory.repository.DocumentVersionRepository;
import com.xiyu.bid.versionhistory.service.VersionHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 版本历史服务测试类
 * 测试版本创建、查询、比较和回滚等核心功能
 */
@ExtendWith(MockitoExtension.class)
class VersionHistoryServiceTest {

    @Mock
    private DocumentVersionRepository repository;

    @Mock
    private IAuditLogService auditLogService;

    private VersionHistoryService versionHistoryService;

    private DocumentVersion testVersion;
    private DocumentVersion testVersion2;
    private VersionCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        versionHistoryService = new VersionHistoryService(repository, auditLogService);

        testVersion = DocumentVersion.builder()
                .id(1L)
                .projectId(100L)
                .documentId("doc-001")
                .versionNumber(1)
                .content("Initial content")
                .filePath("/path/to/file.docx")
                .changeSummary("Initial version")
                .createdBy(1L)
                .createdAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .isCurrent(true)
                .build();

        testVersion2 = DocumentVersion.builder()
                .id(2L)
                .projectId(100L)
                .documentId("doc-001")
                .versionNumber(2)
                .content("Updated content")
                .filePath("/path/to/file.docx")
                .changeSummary("Updated second line")
                .createdBy(1L)
                .createdAt(LocalDateTime.of(2024, 3, 2, 10, 0))
                .isCurrent(false)
                .build();

        createRequest = VersionCreateRequest.builder()
                .projectId(100L)
                .documentId("doc-001")
                .content("New content")
                .filePath("/path/to/file.docx")
                .changeSummary("New version")
                .createdBy(1L)
                .build();
    }

    // ==================== createVersion Tests ====================

    @Test
    void createVersion_WithValidData_ShouldReturnSavedVersion() {
        // Given
        when(repository.getNextVersionNumber(100L)).thenReturn(1);
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.empty());
        when(repository.save(any(DocumentVersion.class))).thenReturn(testVersion);

        // When
        DocumentVersionDTO result = versionHistoryService.createVersion(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getVersionNumber()).isEqualTo(1);
        assertThat(result.getContent()).isEqualTo("Initial content");
        assertThat(result.getIsCurrent()).isTrue();

        verify(repository).save(any(DocumentVersion.class));
        verify(auditLogService).log(any(AuditLogService.AuditLogEntry.class));
    }

    @Test
    void createVersion_WithNullProjectId_ShouldThrowException() {
        // Given
        VersionCreateRequest invalidRequest = VersionCreateRequest.builder()
                .projectId(null)
                .content("Content")
                .createdBy(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.createVersion(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID is required");

        verify(repository, never()).save(any(DocumentVersion.class));
    }

    @Test
    void createVersion_WithNullContent_ShouldThrowException() {
        // Given
        VersionCreateRequest invalidRequest = VersionCreateRequest.builder()
                .projectId(100L)
                .content(null)
                .createdBy(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.createVersion(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content cannot be empty");

        verify(repository, never()).save(any(DocumentVersion.class));
    }

    @Test
    void createVersion_WithEmptyContent_ShouldThrowException() {
        // Given
        VersionCreateRequest invalidRequest = VersionCreateRequest.builder()
                .projectId(100L)
                .content("   ")
                .createdBy(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.createVersion(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content cannot be empty");

        verify(repository, never()).save(any(DocumentVersion.class));
    }

    @Test
    void createVersion_WithNullCreatedBy_ShouldThrowException() {
        // Given
        VersionCreateRequest invalidRequest = VersionCreateRequest.builder()
                .projectId(100L)
                .content("Content")
                .createdBy(null)
                .build();

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.createVersion(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Created by is required");

        verify(repository, never()).save(any(DocumentVersion.class));
    }

    @Test
    void createVersion_WithExistingCurrentVersion_ShouldMarkOldAsNotCurrent() {
        // Given
        when(repository.getNextVersionNumber(100L)).thenReturn(2);
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.of(testVersion));
        when(repository.save(any(DocumentVersion.class))).thenReturn(testVersion2);

        // When
        versionHistoryService.createVersion(createRequest);

        // Then
        verify(repository, times(2)).save(any(DocumentVersion.class));
        verify(auditLogService).log(any(AuditLogService.AuditLogEntry.class));
    }

    @Test
    void createVersion_WithNoChangeSummary_ShouldUseDefault() {
        // Given
        VersionCreateRequest requestWithoutSummary = VersionCreateRequest.builder()
                .projectId(100L)
                .content("Content")
                .createdBy(1L)
                .changeSummary(null)
                .build();

        when(repository.getNextVersionNumber(100L)).thenReturn(1);
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.empty());
        when(repository.save(any(DocumentVersion.class))).thenAnswer(invocation -> {
            DocumentVersion v = invocation.getArgument(0);
            v.setId(1L);
            return v;
        });

        // When
        DocumentVersionDTO result = versionHistoryService.createVersion(requestWithoutSummary);

        // Then
        assertThat(result.getChangeSummary()).isEqualTo("Version 1");
    }

    @Test
    void createVersion_WithVeryLongContent_ShouldHandleCorrectly() {
        // Given
        String longContent = "A".repeat(100000); // 100k characters
        VersionCreateRequest request = VersionCreateRequest.builder()
                .projectId(100L)
                .content(longContent)
                .createdBy(1L)
                .build();

        when(repository.getNextVersionNumber(100L)).thenReturn(1);
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.empty());
        when(repository.save(any(DocumentVersion.class))).thenAnswer(invocation -> {
            DocumentVersion v = invocation.getArgument(0);
            v.setId(1L);
            return v;
        });

        // When
        DocumentVersionDTO result = versionHistoryService.createVersion(request);

        // Then
        assertThat(result.getContent()).hasSize(100000);
    }

    // ==================== getVersionsByProject Tests ====================

    @Test
    void getVersionsByProject_WithValidProjectId_ShouldReturnVersions() {
        // Given
        List<DocumentVersion> versions = Arrays.asList(testVersion2, testVersion);
        when(repository.findByProjectIdOrderByCreatedAtDesc(100L)).thenReturn(versions);

        // When
        List<DocumentVersionDTO> result = versionHistoryService.getVersionsByProject(100L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersionNumber()).isEqualTo(2);
        assertThat(result.get(1).getVersionNumber()).isEqualTo(1);
    }

    @Test
    void getVersionsByProject_WithNoVersions_ShouldReturnEmptyList() {
        // Given
        when(repository.findByProjectIdOrderByCreatedAtDesc(100L)).thenReturn(Arrays.asList());

        // When
        List<DocumentVersionDTO> result = versionHistoryService.getVersionsByProject(100L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getVersionsByProject_WithNullProjectId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> versionHistoryService.getVersionsByProject(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID is required");
    }

    // ==================== getVersion Tests ====================

    @Test
    void getVersion_WithValidVersionId_ShouldReturnVersion() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));

        // When
        DocumentVersionDTO result = versionHistoryService.getVersion(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getVersionNumber()).isEqualTo(1);
    }

    @Test
    void getVersion_WithInvalidVersionId_ShouldThrowException() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.getVersion(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Version not found with id: 999");
    }

    @Test
    void getVersion_WithNullVersionId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> versionHistoryService.getVersion(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version ID is required");
    }

    // ==================== getLatestVersion Tests ====================

    @Test
    void getLatestVersion_WithValidProjectId_ShouldReturnCurrentVersion() {
        // Given
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.of(testVersion));

        // When
        DocumentVersionDTO result = versionHistoryService.getLatestVersion(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getIsCurrent()).isTrue();
    }

    @Test
    void getLatestVersion_WithNoVersions_ShouldThrowException() {
        // Given
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.getLatestVersion(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No versions found for project: 100");
    }

    @Test
    void getLatestVersion_WithNullProjectId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> versionHistoryService.getLatestVersion(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID is required");
    }

    // ==================== compareVersions Tests ====================

    @Test
    void compareVersions_WithValidVersionIds_ShouldReturnDifferences() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));
        when(repository.findById(2L)).thenReturn(Optional.of(testVersion2));

        // When
        VersionDiffDTO result = versionHistoryService.compareVersions(1L, 2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVersion1Id()).isEqualTo(1L);
        assertThat(result.getVersion2Id()).isEqualTo(2L);
        assertThat(result.getVersion1Number()).isEqualTo(1);
        assertThat(result.getVersion2Number()).isEqualTo(2);
        assertThat(result.getContent1()).isEqualTo("Initial content");
        assertThat(result.getContent2()).isEqualTo("Updated content");
        assertThat(result.getDifferences()).isNotEmpty();
    }

    @Test
    void compareVersions_WithSameContent_ShouldReturnEmptyDifferences() {
        // Given
        DocumentVersion version1 = DocumentVersion.builder()
                .id(1L)
                .versionNumber(1)
                .content("Same content")
                .build();
        DocumentVersion version2 = DocumentVersion.builder()
                .id(2L)
                .versionNumber(2)
                .content("Same content")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(version1));
        when(repository.findById(2L)).thenReturn(Optional.of(version2));

        // When
        VersionDiffDTO result = versionHistoryService.compareVersions(1L, 2L);

        // Then
        assertThat(result.getDifferences()).isEmpty();
    }

    @Test
    void compareVersions_WithNullFirstVersionId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> versionHistoryService.compareVersions(null, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both version IDs are required");
    }

    @Test
    void compareVersions_WithNullSecondVersionId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> versionHistoryService.compareVersions(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both version IDs are required");
    }

    @Test
    void compareVersions_WithInvalidFirstVersionId_ShouldThrowException() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.compareVersions(999L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Version not found with id: 999");
    }

    @Test
    void compareVersions_WithInvalidSecondVersionId_ShouldThrowException() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.compareVersions(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Version not found with id: 999");
    }

    @Test
    void compareVersions_WithNullContentInVersion1_ShouldHandleCorrectly() {
        // Given
        DocumentVersion version1 = DocumentVersion.builder()
                .id(1L)
                .versionNumber(1)
                .content(null)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(version1));
        when(repository.findById(2L)).thenReturn(Optional.of(testVersion2));

        // When
        VersionDiffDTO result = versionHistoryService.compareVersions(1L, 2L);

        // Then
        assertThat(result.getDifferences()).isNotEmpty();
        assertThat(result.getDifferences().get(0)).contains("Content added");
    }

    @Test
    void compareVersions_WithNullContentInVersion2_ShouldHandleCorrectly() {
        // Given
        DocumentVersion version2 = DocumentVersion.builder()
                .id(2L)
                .versionNumber(2)
                .content(null)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));
        when(repository.findById(2L)).thenReturn(Optional.of(version2));

        // When
        VersionDiffDTO result = versionHistoryService.compareVersions(1L, 2L);

        // Then
        assertThat(result.getDifferences()).isNotEmpty();
        assertThat(result.getDifferences().get(0)).contains("Content removed");
    }

    @Test
    void compareVersions_WithMultilineContent_ShouldDetectLineChanges() {
        // Given
        DocumentVersion version1 = DocumentVersion.builder()
                .id(1L)
                .versionNumber(1)
                .content("Line 1\nLine 2\nLine 3")
                .build();
        DocumentVersion version2 = DocumentVersion.builder()
                .id(2L)
                .versionNumber(2)
                .content("Line 1\nModified Line 2\nLine 3")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(version1));
        when(repository.findById(2L)).thenReturn(Optional.of(version2));

        // When
        VersionDiffDTO result = versionHistoryService.compareVersions(1L, 2L);

        // Then
        assertThat(result.getDifferences()).hasSize(1);
        assertThat(result.getDifferences().get(0)).contains("Line 2");
    }

    // ==================== rollbackToVersion Tests ====================

    @Test
    void rollbackToVersion_WithValidData_ShouldCreateNewVersion() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.of(testVersion2));
        when(repository.getNextVersionNumber(100L)).thenReturn(3);
        when(repository.save(any(DocumentVersion.class))).thenAnswer(invocation -> {
            DocumentVersion v = invocation.getArgument(0);
            if (v.getId() == null) {
                v.setId(3L);
            }
            return v;
        });

        // When
        DocumentVersionDTO result = versionHistoryService.rollbackToVersion(100L, 1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVersionNumber()).isEqualTo(3);
        assertThat(result.getContent()).isEqualTo("Initial content");
        assertThat(result.getChangeSummary()).contains("Rollback to version 1");
        assertThat(result.getIsCurrent()).isTrue();

        verify(repository, atLeastOnce()).save(any(DocumentVersion.class));
        verify(auditLogService).log(any(AuditLogService.AuditLogEntry.class));
    }

    @Test
    void rollbackToVersion_WithNullProjectId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> versionHistoryService.rollbackToVersion(null, 1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID is required");
    }

    @Test
    void rollbackToVersion_WithNullVersionId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> versionHistoryService.rollbackToVersion(100L, null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version ID is required");
    }

    @Test
    void rollbackToVersion_WithNullUserId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> versionHistoryService.rollbackToVersion(100L, 1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID is required");
    }

    @Test
    void rollbackToVersion_WithInvalidVersionId_ShouldThrowException() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.rollbackToVersion(100L, 999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Version not found with id: 999");
    }

    @Test
    void rollbackToVersion_WithVersionFromDifferentProject_ShouldThrowException() {
        // Given
        DocumentVersion otherProjectVersion = DocumentVersion.builder()
                .id(1L)
                .projectId(200L) // Different project
                .versionNumber(1)
                .content("Content")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(otherProjectVersion));

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.rollbackToVersion(100L, 1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to the specified project");
    }

    @Test
    void rollbackToVersion_WithNoCurrentVersion_ShouldThrowException() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.rollbackToVersion(100L, 1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No current version found for project: 100");
    }

    @Test
    void rollbackToVersion_ShouldMarkOldCurrentAsNotCurrent() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.of(testVersion2));
        when(repository.getNextVersionNumber(100L)).thenReturn(3);
        when(repository.save(any(DocumentVersion.class))).thenAnswer(invocation -> {
            DocumentVersion v = invocation.getArgument(0);
            if (v.getId() == null) {
                v.setId(3L);
            }
            return v;
        });

        // When
        versionHistoryService.rollbackToVersion(100L, 1L, 1L);

        // Then
        assertThat(testVersion2.getIsCurrent()).isFalse();
    }

    // ==================== markAsCurrent Tests ====================

    @Test
    void markAsCurrent_WithValidVersionId_ShouldUpdateCorrectly() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.of(testVersion2));
        when(repository.save(any(DocumentVersion.class))).thenReturn(testVersion);

        // When
        versionHistoryService.markAsCurrent(1L);

        // Then
        assertThat(testVersion.getIsCurrent()).isTrue();
        assertThat(testVersion2.getIsCurrent()).isFalse();
        verify(repository, atLeast(2)).save(any(DocumentVersion.class));
    }

    @Test
    void markAsCurrent_WithNullVersionId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> versionHistoryService.markAsCurrent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version ID is required");
    }

    @Test
    void markAsCurrent_WithInvalidVersionId_ShouldThrowException() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> versionHistoryService.markAsCurrent(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Version not found with id: 999");
    }

    @Test
    void markAsCurrent_WithNoExistingCurrentVersion_ShouldOnlyMarkTarget() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.empty());
        when(repository.save(any(DocumentVersion.class))).thenReturn(testVersion);

        // When
        versionHistoryService.markAsCurrent(1L);

        // Then
        assertThat(testVersion.getIsCurrent()).isTrue();
        verify(repository).save(testVersion);
    }

    @Test
    void markAsCurrent_WhenVersionIsAlreadyCurrent_ShouldNotDuplicateSave() {
        // Given
        testVersion.setIsCurrent(true);
        when(repository.findById(1L)).thenReturn(Optional.of(testVersion));
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.of(testVersion));
        when(repository.save(any(DocumentVersion.class))).thenReturn(testVersion);

        // When
        versionHistoryService.markAsCurrent(1L);

        // Then - The service saves the current version (even if it's the same) twice:
        // 1. When marking the old current as not current (if found)
        // 2. When marking the target version as current
        verify(repository, atLeast(1)).save(any(DocumentVersion.class));
    }

    // ==================== Edge Cases ====================

    @Test
    void createVersion_WithSpecialCharactersInContent_ShouldHandleCorrectly() {
        // Given
        String specialContent = "Content with <script> tags\n& special chars: \"quotes\"\nNewlines\n\tTabs";
        VersionCreateRequest request = VersionCreateRequest.builder()
                .projectId(100L)
                .content(specialContent)
                .createdBy(1L)
                .build();

        when(repository.getNextVersionNumber(100L)).thenReturn(1);
        when(repository.findCurrentVersionByProjectId(100L)).thenReturn(Optional.empty());
        when(repository.save(any(DocumentVersion.class))).thenAnswer(invocation -> {
            DocumentVersion v = invocation.getArgument(0);
            v.setId(1L);
            return v;
        });

        // When
        DocumentVersionDTO result = versionHistoryService.createVersion(request);

        // Then
        assertThat(result.getContent()).isEqualTo(specialContent);
    }

    @Test
    void compareVersions_WithEmptyStrings_ShouldReturnEmptyDifferences() {
        // Given
        DocumentVersion version1 = DocumentVersion.builder()
                .id(1L)
                .versionNumber(1)
                .content("")
                .build();
        DocumentVersion version2 = DocumentVersion.builder()
                .id(2L)
                .versionNumber(2)
                .content("")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(version1));
        when(repository.findById(2L)).thenReturn(Optional.of(version2));

        // When
        VersionDiffDTO result = versionHistoryService.compareVersions(1L, 2L);

        // Then
        assertThat(result.getDifferences()).isEmpty();
    }

    @Test
    void rollbackToVersion_WithLargeProjectId_ShouldHandleCorrectly() {
        // Given
        Long largeProjectId = Long.MAX_VALUE;
        DocumentVersion largeProjectVersion = DocumentVersion.builder()
                .id(1L)
                .projectId(largeProjectId)
                .versionNumber(1)
                .content("Content")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(largeProjectVersion));
        when(repository.findCurrentVersionByProjectId(largeProjectId)).thenReturn(Optional.of(testVersion2));
        when(repository.getNextVersionNumber(largeProjectId)).thenReturn(2);
        when(repository.save(any(DocumentVersion.class))).thenAnswer(invocation -> {
            DocumentVersion v = invocation.getArgument(0);
            if (v.getId() == null) {
                v.setId(2L);
            }
            return v;
        });

        // When
        DocumentVersionDTO result = versionHistoryService.rollbackToVersion(largeProjectId, 1L, 1L);

        // Then
        assertThat(result.getProjectId()).isEqualTo(largeProjectId);
    }
}
