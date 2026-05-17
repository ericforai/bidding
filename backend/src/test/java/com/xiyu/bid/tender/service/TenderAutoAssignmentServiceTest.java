package com.xiyu.bid.tender.service;

import com.xiyu.bid.crm.application.CrmProjectClient;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.crm.domain.AssignmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderAutoAssignmentServiceTest {

    @Mock
    private CrmProjectClient crmProjectClient;

    private TenderAutoAssignmentService autoAssignmentService;
    private Tender tender;

    @BeforeEach
    void setUp() {
        autoAssignmentService = new TenderAutoAssignmentService(crmProjectClient);
        tender = Tender.builder()
                .id(1L)
                .title("测试标讯")
                .purchaserName("上海西域采购中心")
                .budget(new BigDecimal("150.00"))
                .publishDate(LocalDate.now())
                .deadline(LocalDateTime.now().plusDays(20))
                .status(Tender.Status.PENDING_ASSIGNMENT)
                .build();
    }

    @Test
    @DisplayName("根据业主单位匹配成功 - 返回负责人信息")
    void tryAutoAssign_MatchFound_ShouldReturnManagerInfo() {
        when(crmProjectClient.findProjectByPurchaser("上海西域采购中心"))
                .thenReturn(AssignmentResult.success(
                        "CRM-001", "PM-001", "张三", "DEPT-001", "销售部"));

        AssignmentResult result = autoAssignmentService.tryAutoAssign(tender);

        assertThat(result.isMatched()).isTrue();
        assertThat(result.crmProjectId()).isEqualTo("CRM-001");
        assertThat(result.projectManagerId()).isEqualTo("PM-001");
        assertThat(result.projectManagerName()).isEqualTo("张三");
        assertThat(result.departmentId()).isEqualTo("DEPT-001");
        assertThat(result.departmentName()).isEqualTo("销售部");
        verify(crmProjectClient).findProjectByPurchaser("上海西域采购中心");
    }

    @Test
    @DisplayName("根据业主单位匹配失败 - 返回 noMatch")
    void tryAutoAssign_NoMatch_ShouldReturnNoMatch() {
        when(crmProjectClient.findProjectByPurchaser("上海西域采购中心"))
                .thenReturn(AssignmentResult.noMatch());

        AssignmentResult result = autoAssignmentService.tryAutoAssign(tender);

        assertThat(result.isMatched()).isFalse();
        assertThat(result.crmProjectId()).isNull();
        assertThat(result.projectManagerId()).isNull();
        assertThat(result.projectManagerName()).isNull();
    }

    @Test
    @DisplayName("purchaserName 为空 - 返回 noMatch")
    void tryAutoAssign_BlankPurchaserName_ShouldReturnNoMatch() {
        tender.setPurchaserName(null);

        AssignmentResult result = autoAssignmentService.tryAutoAssign(tender);

        assertThat(result.isMatched()).isFalse();
    }

    @Test
    @DisplayName("tender 为空 - 返回 noMatch")
    void tryAutoAssign_NullTender_ShouldReturnNoMatch() {
        AssignmentResult result = autoAssignmentService.tryAutoAssign(null);

        assertThat(result.isMatched()).isFalse();
    }

    @Test
    @DisplayName("purchaserName 前后有空格 - 自动 trim 后匹配")
    void tryAutoAssign_TrimmedPurchaserName_ShouldMatch() {
        tender.setPurchaserName("  上海西域采购中心  ");
        when(crmProjectClient.findProjectByPurchaser("上海西域采购中心"))
                .thenReturn(AssignmentResult.success(null, null, "李四", null, null));

        AssignmentResult result = autoAssignmentService.tryAutoAssign(tender);

        assertThat(result.isMatched()).isTrue();
        assertThat(result.projectManagerName()).isEqualTo("李四");
    }

    @Test
    @DisplayName("autoAssignIfPossible_匹配成功返回 true")
    void autoAssignIfPossible_Match_ShouldReturnTrue() {
        when(crmProjectClient.findProjectByPurchaser("上海西域采购中心"))
                .thenReturn(AssignmentResult.success(null, null, "王五", null, null));

        boolean result = autoAssignmentService.autoAssignIfPossible(tender);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("autoAssignIfPossible_匹配失败返回 false")
    void autoAssignIfPossible_NoMatch_ShouldReturnFalse() {
        when(crmProjectClient.findProjectByPurchaser(anyString()))
                .thenReturn(AssignmentResult.noMatch());

        boolean result = autoAssignmentService.autoAssignIfPossible(tender);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("autoAssignIfPossible_tender 为空返回 false")
    void autoAssignIfPossible_NullTender_ShouldReturnFalse() {
        boolean result = autoAssignmentService.autoAssignIfPossible(null);

        assertThat(result).isFalse();
    }
}
