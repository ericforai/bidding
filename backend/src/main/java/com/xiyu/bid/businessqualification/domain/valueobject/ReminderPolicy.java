package com.xiyu.bid.businessqualification.domain.valueobject;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class ReminderPolicy {
    boolean enabled;
    int reminderDays;
    LocalDateTime lastRemindedAt;

    public ReminderPolicy recordReminder(LocalDateTime remindedAt) {
        return toBuilder().lastRemindedAt(remindedAt).build();
    }
}
