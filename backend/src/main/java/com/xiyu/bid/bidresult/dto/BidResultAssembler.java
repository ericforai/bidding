package com.xiyu.bid.bidresult.dto;

import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import com.xiyu.bid.bidresult.entity.BidResultReminder;

import java.util.Optional;

public class BidResultAssembler {

    public static BidResultReminderDTO toReminderDTO(BidResultReminder entity) {
        if (entity == null) return null;
        return BidResultReminderDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .projectName(entity.getProjectName())
                .ownerId(entity.getOwnerId())
                .ownerName(entity.getOwnerName())
                .reminderType(entity.getReminderType())
                .status(entity.getStatus())
                .remindTime(entity.getRemindTime())
                .lastReminderComment(entity.getLastReminderComment())
                .lastResultId(entity.getLastResultId())
                .build();
    }

    public static BidResultFetchResultDTO toFetchResultDTO(BidResultFetchResult entity) {
        if (entity == null) return null;
        return BidResultFetchResultDTO.builder()
                .id(entity.getId())
                .source(entity.getSource())
                .tenderId(entity.getTenderId())
                .projectId(entity.getProjectId())
                .projectName(entity.getProjectName())
                .result(entity.getResult())
                .amount(entity.getAmount())
                .fetchTime(entity.getFetchTime())
                .status(entity.getStatus())
                .confirmedAt(entity.getConfirmedAt())
                .confirmedBy(entity.getConfirmedBy())
                .ignoredReason(entity.getIgnoredReason())
                .registrationType(entity.getRegistrationType())
                .contractStartDate(entity.getContractStartDate())
                .contractEndDate(entity.getContractEndDate())
                .contractDurationMonths(entity.getContractDurationMonths())
                .remark(entity.getRemark())
                .skuCount(entity.getSkuCount())
                .winAnnounceDocUrl(entity.getWinAnnounceDocUrl())
                .build();
    }
}
