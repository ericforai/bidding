package com.xiyu.bid.businessqualification.domain.service;

import com.xiyu.bid.businessqualification.domain.model.BusinessQualification;
import com.xiyu.bid.businessqualification.domain.model.QualificationLoan;
import com.xiyu.bid.businessqualification.domain.valueobject.LoanStatus;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationStatus;
import com.xiyu.bid.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class QualificationLoanPolicy {

    public void ensureCanBorrow(BusinessQualification qualification) {
        if (qualification.currentBorrowStatus() == LoanStatus.BORROWED) {
            throw new BusinessException(409, "该资质当前已借出，不能重复借阅");
        }
        if (qualification.status() == QualificationStatus.EXPIRED) {
            throw new BusinessException(409, "已过期资质不能借阅");
        }
    }

    public void ensureCanReturn(BusinessQualification qualification, QualificationLoan activeLoan) {
        if (qualification.currentBorrowStatus() != LoanStatus.BORROWED || activeLoan == null) {
            throw new BusinessException(409, "该资质当前没有活动借阅记录");
        }
    }
}
