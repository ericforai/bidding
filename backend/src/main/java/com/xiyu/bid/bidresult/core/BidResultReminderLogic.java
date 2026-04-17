package com.xiyu.bid.bidresult.core;

import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import com.xiyu.bid.bidresult.entity.BidResultReminder;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Functional Core for BidResult Reminder logic.
 * Contains pure functions for state transitions and entity assembly.
 */
public class BidResultReminderLogic {

    /**
     * Determines the reminder type based on the fetch result.
     */
    public static BidResultReminder.ReminderType determineType(BidResultFetchResult.Result result) {
        return result == BidResultFetchResult.Result.WON
                ? BidResultReminder.ReminderType.NOTICE
                : BidResultReminder.ReminderType.REPORT;
    }

    /**
     * Calculates the target state of a BidResultReminder.
     * This is a pure function that takes all dependencies as parameters.
     */
    public static BidResultReminder calculateReminderState(
            BidResultFetchResult fetchResult,
            BidResultReminder existingReminder,
            Long ownerId,
            String ownerName,
            String comment,
            Long operatorId,
            String operatorName,
            LocalDateTime currentTime,
            boolean isSendAction) {

        BidResultReminder reminder = existingReminder;

        if (reminder == null) {
            reminder = BidResultReminder.builder()
                    .projectId(fetchResult.getProjectId())
                    .projectName(fetchResult.getProjectName())
                    .ownerId(ownerId)
                    .ownerName(ownerName)
                    .reminderType(determineType(fetchResult.getResult()))
                    .createdBy(operatorId)
                    .createdByName(operatorName)
                    .build();
        }

        // Logical equivalent of ensurePendingReminderForResult
        if (reminder.getStatus() == null || reminder.getStatus() != BidResultReminder.ReminderStatus.UPLOADED) {
            reminder.setStatus(BidResultReminder.ReminderStatus.PENDING);
        }
        
        reminder.setRemindTime(Optional.ofNullable(reminder.getRemindTime()).orElse(currentTime));
        reminder.setLastReminderComment(Optional.ofNullable(comment)
                .filter(text -> !text.isBlank())
                .orElse("待上传中标资料"));
        reminder.setLastResultId(fetchResult.getId());
        
        if (reminder.getCreatedByName() == null || reminder.getCreatedByName().isBlank()) {
            reminder.setCreatedByName(operatorName);
        }

        // Additional transitions if this is a send action
        if (isSendAction) {
            reminder.setStatus(BidResultReminder.ReminderStatus.REMINDED);
            reminder.setRemindTime(currentTime);
            reminder.setLastReminderComment(Optional.ofNullable(comment)
                    .filter(text -> !text.isBlank())
                    .orElse("已发送上传提醒"));
        }

        return reminder;
    }
}
