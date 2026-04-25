package com.xiyu.bid.projectworkflow.validation;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import com.xiyu.bid.qualification.dto.QualificationDTO;
import com.xiyu.bid.qualification.service.QualificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QualificationMatcher {

    private final QualificationService qualificationService;

    public QualificationMatchResult match(TenderRequirementProfile profile) {
        List<QualificationDTO> allQualifications = qualificationService.getValidQualifications();
        List<String> requirements = profile.qualificationRequirements();
        
        List<MatchedQualification> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (requirements == null || requirements.isEmpty()) {
            return new QualificationMatchResult(matched, missing);
        }

        for (String req : requirements) {
            boolean found = false;
            for (QualificationDTO qual : allQualifications) {
                if (qual.getName() != null && req.toLowerCase().contains(qual.getName().toLowerCase())) {
                    matched.add(new MatchedQualification(req, qual));
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(req);
            }
        }

        return new QualificationMatchResult(matched, missing);
    }

    public record QualificationMatchResult(
            List<MatchedQualification> matched,
            List<String> missing
    ) {}

    public record MatchedQualification(
            String requirementText,
            QualificationDTO qualification
    ) {}
}
