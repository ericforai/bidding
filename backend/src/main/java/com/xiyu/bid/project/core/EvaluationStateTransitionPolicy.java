// Input: 当前 / 目标 EvaluationSubStage
// Output: Allow|Deny - 评标 3 步线性 FSM (IN_PROGRESS → AWAITING_BOARD → ANNOUNCED)
// Pos: project/core/ - 纯规则，无 Spring/JPA
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

/**
 * PRD §3.3 评标子状态线性 FSM 决策。
 * <p>规则：</p>
 * <ul>
 *   <li>仅允许相邻顺推：IN_PROGRESS → AWAITING_BOARD → ANNOUNCED。</li>
 *   <li>跨级跳转、倒退、同态自循环、ANNOUNCED 出向、null 入参均拒绝。</li>
 * </ul>
 */
public final class EvaluationStateTransitionPolicy {

    private EvaluationStateTransitionPolicy() {
    }

    public static Decision decide(EvaluationSubStage current, EvaluationSubStage requested) {
        if (current == null || requested == null) {
            return new Decision.Deny("evaluation sub-stage 不能为空");
        }
        if (current == requested) {
            return new Decision.Deny("不能切换到当前子状态");
        }
        EvaluationSubStage expected = next(current);
        if (expected == null) {
            return new Decision.Deny("ANNOUNCED 已是评标终态，不可再切换");
        }
        if (requested != expected) {
            return new Decision.Deny("评标子状态非法跳转：" + current + "→" + requested + "，仅允许顺推到 " + expected);
        }
        return Decision.ALLOW;
    }

    private static EvaluationSubStage next(EvaluationSubStage s) {
        return switch (s) {
            case IN_PROGRESS -> EvaluationSubStage.AWAITING_BOARD;
            case AWAITING_BOARD -> EvaluationSubStage.ANNOUNCED;
            case ANNOUNCED -> null;
        };
    }

    /** Sealed Decision: Allow | Deny{reason}. */
    public sealed interface Decision permits Decision.Allow, Decision.Deny {
        Decision ALLOW = new Allow();

        default boolean allowed() {
            return this instanceof Allow;
        }

        record Allow() implements Decision {
        }

        record Deny(String reason) implements Decision {
        }
    }
}
