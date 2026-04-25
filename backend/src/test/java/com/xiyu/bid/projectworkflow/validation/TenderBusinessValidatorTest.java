package com.xiyu.bid.projectworkflow.validation;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class TenderBusinessValidatorTest {
    private final TenderBusinessValidator validator = new TenderBusinessValidator();

    @Test
    void shouldWarnOnLowBudget() {
        TenderRequirementProfile profile = createProfile(new BigDecimal("5000"), null, null);
        List<String> warnings = validator.validate(profile);
        assertThat(warnings).anyMatch(s -> s.contains("unusually low"));
    }

    @Test
    void shouldWarnOnTimelineConflict() {
        LocalDate publish = LocalDate.now();
        LocalDateTime deadline = publish.minusDays(1).atStartOfDay();
        TenderRequirementProfile profile = createProfile(null, publish, deadline);
        List<String> warnings = validator.validate(profile);
        assertThat(warnings).anyMatch(s -> s.contains("cannot be before"));
    }

    private TenderRequirementProfile createProfile(BigDecimal budget, LocalDate publish, LocalDateTime deadline) {
        return new TenderRequirementProfile(null, null, null, null, budget, null, null, publish, deadline, 
            List.of(), List.of(), List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of());
    }
}
