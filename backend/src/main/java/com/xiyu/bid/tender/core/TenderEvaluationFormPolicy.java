// Input: TenderEvaluationSubmitRequest（评估表 7 字段值载体）
// Output: ValidationResult（字段级错误集合，全部一次性收集；空集合 == 通过）
// Pos: 纯核心层（core）- 不依赖 Spring / JPA / 任何外部框架
// 维护声明: 业务规则的唯一权威；service 层不允许重复校验或绕过此策略。
//          错误码集合是稳定契约：REQUIRED / INVALID_RANGE / MIN_VALUE。
package com.xiyu.bid.tender.core;

import com.xiyu.bid.tender.dto.TenderEvaluationSubmitRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 标讯项目评估表提交校验策略（纯函数）。
 *
 * <p>规则（V119 设计基线）：
 * <ul>
 *   <li>projectBackground：必填，禁止 null/空白 → REQUIRED</li>
 *   <li>competitorAnalysis：必填，禁止 null/空白 → REQUIRED</li>
 *   <li>contractPeriodStart：必填 → REQUIRED</li>
 *   <li>contractPeriodEnd：必填 → REQUIRED</li>
 *   <li>contractPeriod（逻辑字段）：start 晚于 end → INVALID_RANGE（仅当两端都存在时检查）</li>
 *   <li>shortlistedCount：必填且 ≥ 1 → REQUIRED / MIN_VALUE</li>
 *   <li>platformServiceFee：必填且 ≥ 0 → REQUIRED / MIN_VALUE</li>
 *   <li>previousQuotation：非必填，无任何约束</li>
 *   <li>bidRecommendation：非必填，无任何约束</li>
 * </ul>
 *
 * <p>调用约定：
 * <ul>
 *   <li>{@code validate(null)} → 抛 NullPointerException（程序员错误，快速失败）</li>
 *   <li>合法输入 → 返回 {@code ValidationResult} 含 0..N 条 FieldError，永不抛出业务错误</li>
 * </ul>
 */
public final class TenderEvaluationFormPolicy {

    private static final String CODE_REQUIRED = "REQUIRED";
    private static final String CODE_INVALID_RANGE = "INVALID_RANGE";
    private static final String CODE_MIN_VALUE = "MIN_VALUE";

    private TenderEvaluationFormPolicy() {
        // 工具类不可实例化
    }

    /**
     * 校验提交请求。错误一次性收集，不在首条出错时短路。
     *
     * @param req 评估表请求；不可为 null
     * @return 校验结果；调用方按 {@link ValidationResult#isValid()} 分流
     * @throws NullPointerException 当 {@code req == null}
     */
    public static ValidationResult validate(TenderEvaluationSubmitRequest req) {
        Objects.requireNonNull(req, "request must not be null");

        List<FieldError> errors = new ArrayList<>();

        // 1) projectBackground
        if (isBlank(req.projectBackground())) {
            errors.add(new FieldError(
                    "projectBackground", CODE_REQUIRED, "项目背景不能为空"));
        }

        // 2) competitorAnalysis
        if (isBlank(req.competitorAnalysis())) {
            errors.add(new FieldError(
                    "competitorAnalysis", CODE_REQUIRED, "竞争对手情况不能为空"));
        }

        // 3-5) 合同周期：start / end / 区间
        boolean startMissing = req.contractPeriodStart() == null;
        boolean endMissing = req.contractPeriodEnd() == null;
        if (startMissing) {
            errors.add(new FieldError(
                    "contractPeriodStart", CODE_REQUIRED, "项目合同周期起不能为空"));
        }
        if (endMissing) {
            errors.add(new FieldError(
                    "contractPeriodEnd", CODE_REQUIRED, "项目合同周期止不能为空"));
        }
        if (!startMissing && !endMissing
                && req.contractPeriodStart().isAfter(req.contractPeriodEnd())) {
            errors.add(new FieldError(
                    "contractPeriod", CODE_INVALID_RANGE, "项目合同周期起不能晚于周期止"));
        }

        // 6) shortlistedCount
        if (req.shortlistedCount() == null) {
            errors.add(new FieldError(
                    "shortlistedCount", CODE_REQUIRED, "入围家数不能为空"));
        } else if (req.shortlistedCount() < 1) {
            errors.add(new FieldError(
                    "shortlistedCount", CODE_MIN_VALUE, "入围家数不能小于 1"));
        }

        // 7) platformServiceFee
        if (req.platformServiceFee() == null) {
            errors.add(new FieldError(
                    "platformServiceFee", CODE_REQUIRED, "平台服务费不能为空"));
        } else if (req.platformServiceFee().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new FieldError(
                    "platformServiceFee", CODE_MIN_VALUE, "平台服务费不能为负数"));
        }

        return new ValidationResult(errors);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
