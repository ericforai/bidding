package com.xiyu.bid.tender.service;

import com.xiyu.bid.ai.service.AiService;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.tender.dto.TenderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenderServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private AiService aiService;

    @InjectMocks
    private TenderService tenderService;

    private Tender tender;
    private TenderDTO tenderDTO;

    @BeforeEach
    void setUp() {
        tender = Tender.builder()
                .id(1L)
                .title("2026年西域数智化采购项目")
                .source("中国招标投标公共服务平台")
                .budget(new BigDecimal("1500000.00"))
                .deadline(LocalDateTime.now().plusDays(20))
                .status(Tender.Status.PENDING)
                .build();

        tenderDTO = TenderDTO.builder()
                .id(1L)
                .title("2026年西域数智化采购项目")
                .source("中国招标投标公共服务平台")
                .budget(new BigDecimal("1500000.00"))
                .deadline(tender.getDeadline())
                .status(Tender.Status.PENDING)
                .build();
    }

    @Test
    @DisplayName("创建标讯 - 成功")
    void createTender_ShouldReturnSavedTender() {
        when(tenderRepository.save(any(Tender.class))).thenReturn(tender);

        TenderDTO savedDto = tenderService.createTender(tenderDTO);

        assertThat(savedDto.getTitle()).isEqualTo(tenderDTO.getTitle());
        verify(tenderRepository, times(1)).save(any(Tender.class));
    }

    @Test
    @DisplayName("根据ID查询标讯 - 成功")
    void getTenderById_ShouldReturnTender() {
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));

        TenderDTO foundDto = tenderService.getTenderById(1L);

        assertThat(foundDto.getId()).isEqualTo(1L);
        assertThat(foundDto.getTitle()).isEqualTo(tender.getTitle());
    }

    @Test
    @DisplayName("根据ID查询标讯 - 未找到抛出异常")
    void getTenderById_NotFound_ShouldThrowException() {
        when(tenderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tenderService.getTenderById(99L));
    }

    @Test
    @DisplayName("更新标讯 - 成功")
    void updateTender_ShouldUpdateAndReturnTender() {
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));
        when(tenderRepository.save(any(Tender.class))).thenReturn(tender);

        TenderDTO updateDto = TenderDTO.builder()
                .title("更新后的项目名称")
                .status(Tender.Status.TRACKING)
                .build();

        TenderDTO result = tenderService.updateTender(1L, updateDto);

        assertThat(result.getTitle()).isEqualTo("更新后的项目名称");
        assertThat(result.getStatus()).isEqualTo(Tender.Status.TRACKING);
    }

    @Test
    @DisplayName("AI分析标讯 - 成功验证准确性流程")
    void analyzeTender_ShouldCallAiServiceAndUpdate() {
        // Arrange
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));
        
        // Mock AI Service analysis
        // In realistic scenario, AiService will update the tender in DB
        // So we need to mock the second findById to return the "analyzed" tender
        Tender analyzedTender = Tender.builder()
                .id(1L)
                .title(tender.getTitle())
                .aiScore(85)
                .riskLevel(Tender.RiskLevel.LOW)
                .build();
        
        when(aiService.analyzeTender(eq(1L), anyMap()))
                .thenReturn(CompletableFuture.completedFuture(null));
        
        // The service calls findById twice: once at the start, once after analysis
        when(tenderRepository.findById(1L))
                .thenReturn(Optional.of(tender)) // First call
                .thenReturn(Optional.of(analyzedTender)); // Second call after analysis

        // Act
        TenderDTO result = tenderService.analyzeTender(1L);

        // Assert
        assertThat(result.getAiScore()).isEqualTo(85);
        assertThat(result.getRiskLevel()).isEqualTo(Tender.RiskLevel.LOW);
        verify(aiService, times(1)).analyzeTender(eq(1L), anyMap());
        verify(tenderRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("AI分析标讯 - 超时或异常处理")
    void analyzeTender_ServiceException_ShouldThrowRuntimeException() {
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tender));
        
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("AI Provider Offline"));
        
        when(aiService.analyzeTender(eq(1L), anyMap())).thenReturn(failedFuture);

        assertThrows(RuntimeException.class, () -> tenderService.analyzeTender(1L));
    }
}
