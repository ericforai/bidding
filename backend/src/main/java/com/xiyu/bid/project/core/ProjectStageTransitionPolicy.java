// Input: current stage, requested stage, gate inputs
// Output: Allow / Deny(reason) sealed Decision; linear-only, CLOSED terminal
// Pos: project/core/ - pure rule, no Spring/JPA
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

import java.util.Objects;

/**
 * PRD §5.4 项目阶段线性 FSM 决策。
 * <p>
 * 规则：
 * <ul>
 *   <li>仅允许相邻阶段顺向推进（INITIATED→...→CLOSED）。</li>
 *   <li>CLOSED 为终态，任何离开均拒绝。</li>
 *   <li>跨级跳转、倒退、同态自循环均拒绝。</li>
 *   <li>EVALUATING 内部子状态线性：IN_PROGRESS→AWAITING_BOARD→ANNOUNCED。</li>
 * </ul>
 */
public final class ProjectStageTransitionPolicy {

    private ProjectStageTransitionPolicy() {
    }

    public static Decision decide(ProjectStage current, ProjectStage requested, GateInputs gateInputs) {
        if (current == null || requested == null) {
            return new Decision.Deny("current/requested stage 不能为空");
        }
        if (current.isTerminal()) {
            return new Decision.Deny("项目已结项，不可再次切换阶段");
        }
        if (current == requested) {
            return new Decision.Deny("不能切换到当前阶段（同态）");
        }
        ProjectStage expectedNext = next(current);
        if (expectedNext == null || requested != expectedNext) {
            return new Decision.Deny("非法跳转：" + current + "→" + requested + "，仅允许线性顺推到 " + expectedNext);
        }
        // gateInputs 仅提供给将来扩展（如保证金/任务全完成），此处不强校验，由 shell 层拼装。
        Objects.requireNonNull(gateInputs, "gateInputs 不能为空");
        return Decision.ALLOW;
    }

    public static Decision decideEvaluationSub(EvaluationSubStage current, EvaluationSubStage requested) {
        if (current == null || requested == null) {
            return new Decision.Deny("evaluation sub-stage 不能为空");
        }
        if (current == requested) {
            return new Decision.Deny("不能切换到当前子状态");
        }
        EvaluationSubStage expected = nextSub(current);
        if (expected == null || requested != expected) {
            return new Decision.Deny("评标子状态非法跳转：" + current + "→" + requested);
        }
        return Decision.ALLOW;
    }

    private static ProjectStage next(ProjectStage s) {
        return switch (s) {
            case INITIATED -> ProjectStage.DRAFTING;
            case DRAFTING -> ProjectStage.EVALUATING;
            case EVALUATING -> ProjectStage.RESULT_PENDING;
            case RESULT_PENDING -> ProjectStage.RETROSPECTIVE;
            case RETROSPECTIVE -> ProjectStage.CLOSED;
            case CLOSED -> null;
        };
    }

    private static EvaluationSubStage nextSub(EvaluationSubStage s) {
        return switch (s) {
            case IN_PROGRESS -> EvaluationSubStage.AWAITING_BOARD;
            case AWAITING_BOARD -> EvaluationSubStage.ANNOUNCED;
            case ANNOUNCED -> null;
        };
    }

    /** Gate inputs 占位：未来由 shell 注入（保证金已退回、任务全完成等）。 */
    public record GateInputs(boolean allTasksCompleted, boolean depositReturnedOrNotRequired) {
        public static final GateInputs EMPTY = new GateInputs(false, false);
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
