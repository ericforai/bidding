// Input: 4 result types × missing-field 矩阵 + 锁定字段差异
// Output: JUnit5 断言覆盖必填校验 + lockedFields 行为
// Pos: backend test source - pure JUnit5
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitiationFieldPolicyTest {

    private static InitiationFieldPolicy.InitiationInput full() {
        return new InitiationFieldPolicy.InitiationInput(
                "国家电网",
                3,
                12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("100000.00"),
                LocalDateTime.of(2026, 6, 1, 9, 30),
                42L,
                "投标部",
                new BigDecimal("50000.00"),
                "银行汇票",
                null);
    }

    @Test
    void fullInput_allowed() {
        assertTrue(InitiationFieldPolicy.validate(full()).allowed());
    }

    @Test
    void missingOwnerUnit_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("  ", 3, 12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("1"), LocalDateTime.now(), 1L, "部门",
                new BigDecimal("1"), "汇票", null);
        var d = InitiationFieldPolicy.validate(in);
        var deny = assertInstanceOf(InitiationFieldPolicy.Decision.Deny.class, d);
        assertTrue(deny.missingFields().contains("ownerUnit"));
    }

    @Test
    void missingExpectedBidders_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 0, 12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("1"), LocalDateTime.now(), 1L, "部门",
                new BigDecimal("1"), "汇票", null);
        var deny = assertInstanceOf(InitiationFieldPolicy.Decision.Deny.class,
                InitiationFieldPolicy.validate(in));
        assertTrue(deny.missingFields().contains("expectedBidders"));
    }

    @Test
    void missingProjectType_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 3, 12, null,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("1"), LocalDateTime.now(), 1L, "部门",
                new BigDecimal("1"), "汇票", null);
        assertTrue(((InitiationFieldPolicy.Decision.Deny)
                InitiationFieldPolicy.validate(in)).missingFields().contains("projectType"));
    }

    @Test
    void missingCustomerType_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 3, 12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                null, new BigDecimal("1"), LocalDateTime.now(), 1L, "部门",
                new BigDecimal("1"), "汇票", null);
        assertTrue(((InitiationFieldPolicy.Decision.Deny)
                InitiationFieldPolicy.validate(in)).missingFields().contains("customerType"));
    }

    @Test
    void missingAnnualRevenue_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 3, 12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                BigDecimal.ZERO, LocalDateTime.now(), 1L, "部门",
                new BigDecimal("1"), "汇票", null);
        assertTrue(((InitiationFieldPolicy.Decision.Deny)
                InitiationFieldPolicy.validate(in)).missingFields().contains("annualRevenue"));
    }

    @Test
    void missingBidOpenTime_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 3, 12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("1"), null, 1L, "部门",
                new BigDecimal("1"), "汇票", null);
        assertTrue(((InitiationFieldPolicy.Decision.Deny)
                InitiationFieldPolicy.validate(in)).missingFields().contains("bidOpenTime"));
    }

    @Test
    void missingOwnerUserId_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 3, 12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("1"), LocalDateTime.now(), null, "部门",
                new BigDecimal("1"), "汇票", null);
        assertTrue(((InitiationFieldPolicy.Decision.Deny)
                InitiationFieldPolicy.validate(in)).missingFields().contains("ownerUserId"));
    }

    @Test
    void missingDepartment_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 3, 12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("1"), LocalDateTime.now(), 1L, "",
                new BigDecimal("1"), "汇票", null);
        assertTrue(((InitiationFieldPolicy.Decision.Deny)
                InitiationFieldPolicy.validate(in)).missingFields().contains("departmentSnapshot"));
    }

    @Test
    void missingDepositAmount_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 3, 12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("1"), LocalDateTime.now(), 1L, "部门",
                null, "汇票", null);
        assertTrue(((InitiationFieldPolicy.Decision.Deny)
                InitiationFieldPolicy.validate(in)).missingFields().contains("depositAmount"));
    }

    @Test
    void missingDepositMethod_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 3, 12,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("1"), LocalDateTime.now(), 1L, "部门",
                new BigDecimal("1"), null, null);
        assertTrue(((InitiationFieldPolicy.Decision.Deny)
                InitiationFieldPolicy.validate(in)).missingFields().contains("depositPaymentMethod"));
    }

    @Test
    void missingContractPeriod_denied() {
        var in = new InitiationFieldPolicy.InitiationInput("国网", 3, null,
                InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING,
                InitiationFieldPolicy.CustomerType.CENTRAL_SOE,
                new BigDecimal("1"), LocalDateTime.now(), 1L, "部门",
                new BigDecimal("1"), "汇票", null);
        assertTrue(((InitiationFieldPolicy.Decision.Deny)
                InitiationFieldPolicy.validate(in)).missingFields().contains("contractPeriodMonths"));
    }

    @Test
    void competitors_optional() {
        // null 竞争对手仍允许
        assertTrue(InitiationFieldPolicy.validate(full()).allowed());
    }

    @Test
    void lockedFields_immutable() {
        assertEquals(2, InitiationFieldPolicy.lockedFields().size());
        assertTrue(InitiationFieldPolicy.lockedFields().contains("bidOpenTime"));
        assertTrue(InitiationFieldPolicy.lockedFields().contains("ownerUnit"));
    }

    @Test
    void update_unlocked_alwaysAllow() {
        var a = full();
        var b = new InitiationFieldPolicy.InitiationInput(
                "新业主", a.expectedBidders(), a.contractPeriodMonths(),
                a.projectType(), a.customerType(), a.annualRevenue(),
                LocalDateTime.of(2027, 1, 1, 9, 0),
                a.ownerUserId(), a.departmentSnapshot(),
                a.depositAmount(), a.depositPaymentMethod(), a.competitors());
        assertTrue(InitiationFieldPolicy.validateUpdate(a, b, false).allowed());
    }

    @Test
    void update_locked_changingBidOpenTime_denied() {
        var a = full();
        var b = new InitiationFieldPolicy.InitiationInput(
                a.ownerUnit(), a.expectedBidders(), a.contractPeriodMonths(),
                a.projectType(), a.customerType(), a.annualRevenue(),
                LocalDateTime.of(2027, 1, 1, 9, 0),
                a.ownerUserId(), a.departmentSnapshot(),
                a.depositAmount(), a.depositPaymentMethod(), a.competitors());
        var d = InitiationFieldPolicy.validateUpdate(a, b, true);
        assertFalse(d.allowed());
        var deny = assertInstanceOf(InitiationFieldPolicy.Decision.Deny.class, d);
        assertTrue(deny.missingFields().contains("bidOpenTime"));
    }

    @Test
    void update_locked_changingOwnerUnit_denied() {
        var a = full();
        var b = new InitiationFieldPolicy.InitiationInput(
                "新业主", a.expectedBidders(), a.contractPeriodMonths(),
                a.projectType(), a.customerType(), a.annualRevenue(),
                a.bidOpenTime(),
                a.ownerUserId(), a.departmentSnapshot(),
                a.depositAmount(), a.depositPaymentMethod(), a.competitors());
        var deny = assertInstanceOf(InitiationFieldPolicy.Decision.Deny.class,
                InitiationFieldPolicy.validateUpdate(a, b, true));
        assertTrue(deny.missingFields().contains("ownerUnit"));
    }

    @Test
    void update_locked_noLockedDelta_allowed() {
        var a = full();
        // 仅修改非锁定字段（保证金额）
        var b = new InitiationFieldPolicy.InitiationInput(
                a.ownerUnit(), a.expectedBidders(), a.contractPeriodMonths(),
                a.projectType(), a.customerType(), a.annualRevenue(),
                a.bidOpenTime(),
                a.ownerUserId(), a.departmentSnapshot(),
                new BigDecimal("99999"), a.depositPaymentMethod(), a.competitors());
        assertTrue(InitiationFieldPolicy.validateUpdate(a, b, true).allowed());
    }
}
