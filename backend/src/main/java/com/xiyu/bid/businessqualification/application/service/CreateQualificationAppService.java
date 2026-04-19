package com.xiyu.bid.businessqualification.application.service;

import com.xiyu.bid.businessqualification.application.command.QualificationUpsertCommand;
import com.xiyu.bid.businessqualification.domain.model.BusinessQualification;
import com.xiyu.bid.businessqualification.domain.port.BusinessQualificationRepository;
import com.xiyu.bid.businessqualification.domain.valueobject.LoanStatus;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationSubject;
import com.xiyu.bid.businessqualification.domain.valueobject.ReminderPolicy;
import com.xiyu.bid.businessqualification.domain.valueobject.ValidityPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateQualificationAppService {

    private final BusinessQualificationRepository repository;

    @Transactional
    public BusinessQualification create(QualificationUpsertCommand command) {
        QualificationSubject subject = QualificationSubject.builder()
                .type(command.getSubjectType())
                .name(command.getSubjectName())
                .build();
        subject.validate();

        BusinessQualification qualification = BusinessQualification.builder()
                .name(command.getName())
                .subject(subject)
                .category(command.getCategory())
                .certificateNo(command.getCertificateNo())
                .issuer(command.getIssuer())
                .holderName(command.getHolderName())
                .validityPeriod(ValidityPeriod.builder()
                        .issueDate(command.getIssueDate())
                        .expiryDate(command.getExpiryDate())
                        .build())
                .reminderPolicy(ReminderPolicy.builder()
                        .enabled(Boolean.TRUE.equals(command.getReminderEnabled()) || command.getReminderEnabled() == null)
                        .reminderDays(command.getReminderDays() == null ? 30 : command.getReminderDays())
                        .build())
                .currentBorrowStatus(LoanStatus.AVAILABLE)
                .fileUrl(command.getFileUrl())
                .attachments(command.getAttachments() == null ? List.of() : command.getAttachments())
                .build();

        return repository.save(qualification);
    }
}
