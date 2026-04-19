package com.xiyu.bid.businessqualification.application.service;

import com.xiyu.bid.businessqualification.application.command.QualificationBorrowCommand;
import com.xiyu.bid.businessqualification.domain.model.BusinessQualification;
import com.xiyu.bid.businessqualification.domain.model.QualificationLoan;
import com.xiyu.bid.businessqualification.domain.port.BusinessQualificationRepository;
import com.xiyu.bid.businessqualification.domain.port.QualificationLoanRecordRepository;
import com.xiyu.bid.businessqualification.domain.service.QualificationLoanPolicy;
import com.xiyu.bid.businessqualification.domain.valueobject.LoanStatus;
import com.xiyu.bid.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BorrowQualificationAppService {

    private final BusinessQualificationRepository qualificationRepository;
    private final QualificationLoanRecordRepository loanRecordRepository;
    private final QualificationLoanPolicy loanPolicy;

    @Transactional
    public QualificationLoan borrow(Long qualificationId, QualificationBorrowCommand command) {
        BusinessQualification qualification = qualificationRepository.findById(qualificationId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessQualification", String.valueOf(qualificationId)));

        BusinessQualification borrowedQualification = qualification.borrow(
                loanPolicy,
                command.getBorrower(),
                command.getDepartment(),
                command.getProjectId(),
                command.getPurpose(),
                command.getExpectedReturnDate());

        qualificationRepository.save(borrowedQualification);

        QualificationLoan loan = QualificationLoan.builder()
                .qualificationId(qualificationId)
                .borrower(command.getBorrower())
                .department(command.getDepartment())
                .projectId(command.getProjectId())
                .purpose(command.getPurpose())
                .remark(command.getRemark())
                .borrowedAt(LocalDateTime.now())
                .expectedReturnDate(command.getExpectedReturnDate())
                .status(LoanStatus.BORROWED)
                .build();
        return loanRecordRepository.save(loan);
    }
}
