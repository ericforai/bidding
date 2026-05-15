// Input: 4 result types × missing-field 矩阵
// Output: JUnit5 断言覆盖必填校验
// Pos: backend test source - pure JUnit5
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrospectiveFieldPolicyTest {

    @Test
    void won_complete_allowed() {
        var d = RetrospectiveFieldPolicy.validate(BidResultType.WON,
                new RetrospectiveFieldPolicy.RetrospectiveInput("亮点", "优势", null, null, "建议"));
        assertTrue(d.allowed());
    }

    @Test
    void won_missingWinFactors_denied() {
        var d = RetrospectiveFieldPolicy.validate(BidResultType.WON,
                new RetrospectiveFieldPolicy.RetrospectiveInput("亮点", "  ", null, null, "建议"));
        assertFalse(d.allowed());
        var deny = assertInstanceOf(RetrospectiveFieldPolicy.Decision.Deny.class, d);
        assertTrue(deny.missing().contains("winFactors"));
    }

    @Test
    void won_missingAll_threeMissing() {
        var d = RetrospectiveFieldPolicy.validate(BidResultType.WON,
                new RetrospectiveFieldPolicy.RetrospectiveInput(null, null, null, null, null));
        var deny = assertInstanceOf(RetrospectiveFieldPolicy.Decision.Deny.class, d);
        assertEquals(3, deny.missing().size());
    }

    @Test
    void lost_complete_allowed() {
        var d = RetrospectiveFieldPolicy.validate(BidResultType.LOST,
                new RetrospectiveFieldPolicy.RetrospectiveInput(null, null, "丢标", "问题", "改进"));
        assertTrue(d.allowed());
    }

    @Test
    void lost_missingCompetitorNotes_denied() {
        var d = RetrospectiveFieldPolicy.validate(BidResultType.LOST,
                new RetrospectiveFieldPolicy.RetrospectiveInput(null, null, "丢标", null, "改进"));
        var deny = assertInstanceOf(RetrospectiveFieldPolicy.Decision.Deny.class, d);
        assertTrue(deny.missing().contains("competitorNotes"));
        assertEquals(1, deny.missing().size());
    }

    @Test
    void failed_complete_allowed() {
        var d = RetrospectiveFieldPolicy.validate(BidResultType.FAILED,
                new RetrospectiveFieldPolicy.RetrospectiveInput(null, null, "流标", null, "应对"));
        assertTrue(d.allowed());
    }

    @Test
    void failed_missingActions_denied() {
        var d = RetrospectiveFieldPolicy.validate(BidResultType.FAILED,
                new RetrospectiveFieldPolicy.RetrospectiveInput(null, null, "流标", null, null));
        var deny = assertInstanceOf(RetrospectiveFieldPolicy.Decision.Deny.class, d);
        assertTrue(deny.missing().contains("improvementActions"));
    }

    @Test
    void abandoned_complete_allowed() {
        var d = RetrospectiveFieldPolicy.validate(BidResultType.ABANDONED,
                new RetrospectiveFieldPolicy.RetrospectiveInput("决策", null, "弃标", null, null));
        assertTrue(d.allowed());
    }

    @Test
    void abandoned_missingBoth_denied() {
        var d = RetrospectiveFieldPolicy.validate(BidResultType.ABANDONED,
                new RetrospectiveFieldPolicy.RetrospectiveInput("", null, "", null, null));
        var deny = assertInstanceOf(RetrospectiveFieldPolicy.Decision.Deny.class, d);
        assertEquals(2, deny.missing().size());
    }

    @Test
    void nullResultType_denied() {
        var d = RetrospectiveFieldPolicy.validate(null,
                new RetrospectiveFieldPolicy.RetrospectiveInput("a", "b", "c", "d", "e"));
        assertFalse(d.allowed());
    }
}
