package com.xiyu.bid.biddraftagent.controller;

import com.xiyu.bid.biddraftagent.application.BidDraftAgentAppService;
import com.xiyu.bid.biddraftagent.domain.BidDraftSnapshot;
import com.xiyu.bid.biddraftagent.domain.GapCheckResult;
import com.xiyu.bid.biddraftagent.domain.ManualConfirmationDecision;
import com.xiyu.bid.biddraftagent.domain.MaterialMatchScore;
import com.xiyu.bid.biddraftagent.domain.RequirementClassification;
import com.xiyu.bid.biddraftagent.domain.WriteCoverageDecision;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentApplyResponseDTO;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentArtifactDTO;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentReviewDTO;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentRunDTO;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BidDraftAgentControllerTest {

    @Mock
    private BidDraftAgentAppService appService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BidDraftAgentController(appService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
    void getRun_shouldReturnSavedRun() throws Exception {
        when(appService.getRun(11L, 100L)).thenReturn(sampleRunDto());

        mockMvc.perform(get("/api/projects/{projectId}/bid-agent/runs/{runId}", 11L, 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100));

        verify(appService).getRun(11L, 100L);
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

    private BidDraftAgentRunDTO sampleRunDto() {
        return BidDraftAgentRunDTO.builder()
                .id(100L)
                .projectId(11L)
                .tenderId(22L)
                .projectName("华东智慧园区改造项目")
                .tenderTitle("2026园区改造招标公告")
                .status("DRAFTED")
                .snapshot(new BidDraftSnapshot(
                        11L,
                        22L,
                        "华东智慧园区改造项目",
                        "项目需要报价、合同法务条款、实施方案和交付验收材料",
                        "已确认招标背景和客户范围",
                        "西域智算中心",
                        "重点客户",
                        "上海",
                        "信息化",
                        new BigDecimal("5000000"),
                        LocalDate.of(2026, 5, 30),
                        "2026园区改造招标公告",
                        "投标要求包含资质、报价、合同和实施计划",
                        "上海采购集团",
                        "公开招标",
                        List.of("智慧园区", "改造"),
                        List.of("建筑业企业资质证书 / CONSTRUCTION / FIRST"),
                        List.of("法务合同模板 / LEGAL / 投标说明"),
                        List.of("智慧园区实施案例 / 方案 / 交付验收 / 售后支持")
                ))
                .requirementClassification(new RequirementClassification(
                        List.of("价格"),
                        List.of("合同"),
                        List.of("资质"),
                        List.of("技术"),
                        List.of("交付"),
                        List.of("商务"),
                        List.of("项目背景")
                ))
                .materialMatchScore(new MaterialMatchScore(100, 6, 6,
                        List.of("pricing", "legal", "qualification", "technical", "delivery", "commercial"),
                        List.of(), List.of(), List.of()))
                .gapCheck(new GapCheckResult(true, List.of(), List.of()))
                .manualConfirmation(new ManualConfirmationDecision(true, true, true, List.of("需要人工确认")))
                .writeCoverage(new WriteCoverageDecision(100, true,
                        List.of("项目概况", "商务响应"), List.of(), List.of("项目概况")))
                .draftText("draft text")
                .reviewText("review text")
                .artifacts(List.of(BidDraftAgentArtifactDTO.builder()
                        .id(200L)
                        .runId(100L)
                        .artifactType("DRAFT_TEXT")
                        .title("自动生成投标草稿")
                        .content("draft text")
                        .handoffTarget("document-writer")
                        .status("DRAFTED")
                        .createdAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                        .updatedAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                        .build()))
                .reviewedAt(LocalDateTime.of(2026, 4, 22, 11, 0))
                .appliedAt(null)
                .createdAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .build();
    }

    private BidDraftAgentReviewDTO sampleReviewDto() {
        return BidDraftAgentReviewDTO.builder()
                .runId(100L)
                .projectId(11L)
                .status("REVIEWED")
                .reviewSummary("updated review")
                .draftText("draft text")
                .requirementClassification(new RequirementClassification(
                        List.of("价格"),
                        List.of("合同"),
                        List.of("资质"),
                        List.of("技术"),
                        List.of("交付"),
                        List.of("商务"),
                        List.of("项目背景")
                ))
                .materialMatchScore(new MaterialMatchScore(100, 6, 6,
                        List.of("pricing", "legal", "qualification", "technical", "delivery", "commercial"),
                        List.of(), List.of(), List.of()))
                .gapCheck(new GapCheckResult(true, List.of(), List.of()))
                .manualConfirmation(new ManualConfirmationDecision(true, true, true, List.of("需要人工确认")))
                .writeCoverage(new WriteCoverageDecision(100, true,
                        List.of("项目概况", "商务响应"), List.of(), List.of("项目概况")))
                .nextActions(List.of("项目概况", "商务响应"))
                .reviewedAt(LocalDateTime.of(2026, 4, 22, 11, 0))
                .build();
    }
}
