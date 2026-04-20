package com.xiyu.bid.businessqualification.application.service;

import com.xiyu.bid.businessqualification.application.command.QualificationUpsertCommand;
import com.xiyu.bid.businessqualification.domain.model.BusinessQualification;
import com.xiyu.bid.businessqualification.domain.port.BusinessQualificationRepository;
import com.xiyu.bid.businessqualification.domain.service.QualificationValidationResult;
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

        QualificationSubject subject = QualificationSubject.of(
                command.getSubjectType() == null ? existing.subject().getType() : command.getSubjectType(),
                command.getSubjectName() == null ? existing.subject().getName() : command.getSubjectName()
        );
        requireValid(subject.validate());

        BusinessQualification updated = BusinessQualification.create(
                existing.id(),
                command.getName() == null ? existing.name() : command.getName(),
                subject,
                command.getCategory() == null ? existing.category() : command.getCategory(),
                command.getCertificateNo() == null ? existing.certificateNo() : command.getCertificateNo(),
                command.getIssuer() == null ? existing.issuer() : command.getIssuer(),
                command.getHolderName() == null ? existing.holderName() : command.getHolderName(),
                new ValidityPeriod(
                        command.getIssueDate() == null ? existing.validityPeriod().getIssueDate() : command.getIssueDate(),
                        command.getExpiryDate() == null ? existing.validityPeriod().getExpiryDate() : command.getExpiryDate()
                ),
                new ReminderPolicy(
                        command.getReminderEnabled() == null ? existing.reminderPolicy().isEnabled() : command.getReminderEnabled(),
                        command.getReminderDays() == null ? existing.reminderPolicy().getReminderDays() : command.getReminderDays(),
                        existing.reminderPolicy().getLastRemindedAt()
                ),
                existing.currentBorrowStatus(),
                existing.currentBorrower(),
                existing.currentDepartment(),
                existing.currentProjectId(),
                existing.borrowPurpose(),
                existing.expectedReturnDate(),
                command.getFileUrl() == null ? existing.fileUrl() : command.getFileUrl(),
                command.getAttachments() == null || command.getAttachments().isEmpty() ? existing.attachments() : command.getAttachments()
        );

        return repository.save(updated);
    }

    private void requireValid(QualificationValidationResult validationResult) {
        if (!validationResult.valid()) {
            throw new IllegalArgumentException(validationResult.message());
        }
    }
}
