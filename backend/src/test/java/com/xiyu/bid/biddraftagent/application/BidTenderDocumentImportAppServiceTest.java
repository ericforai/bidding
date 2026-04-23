package com.xiyu.bid.biddraftagent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementItemSnapshot;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentApplyResponseDTO;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentRunDTO;
import com.xiyu.bid.biddraftagent.entity.BidRequirementItem;
import com.xiyu.bid.biddraftagent.entity.BidTenderDocumentSnapshot;
import com.xiyu.bid.biddraftagent.repository.BidRequirementItemRepository;
import com.xiyu.bid.biddraftagent.repository.BidTenderDocumentSnapshotRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BidTenderDocumentImportAppServiceTest {

    @Mock
    private ProjectAccessScopeService projectAccessScopeService;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TenderRepository tenderRepository;
    @Mock
    private ProjectDocumentRepository projectDocumentRepository;
    @Mock
    private BidRequirementItemRepository requirementItemRepository;
    @Mock
    private BidTenderDocumentSnapshotRepository documentSnapshotRepository;
    @Mock
    private TenderDocumentStorage documentStorage;
    @Mock
    private TenderDocumentTextExtractor textExtractor;
    @Mock
    private TenderDocumentAnalyzer documentAnalyzer;
    @Mock
    private BidDraftAgentAppService bidDraftAgentAppService;
    @Mock
    private BidAgentOperatorResolver operatorResolver;
    @Mock
    private TransactionTemplate transactionTemplate;

    private BidTenderDocumentImportAppService appService;

    @BeforeEach
    void setUp() {
        BidDraftAgentJsonCodec jsonCodec = new BidDraftAgentJsonCodec(new ObjectMapper().findAndRegisterModules());
        appService = new BidTenderDocumentImportAppService(
                projectAccessScopeService,
                projectRepository,
                tenderRepository,
                projectDocumentRepository,
                requirementItemRepository,
                documentSnapshotRepository,
                documentStorage,
                textExtractor,
                documentAnalyzer,
                new TenderRequirementSnapshotUpdater(),
                new TenderRequirementEntityFactory(),
                jsonCodec,
                bidDraftAgentAppService,
                operatorResolver,
                transactionTemplate
        );
    }

    @Test
    void importAndGenerate_shouldPersistRequirementsUpdateTenderCreateRunAndApply() {
        allowTransactionCallbacks();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "招标文件.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "资格要求：提供资质证书\n技术要求：提供实施方案\n评分标准：技术方案50分".getBytes(StandardCharsets.UTF_8)
        );
        Project project = Project.builder().id(11L).tenderId(22L).name("华东智慧园区改造项目").managerId(1L).build();
        Tender tender = Tender.builder().id(22L).title("旧标题").description("旧描述").tags("旧标签").build();
        TenderRequirementProfile profile = sampleProfile();

        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        when(tenderRepository.findById(22L)).thenReturn(Optional.of(tender));
        when(documentStorage.store(eq(11L), eq("招标文件.docx"), any(), any()))
                .thenReturn(new StoredTenderDocument("bid-agent://tender-documents/11/file", "/tmp/file", "abc"));
        when(textExtractor.extract(eq("招标文件.docx"), any(), any()))
                .thenReturn(new ExtractedTenderDocument("招标文件.docx", file.getContentType(), "抽取正文", 4, "test-extractor"));
        when(documentAnalyzer.analyze(any())).thenReturn(profile);
        when(operatorResolver.currentOperator()).thenReturn(new BidAgentOperator(7L, "张经理"));
        when(projectDocumentRepository.save(any())).thenAnswer(invocation -> {
            ProjectDocument document = invocation.getArgument(0);
            document.setId(501L);
            return document;
        });
        when(documentSnapshotRepository.save(any())).thenAnswer(invocation -> {
            BidTenderDocumentSnapshot snapshot = invocation.getArgument(0);
            snapshot.setId(601L);
            return snapshot;
        });
        when(requirementItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bidDraftAgentAppService.createRun(11L)).thenReturn(BidDraftAgentRunDTO.builder().id(701L).projectId(11L).build());
        when(bidDraftAgentAppService.applyRun(11L, 701L)).thenReturn(BidDraftAgentApplyResponseDTO.builder().runId(701L).readyForWriter(true).build());

        var result = appService.importAndGenerate(11L, file, true);

        assertThat(result.isAppliedToEditor()).isTrue();
        assertThat(result.getDocument().getSnapshotId()).isEqualTo(601L);
        assertThat(result.getRun().getId()).isEqualTo(701L);
        assertThat(tender.getTitle()).isEqualTo("2026园区改造招标公告");
        assertThat(tender.getDescription()).contains("资格要求", "评分标准", "必须提供的材料");
        assertThat(tender.getTags()).contains("智慧园区");

        ArgumentCaptor<ProjectDocument> documentCaptor = ArgumentCaptor.forClass(ProjectDocument.class);
        ArgumentCaptor<List<BidRequirementItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(projectAccessScopeService).assertCurrentUserCanAccessProject(11L);
        verify(projectDocumentRepository).save(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getFileType()).isEqualTo("docx");
        verify(requirementItemRepository).saveAll(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).hasSize(2);
        assertThat(itemCaptor.getValue()).extracting(BidRequirementItem::getCategory)
                .contains("qualification", "technical");
        verify(bidDraftAgentAppService).createRun(11L);
        verify(bidDraftAgentAppService).applyRun(11L, 701L);
    }

    @Test
    void importAndGenerate_shouldKeepParsedSnapshotWhenRunGenerationFails() {
        allowTransactionCallbacks();
        MockMultipartFile file = sampleFile();
        Project project = Project.builder().id(11L).tenderId(22L).name("华东智慧园区改造项目").managerId(1L).build();
        Tender tender = Tender.builder().id(22L).title("旧标题").description("旧描述").tags("旧标签").build();

        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        when(tenderRepository.findById(22L)).thenReturn(Optional.of(tender));
        when(documentStorage.store(eq(11L), eq("招标文件.docx"), any(), any()))
                .thenReturn(new StoredTenderDocument("bid-agent://tender-documents/11/file", "/tmp/file", "abc"));
        when(textExtractor.extract(eq("招标文件.docx"), any(), any()))
                .thenReturn(new ExtractedTenderDocument("招标文件.docx", file.getContentType(), "抽取正文", 4, "test-extractor"));
        when(documentAnalyzer.analyze(any())).thenReturn(sampleProfile());
        when(operatorResolver.currentOperator()).thenReturn(new BidAgentOperator(7L, "张经理"));
        when(projectDocumentRepository.save(any())).thenAnswer(invocation -> {
            ProjectDocument document = invocation.getArgument(0);
            document.setId(501L);
            return document;
        });
        when(documentSnapshotRepository.save(any())).thenAnswer(invocation -> {
            BidTenderDocumentSnapshot snapshot = invocation.getArgument(0);
            snapshot.setId(601L);
            return snapshot;
        });
        when(requirementItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bidDraftAgentAppService.createRun(11L)).thenThrow(new IllegalStateException("provider unavailable"));

        var result = appService.importAndGenerate(11L, file, true);

        assertThat(result.getDocument().getSnapshotId()).isEqualTo(601L);
        assertThat(result.getRun()).isNull();
        assertThat(result.isAppliedToEditor()).isFalse();
        assertThat(result.getMessage()).contains("招标文件已解析并保存", "标书初稿生成失败");
        verify(projectDocumentRepository).save(any());
        verify(documentSnapshotRepository).save(any());
        verify(requirementItemRepository).saveAll(any());
        verify(bidDraftAgentAppService, never()).applyRun(any(), any());
    }

    @Test
    void importAndGenerate_shouldKeepRunWhenEditorApplyFails() {
        allowTransactionCallbacks();
        MockMultipartFile file = sampleFile();
        Project project = Project.builder().id(11L).tenderId(22L).name("华东智慧园区改造项目").managerId(1L).build();
        Tender tender = Tender.builder().id(22L).title("旧标题").description("旧描述").tags("旧标签").build();

        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        when(tenderRepository.findById(22L)).thenReturn(Optional.of(tender));
        when(documentStorage.store(eq(11L), eq("招标文件.docx"), any(), any()))
                .thenReturn(new StoredTenderDocument("bid-agent://tender-documents/11/file", "/tmp/file", "abc"));
        when(textExtractor.extract(eq("招标文件.docx"), any(), any()))
                .thenReturn(new ExtractedTenderDocument("招标文件.docx", file.getContentType(), "抽取正文", 4, "test-extractor"));
        when(documentAnalyzer.analyze(any())).thenReturn(sampleProfile());
        when(operatorResolver.currentOperator()).thenReturn(new BidAgentOperator(7L, "张经理"));
        when(projectDocumentRepository.save(any())).thenAnswer(invocation -> {
            ProjectDocument document = invocation.getArgument(0);
            document.setId(501L);
            return document;
        });
        when(documentSnapshotRepository.save(any())).thenAnswer(invocation -> {
            BidTenderDocumentSnapshot snapshot = invocation.getArgument(0);
            snapshot.setId(601L);
            return snapshot;
        });
        when(requirementItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bidDraftAgentAppService.createRun(11L)).thenReturn(BidDraftAgentRunDTO.builder().id(701L).projectId(11L).build());
        when(bidDraftAgentAppService.applyRun(11L, 701L)).thenThrow(new IllegalStateException("editor locked"));

        var result = appService.importAndGenerate(11L, file, true);

        assertThat(result.getRun().getId()).isEqualTo(701L);
        assertThat(result.getApplyResult()).isNull();
        assertThat(result.isAppliedToEditor()).isFalse();
        assertThat(result.getMessage()).contains("招标文件已解析并生成初稿", "写入文档编辑器失败");
    }

    @Test
    void importAndGenerate_shouldStopBeforeReadingFileWhenProjectAccessDenied() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "招标文件.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "正文".getBytes(StandardCharsets.UTF_8)
        );
        doThrow(new AccessDeniedException("权限不足"))
                .when(projectAccessScopeService).assertCurrentUserCanAccessProject(11L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> appService.importAndGenerate(11L, file, true))
                .isInstanceOf(AccessDeniedException.class);

        verify(documentStorage, never()).store(any(), any(), any(), any());
        verify(textExtractor, never()).extract(any(), any(), any());
        verify(bidDraftAgentAppService, never()).createRun(any());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void allowTransactionCallbacks() {
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    private MockMultipartFile sampleFile() {
        return new MockMultipartFile(
                "file",
                "招标文件.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "资格要求：提供资质证书\n技术要求：提供实施方案\n评分标准：技术方案50分".getBytes(StandardCharsets.UTF_8)
        );
    }

    private TenderRequirementProfile sampleProfile() {
        return new TenderRequirementProfile(
                "华东智慧园区改造项目",
                "2026园区改造招标公告",
                "园区数字化改造",
                "上海采购集团",
                List.of("提供有效资质证书"),
                List.of("提供实施方案"),
                List.of("响应付款和交付条款"),
                List.of("技术方案50分"),
                "2026-05-30",
                List.of("投标函", "授权书"),
                List.of("报价和法务条款需人工确认"),
                List.of("智慧园区", "数字化"),
                List.of(
                        new TenderRequirementItemSnapshot("qualification", "资质证书", "提供有效资质证书", true, "资格要求", 95),
                        new TenderRequirementItemSnapshot("technical", "实施方案", "提供实施、运维和培训方案", true, "技术要求", 92)
                )
        );
    }
}
