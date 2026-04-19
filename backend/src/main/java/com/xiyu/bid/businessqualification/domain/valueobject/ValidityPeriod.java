package com.xiyu.bid.businessqualification.domain.valueobject;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Value
@Builder
public class ValidityPeriod {
    LocalDate issueDate;
    LocalDate expiryDate;

    public long remainingDays(LocalDate today) {
        if (expiryDate == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(today, expiryDate);
    }
}
