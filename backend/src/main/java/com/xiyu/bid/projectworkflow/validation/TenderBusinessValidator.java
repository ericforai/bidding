package com.xiyu.bid.projectworkflow.validation;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TenderBusinessValidator {

    public List<String> validate(TenderRequirementProfile profile) {
        List<String> warnings = new ArrayList<>();

        // Budget sanity check
        if (profile.budget() != null) {
            if (profile.budget().compareTo(new BigDecimal("10000")) < 0) {
                warnings.add("Budget seems unusually low (less than 10,000). Please verify the currency unit.");
            } else if (profile.budget().compareTo(new BigDecimal("10000000000")) > 0) {
                warnings.add("Budget seems unusually high (more than 10 billion). Please verify.");
            }
        } else {
            warnings.add("Budget could not be extracted or is missing.");
        }

        // Timeline conflict check
        LocalDate publishDate = profile.publishDate();
        LocalDateTime deadline = profile.deadline();

        if (publishDate != null && deadline != null) {
            if (deadline.toLocalDate().isBefore(publishDate)) {
                warnings.add("Submission deadline cannot be before the publish date.");
            }
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(publishDate, deadline.toLocalDate());
            if (daysBetween < 3) {
                warnings.add("The timeline between publish date and deadline is unusually tight (less than 3 days).");
            }
        }

        if (deadline != null && deadline.toLocalDate().isBefore(LocalDate.now())) {
            warnings.add("Submission deadline is in the past.");
        }

        return warnings;
    }
}
