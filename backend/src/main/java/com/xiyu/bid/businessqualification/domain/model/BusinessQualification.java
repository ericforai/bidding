package com.xiyu.bid.businessqualification.domain.model;

import com.xiyu.bid.businessqualification.domain.service.QualificationExpiryPolicy;
import com.xiyu.bid.businessqualification.domain.service.QualificationLoanPolicy;
import com.xiyu.bid.businessqualification.domain.valueobject.LoanStatus;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationCategory;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationStatus;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationSubject;
import com.xiyu.bid.businessqualification.domain.valueobject.ReminderPolicy;
import com.xiyu.bid.businessqualification.domain.valueobject.ValidityPeriod;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BusinessQualification(
        Long id,
        String name,
        QualificationSubject subject,
        QualificationCategory category,
        String certificateNo,
        String issuer,
        String holderName,
        ValidityPeriod validityPeriod,
        ReminderPolicy reminderPolicy,
        LoanStatus currentBorrowStatus,
        String currentBorrower,
        String currentDepartment,
        String currentProjectId,
        String borrowPurpose,
        LocalDate expectedReturnDate,
        String fileUrl,
        List<QualificationAttachment> attachments
) {

    public QualificationStatus status() {
        return new QualificationExpiryPolicy().evaluate(validityPeriod, LocalDate.now());
    }

    public long remainingDays() {
        return validityPeriod.remainingDays(LocalDate.now());
    }

    public BusinessQualification borrow(
            QualificationLoanPolicy policy,
            String borrower,
            String department,
            String projectId,
            String purpose,
            LocalDate expectedReturnDateValue
    ) {
        policy.ensureCanBorrow(this);
        return BusinessQualification.builder()
                .id(id)
                .name(name)
                .subject(subject)
                .category(category)
                .certificateNo(certificateNo)
                .issuer(issuer)
                .holderName(holderName)
                .validityPeriod(validityPeriod)
                .reminderPolicy(reminderPolicy)
                .currentBorrowStatus(LoanStatus.BORROWED)
                .currentBorrower(borrower)
                .currentDepartment(department)
                .currentProjectId(projectId)
                .borrowPurpose(purpose)
                .expectedReturnDate(expectedReturnDateValue)
                .fileUrl(fileUrl)
                .attachments(attachments)
                .build();
    }

    public BusinessQualification returnBack(
            QualificationLoanPolicy policy,
            QualificationLoan activeLoan
    ) {
        policy.ensureCanReturn(this, activeLoan);
        return BusinessQualification.builder()
                .id(id)
                .name(name)
                .subject(subject)
                .category(category)
                .certificateNo(certificateNo)
                .issuer(issuer)
                .holderName(holderName)
                .validityPeriod(validityPeriod)
                .reminderPolicy(reminderPolicy)
                .currentBorrowStatus(LoanStatus.AVAILABLE)
                .currentBorrower(null)
                .currentDepartment(null)
                .currentProjectId(null)
                .borrowPurpose(null)
                .expectedReturnDate(null)
                .fileUrl(fileUrl)
                .attachments(attachments)
                .build();
    }

    public BusinessQualification recordReminder(LocalDateTime remindedAt) {
        return BusinessQualification.builder()
                .id(id)
                .name(name)
                .subject(subject)
                .category(category)
                .certificateNo(certificateNo)
                .issuer(issuer)
                .holderName(holderName)
                .validityPeriod(validityPeriod)
                .reminderPolicy(reminderPolicy.recordReminder(remindedAt))
                .currentBorrowStatus(currentBorrowStatus)
                .currentBorrower(currentBorrower)
                .currentDepartment(currentDepartment)
                .currentProjectId(currentProjectId)
                .borrowPurpose(borrowPurpose)
                .expectedReturnDate(expectedReturnDate)
                .fileUrl(fileUrl)
                .attachments(attachments)
                .build();
    }
}
