// Input: DocumentVersionController实现
// Output: 文档版本控制器集成测试
// Pos: Test/集成测试
// 测试文档版本管理HTTP端点的请求处理和响应

package com.xiyu.bid.versionhistory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.versionhistory.controller.DocumentVersionController;
import com.xiyu.bid.versionhistory.dto.DocumentVersionDTO;
import com.xiyu.bid.versionhistory.dto.VersionCreateRequest;
import com.xiyu.bid.versionhistory.dto.VersionDiffDTO;
import com.xiyu.bid.versionhistory.service.VersionHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 文档版本控制器测试类
 * 测试HTTP端点的请求处理和响应
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentVersionControllerTest {

    @Mock
    private VersionHistoryService versionHistoryService;

    @InjectMocks
    private DocumentVersionController documentVersionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private DocumentVersionDTO testVersionDTO;
    private DocumentVersionDTO testVersionDTO2;
    private VersionCreateRequest createRequest;
    private VersionDiffDTO versionDiffDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(documentVersionController)
                .setControllerAdvice(new com.xiyu.bid.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();

        testVersionDTO = DocumentVersionDTO.builder()
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

        testVersionDTO2 = DocumentVersionDTO.builder()
                .id(2L)
                .projectId(100L)
                .documentId("doc-001")
                .versionNumber(2)
                .content("Updated content")
                .filePath("/path/to/file.docx")
                .changeSummary("Updated version")
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

        versionDiffDTO = VersionDiffDTO.builder()
                .version1Id(1L)
                .version2Id(2L)
                .version1Number(1)
                .version2Number(2)
                .content1("Initial content")
                .content2("Updated content")
                .differences(Arrays.asList("Line 1 changed from 'Initial' to 'Updated'"))
                .build();
    }

    // ==================== GET /api/documents/{projectId}/versions Tests ====================

    @Test
    void getVersionsByProject_WithValidProjectId_ShouldReturn200() throws Exception {
        // Given
        List<DocumentVersionDTO> versions = Arrays.asList(testVersionDTO2, testVersionDTO);
        when(versionHistoryService.getVersionsByProject(100L)).thenReturn(versions);

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[1].id").value(1));

        verify(versionHistoryService).getVersionsByProject(100L);
    }

    @Test
    void getVersionsByProject_WithEmptyVersions_ShouldReturn200WithEmptyArray() throws Exception {
        // Given
        when(versionHistoryService.getVersionsByProject(100L)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(versionHistoryService).getVersionsByProject(100L);
    }

    @Test
    void getVersionsByProject_WithInvalidProjectId_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid project ID"));

        verify(versionHistoryService, never()).getVersionsByProject(any());
    }

    @Test
    void getVersionsByProject_WithZeroProjectId_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid project ID"));

        verify(versionHistoryService, never()).getVersionsByProject(any());
    }

    // ==================== GET /api/documents/{projectId}/versions/latest Tests ====================

    @Test
    void getLatestVersion_WithValidProjectId_ShouldReturn200() throws Exception {
        // Given
        when(versionHistoryService.getLatestVersion(100L)).thenReturn(testVersionDTO);

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/latest", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.projectId").value(100))
                .andExpect(jsonPath("$.data.isCurrent").value(true));

        verify(versionHistoryService).getLatestVersion(100L);
    }

    @Test
    void getLatestVersion_WithNonExistentProject_ShouldReturn404() throws Exception {
        // Given
        when(versionHistoryService.getLatestVersion(999L))
                .thenThrow(new ResourceNotFoundException("No versions found for project: 999"));

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/latest", 999L))
                .andExpect(status().isNotFound());

        verify(versionHistoryService).getLatestVersion(999L);
    }

    @Test
    void getLatestVersion_WithInvalidProjectId_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/latest", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid project ID"));

        verify(versionHistoryService, never()).getLatestVersion(any());
    }

    // ==================== GET /api/documents/{projectId}/versions/{versionId} Tests ====================

    @Test
    void getVersion_WithValidIds_ShouldReturn200() throws Exception {
        // Given
        when(versionHistoryService.getVersion(1L)).thenReturn(testVersionDTO);

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/{versionId}", 100L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.versionNumber").value(1));

        verify(versionHistoryService).getVersion(1L);
    }

    @Test
    void getVersion_WithNonExistentVersion_ShouldReturn404() throws Exception {
        // Given
        when(versionHistoryService.getVersion(999L))
                .thenThrow(new ResourceNotFoundException("Version not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/{versionId}", 100L, 999L))
                .andExpect(status().isNotFound());

        verify(versionHistoryService).getVersion(999L);
    }

    @Test
    void getVersion_WithInvalidVersionId_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/{versionId}", 100L, -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid version ID"));

        verify(versionHistoryService, never()).getVersion(any());
    }

    // ==================== POST /api/documents/{projectId}/versions Tests ====================

    @Test
    void createVersion_WithValidData_ShouldReturn201() throws Exception {
        // Given
        when(versionHistoryService.createVersion(any(VersionCreateRequest.class))).thenReturn(testVersionDTO);

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Version created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.projectId").value(100));

        verify(versionHistoryService).createVersion(any(VersionCreateRequest.class));
    }

    @Test
    void createVersion_WithMismatchedProjectId_ShouldReturn400() throws Exception {
        // Given
        VersionCreateRequest mismatchedRequest = VersionCreateRequest.builder()
                .projectId(200L) // Different from path variable (100L)
                .documentId("doc-001")
                .content("Content")
                .createdBy(1L)
                .build();

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchedRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Project ID in path does not match request body"));

        verify(versionHistoryService, never()).createVersion(any());
    }

    @Test
    void createVersion_WithNullContent_ShouldReturn400() throws Exception {
        // Given
        VersionCreateRequest invalidRequest = VersionCreateRequest.builder()
                .projectId(100L)
                .documentId("doc-001")
                .content(null)
                .createdBy(1L)
                .build();

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(versionHistoryService, never()).createVersion(any());
    }

    @Test
    void createVersion_WithEmptyContent_ShouldReturn400() throws Exception {
        // Given
        VersionCreateRequest invalidRequest = VersionCreateRequest.builder()
                .projectId(100L)
                .documentId("doc-001")
                .content("")
                .createdBy(1L)
                .build();

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(versionHistoryService, never()).createVersion(any());
    }

    @Test
    void createVersion_WithNullCreatedBy_ShouldReturn400() throws Exception {
        // Given
        VersionCreateRequest invalidRequest = VersionCreateRequest.builder()
                .projectId(100L)
                .documentId("doc-001")
                .content("Content")
                .createdBy(null)
                .build();

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(versionHistoryService, never()).createVersion(any());
    }

    @Test
    void createVersion_WithLargeContent_ShouldReturn201() throws Exception {
        // Given
        String largeContent = "A".repeat(10000);
        VersionCreateRequest request = VersionCreateRequest.builder()
                .projectId(100L)
                .documentId("doc-001")
                .content(largeContent)
                .createdBy(1L)
                .build();

        DocumentVersionDTO largeVersionDTO = DocumentVersionDTO.builder()
                .id(1L)
                .projectId(100L)
                .content(largeContent)
                .versionNumber(1)
                .build();

        when(versionHistoryService.createVersion(any(VersionCreateRequest.class))).thenReturn(largeVersionDTO);

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value(largeContent));

        verify(versionHistoryService).createVersion(any(VersionCreateRequest.class));
    }

    // ==================== GET /api/documents/{projectId}/versions/{v1}/compare/{v2} Tests ====================

    @Test
    void compareVersions_WithValidVersionIds_ShouldReturn200() throws Exception {
        // Given
        when(versionHistoryService.compareVersions(1L, 2L)).thenReturn(versionDiffDTO);

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/{v1}/compare/{v2}", 100L, 1L, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version1Id").value(1))
                .andExpect(jsonPath("$.data.version2Id").value(2))
                .andExpect(jsonPath("$.data.version1Number").value(1))
                .andExpect(jsonPath("$.data.version2Number").value(2))
                .andExpect(jsonPath("$.data.differences").isArray());

        verify(versionHistoryService).compareVersions(1L, 2L);
    }

    @Test
    void compareVersions_WithSameVersionIds_ShouldReturn200() throws Exception {
        // Given
        VersionDiffDTO sameDiff = VersionDiffDTO.builder()
                .version1Id(1L)
                .version2Id(1L)
                .version1Number(1)
                .version2Number(1)
                .content1("Same content")
                .content2("Same content")
                .differences(Collections.emptyList())
                .build();
        when(versionHistoryService.compareVersions(1L, 1L)).thenReturn(sameDiff);

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/{v1}/compare/{v2}", 100L, 1L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.differences").isArray())
                .andExpect(jsonPath("$.data.differences.length()").value(0));

        verify(versionHistoryService).compareVersions(1L, 1L);
    }

    @Test
    void compareVersions_WithNonExistentVersion1_ShouldReturn404() throws Exception {
        // Given
        when(versionHistoryService.compareVersions(999L, 2L))
                .thenThrow(new ResourceNotFoundException("Version not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/{v1}/compare/{v2}", 100L, 999L, 2L))
                .andExpect(status().isNotFound());

        verify(versionHistoryService).compareVersions(999L, 2L);
    }

    @Test
    void compareVersions_WithInvalidVersionId1_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/{v1}/compare/{v2}", 100L, -1, 2L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid version IDs"));

        verify(versionHistoryService, never()).compareVersions(any(), any());
    }

    @Test
    void compareVersions_WithInvalidVersionId2_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/{v1}/compare/{v2}", 100L, 1L, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid version IDs"));

        verify(versionHistoryService, never()).compareVersions(any(), any());
    }

    // ==================== POST /api/documents/{projectId}/versions/{versionId}/rollback Tests ====================

    @Test
    void rollbackToVersion_WithValidData_ShouldReturn200() throws Exception {
        // Given
        when(versionHistoryService.rollbackToVersion(eq(100L), eq(1L), eq(1L))).thenReturn(testVersionDTO);

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions/{versionId}/rollback", 100L, 1L)
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Rolled back successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(versionHistoryService).rollbackToVersion(eq(100L), eq(1L), eq(1L));
    }

    @Test
    void rollbackToVersion_WithNullUserId_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions/{versionId}/rollback", 100L, 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User ID is required"));

        verify(versionHistoryService, never()).rollbackToVersion(any(), any(), any());
    }

    @Test
    void rollbackToVersion_WithNonExistentVersion_ShouldReturn404() throws Exception {
        // Given
        when(versionHistoryService.rollbackToVersion(eq(100L), eq(999L), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Version not found with id: 999"));

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions/{versionId}/rollback", 100L, 999L)
                        .param("userId", "1"))
                .andExpect(status().isNotFound());

        verify(versionHistoryService).rollbackToVersion(eq(100L), eq(999L), eq(1L));
    }

    @Test
    void rollbackToVersion_WithDifferentUserId_ShouldReturn200() throws Exception {
        // Given
        DocumentVersionDTO rolledBackVersion = DocumentVersionDTO.builder()
                .id(3L)
                .projectId(100L)
                .versionNumber(3)
                .content("Rolled back content")
                .changeSummary("Rollback to version 1")
                .createdBy(2L)
                .isCurrent(true)
                .build();

        when(versionHistoryService.rollbackToVersion(eq(100L), eq(1L), eq(2L))).thenReturn(rolledBackVersion);

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions/{versionId}/rollback", 100L, 1L)
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.createdBy").value(2))
                .andExpect(jsonPath("$.data.changeSummary").value("Rollback to version 1"));

        verify(versionHistoryService).rollbackToVersion(eq(100L), eq(1L), eq(2L));
    }

    // ==================== Edge Cases ====================

    @Test
    void createVersion_WithSpecialCharactersInContent_ShouldReturn201() throws Exception {
        // Given
        String specialContent = "Content with <script>alert('xss')</script>\n& special \"quotes\"";
        VersionCreateRequest request = VersionCreateRequest.builder()
                .projectId(100L)
                .documentId("doc-001")
                .content(specialContent)
                .createdBy(1L)
                .build();

        DocumentVersionDTO specialVersionDTO = DocumentVersionDTO.builder()
                .id(1L)
                .projectId(100L)
                .content(specialContent)
                .versionNumber(1)
                .build();

        when(versionHistoryService.createVersion(any(VersionCreateRequest.class))).thenReturn(specialVersionDTO);

        // When & Then
        mockMvc.perform(post("/api/documents/{projectId}/versions", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value(specialContent));

        verify(versionHistoryService).createVersion(any(VersionCreateRequest.class));
    }

    @Test
    void getVersionsByProject_WithVeryLargeProjectId_ShouldReturn200() throws Exception {
        // Given
        Long largeProjectId = Long.MAX_VALUE;
        when(versionHistoryService.getVersionsByProject(largeProjectId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions", largeProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(versionHistoryService).getVersionsByProject(largeProjectId);
    }

    @Test
    void createVersion_WithNullDocumentId_ShouldReturn201() throws Exception {
        // Given
        VersionCreateRequest requestWithoutDocId = VersionCreateRequest.builder()
                .projectId(100L)
                .documentId(null)
                .content("Content")
                .createdBy(1L)
                .build();

        when(versionHistoryService.createVersion(any(VersionCreateRequest.class))).thenReturn(testVersionDTO);

        // When & Then - documentId is optional
        mockMvc.perform(post("/api/documents/{projectId}/versions", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutDocId)))
                .andExpect(status().isCreated());

        verify(versionHistoryService).createVersion(any(VersionCreateRequest.class));
    }

    @Test
    void compareVersions_WithReversedOrder_ShouldReturn200() throws Exception {
        // Given
        VersionDiffDTO reversedDiff = VersionDiffDTO.builder()
                .version1Id(2L)
                .version2Id(1L)
                .version1Number(2)
                .version2Number(1)
                .content1("Updated content")
                .content2("Initial content")
                .differences(Arrays.asList("Content changed"))
                .build();

        when(versionHistoryService.compareVersions(2L, 1L)).thenReturn(reversedDiff);

        // When & Then
        mockMvc.perform(get("/api/documents/{projectId}/versions/{v1}/compare/{v2}", 100L, 2L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version1Id").value(2))
                .andExpect(jsonPath("$.data.version2Id").value(1));

        verify(versionHistoryService).compareVersions(2L, 1L);
    }
}
