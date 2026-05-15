// Input: hasDeposit × returnStatus × evidence 矩阵
// Output: JUnit5 断言覆盖 §3.6 结项闸门规则
// Pos: backend test source - pure JUnit5
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

import com.xiyu.bid.project.core.ProjectClosureGatePolicy.ClosureInput;
import com.xiyu.bid.project.core.ProjectClosureGatePolicy.Decision;
import com.xiyu.bid.project.core.ProjectClosureGatePolicy.DepositReturnStatus;
import com.xiyu.bid.project.core.ProjectClosureGatePolicy.DepositSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectClosureGatePolicyTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 5, 1, 10, 0);
    private static final Long DOC = 88L;

    // ----- Allow 路径 -----

    @Test
    void noDeposit_allowed() {
        var d = ProjectClosureGatePolicy.decide(DepositSnapshot.none(), ClosureInput.EMPTY);
        assertTrue(d.allowed());
    }

    @Test
    void hasDeposit_returned_withDateAndDoc_allowed() {
        var d = ProjectClosureGatePolicy.decide(
                DepositSnapshot.returned(WHEN, DOC), ClosureInput.EMPTY);
        assertTrue(d.allowed());
    }

    @Test
    void hasDeposit_returned_factoryConstructor_allowed() {
        var snap = new DepositSnapshot(true, DepositReturnStatus.RETURNED, WHEN, DOC);
        var d = ProjectClosureGatePolicy.decide(snap, ClosureInput.EMPTY);
        assertTrue(d.allowed());
    }

    // ----- Deny 路径：保证金未退回（PRD 核心闸门） -----

    @Test
    void hasDeposit_notReturned_deniedWithCoreReason() {
        var d = ProjectClosureGatePolicy.decide(DepositSnapshot.notReturned(), ClosureInput.EMPTY);
        assertFalse(d.allowed());
        var deny = assertInstanceOf(Decision.Deny.class, d);
        assertTrue(deny.reasons().contains("保证金未退回"));
    }

    @Test
    void hasDeposit_notReturned_explicitConstructor_deniedWithCoreReason() {
        var snap = new DepositSnapshot(true, DepositReturnStatus.NOT_RETURNED, null, null);
        var d = ProjectClosureGatePolicy.decide(snap, ClosureInput.EMPTY);
        var deny = assertInstanceOf(Decision.Deny.class, d);
        assertEquals("保证金未退回", deny.reasons().get(0));
    }

    @Test
    void hasDeposit_naStatus_deniedAsAnomaly() {
        var snap = new DepositSnapshot(true, DepositReturnStatus.NA, null, null);
        var d = ProjectClosureGatePolicy.decide(snap, ClosureInput.EMPTY);
        var deny = assertInstanceOf(Decision.Deny.class, d);
        assertTrue(deny.reasons().get(0).contains("保证金状态异常"));
    }

    // ----- Deny 路径：返回但缺凭证 -----

    @Test
    void hasDeposit_returned_missingDate_denied() {
        var snap = new DepositSnapshot(true, DepositReturnStatus.RETURNED, null, DOC);
        var d = ProjectClosureGatePolicy.decide(snap, ClosureInput.EMPTY);
        var deny = assertInstanceOf(Decision.Deny.class, d);
        assertTrue(deny.reasons().contains("缺少保证金退回日期"));
    }

    @Test
    void hasDeposit_returned_missingEvidence_denied() {
        var snap = new DepositSnapshot(true, DepositReturnStatus.RETURNED, WHEN, null);
        var d = ProjectClosureGatePolicy.decide(snap, ClosureInput.EMPTY);
        var deny = assertInstanceOf(Decision.Deny.class, d);
        assertTrue(deny.reasons().contains("缺少保证金退回凭证"));
    }

    @Test
    void hasDeposit_returned_evidenceZero_denied() {
        var snap = new DepositSnapshot(true, DepositReturnStatus.RETURNED, WHEN, 0L);
        var d = ProjectClosureGatePolicy.decide(snap, ClosureInput.EMPTY);
        var deny = assertInstanceOf(Decision.Deny.class, d);
        assertTrue(deny.reasons().contains("缺少保证金退回凭证"));
    }

    @Test
    void hasDeposit_returned_missingBoth_denied_twoReasons() {
        var snap = new DepositSnapshot(true, DepositReturnStatus.RETURNED, null, null);
        var d = ProjectClosureGatePolicy.decide(snap, ClosureInput.EMPTY);
        var deny = assertInstanceOf(Decision.Deny.class, d);
        assertEquals(2, deny.reasons().size());
        assertTrue(deny.reasons().contains("缺少保证金退回日期"));
        assertTrue(deny.reasons().contains("缺少保证金退回凭证"));
    }

    // ----- reasonText 与 null 防御 -----

    @Test
    void deny_reasonText_joinsWithSemicolon() {
        var snap = new DepositSnapshot(true, DepositReturnStatus.RETURNED, null, null);
        var d = (Decision.Deny) ProjectClosureGatePolicy.decide(snap, ClosureInput.EMPTY);
        assertTrue(d.reasonText().contains("；"));
    }

    @Test
    void nullSnapshot_throws() {
        assertThrows(NullPointerException.class,
                () -> ProjectClosureGatePolicy.decide(null, ClosureInput.EMPTY));
    }

    @Test
    void nullInput_throws() {
        assertThrows(NullPointerException.class,
                () -> ProjectClosureGatePolicy.decide(DepositSnapshot.none(), null));
    }

    // ----- 工厂方法独立测试 -----

    @Test
    void factory_none_isNotHasDeposit() {
        DepositSnapshot s = DepositSnapshot.none();
        assertFalse(s.hasDeposit());
        assertEquals(DepositReturnStatus.NA, s.returnStatus());
    }

    @Test
    void factory_notReturned_isHasDepositNotReturned() {
        DepositSnapshot s = DepositSnapshot.notReturned();
        assertTrue(s.hasDeposit());
        assertEquals(DepositReturnStatus.NOT_RETURNED, s.returnStatus());
    }

    @Test
    void factory_returned_isHasDepositReturned() {
        DepositSnapshot s = DepositSnapshot.returned(WHEN, DOC);
        assertTrue(s.hasDeposit());
        assertEquals(DepositReturnStatus.RETURNED, s.returnStatus());
        assertEquals(DOC, s.evidenceDocId());
        assertEquals(WHEN, s.returnDate());
    }
}
