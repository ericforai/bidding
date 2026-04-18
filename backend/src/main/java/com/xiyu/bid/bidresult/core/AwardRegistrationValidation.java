package com.xiyu.bid.bidresult.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * FP-Java Profile：声明式校验，消除所有 errors.add 这种命令式副作用。
 */
public final class AwardRegistrationValidation {

    private AwardRegistrationValidation() {
    }

    public static ValidationResult validate(AwardRegistration reg) {
        if (reg == null) {
            return ValidationResult.failure(List.of("registration must not be null"));
        }

        List<String> errors = Stream.of(
                check(reg.projectId() == null, "项目不能为空"),
                check(reg.projectName() == null || reg.projectName().isBlank(), "项目名称不能为空"),
                check(reg.result() == null, "投标结果必须指定中标或未中标"),
                check(reg.result() == AwardRegistration.ResultOutcome.WON && 
                      (reg.amount() == null || reg.amount().compareTo(BigDecimal.ZERO) <= 0), 
                      "中标时必须填写金额且大于 0"),
                check(reg.contractStartDate() != null && reg.contractEndDate() != null && 
                      reg.contractEndDate().isBefore(reg.contractStartDate()), 
                      "合同结束日期不得早于开始日期"),
                check(reg.contractDurationMonths() != null && reg.contractDurationMonths() < 0, "合同月数不得为负"),
                check(reg.skuCount() != null && reg.skuCount() < 0, "SKU 数量不得为负"),
                check(reg.remark() != null && reg.remark().length() > 2000, "备注不得超过 2000 字"),
                validateUrl(reg.winAnnounceDocUrl()).orElse(null)
        )
        .filter(Objects::nonNull)
        .toList();

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private static String check(boolean condition, String message) {
        return condition ? message : null;
    }

    private static Optional<String> validateUrl(String url) {
        if (url == null || url.isBlank()) return Optional.empty();
        String trimmed = url.trim();
        if (trimmed.length() > 500) return Optional.of("通知书 URL 不得超过 500 字符");
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
            return Optional.of("通知书 URL 必须以 http:// 或 https:// 开头");
        }
        return Optional.empty();
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public ValidationResult {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }

        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }
}
