package com.xiyu.bid.tender.service;

import com.xiyu.bid.ai.service.AiService;
import com.xiyu.bid.batch.repository.TenderAssignmentRecordRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import com.xiyu.bid.task.dto.TaskDTO;
import com.xiyu.bid.task.service.TaskService;
import com.xiyu.bid.tender.dto.TenderAbandonRequest;
import com.xiyu.bid.tender.dto.TenderBidResponse;
import com.xiyu.bid.tender.dto.TenderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderCommandServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenderAssignmentRecordRepository tenderAssignmentRecordRepository;

    @Mock
    private ProjectAccessScopeService projectAccessScopeService;

    @Mock
    private AiService aiService;

    @Mock
    private TaskService taskService;

    @Mock
    private TenderAssignmentPermissions tenderAssignmentPermissions;

    private TenderCommandService tenderCommandService;
    private TenderQueryService tenderQueryService;
    private com.xiyu.bid.batch.core.TenderStatusTransitionPolicy statusTransitionPolicy;
    private Tender tender;
    private TenderDTO tenderDTO;

    @BeforeEach
    void setUp() {
        TenderMapper tenderMapper = new TenderMapper();
        TenderProjectAccessGuard accessGuard = new TenderProjectAccessGuard(projectRepository, projectAccessScopeService);
        statusTransitionPolicy = new com.xiyu.bid.batch.core.TenderStatusTransitionPolicy();
        tenderCommandService = new TenderCommandService(
                tenderRepository, aiService, tenderMapper, accessGuard,
                statusTransitionPolicy, taskService, tenderAssignmentPermissions);
        // Permissive by default — individual instance-permission tests override.
        org.mockito.Mockito.lenient()
                .when(tenderAssignmentPermissions.canDecide(any(), any()))
                .thenReturn(true);
        tenderQueryService = new TenderQueryService(tenderRepository, tenderMapper, accessGuard,
                projectRepository, userRepository, tenderAssignmentRecordRepository);

        tender = Tender.builder()
                .id(1L)
                .title("2026年西域数智化采购项目")
                .source("外部标讯聚合平台")
                .budget(new BigDecimal("1500000.00"))
                .region("上海")
                .industry("数据中心")
                .tenderAgency("上海招标代理有限公司")
                .purchaserName("上海西域采购中心")
                .purchaserHash("hash-shanghai-xiyu")
                .publishDate(LocalDate.of(2026, 4, 21))
                .deadline(LocalDateTime.now().plusDays(20))
                .bidOpeningTime(LocalDateTime.now().plusDays(22))
                .status(Tender.Status.PENDING_ASSIGNMENT)
                .contactName("王经理")
                .contactPhone("13800138000")
                .customerType("KA 客户")
                .priority("A")
                .description("采购 GPU 服务器")
                .tags("数据中心,GPU")
                .build();

        tenderDTO = TenderDTO.builder()
                .id(1L)
                .title("2026年西域数智化采购项目")
                .source("外部标讯聚合平台")
                .budget(new BigDecimal("1500000.00"))
                .region("上海")
                .industry("数据中心")
                .tenderAgency("上海招标代理有限公司")
                .purchaserName("上海西域采购中心")
                .sourceDocumentName("招标文件.pdf")
                .sourceDocumentFileType("application/pdf")
                .sourceDocumentFileUrl("doc-insight://TENDER_INTAKE/manual-tender/hash-招标文件.pdf")
                .publishDate(LocalDate.of(2026, 4, 21))
                .deadline(tender.getDeadline())
                .bidOpeningTime(tender.getBidOpeningTime())
                .customerType("KA 客户")
                .priority("A")
                .status(Tender.Status.PENDING_ASSIGNMENT)
                .tags(java.util.List.of("数据中心", "GPU"))
                .build();
    }

    @Test
    @DisplayName("创建标讯 - 保存真实检索字段并生成采购方哈希")
    void createTender_ShouldReturnSavedTender() {
        when(tenderRepository.save(any(Tender.class))).thenAnswer(invocation -> {
            Tender saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TenderDTO savedDto = tenderCommandService.createTender(tenderDTO);

        assertThat(savedDto.getTitle()).isEqualTo(tenderDTO.getTitle());
        assertThat(savedDto.getRegion()).isEqualTo("上海");
        assertThat(savedDto.getIndustry()).isEqualTo("数据中心");
        assertThat(savedDto.getTenderAgency()).isEqualTo("上海招标代理有限公司");
        assertThat(savedDto.getBidOpeningTime()).isEqualTo(tender.getBidOpeningTime());
        assertThat(savedDto.getCustomerType()).isEqualTo("KA 客户");
        assertThat(savedDto.getPriority()).isEqualTo("A");
        assertThat(savedDto.getPurchaserHash()).isNotBlank();
        assertThat(savedDto.getSourceDocumentFileUrl())
                .isEqualTo("doc-insight://TENDER_INTAKE/manual-tender/hash-招标文件.pdf");
        assertThat(savedDto.getTags()).containsExactly("数据中心", "GPU");
        verify(tenderRepository, times(1)).save(any(Tender.class));
    }

    @Test
    @DisplayName("根据ID查询标讯 - 成功")
    void getTenderById_ShouldReturnTender() {
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));
        when(projectRepository.findByTenderId(1L)).thenReturn(java.util.List.of());
        when(tenderAssignmentRecordRepository.findByTenderIdOrderByAssignedAtDesc(1L)).thenReturn(java.util.List.of());

        TenderDTO foundDto = tenderQueryService.getTenderById(1L);

        assertThat(foundDto.getId()).isEqualTo(1L);
        assertThat(foundDto.getTitle()).isEqualTo(tender.getTitle());
        assertThat(foundDto.getPurchaserName()).isEqualTo("上海西域采购中心");
        assertThat(foundDto.getTags()).containsExactly("数据中心", "GPU");
    }

    @Test
    @DisplayName("根据ID查询标讯 - 关联项目不可见时拒绝")
    void getTenderById_ShouldRejectTenderLinkedToInvisibleProject() {
        Project project = Project.builder().id(100L).tenderId(1L).build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));
        when(projectRepository.findByTenderId(1L)).thenReturn(java.util.List.of(project));
        doThrow(new org.springframework.security.access.AccessDeniedException("权限不足"))
                .when(projectAccessScopeService).assertCurrentUserCanAccessProject(100L);

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> tenderQueryService.getTenderById(1L));
    }

    @Test
    @DisplayName("根据ID查询标讯 - 未找到抛出异常")
    void getTenderById_NotFound_ShouldThrowException() {
        when(tenderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tenderQueryService.getTenderById(99L));
    }

    @Test
    @DisplayName("更新标讯 - 成功")
    void updateTender_ShouldUpdateAndReturnTender() {
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenderDTO updateDto = TenderDTO.builder()
                .title("更新后的项目名称")
                .status(Tender.Status.TRACKING)
                .region("北京")
                .tags(java.util.List.of("更新", "重点"))
                .build();

        TenderDTO result = tenderCommandService.updateTender(1L, updateDto);

        assertThat(result.getTitle()).isEqualTo("更新后的项目名称");
        assertThat(result.getStatus()).isEqualTo(Tender.Status.TRACKING);
        assertThat(result.getRegion()).isEqualTo("北京");
        assertThat(result.getTags()).containsExactly("更新", "重点");
    }

    @Test
    @DisplayName("更新标讯 - 关联项目不可见时不保存")
    void updateTender_ShouldRejectTenderLinkedToInvisibleProject() {
        Project project = Project.builder().id(100L).tenderId(1L).build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));
        when(projectRepository.findByTenderId(1L)).thenReturn(java.util.List.of(project));
        doThrow(new org.springframework.security.access.AccessDeniedException("权限不足"))
                .when(projectAccessScopeService).assertCurrentUserCanAccessProject(100L);

        TenderDTO updateDto = TenderDTO.builder().title("更新后的项目名称").build();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> tenderCommandService.updateTender(1L, updateDto));
        verify(tenderRepository, never()).save(any(Tender.class));
    }

    @Test
    @DisplayName("AI分析标讯 - 携带真实字段上下文")
    void analyzeTender_ShouldCallAiServiceAndUpdate() {
        Tender analyzedTender = Tender.builder()
                .id(1L)
                .title(tender.getTitle())
                .aiScore(85)
                .riskLevel(Tender.RiskLevel.LOW)
                .build();

        when(aiService.analyzeTender(eq(1L), anyMap()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(tenderRepository.findById(1L))
                .thenReturn(Optional.of(tender))
                .thenReturn(Optional.of(analyzedTender));

        TenderDTO result = tenderCommandService.analyzeTender(1L);

        assertThat(result.getAiScore()).isEqualTo(85);
        assertThat(result.getRiskLevel()).isEqualTo(Tender.RiskLevel.LOW);
        verify(aiService, times(1)).analyzeTender(eq(1L), org.mockito.ArgumentMatchers.argThat(context ->
                context.containsKey("region")
                        && context.containsKey("industry")
                        && context.containsKey("tenderAgency")
                        && context.containsKey("bidOpeningTime")
                        && context.containsKey("customerType")
                        && context.containsKey("priority")
                        && context.containsKey("purchaserName")
                        && context.containsKey("description")
        ));
        verify(tenderRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("AI分析标讯 - 超时或异常处理")
    void analyzeTender_ServiceException_ShouldThrowRuntimeException() {
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("AI Provider Offline"));

        when(aiService.analyzeTender(eq(1L), anyMap())).thenReturn(failedFuture);

        assertThrows(RuntimeException.class, () -> tenderCommandService.analyzeTender(1L));
    }

    // ========== 投标/弃标 测试用例 ==========

    @Test
    @DisplayName("投标 - 成功投标并创建待办")
    void participateBid_Success() {
        Tender pendingTender = Tender.builder()
                .id(1L).title("测试标讯").budget(new BigDecimal("100.00")).status(Tender.Status.PENDING_ASSIGNMENT).build();
        TaskDTO createdTask = TaskDTO.builder().id(100L).title("【待立项】测试标讯").status(Task.Status.TODO).build();

        when(tenderRepository.findById(1L)).thenReturn(Optional.of(pendingTender));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskService.createTask(any(TaskDTO.class))).thenReturn(createdTask);

        TenderBidResponse response = tenderCommandService.participateBid(1L, 10L);

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getMessage()).isEqualTo("投标成功，已生成项目立项待办");
        assertThat(response.getTodoId()).isEqualTo(100L);
        verify(taskService).createTask(org.mockito.ArgumentMatchers.argThat(task ->
                task.getTitle().contains("【待立项】")
                && task.getAssigneeId() == 10L
                && task.getPriority() == Task.Priority.HIGH
        ));
    }

    @Test
    @DisplayName("投标 - 已投标状态返回失败")
    void participateBid_AlreadyBidded() {
        Tender biddenTender = Tender.builder().id(1L).title("测试标讯").status(Tender.Status.BIDDING).build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(biddenTender));

        TenderBidResponse response = tenderCommandService.participateBid(1L, 10L);

        assertThat(response.isAccepted()).isFalse();
        assertThat(response.getMessage()).isEqualTo("该标讯已投标");
        verify(taskService, never()).createTask(any());
    }

    @Test
    @DisplayName("投标 - 已弃标状态无法投标")
    void participateBid_AbandonedCannotBid() {
        Tender abandonedTender = Tender.builder().id(1L).title("测试标讯").status(Tender.Status.ABANDONED).build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(abandonedTender));

        TenderBidResponse response = tenderCommandService.participateBid(1L, 10L);

        assertThat(response.isAccepted()).isFalse();
        assertThat(response.getMessage()).isEqualTo("该标讯已放弃，无法投标");
        verify(taskService, never()).createTask(any());
    }

    @Test
    @DisplayName("弃标 - 成功弃标")
    void abandonBid_Success() {
        Tender trackingTender = Tender.builder().id(1L).title("测试标讯").status(Tender.Status.TRACKING).build();
        TenderAbandonRequest req = TenderAbandonRequest.builder().reason("预算超出预期").build();

        when(tenderRepository.findById(1L)).thenReturn(Optional.of(trackingTender));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(inv -> inv.getArgument(0));

        TenderBidResponse response = tenderCommandService.abandonBid(1L, req, 10L);

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getMessage()).isEqualTo("已放弃该标讯");
        verify(tenderRepository).save(org.mockito.ArgumentMatchers.argThat(t -> t.getStatus() == Tender.Status.ABANDONED));
    }

    @Test
    @DisplayName("弃标 - 已弃标状态返回失败")
    void abandonBid_AlreadyAbandoned() {
        Tender abandonedTender = Tender.builder().id(1L).title("测试标讯").status(Tender.Status.ABANDONED).build();
        TenderAbandonRequest req = TenderAbandonRequest.builder().reason("测试原因").build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(abandonedTender));

        TenderBidResponse response = tenderCommandService.abandonBid(1L, req, 10L);

        assertThat(response.isAccepted()).isFalse();
        assertThat(response.getMessage()).isEqualTo("该标讯已放弃");
        verify(tenderRepository, never()).save(any());
    }

    @Test
    @DisplayName("弃标 - 已投标状态无法弃标")
    void abandonBid_BiddedCannotAbandon() {
        Tender biddenTender = Tender.builder().id(1L).title("测试标讯").status(Tender.Status.BIDDING).build();
        TenderAbandonRequest req = TenderAbandonRequest.builder().reason("测试原因").build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(biddenTender));

        TenderBidResponse response = tenderCommandService.abandonBid(1L, req, 10L);

        assertThat(response.isAccepted()).isFalse();
        assertThat(response.getMessage()).isEqualTo("该标讯已投标，无法弃标");
        verify(tenderRepository, never()).save(any());
    }

    // ========== 实例级权限 (canDecide) 测试用例 ==========

    @Test
    @DisplayName("投标 - 非分配人（canDecide=false）抛 AccessDeniedException，不写库")
    void participateBid_nonAssigner_throwsForbidden() {
        Tender pendingTender = Tender.builder()
                .id(1L).title("测试标讯").budget(new BigDecimal("100.00"))
                .status(Tender.Status.PENDING_ASSIGNMENT).build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(pendingTender));
        when(tenderAssignmentPermissions.canDecide(1L, 999L)).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> tenderCommandService.participateBid(1L, 999L));

        verify(tenderRepository, never()).save(any());
        verify(taskService, never()).createTask(any());
    }

    @Test
    @DisplayName("投标 - 分配人（canDecide=true）正常完成")
    void participateBid_authorizedAssigner_succeeds() {
        Tender pendingTender = Tender.builder()
                .id(1L).title("测试标讯").budget(new BigDecimal("100.00"))
                .status(Tender.Status.PENDING_ASSIGNMENT).build();
        TaskDTO createdTask = TaskDTO.builder().id(100L).title("【待立项】测试标讯").status(Task.Status.TODO).build();

        when(tenderRepository.findById(1L)).thenReturn(Optional.of(pendingTender));
        when(tenderAssignmentPermissions.canDecide(1L, 5L)).thenReturn(true);
        when(tenderRepository.save(any(Tender.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskService.createTask(any(TaskDTO.class))).thenReturn(createdTask);

        TenderBidResponse response = tenderCommandService.participateBid(1L, 5L);

        assertThat(response.isAccepted()).isTrue();
        verify(taskService).createTask(any(TaskDTO.class));
    }

    @Test
    @DisplayName("弃标 - 非分配人（canDecide=false）抛 AccessDeniedException，不写库")
    void abandonBid_nonAssigner_throwsForbidden() {
        Tender trackingTender = Tender.builder()
                .id(1L).title("测试标讯").status(Tender.Status.TRACKING).build();
        TenderAbandonRequest req = TenderAbandonRequest.builder().reason("预算不足").build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(trackingTender));
        when(tenderAssignmentPermissions.canDecide(1L, 999L)).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> tenderCommandService.abandonBid(1L, req, 999L));

        verify(tenderRepository, never()).save(any());
    }

    @Test
    @DisplayName("弃标 - 分配人（canDecide=true）正常完成")
    void abandonBid_authorizedAssigner_succeeds() {
        Tender trackingTender = Tender.builder()
                .id(1L).title("测试标讯").status(Tender.Status.TRACKING).build();
        TenderAbandonRequest req = TenderAbandonRequest.builder().reason("预算不足").build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(trackingTender));
        when(tenderAssignmentPermissions.canDecide(1L, 5L)).thenReturn(true);
        when(tenderRepository.save(any(Tender.class))).thenAnswer(inv -> inv.getArgument(0));

        TenderBidResponse response = tenderCommandService.abandonBid(1L, req, 5L);

        assertThat(response.isAccepted()).isTrue();
        verify(tenderRepository).save(any(Tender.class));
    }
}
