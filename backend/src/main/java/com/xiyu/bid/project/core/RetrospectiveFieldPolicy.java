// Input: BidResultType + RetrospectiveInput (字段集合)
// Output: Decision (Allow | Deny{missing}) -- PRD §3.5.1 必填字段矩阵
// Pos: project/core/ - pure rule, no Spring/JPA
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * PRD §3.5.1 复盘必填字段策略：按 4 类结果分支校验必填项。
 * 字段语义：
 * <ul>
 *   <li>summary — 总结/亮点/决策说明</li>
 *   <li>winFactors — 中标优势</li>
 *   <li>lossReasons — 丢标/流标/弃标原因</li>
 *   <li>competitorNotes — 问题分析</li>
 *   <li>improvementActions — 建议/改进措施/应对方案</li>
 * </ul>
 * 必填映射：
 * <ul>
 *   <li>WON: winFactors, summary, improvementActions</li>
 *   <li>LOST: lossReasons, competitorNotes, improvementActions</li>
 *   <li>FAILED: lossReasons, improvementActions</li>
 *   <li>ABANDONED: lossReasons, summary</li>
 * </ul>
 */
public final class RetrospectiveFieldPolicy {

    private RetrospectiveFieldPolicy() {
    }

    public static Decision validate(BidResultType resultType, RetrospectiveInput input) {
        if (resultType == null) {
            return new Decision.Deny(List.of("resultType"));
        }
        Objects.requireNonNull(input, "input 不能为空");
        List<String> missing = new ArrayList<>();
        switch (resultType) {
            case WON -> {
                requireField("winFactors", input.winFactors(), missing);
                requireField("summary", input.summary(), missing);
                requireField("improvementActions", input.improvementActions(), missing);
            }
            case LOST -> {
                requireField("lossReasons", input.lossReasons(), missing);
                requireField("competitorNotes", input.competitorNotes(), missing);
                requireField("improvementActions", input.improvementActions(), missing);
            }
            case FAILED -> {
                requireField("lossReasons", input.lossReasons(), missing);
                requireField("improvementActions", input.improvementActions(), missing);
            }
            case ABANDONED -> {
                requireField("lossReasons", input.lossReasons(), missing);
                requireField("summary", input.summary(), missing);
            }
        }
        return missing.isEmpty()
                ? Decision.ALLOW
                : new Decision.Deny(Collections.unmodifiableList(missing));
    }

    private static void requireField(String name, String value, List<String> missing) {
        if (value == null || value.trim().isEmpty()) {
            missing.add(name);
        }
    }

    /** 复盘字段输入。所有字段可为 null，由策略按 resultType 决定必填。 */
    public record RetrospectiveInput(
            String summary,
            String winFactors,
            String lossReasons,
            String competitorNotes,
            String improvementActions) {
    }

    /** Sealed Decision: Allow | Deny{missing}. */
    public sealed interface Decision permits Decision.Allow, Decision.Deny {
        Decision ALLOW = new Allow();

        default boolean allowed() {
            return this instanceof Allow;
        }

        record Allow() implements Decision {
        }

        record Deny(List<String> missing) implements Decision {
            public String reason() {
                return "缺少必填字段：" + String.join(",", missing);
            }
        }
    }
}
