// Input: TenderRequirementProfile（资质要求列表）、QualificationService（企业已持有资质）
// Output: QualificationMatchResult（已匹配资质列表 + 缺失资质列表）
// Pos: biddraftagent/domain/validation — 资质比对纯核心逻辑
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.biddraftagent.domain.validation;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import com.xiyu.bid.qualification.dto.QualificationDTO;
import com.xiyu.bid.qualification.service.QualificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QualificationMatcher {

    private final QualificationService qualificationService;

    /**
     * 每个资质名称对应一个预编译的词边界 Pattern（仅用于 length <= 5 的短名称），避免在循环内重复编译。
     * ConcurrentHashMap 以应对可能的并发调用场景。
     */
    private final Map<String, Pattern> shortNamePatternCache = new ConcurrentHashMap<>();

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

    /**
     * 智能匹配：
     * - 长名称（length > 5，如"ISO9001质量管理体系"）：使用不区分大小写的子串匹配，误匹配风险低。
     * - 短名称（length <= 5，如"ISO"）：如果全部是英文字母（如"ISO"），仅使用词边界正则匹配，防止"ISO"匹配"ISOLATED"。
     *   如果包含非英文字母（如中文字符"涉密甲级"或特殊字符"C++"），正则的 \b 无法按预期工作，此时降级使用 contains。
     */
    boolean isSmartMatch(String source, String target) {
        if (target.length() > 5 || !target.matches("^[a-zA-Z]+$")) {
            return source.toLowerCase().contains(target.toLowerCase());
        }
        // Short name (pure English letters): word boundary match only
        Pattern pattern = shortNamePatternCache.computeIfAbsent(
                target,
                t -> Pattern.compile("\\b" + Pattern.quote(t) + "\\b", Pattern.CASE_INSENSITIVE)
        );
        return pattern.matcher(source).find();
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
