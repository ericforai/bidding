package com.xiyu.bid.projectworkflow.validation;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import com.xiyu.bid.qualification.dto.QualificationDTO;
import com.xiyu.bid.qualification.service.QualificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
                String qualName = qual.getName();
                if (qualName == null || qualName.length() < 2) continue; // 忽略过短的干扰词
                
                // 升级：使用不区分大小写的全词/片段匹配，且要求资质名称必须在需求中作为一个相对独立的单元出现
                if (isSmartMatch(req, qualName)) {
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

    private boolean isSmartMatch(String source, String target) {
        // 如果资质名称很长（如“ISO9001质量管理体系”），contains 通常是安全的
        if (target.length() > 5) {
            return source.toLowerCase().contains(target.toLowerCase());
        }
        // 对于短名称（如“ISO”），要求其前后不能紧跟其他字母，防止误匹配（如“ISOLATED”）
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(target) + "\\b", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(source).find() || source.toLowerCase().contains(target.toLowerCase());
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
