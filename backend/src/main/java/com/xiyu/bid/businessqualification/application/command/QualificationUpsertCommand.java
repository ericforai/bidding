package com.xiyu.bid.businessqualification.application.command;

import com.xiyu.bid.businessqualification.domain.model.QualificationAttachment;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationCategory;
import com.xiyu.bid.businessqualification.domain.valueobject.QualificationSubjectType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class QualificationUpsertCommand {
    String name;
    QualificationSubjectType subjectType;
    String subjectName;
    QualificationCategory category;
    String certificateNo;
    String issuer;
    String holderName;
    LocalDate issueDate;
    LocalDate expiryDate;
    Boolean reminderEnabled;
    Integer reminderDays;
    String fileUrl;
    List<QualificationAttachment> attachments;
}
