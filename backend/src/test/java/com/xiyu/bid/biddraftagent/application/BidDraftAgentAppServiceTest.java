package com.xiyu.bid.biddraftagent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.biddraftagent.domain.BidDraftSnapshot;
import com.xiyu.bid.biddraftagent.domain.GapCheckResult;
import com.xiyu.bid.biddraftagent.domain.ManualConfirmationDecision;
import com.xiyu.bid.biddraftagent.domain.MaterialMatchScore;
import com.xiyu.bid.biddraftagent.domain.RequirementClassification;
import com.xiyu.bid.biddraftagent.domain.WriteCoverageDecision;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentApplyResponseDTO;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentRunDTO;
import com.xiyu.bid.biddraftagent.entity.BidAgentArtifact;
import com.xiyu.bid.biddraftagent.entity.BidAgentRun;
import com.xiyu.bid.biddraftagent.repository.BidAgentArtifactRepository;
import com.xiyu.bid.biddraftagent.repository.BidAgentRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidDraftAgentAppServiceTest {

    @Mock
    private BidDraftSnapshotAssembler snapshotAssembler;

    @Mock
    private BidDraftTextGenerator textGenerator;

    @Mock
    private BidAgentRunRepository runRepository;

    @Mock
    private BidAgentArtifactRepository artifactRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private BidDraftAgentAppService appService;

    @BeforeEach
    void setUp() {
        BidDraftAgentJsonCodec jsonCodec = new BidDraftAgentJsonCodec(objectMapper);
        appService = new BidDraftAgentAppService(
                snapshotAssembler,
                new BidDraftAgentEvaluator(),
                textGenerator,
                new BidDraftAgentEntityFactory(jsonCodec),
                new BidDraftAgentRunMapper(jsonCodec),
                jsonCodec,
                runRepository,
                artifactRepository
        );
    }

    @Test
    void createRun_shouldPersistRunAndArtifacts() {
        BidDraftSnapshot snapshot = sampleSnapshot();
        RequirementClassification classification = new RequirementClassification(
                List.of("价格"),
                List.of("合同"),
                List.of("资质"),
                List.of("技术"),
                List.of("交付"),
                List.of("商务"),
                List.of("项目背景")
        );
        MaterialMatchScore materialMatchScore = new MaterialMatchScore(100, 6, 6,
                List.of("pricing", "legal", "qualification", "technical", "delivery", "commercial"),
                List.of(), List.of("pricing:supported"), List.of());
        GapCheckResult gapCheck = new GapCheckResult(true, List.of(), List.of());
        ManualConfirmationDecision manualConfirmation = new ManualConfirmationDecision(true, true, true, List.of("需要人工确认"));
        WriteCoverageDecision writeCoverage = new WriteCoverageDecision(100, true,
                List.of("项目概况", "商务响应"), List.of(), List.of("项目概况"));
        BidDraftGenerationResult generationResult = new BidDraftGenerationResult(
                "draft text",
                "review summary",
                List.of(
                        new GeneratedArtifactSpec("DRAFT_TEXT", "自动生成投标草稿", "draft text", "document-writer"),
                        new GeneratedArtifactSpec("REVIEW_SUMMARY", "草稿审阅摘要", "review summary", "bid-reviewer")
                )
        );

        when(snapshotAssembler.assemble(11L)).thenReturn(snapshot);
        when(textGenerator.generate(any(), any(), any(), any(), any(), any())).thenReturn(generationResult);
        when(runRepository.save(any())).thenAnswer(invocation -> {
            BidAgentRun run = invocation.getArgument(0);
            run.setId(100L);
            run.setCreatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
            run.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
            return run;
        });
        when(artifactRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<BidAgentArtifact> artifacts = invocation.getArgument(0);
            for (int i = 0; i < artifacts.size(); i++) {
                artifacts.get(i).setId(200L + i);
                artifacts.get(i).setCreatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
                artifacts.get(i).setUpdatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
            }
            return artifacts;
        });

        BidDraftAgentRunDTO dto = appService.createRun(11L);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getProjectId()).isEqualTo(11L);
        assertThat(dto.getArtifacts()).hasSize(2);
        assertThat(dto.getArtifacts().get(0).getArtifactType()).isEqualTo("DRAFT_TEXT");
        assertThat(dto.getDraftText()).isEqualTo("draft text");

        ArgumentCaptor<BidAgentRun> runCaptor = ArgumentCaptor.forClass(BidAgentRun.class);
        verify(runRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getProjectName()).isEqualTo("华东智慧园区改造项目");
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(BidAgentRun.Status.DRAFTED);
    }

    @Test
    void reviewCurrentDraft_shouldUpdateStoredRunAndReturnReviewView() throws Exception {
        BidDraftSnapshot snapshot = sampleSnapshot();
        BidAgentRun run = baseRun(snapshot);
        run.setId(100L);
        run.setStatus(BidAgentRun.Status.DRAFTED);
        run.setReviewText("old review");

        BidDraftGenerationResult generationResult = new BidDraftGenerationResult(
                run.getDraftText(),
                "updated review",
                List.of(new GeneratedArtifactSpec("REVIEW_SUMMARY", "草稿审阅摘要", "updated review", "bid-reviewer"))
        );
        BidAgentArtifact reviewArtifact = BidAgentArtifact.builder()
                .id(300L)
                .runId(100L)
                .projectId(11L)
                .artifactType("REVIEW_SUMMARY")
                .title("草稿审阅摘要")
                .content("old review")
                .handoffTarget("bid-reviewer")
                .status(BidAgentArtifact.Status.DRAFTED)
                .createdAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .build();

        when(runRepository.findTopByProjectIdOrderByCreatedAtDesc(11L)).thenReturn(Optional.of(run));
        when(textGenerator.generate(any(), any(), any(), any(), any(), any())).thenReturn(generationResult);
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(artifactRepository.findByRunIdAndArtifactType(100L, "REVIEW_SUMMARY")).thenReturn(Optional.of(reviewArtifact));
        when(artifactRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var reviewDto = appService.reviewCurrentDraft(11L);

        assertThat(reviewDto.getRunId()).isEqualTo(100L);
        assertThat(reviewDto.getReviewSummary()).isEqualTo("updated review");
        assertThat(reviewDto.getNextActions()).isNotEmpty();
        verify(artifactRepository).save(reviewArtifact);
    }

    @Test
    void applyRun_shouldMarkPrimaryArtifactReadyForWriter() throws Exception {
        BidAgentRun run = baseRun(sampleSnapshot());
        run.setId(100L);
        run.setStatus(BidAgentRun.Status.REVIEWED);

        BidAgentArtifact draftArtifact = BidAgentArtifact.builder()
                .id(200L)
                .runId(100L)
                .projectId(11L)
                .artifactType("DRAFT_TEXT")
                .title("自动生成投标草稿")
                .content("draft text")
                .handoffTarget("document-writer")
                .status(BidAgentArtifact.Status.DRAFTED)
                .createdAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .build();
        BidAgentArtifact reviewArtifact = BidAgentArtifact.builder()
                .id(201L)
                .runId(100L)
                .projectId(11L)
                .artifactType("REVIEW_SUMMARY")
                .title("草稿审阅摘要")
                .content("review summary")
                .handoffTarget("bid-reviewer")
                .status(BidAgentArtifact.Status.DRAFTED)
                .createdAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .build();

        when(runRepository.findByIdAndProjectId(100L, 11L)).thenReturn(Optional.of(run));
        when(artifactRepository.findByRunIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(draftArtifact, reviewArtifact));
        when(artifactRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BidDraftAgentApplyResponseDTO response = appService.applyRun(11L, 100L);

        assertThat(response.isReadyForWriter()).isTrue();
        assertThat(response.getArtifactType()).isEqualTo("DRAFT_TEXT");
        assertThat(draftArtifact.getStatus()).isEqualTo(BidAgentArtifact.Status.READY_FOR_WRITER);
        verify(artifactRepository).save(draftArtifact);
    }

    private BidDraftSnapshot sampleSnapshot() {
        return new BidDraftSnapshot(
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
        );
    }

    private BidAgentRun baseRun(BidDraftSnapshot snapshot) throws Exception {
        RequirementClassification classification = new RequirementClassification(
                List.of("价格"),
                List.of("合同"),
                List.of("资质"),
                List.of("技术"),
                List.of("交付"),
                List.of("商务"),
                List.of("项目背景")
        );
        MaterialMatchScore materialMatchScore = new MaterialMatchScore(100, 6, 6,
                List.of("pricing", "legal", "qualification", "technical", "delivery", "commercial"),
                List.of(), List.of(), List.of());
        GapCheckResult gapCheck = new GapCheckResult(true, List.of(), List.of());
        ManualConfirmationDecision manualConfirmation = new ManualConfirmationDecision(true, true, true, List.of("需要人工确认"));
        WriteCoverageDecision writeCoverage = new WriteCoverageDecision(100, true,
                List.of("项目概况", "商务响应"), List.of(), List.of("项目概况"));

        return BidAgentRun.builder()
                .projectId(snapshot.projectId())
                .tenderId(snapshot.tenderId())
                .projectName(snapshot.projectName())
                .tenderTitle(snapshot.tenderTitle())
                .status(BidAgentRun.Status.DRAFTED)
                .snapshotJson(objectMapper().writeValueAsString(snapshot))
                .requirementClassificationJson(objectMapper().writeValueAsString(classification))
                .materialMatchScoreJson(objectMapper().writeValueAsString(materialMatchScore))
                .gapCheckJson(objectMapper().writeValueAsString(gapCheck))
                .manualConfirmationJson(objectMapper().writeValueAsString(manualConfirmation))
                .writeCoverageJson(objectMapper().writeValueAsString(writeCoverage))
                .draftText("draft text")
                .reviewText("review text")
                .generatorKey("deterministic-v1")
                .createdAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 22, 10, 0))
                .build();
    }

    private ObjectMapper objectMapper() {
        return objectMapper;
    }
}
