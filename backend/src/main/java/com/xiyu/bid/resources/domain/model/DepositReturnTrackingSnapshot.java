package com.xiyu.bid.resources.domain.model;

import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import com.xiyu.bid.resources.entity.Expense;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record DepositReturnTrackingSnapshot(
        Long expenseId,
        Long projectId,
        Expense.ExpenseStatus expenseStatus,
        LocalDate expectedReturnDate,
        LocalDateTime lastReminderAt,
        BidResultFetchResult.Result bidResult
) {

    public boolean hasConfirmedBidResult() {
        return bidResult != null;
    }
}
