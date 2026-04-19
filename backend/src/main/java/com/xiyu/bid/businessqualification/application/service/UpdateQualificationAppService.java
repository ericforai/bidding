package com.xiyu.bid.businessqualification.application.service;

import com.xiyu.bid.businessqualification.application.command.QualificationUpsertCommand;
import com.xiyu.bid.businessqualification.domain.model.BusinessQualification;
import com.xiyu.bid.businessqualification.domain.port.BusinessQualificationRepository;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationSubject;
import com.xiyu.bid.businessqualification.domain.valueobject.ReminderPolicy;
import com.xiyu.bid.businessqualification.domain.valueobject.ValidityPeriod;
import com.xiyu.bid.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateQualificationAppService {

    private final BusinessQualificationRepository repository;

    @Transactional
    public BusinessQualification update(Long id, QualificationUpsertCommand command) {
        BusinessQualification existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessQualification", String.valueOf(id)));

        BusinessQualification updated = BusinessQualification.builder()
                .id(existing.id())
                .name(command.getName() == null ? existing.name() : command.getName())
                .subject(QualificationSubject.builder()
                        .type(command.getSubjectType() == null ? existing.subject().getType() : command.getSubjectType())
                        .name(command.getSubjectName() == null ? existing.subject().getName() : command.getSubjectName())
                        .build())
                .category(command.getCategory() == null ? existing.category() : command.getCategory())
                .certificateNo(command.getCertificateNo() == null ? existing.certificateNo() : command.getCertificateNo())
                .issuer(command.getIssuer() == null ? existing.issuer() : command.getIssuer())
                .holderName(command.getHolderName() == null ? existing.holderName() : command.getHolderName())
                .validityPeriod(ValidityPeriod.builder()
                        .issueDate(command.getIssueDate() == null ? existing.validityPeriod().getIssueDate() : command.getIssueDate())
                        .expiryDate(command.getExpiryDate() == null ? existing.validityPeriod().getExpiryDate() : command.getExpiryDate())
                        .build())
                .reminderPolicy(ReminderPolicy.builder()
                        .enabled(command.getReminderEnabled() == null ? existing.reminderPolicy().isEnabled() : command.getReminderEnabled())
                        .reminderDays(command.getReminderDays() == null ? existing.reminderPolicy().getReminderDays() : command.getReminderDays())
                        .lastRemindedAt(existing.reminderPolicy().getLastRemindedAt())
                        .build())
                .currentBorrowStatus(existing.currentBorrowStatus())
                .currentBorrower(existing.currentBorrower())
                .currentDepartment(existing.currentDepartment())
                .currentProjectId(existing.currentProjectId())
                .borrowPurpose(existing.borrowPurpose())
                .expectedReturnDate(existing.expectedReturnDate())
                .fileUrl(command.getFileUrl() == null ? existing.fileUrl() : command.getFileUrl())
                .attachments(command.getAttachments() == null || command.getAttachments().isEmpty() ? existing.attachments() : command.getAttachments())
                .build();

        return repository.save(updated);
    }
}
