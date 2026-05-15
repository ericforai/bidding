// Input: DepositSnapshot + ClosureInput
// Output: Decision (Allow | Deny{reasons}) -- PRD §3.6 结项保证金强校验闸门
// Pos: project/core/ - pure rule, no Spring/JPA
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * PRD §3.6 项目结项闸门策略（纯规则，无 Spring/JPA）。
 *
 * <p>核心规则：
 * <ul>
 *   <li>当项目存在保证金（hasDeposit=true）且未退回（returnStatus != RETURNED），结项被拒绝。</li>
 *   <li>当 returnStatus == RETURNED，必须提供退回日期 + 退回凭证文档 ID。</li>
 *   <li>当 returnStatus == NA（无保证金 / 不需要退回）等价于 hasDeposit=false 时无关闸门。</li>
 *   <li>所有错误以 reasons 列表返回，便于前端整体提示。</li>
 * </ul>
 */
public final class ProjectClosureGatePolicy {

    private ProjectClosureGatePolicy() {
    }

    public static Decision decide(DepositSnapshot snapshot, ClosureInput input) {
        Objects.requireNonNull(snapshot, "snapshot 不能为空");
        Objects.requireNonNull(input, "input 不能为空");
        List<String> reasons = new ArrayList<>();

        if (snapshot.hasDeposit()) {
            if (snapshot.returnStatus() == DepositReturnStatus.NA) {
                reasons.add("保证金状态异常：存在保证金但状态为 NA");
            } else if (snapshot.returnStatus() != DepositReturnStatus.RETURNED) {
                reasons.add("保证金未退回");
            } else {
                if (snapshot.returnDate() == null) {
                    reasons.add("缺少保证金退回日期");
                }
                if (snapshot.evidenceDocId() == null || snapshot.evidenceDocId() <= 0L) {
                    reasons.add("缺少保证金退回凭证");
                }
            }
        }

        return reasons.isEmpty()
                ? Decision.ALLOW
                : new Decision.Deny(Collections.unmodifiableList(reasons));
    }

    /** 保证金退回状态。NA 仅在 hasDeposit=false 时有意义。 */
    public enum DepositReturnStatus {
        NOT_RETURNED,
        RETURNED,
        NA
    }

    /**
     * 保证金快照（只读视图）。
     * @param hasDeposit     项目是否存在保证金
     * @param returnStatus   退回状态
     * @param returnDate     退回时间（hasDeposit && RETURNED 时必填）
     * @param evidenceDocId  退回凭证文档 ID（hasDeposit && RETURNED 时必填）
     */
    public record DepositSnapshot(
            boolean hasDeposit,
            DepositReturnStatus returnStatus,
            LocalDateTime returnDate,
            Long evidenceDocId) {

        public static DepositSnapshot none() {
            return new DepositSnapshot(false, DepositReturnStatus.NA, null, null);
        }

        public static DepositSnapshot notReturned() {
            return new DepositSnapshot(true, DepositReturnStatus.NOT_RETURNED, null, null);
        }

        public static DepositSnapshot returned(LocalDateTime when, Long docId) {
            return new DepositSnapshot(true, DepositReturnStatus.RETURNED, when, docId);
        }
    }

    /** 结项提交输入（占位：未来扩展归档/备注校验）。 */
    public record ClosureInput(String archiveLocation, String notes) {
        public static final ClosureInput EMPTY = new ClosureInput(null, null);
    }

    /** Sealed Decision: Allow | Deny{reasons}. */
    public sealed interface Decision permits Decision.Allow, Decision.Deny {
        Decision ALLOW = new Allow();

        default boolean allowed() {
            return this instanceof Allow;
        }

        record Allow() implements Decision {
        }

        record Deny(List<String> reasons) implements Decision {
            public String reasonText() {
                return String.join("；", reasons);
            }
        }
    }
}
