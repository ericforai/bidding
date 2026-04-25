package com.xiyu.bid.tender.service;

import com.xiyu.bid.ai.service.AiService;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
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
    private ProjectAccessScopeService projectAccessScopeService;

    @Mock
    private AiService aiService;

    private TenderCommandService tenderCommandService;
    private TenderQueryService tenderQueryService;
    private Tender tender;
    private TenderDTO tenderDTO;

    @BeforeEach
    void setUp() {
        TenderMapper tenderMapper = new TenderMapper();
        TenderProjectAccessGuard accessGuard = new TenderProjectAccessGuard(projectRepository, projectAccessScopeService);
        tenderCommandService = new TenderCommandService(tenderRepository, aiService, tenderMapper, accessGuard);
        tenderQueryService = new TenderQueryService(tenderRepository, tenderMapper, accessGuard);

        tender = Tender.builder()
                .id(1L)
                .title("2026年西域数智化采购项目")
                .source("外部标讯聚合平台")
                .budget(new BigDecimal("1500000.00"))
                .region("上海")
                .industry("数据中心")
                .purchaserName("上海西域采购中心")
                .purchaserHash("hash-shanghai-xiyu")
                .publishDate(LocalDate.of(2026, 4, 21))
                .deadline(LocalDateTime.now().plusDays(20))
                .status(Tender.Status.PENDING)
                .contactName("王经理")
                .contactPhone("13800138000")
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
                .purchaserName("上海西域采购中心")
                .publishDate(LocalDate.of(2026, 4, 21))
                .deadline(tender.getDeadline())
                .status(Tender.Status.PENDING)
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
        assertThat(savedDto.getPurchaserHash()).isNotBlank();
        assertThat(savedDto.getTags()).containsExactly("数据中心", "GPU");
        verify(tenderRepository, times(1)).save(any(Tender.class));
    }

    @Test
    @DisplayName("根据ID查询标讯 - 成功")
    void getTenderById_ShouldReturnTender() {
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));

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
}
