package com.xiyu.bid.biddraftagent.controller;

import com.xiyu.bid.biddraftagent.application.BidDraftAgentAppService;
import com.xiyu.bid.biddraftagent.application.BidTenderDocumentImportAppService;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentApplyResponseDTO;
import com.xiyu.bid.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.xiyu.bid.biddraftagent.controller.BidDraftAgentControllerFixtures.sampleImportDto;
import static com.xiyu.bid.biddraftagent.controller.BidDraftAgentControllerFixtures.sampleReviewDto;
import static com.xiyu.bid.biddraftagent.controller.BidDraftAgentControllerFixtures.sampleRunDto;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BidDraftAgentControllerTest {

    @Mock
    private BidDraftAgentAppService appService;

    @Mock
    private BidTenderDocumentImportAppService importAppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BidDraftAgentController(appService, importAppService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void importTenderDocument_shouldUploadParseGenerateAndApply() throws Exception {
        when(importAppService.importAndGenerate(eq(11L), any(), eq(true))).thenReturn(sampleImportDto());

        mockMvc.perform(multipart("/api/projects/{projectId}/bid-agent/tender-documents", 11L)
                        .file("file", "招标范围和评分标准".getBytes())
                        .param("applyToEditor", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("招标文件已解析，标书初稿已写入文档编辑器"))
                .andExpect(jsonPath("$.data.document.name").value("file"))
                .andExpect(jsonPath("$.data.requirementProfile.technicalRequirements[0]").value("提供实施方案"))
                .andExpect(jsonPath("$.data.run.id").value(100))
                .andExpect(jsonPath("$.data.appliedToEditor").value(true));

        verify(importAppService).importAndGenerate(eq(11L), any(), eq(true));
    }

    @Test
    void importTenderDocument_shouldReturnForbiddenWhenProjectAccessDenied() throws Exception {
        when(importAppService.importAndGenerate(eq(12L), any(), eq(true)))
                .thenThrow(new AccessDeniedException("权限不足"));

        mockMvc.perform(multipart("/api/projects/{projectId}/bid-agent/tender-documents", 12L)
                        .file("file", "招标范围和评分标准".getBytes())
                        .param("applyToEditor", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403));

        verify(importAppService).importAndGenerate(eq(12L), any(), eq(true));
    }

    @Test
    void createRun_shouldReturnCreatedRunPayload() throws Exception {
        when(appService.createRun(11L)).thenReturn(sampleRunDto());

        mockMvc.perform(post("/api/projects/{projectId}/bid-agent/runs", 11L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectName").value("华东智慧园区改造项目"))
                .andExpect(jsonPath("$.data.artifacts[0].artifactType").value("DRAFT_TEXT"));

        verify(appService).createRun(11L);
    }

    @Test
    void createRun_shouldReturnForbiddenWhenProjectAccessDenied() throws Exception {
        when(appService.createRun(12L)).thenThrow(new AccessDeniedException("权限不足"));

        mockMvc.perform(post("/api/projects/{projectId}/bid-agent/runs", 12L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403));

        verify(appService).createRun(12L);
    }

    @Test
    void getRun_shouldReturnSavedRun() throws Exception {
        when(appService.getRun(11L, 100L)).thenReturn(sampleRunDto());

        mockMvc.perform(get("/api/projects/{projectId}/bid-agent/runs/{runId}", 11L, 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100));

        verify(appService).getRun(11L, 100L);
    }

    @Test
    void getRun_shouldReturnForbiddenWhenProjectAccessDenied() throws Exception {
        when(appService.getRun(12L, 100L)).thenThrow(new AccessDeniedException("权限不足"));

        mockMvc.perform(get("/api/projects/{projectId}/bid-agent/runs/{runId}", 12L, 100L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403));

        verify(appService).getRun(12L, 100L);
    }

    @Test
    void reviewCurrentDraft_shouldReturnReviewSummary() throws Exception {
        when(appService.reviewCurrentDraft(11L)).thenReturn(sampleReviewDto());

        mockMvc.perform(post("/api/projects/{projectId}/bid-agent/reviews", 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewSummary").value("updated review"))
                .andExpect(jsonPath("$.data.nextActions[0]").value("项目概况"));

        verify(appService).reviewCurrentDraft(11L);
    }

    @Test
    void reviewCurrentDraft_shouldReturnForbiddenWhenProjectAccessDenied() throws Exception {
        when(appService.reviewCurrentDraft(12L)).thenThrow(new AccessDeniedException("权限不足"));

        mockMvc.perform(post("/api/projects/{projectId}/bid-agent/reviews", 12L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403));

        verify(appService).reviewCurrentDraft(12L);
    }

    @Test
    void reviewRun_shouldReviewTheRequestedRun() throws Exception {
        when(appService.reviewRun(11L, 100L)).thenReturn(sampleReviewDto());

        mockMvc.perform(post("/api/projects/{projectId}/bid-agent/runs/{runId}/reviews", 11L, 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewSummary").value("updated review"));

        verify(appService).reviewRun(11L, 100L);
    }

    @Test
    void reviewRun_shouldReturnForbiddenWhenProjectAccessDenied() throws Exception {
        when(appService.reviewRun(12L, 100L)).thenThrow(new AccessDeniedException("权限不足"));

        mockMvc.perform(post("/api/projects/{projectId}/bid-agent/runs/{runId}/reviews", 12L, 100L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403));

        verify(appService).reviewRun(12L, 100L);
    }

    @Test
    void applyRun_shouldMarkArtifactReadyForWriter() throws Exception {
        when(appService.applyRun(11L, 100L)).thenReturn(BidDraftAgentApplyResponseDTO.builder()
                .runId(100L)
                .artifactId(200L)
                .artifactType("DRAFT_TEXT")
                .status("READY_FOR_WRITER")
                .readyForWriter(true)
                .handoffTarget("document-writer")
                .message("草稿产物已标记为文档写手可用")
                .build());

        mockMvc.perform(post("/api/projects/{projectId}/bid-agent/runs/{runId}/apply", 11L, 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readyForWriter").value(true))
                .andExpect(jsonPath("$.data.status").value("READY_FOR_WRITER"));

        verify(appService).applyRun(11L, 100L);
    }

    @Test
    void applyRun_shouldReturnForbiddenWhenProjectAccessDenied() throws Exception {
        when(appService.applyRun(12L, 100L)).thenThrow(new AccessDeniedException("权限不足"));

        mockMvc.perform(post("/api/projects/{projectId}/bid-agent/runs/{runId}/apply", 12L, 100L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403));

        verify(appService).applyRun(12L, 100L);
    }
}
