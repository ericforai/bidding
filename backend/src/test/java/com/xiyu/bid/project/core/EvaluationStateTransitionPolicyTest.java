// Input: 评标子状态转换案例
// Output: JUnit5 断言覆盖线性 happy + 拒绝矩阵
// Pos: backend test source - 纯 JUnit5
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationStateTransitionPolicyTest {

    @Test
    void linearHappyPath_allAllowed() {
        assertTrue(EvaluationStateTransitionPolicy.decide(
                EvaluationSubStage.IN_PROGRESS, EvaluationSubStage.AWAITING_BOARD).allowed());
        assertTrue(EvaluationStateTransitionPolicy.decide(
                EvaluationSubStage.AWAITING_BOARD, EvaluationSubStage.ANNOUNCED).allowed());
    }

    @ParameterizedTest
    @MethodSource("illegalPairs")
    void illegalJumps_allDenied(EvaluationSubStage from, EvaluationSubStage to) {
        var d = EvaluationStateTransitionPolicy.decide(from, to);
        assertFalse(d.allowed(), "应拒绝 " + from + "→" + to);
        assertInstanceOf(EvaluationStateTransitionPolicy.Decision.Deny.class, d);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> illegalPairs() {
        List<org.junit.jupiter.params.provider.Arguments> args = new ArrayList<>();
        EvaluationSubStage[] all = EvaluationSubStage.values();
        for (EvaluationSubStage f : all) {
            for (EvaluationSubStage t : all) {
                if (isLegalLinear(f, t)) continue;
                args.add(org.junit.jupiter.params.provider.Arguments.of(f, t));
            }
        }
        return args.stream();
    }

    private static boolean isLegalLinear(EvaluationSubStage from, EvaluationSubStage to) {
        return switch (from) {
            case IN_PROGRESS -> to == EvaluationSubStage.AWAITING_BOARD;
            case AWAITING_BOARD -> to == EvaluationSubStage.ANNOUNCED;
            case ANNOUNCED -> false;
        };
    }

    @Test
    void announcedTerminal_allOutgoingDenied() {
        for (EvaluationSubStage to : EvaluationSubStage.values()) {
            assertFalse(EvaluationStateTransitionPolicy.decide(
                    EvaluationSubStage.ANNOUNCED, to).allowed());
        }
    }

    @Test
    void selfTransitionDenied() {
        for (EvaluationSubStage s : EvaluationSubStage.values()) {
            assertFalse(EvaluationStateTransitionPolicy.decide(s, s).allowed());
        }
    }

    @Test
    void nullArguments_denied() {
        assertFalse(EvaluationStateTransitionPolicy.decide(null, EvaluationSubStage.IN_PROGRESS).allowed());
        assertFalse(EvaluationStateTransitionPolicy.decide(EvaluationSubStage.IN_PROGRESS, null).allowed());
        assertFalse(EvaluationStateTransitionPolicy.decide(null, null).allowed());
    }

    @Test
    void reverseDenied() {
        assertFalse(EvaluationStateTransitionPolicy.decide(
                EvaluationSubStage.AWAITING_BOARD, EvaluationSubStage.IN_PROGRESS).allowed());
        assertFalse(EvaluationStateTransitionPolicy.decide(
                EvaluationSubStage.ANNOUNCED, EvaluationSubStage.AWAITING_BOARD).allowed());
    }

    @Test
    void skipDenied() {
        assertFalse(EvaluationStateTransitionPolicy.decide(
                EvaluationSubStage.IN_PROGRESS, EvaluationSubStage.ANNOUNCED).allowed());
    }
}
