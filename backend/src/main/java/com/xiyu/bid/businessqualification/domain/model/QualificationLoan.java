package com.xiyu.bid.businessqualification.domain.model;

import com.xiyu.bid.businessqualification.domain.valueobject.LoanStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class QualificationLoan {
    Long id;
    Long qualificationId;
    String borrower;
    String department;
    String projectId;
    String purpose;
    String remark;
    LocalDateTime borrowedAt;
    LocalDate expectedReturnDate;
    LocalDateTime returnedAt;
    String returnRemark;
    LoanStatus status;

    public QualificationLoan markReturned(LocalDateTime returnedAtValue, String returnRemarkValue) {
        return toBuilder()
                .returnedAt(returnedAtValue)
                .returnRemark(returnRemarkValue)
                .status(LoanStatus.RETURNED)
                .build();
    }
}
