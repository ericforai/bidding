package com.xiyu.bid.qualification.service;

import com.xiyu.bid.businessqualification.application.service.BorrowQualificationAppService;
import com.xiyu.bid.businessqualification.application.service.CreateQualificationAppService;
import com.xiyu.bid.businessqualification.application.service.DeleteQualificationAppService;
import com.xiyu.bid.businessqualification.application.service.GetQualificationBorrowRecordsAppService;
import com.xiyu.bid.businessqualification.application.service.ListQualificationsAppService;
import com.xiyu.bid.businessqualification.application.service.ReturnQualificationAppService;
import com.xiyu.bid.businessqualification.application.service.ScanExpiringQualificationsAppService;
import com.xiyu.bid.businessqualification.application.service.UpdateQualificationAppService;
import com.xiyu.bid.businessqualification.domain.model.BusinessQualification;
import com.xiyu.bid.businessqualification.domain.model.QualificationLoan;
import com.xiyu.bid.businessqualification.domain.valueobject.LoanStatus;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationCategory;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationSubject;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationSubjectType;
import com.xiyu.bid.businessqualification.domain.valueobject.ReminderPolicy;
import com.xiyu.bid.businessqualification.domain.valueobject.ValidityPeriod;
import com.xiyu.bid.qualification.dto.QualificationBorrowRequest;
import com.xiyu.bid.qualification.dto.QualificationDTO;
import com.xiyu.bid.qualification.dto.QualificationReturnRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QualificationServiceTest {

    @Mock private CreateQualificationAppService createQualificationAppService;
    @Mock private UpdateQualificationAppService updateQualificationAppService;
    @Mock private BorrowQualificationAppService borrowQualificationAppService;
    @Mock private ReturnQualificationAppService returnQualificationAppService;
    @Mock private ListQualificationsAppService listQualificationsAppService;
    @Mock private GetQualificationBorrowRecordsAppService getQualificationBorrowRecordsAppService;
    @Mock private ScanExpiringQualificationsAppService scanExpiringQualificationsAppService;
    @Mock private DeleteQualificationAppService deleteQualificationAppService;
    @Spy private QualificationDtoMapper mapper = new QualificationDtoMapper();

    @InjectMocks
    private QualificationService qualificationService;

    @Test
    @DisplayName("获取资质详情 - 返回到期衍生字段")
    void getQualificationById_ShouldReturnRemainingDaysAndAlertFields() {
        when(listQualificationsAppService.get(3L)).thenReturn(sampleQualification(3L, LocalDate.now().plusDays(20), LoanStatus.AVAILABLE));

        QualificationDTO result = qualificationService.getQualificationById(3L);

        assertThat(result.getRemainingDays()).isEqualTo(20);
        assertThat(result.getAlertLevel()).isEqualTo("warning");
        assertThat(result.getStatus()).isEqualTo("expiring");
    }

    @Test
    @DisplayName("借阅与归还 - 通过新应用服务完成兼容编排")
    void borrowAndReturn_ShouldDelegateToApplicationServices() {
        when(listQualificationsAppService.get(1L)).thenReturn(sampleQualification(1L, LocalDate.now().plusDays(90), LoanStatus.BORROWED));
        when(borrowQualificationAppService.borrow(eq(1L), any()))
                .thenReturn(QualificationLoan.builder()
                        .id(9L)
                        .qualificationId(1L)
                        .borrower("小王")
                        .borrowedAt(LocalDateTime.now())
                        .status(LoanStatus.BORROWED)
                        .build());
        when(returnQualificationAppService.returnLoan(eq(1L), any()))
                .thenReturn(QualificationLoan.builder()
                        .id(9L)
                        .qualificationId(1L)
                        .borrower("小王")
                        .borrowedAt(LocalDateTime.now().minusDays(2))
                        .returnedAt(LocalDateTime.now())
                        .status(LoanStatus.RETURNED)
                        .build());

        var borrowed = qualificationService.borrowQualification(1L, QualificationBorrowRequest.builder().borrower("小王").build());
        var returned = qualificationService.returnQualification(1L, QualificationReturnRequest.builder().returnRemark("已归档").build());

        assertThat(borrowed.getBorrower()).isEqualTo("小王");
        assertThat(returned.getStatus()).isEqualTo("returned");
        verify(borrowQualificationAppService).borrow(eq(1L), any());
        verify(returnQualificationAppService).returnLoan(eq(1L), any());
    }

    @Test
    @DisplayName("借阅记录 - 返回兼容 DTO 列表")
    void getBorrowRecords_ShouldMapCompatibilityPayload() {
        BusinessQualification qualification = sampleQualification(1L, LocalDate.now().plusDays(60), LoanStatus.BORROWED);
        when(listQualificationsAppService.get(1L)).thenReturn(qualification);
        when(getQualificationBorrowRecordsAppService.getBorrowRecords(1L)).thenReturn(List.of(
                QualificationLoan.builder()
                        .id(7L)
                        .qualificationId(1L)
                        .borrower("小王")
                        .borrowedAt(LocalDateTime.now().minusDays(3))
                        .expectedReturnDate(LocalDate.now().plusDays(4))
                        .status(LoanStatus.BORROWED)
                        .build()));

        var records = qualificationService.getBorrowRecords(1L);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getQualificationName()).isEqualTo("高新技术企业证书");
    }

    private BusinessQualification sampleQualification(Long id, LocalDate expiryDate, LoanStatus loanStatus) {
        return BusinessQualification.builder()
                .id(id)
                .name("高新技术企业证书")
                .subject(QualificationSubject.builder().type(QualificationSubjectType.COMPANY).name("西域科技").build())
                .category(QualificationCategory.PRODUCT)
                .certificateNo("GX-001")
                .issuer("科技局")
                .holderName("张三")
                .validityPeriod(ValidityPeriod.builder()
                        .issueDate(LocalDate.now().minusYears(1))
                        .expiryDate(expiryDate)
                        .build())
                .reminderPolicy(ReminderPolicy.builder().enabled(true).reminderDays(30).build())
                .currentBorrowStatus(loanStatus)
                .currentBorrower(loanStatus == LoanStatus.BORROWED ? "小王" : null)
                .attachments(List.of())
                .build();
    }
}
