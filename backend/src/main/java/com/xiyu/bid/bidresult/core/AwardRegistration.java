package com.xiyu.bid.bidresult.core;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input: 手工登记投标结果的纯值
 */
public record AwardRegistration(
        Long projectId,
        String projectName,
        ResultOutcome result,
        BigDecimal amount,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        Integer contractDurationMonths,
        String remark,
        Integer skuCount,
        String winAnnounceDocUrl
) {
    public enum ResultOutcome {
        WON,
        LOST
    }
}
