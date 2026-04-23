package com.xiyu.bid.biddraftagent.domain;

import java.util.List;

public record ManualConfirmationDecision(
        boolean pricingConfirmationRequired,
        boolean legalConfirmationRequired,
        boolean qualificationAuthenticityConfirmationRequired,
        List<String> reasons
) {

    public ManualConfirmationDecision {
        reasons = List.copyOf(reasons);
    }

    public boolean requiresConfirmation() {
        return pricingConfirmationRequired
                || legalConfirmationRequired
                || qualificationAuthenticityConfirmationRequired;
    }
}
