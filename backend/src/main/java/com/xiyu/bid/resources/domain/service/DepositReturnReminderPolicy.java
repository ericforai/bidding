package com.xiyu.bid.resources.domain.service;

import com.xiyu.bid.resources.domain.model.DepositReturnReminderDecision;
import com.xiyu.bid.resources.domain.model.DepositReturnTrackingSnapshot;
import com.xiyu.bid.resources.domain.valueobject.DepositReturnReminderStage;
import com.xiyu.bid.resources.entity.Expense;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DepositReturnReminderPolicy {

    public DepositReturnReminderDecision evaluate(
            DepositReturnTrackingSnapshot snapshot,
            int warnDays,
            LocalDate today,
            LocalDateTime now
    ) {
        if (!isReminderEligible(snapshot)) {
            return noReminder();
        }

        long daysUntilDue = ChronoUnit.DAYS.between(today, snapshot.expectedReturnDate());
        boolean remindedToday = snapshot.lastReminderAt() != null
                && snapshot.lastReminderAt().toLocalDate().isEqual(now.toLocalDate());

        if (daysUntilDue < 0 && !remindedToday) {
            return DepositReturnReminderDecision.builder()
                    .shouldRemind(true)
                    .stage(DepositReturnReminderStage.OVERDUE)
                    .overdueDays(Math.abs(daysUntilDue))
                    .daysUntilDue(daysUntilDue)
                    .build();
        }

        if (daysUntilDue <= warnDays && daysUntilDue >= 0 && !remindedToday) {
            return DepositReturnReminderDecision.builder()
                    .shouldRemind(true)
                    .stage(DepositReturnReminderStage.DUE_SOON)
                    .overdueDays(0)
                    .daysUntilDue(daysUntilDue)
                    .build();
        }

        return noReminder();
    }

    public boolean canSendManualReminder(DepositReturnTrackingSnapshot snapshot) {
        return isReminderEligible(snapshot);
    }

    private DepositReturnReminderDecision noReminder() {
        return DepositReturnReminderDecision.builder()
                .shouldRemind(false)
                .stage(DepositReturnReminderStage.DUE_SOON)
                .overdueDays(0)
                .daysUntilDue(Long.MAX_VALUE)
                .build();
    }

    private boolean isReminderEligible(DepositReturnTrackingSnapshot snapshot) {
        if (snapshot == null
                || !snapshot.hasConfirmedBidResult()
                || snapshot.expectedReturnDate() == null) {
            return false;
        }

        return snapshot.expenseStatus() == Expense.ExpenseStatus.APPROVED
                || snapshot.expenseStatus() == Expense.ExpenseStatus.PAID
                || snapshot.expenseStatus() == Expense.ExpenseStatus.RETURN_REQUESTED;
    }
}
