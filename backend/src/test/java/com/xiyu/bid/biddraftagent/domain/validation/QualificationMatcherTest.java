// Input: QualificationMatcher（isSmartMatch 方法）
// Output: 资质匹配行为验证
// Pos: Test/biddraftagent/domain/validation
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.biddraftagent.domain.validation;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import com.xiyu.bid.qualification.dto.QualificationDTO;
import com.xiyu.bid.qualification.service.QualificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QualificationMatcherTest {

    @Mock
    private QualificationService qualificationService;

    private QualificationMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new QualificationMatcher(qualificationService);
    }

    // ── isSmartMatch 单元测试 ────────────────────────────────────────────────

    /**
     * HIGH-6 回归测试：短名称"ISO"不得匹配"ISOLATED system"。
     * 移除了 || contains 回退后，词边界匹配必须拒绝此情形。
     */
    @Test
    void isSmartMatch_shortName_shouldNotMatchSubstring() {
        assertThat(matcher.isSmartMatch("ISOLATED system", "ISO")).isFalse();
    }

    @Test
    void isSmartMatch_shortName_exactWord_shouldMatch() {
        assertThat(matcher.isSmartMatch("符合 ISO 认证要求", "ISO")).isTrue();
    }

    @Test
    void isSmartMatch_shortName_caseInsensitive_shouldMatch() {
        assertThat(matcher.isSmartMatch("已取得 iso 认证", "ISO")).isTrue();
    }

    @Test
    void isSmartMatch_longName_substringMatch_shouldMatch() {
        assertThat(matcher.isSmartMatch("需具备ISO9001质量管理体系认证", "ISO9001质量管理体系")).isTrue();
    }

    @Test
    void isSmartMatch_longName_caseInsensitive_shouldMatch() {
        assertThat(matcher.isSmartMatch("需具备iso9001质量管理体系认证", "ISO9001质量管理体系")).isTrue();
    }

    @Test
    void isSmartMatch_longName_notPresent_shouldNotMatch() {
        assertThat(matcher.isSmartMatch("需要安全生产许可证", "ISO9001质量管理体系")).isFalse();
    }

    @Test
    void isSmartMatch_chineseQualification_shouldMatch() {
        assertThat(matcher.isSmartMatch("投标人须持有建筑工程施工总承包一级资质", "建筑工程施工总承包一级")).isTrue();
    }

    @Test
    void isSmartMatch_chineseQualification_notPresent_shouldNotMatch() {
        assertThat(matcher.isSmartMatch("投标人须持有市政工程总承包一级资质", "建筑工程施工总承包一级")).isFalse();
    }

    // ── match() 集成场景 ─────────────────────────────────────────────────────

    @Test
    void match_emptyRequirements_shouldReturnEmptyResult() {
        when(qualificationService.getValidQualifications()).thenReturn(List.of());
        TenderRequirementProfile profile = profileWithRequirements(List.of());

        QualificationMatcher.QualificationMatchResult result = matcher.match(profile);

        assertThat(result.matched()).isEmpty();
        assertThat(result.missing()).isEmpty();
    }

    @Test
    void match_nullRequirements_shouldReturnEmptyResult() {
        when(qualificationService.getValidQualifications()).thenReturn(List.of());
        TenderRequirementProfile profile = profileWithRequirements(null);

        QualificationMatcher.QualificationMatchResult result = matcher.match(profile);

        assertThat(result.matched()).isEmpty();
        assertThat(result.missing()).isEmpty();
    }

    @Test
    void match_exactMatch_shouldBeInMatched() {
        QualificationDTO qual = new QualificationDTO();
        qual.setName("建筑工程施工总承包一级");
        when(qualificationService.getValidQualifications()).thenReturn(List.of(qual));

        TenderRequirementProfile profile = profileWithRequirements(
                List.of("投标人须持有建筑工程施工总承包一级资质"));

        QualificationMatcher.QualificationMatchResult result = matcher.match(profile);

        assertThat(result.matched()).hasSize(1);
        assertThat(result.missing()).isEmpty();
    }

    @Test
    void match_noMatchingQualification_shouldBeInMissing() {
        QualificationDTO qual = new QualificationDTO();
        qual.setName("市政工程总承包一级");
        when(qualificationService.getValidQualifications()).thenReturn(List.of(qual));

        TenderRequirementProfile profile = profileWithRequirements(
                List.of("需持有建筑工程施工总承包一级资质"));

        QualificationMatcher.QualificationMatchResult result = matcher.match(profile);

        assertThat(result.matched()).isEmpty();
        assertThat(result.missing()).hasSize(1);
    }

    @Test
    void match_mixedCaseShortName_shouldNotFalselyMatch() {
        QualificationDTO qual = new QualificationDTO();
        qual.setName("ISO");
        when(qualificationService.getValidQualifications()).thenReturn(List.of(qual));

        // "ISOLATED" 应该不匹配 "ISO"
        TenderRequirementProfile profile = profileWithRequirements(List.of("ISOLATED system requirement"));

        QualificationMatcher.QualificationMatchResult result = matcher.match(profile);

        assertThat(result.matched()).isEmpty();
        assertThat(result.missing()).hasSize(1);
    }

    @Test
    void match_emptyQualificationList_allRequirementsMissing() {
        when(qualificationService.getValidQualifications()).thenReturn(List.of());

        TenderRequirementProfile profile = profileWithRequirements(
                List.of("需持有资质A", "需持有资质B"));

        QualificationMatcher.QualificationMatchResult result = matcher.match(profile);

        assertThat(result.matched()).isEmpty();
        assertThat(result.missing()).hasSize(2);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private TenderRequirementProfile profileWithRequirements(List<String> requirements) {
        return new TenderRequirementProfile(
                null, null, null, null, null, null, null, null, null,
                requirements,
                List.of(), List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of()
        );
    }
}
