// Input: ProjectStage + fieldName
// Output: Decision (Allow | Deny{reason}) -- PRD §3.6 结项后全字段锁定 + §3.1.2 立项锁定
// Pos: project/core/ - pure rule, no Spring/JPA
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

import java.util.Objects;
import java.util.Set;

/**
 * PRD §3.6 全字段锁定策略 + §3.1.2 立项锁定字段策略（纯规则，无 Spring/JPA）。
 *
 * <p>规则：
 * <ul>
 *   <li>当阶段为 CLOSED：拒绝所有字段写入（"项目已结项，全字段锁定"）。</li>
 *   <li>当阶段为 INITIATED 或更后：拒绝写入立项锁定字段（bidOpenTime / ownerUnit）。</li>
 *   <li>其余字段、非 CLOSED 阶段一律允许（具体业务校验由 service 层处理）。</li>
 * </ul>
 */
public final class ProjectFieldLockPolicy {

    private static final Set<String> INITIATION_LOCKED_FIELDS = Set.of("bidOpenTime", "ownerUnit");

    private ProjectFieldLockPolicy() {
    }

    public static Decision assertWritable(ProjectStage stage, String fieldName) {
        Objects.requireNonNull(stage, "stage 不能为空");
        if (fieldName == null || fieldName.isBlank()) {
            return new Decision.Deny("fieldName 不能为空");
        }
        if (stage == ProjectStage.CLOSED) {
            return new Decision.Deny("项目已结项，全字段锁定");
        }
        if (INITIATION_LOCKED_FIELDS.contains(fieldName)) {
            // 立项提交后这两个字段已锁。即便仍在 INITIATED 之后阶段也不可改。
            return new Decision.Deny("提交后不可修改：" + fieldName);
        }
        return Decision.ALLOW;
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
