package com.xiyu.bid.tender.core;

// Phase 2 TDD (RED): These tests intentionally reference symbols that do NOT
// yet exist (`TenderEvaluationFormPolicy`, `ValidationResult`, `FieldError`).
// Phase 3 will create them at:
//   backend/src/main/java/com/xiyu/bid/tender/core/TenderEvaluationFormPolicy.java
//
// Note on the local ValidationResult: existing project ValidationResult records
// (com.xiyu.bid.workflowform.domain.ValidationResult,
//  com.xiyu.bid.bidmatch.domain.ValidationResult,
//  com.xiyu.bid.integration.domain.ValidationResult)
// only carry List<String> errors with no field-level coding. The form policy
// needs field-coded errors (field + code + message), so a new
// com.xiyu.bid.tender.core.ValidationResult + FieldError pair is the right
// shape. Phase 3 must define them in the same `core` package.

import com.xiyu.bid.tender.dto.TenderEvaluationSubmitRequest;
import com.xiyu.bid.tender.entity.TenderEvaluation.BidRecommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TenderEvaluationFormPolicy - pure-core form validation")
class TenderEvaluationFormPolicyTest {

    // ---------- helpers ----------

    /** Build a fully-populated, valid request. Tests mutate individual fields. */
    private static TenderEvaluationSubmitRequest validRequest() {
        return new TenderEvaluationSubmitRequest(
                "项目背景描述：这是一个核心系统升级项目。",
                "竞争对手：A、B、C 三家公司。",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                3,
                new BigDecimal("1500.00"),
                "上一次报价 120 万元",
                BidRecommendation.RECOMMEND
        );
    }

    private static TenderEvaluationSubmitRequest withProjectBackground(String v) {
        TenderEvaluationSubmitRequest r = validRequest();
        return new TenderEvaluationSubmitRequest(v, r.competitorAnalysis(), r.contractPeriodStart(),
                r.contractPeriodEnd(), r.shortlistedCount(), r.platformServiceFee(),
                r.previousQuotation(), r.bidRecommendation());
    }

    private static TenderEvaluationSubmitRequest withCompetitorAnalysis(String v) {
        TenderEvaluationSubmitRequest r = validRequest();
        return new TenderEvaluationSubmitRequest(r.projectBackground(), v, r.contractPeriodStart(),
                r.contractPeriodEnd(), r.shortlistedCount(), r.platformServiceFee(),
                r.previousQuotation(), r.bidRecommendation());
    }

    private static TenderEvaluationSubmitRequest withContractPeriod(LocalDate s, LocalDate e) {
        TenderEvaluationSubmitRequest r = validRequest();
        return new TenderEvaluationSubmitRequest(r.projectBackground(), r.competitorAnalysis(), s, e,
                r.shortlistedCount(), r.platformServiceFee(),
                r.previousQuotation(), r.bidRecommendation());
    }

    private static TenderEvaluationSubmitRequest withShortlistedCount(Integer v) {
        TenderEvaluationSubmitRequest r = validRequest();
        return new TenderEvaluationSubmitRequest(r.projectBackground(), r.competitorAnalysis(),
                r.contractPeriodStart(), r.contractPeriodEnd(), v, r.platformServiceFee(),
                r.previousQuotation(), r.bidRecommendation());
    }

    private static TenderEvaluationSubmitRequest withPlatformServiceFee(BigDecimal v) {
        TenderEvaluationSubmitRequest r = validRequest();
        return new TenderEvaluationSubmitRequest(r.projectBackground(), r.competitorAnalysis(),
                r.contractPeriodStart(), r.contractPeriodEnd(), r.shortlistedCount(), v,
                r.previousQuotation(), r.bidRecommendation());
    }

    private static TenderEvaluationSubmitRequest withPreviousQuotation(String v) {
        TenderEvaluationSubmitRequest r = validRequest();
        return new TenderEvaluationSubmitRequest(r.projectBackground(), r.competitorAnalysis(),
                r.contractPeriodStart(), r.contractPeriodEnd(), r.shortlistedCount(),
                r.platformServiceFee(), v, r.bidRecommendation());
    }

    private static TenderEvaluationSubmitRequest withBidRecommendation(BidRecommendation v) {
        TenderEvaluationSubmitRequest r = validRequest();
        return new TenderEvaluationSubmitRequest(r.projectBackground(), r.competitorAnalysis(),
                r.contractPeriodStart(), r.contractPeriodEnd(), r.shortlistedCount(),
                r.platformServiceFee(), r.previousQuotation(), v);
    }

    // ---------- 1) happy path ----------

    @Test
    @DisplayName("所有必填字段就位 -> 校验通过，无错误")
    void validate_allRequiredFieldsPresent_returnsValid() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(validRequest());

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    // ---------- 2-3) blank required strings ----------

    @Test
    @DisplayName("projectBackground 为 null/空白 -> 单条 REQUIRED 错误")
    void validate_projectBackgroundBlank_returnsError() {
        ValidationResult nullCase = TenderEvaluationFormPolicy.validate(withProjectBackground(null));
        ValidationResult blankCase = TenderEvaluationFormPolicy.validate(withProjectBackground("   "));

        for (ValidationResult result : List.of(nullCase, blankCase)) {
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(1);
            FieldError err = result.errors().get(0);
            assertThat(err.field()).isEqualTo("projectBackground");
            assertThat(err.code()).isIn("REQUIRED", "BLANK");
            assertThat(err.message()).isNotBlank();
        }
    }

    @Test
    @DisplayName("competitorAnalysis 为 null/空白 -> 单条 REQUIRED 错误")
    void validate_competitorAnalysisBlank_returnsError() {
        ValidationResult nullCase = TenderEvaluationFormPolicy.validate(withCompetitorAnalysis(null));
        ValidationResult blankCase = TenderEvaluationFormPolicy.validate(withCompetitorAnalysis(""));

        for (ValidationResult result : List.of(nullCase, blankCase)) {
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(1);
            FieldError err = result.errors().get(0);
            assertThat(err.field()).isEqualTo("competitorAnalysis");
            assertThat(err.code()).isIn("REQUIRED", "BLANK");
        }
    }

    // ---------- 4-7) contract period ----------

    @Test
    @DisplayName("contractPeriodStart 为 null -> 错误")
    void validate_contractPeriodStartNull_returnsError() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(
                withContractPeriod(null, LocalDate.of(2026, 12, 31)));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(FieldError::field).contains("contractPeriodStart");
    }

    @Test
    @DisplayName("contractPeriodEnd 为 null -> 错误")
    void validate_contractPeriodEndNull_returnsError() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(
                withContractPeriod(LocalDate.of(2026, 1, 1), null));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(FieldError::field).contains("contractPeriodEnd");
    }

    @Test
    @DisplayName("contractPeriodStart 晚于 End -> 单条 contractPeriod INVALID_RANGE 错误")
    void validate_contractPeriodStartAfterEnd_returnsError() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(
                withContractPeriod(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 1, 1)));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).hasSize(1);
        FieldError err = result.errors().get(0);
        assertThat(err.field()).isEqualTo("contractPeriod");
        assertThat(err.code()).isEqualTo("INVALID_RANGE");
    }

    @Test
    @DisplayName("contractPeriodStart 等于 End -> 视为有效（单日项目）")
    void validate_contractPeriodStartEqualEnd_isValid() {
        LocalDate sameDay = LocalDate.of(2026, 6, 1);
        ValidationResult result = TenderEvaluationFormPolicy.validate(
                withContractPeriod(sameDay, sameDay));

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    // ---------- 8-10) shortlistedCount ----------

    @Test
    @DisplayName("shortlistedCount = 0 -> 错误（最小值 1）")
    void validate_shortlistedCountZero_returnsError() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(withShortlistedCount(0));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(FieldError::field).contains("shortlistedCount");
    }

    @Test
    @DisplayName("shortlistedCount 为负数 -> 错误")
    void validate_shortlistedCountNegative_returnsError() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(withShortlistedCount(-1));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(FieldError::field).contains("shortlistedCount");
    }

    @Test
    @DisplayName("shortlistedCount 为 null -> 错误")
    void validate_shortlistedCountNull_returnsError() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(withShortlistedCount(null));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(FieldError::field).contains("shortlistedCount");
    }

    // ---------- 11-13) platformServiceFee ----------

    @Test
    @DisplayName("platformServiceFee 为负数 -> 错误")
    void validate_platformServiceFeeNegative_returnsError() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(
                withPlatformServiceFee(new BigDecimal("-0.01")));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(FieldError::field).contains("platformServiceFee");
    }

    @Test
    @DisplayName("platformServiceFee 为 null -> 错误")
    void validate_platformServiceFeeNull_returnsError() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(withPlatformServiceFee(null));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(FieldError::field).contains("platformServiceFee");
    }

    @Test
    @DisplayName("platformServiceFee = 0 -> 视为有效（免费服务也是合法值）")
    void validate_platformServiceFeeZero_isValid() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(
                withPlatformServiceFee(BigDecimal.ZERO));

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    // ---------- 14-15) optional fields ----------

    @Test
    @DisplayName("previousQuotation 缺省（null/空白）+ 其它必填齐全 -> 有效")
    void validate_previousQuotationOmitted_isValid() {
        ValidationResult nullCase = TenderEvaluationFormPolicy.validate(withPreviousQuotation(null));
        ValidationResult blankCase = TenderEvaluationFormPolicy.validate(withPreviousQuotation("   "));

        assertThat(nullCase.isValid()).isTrue();
        assertThat(nullCase.errors()).isEmpty();
        assertThat(blankCase.isValid()).isTrue();
        assertThat(blankCase.errors()).isEmpty();
    }

    @Test
    @DisplayName("bidRecommendation 缺省（null）+ 其它必填齐全 -> 有效")
    void validate_bidRecommendationOmitted_isValid() {
        ValidationResult result = TenderEvaluationFormPolicy.validate(withBidRecommendation(null));

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    // ---------- 16) aggregate errors ----------

    @Test
    @DisplayName("3 个必填字段同时缺失 -> 恰好 3 条错误，每字段各一条（顺序不限）")
    void validate_multipleErrors_returnsAllOfThem() {
        TenderEvaluationSubmitRequest input = new TenderEvaluationSubmitRequest(
                null,                                  // projectBackground missing
                "竞争对手分析正文",                       // OK
                LocalDate.of(2026, 1, 1),              // OK
                LocalDate.of(2026, 12, 31),            // OK
                null,                                  // shortlistedCount missing
                null,                                  // platformServiceFee missing
                null,                                  // optional
                null                                   // optional
        );

        ValidationResult result = TenderEvaluationFormPolicy.validate(input);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).hasSize(3);
        assertThat(result.errors()).extracting(FieldError::field)
                .containsExactlyInAnyOrder(
                        "projectBackground",
                        "shortlistedCount",
                        "platformServiceFee");
    }

    // ---------- 17) programmer error: null input ----------

    @Test
    @DisplayName("validate(null) -> 抛 NullPointerException（程序员错误，快速失败）")
    void validate_nullInput_throwsNPE() {
        assertThatThrownBy(() -> TenderEvaluationFormPolicy.validate(null))
                .isInstanceOf(NullPointerException.class);
    }
}
