// Input: tenderId + 当前用户 id
// Output: 实例级权限判定 (boolean) — 是否可填 / 是否可决策
// Pos: Service/权限支撑层（命令式外壳）
// 维护声明: 仅做数据访问 + 委托纯规则；判定逻辑统一在 AssignmentPermissionRules。
package com.xiyu.bid.tender.service;

import com.xiyu.bid.batch.entity.TenderAssignmentRecord;
import com.xiyu.bid.batch.repository.TenderAssignmentRecordRepository;
import com.xiyu.bid.tender.core.AssignmentPermissionRules;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 标讯实例级权限的应用外壳。
 *
 * <p>规则统一在 {@link AssignmentPermissionRules}；本类只做：
 * <ol>
 *   <li>查询 latest {@link TenderAssignmentRecord}</li>
 *   <li>把记录与用户 id 委托给纯规则</li>
 * </ol>
 */
@Component
public class TenderAssignmentPermissions {

    private final TenderAssignmentRecordRepository repository;

    public TenderAssignmentPermissions(TenderAssignmentRecordRepository repository) {
        this.repository = repository;
    }

    /** 用户是否为该标讯的 latest assignee（可填 / 提交评估表）。 */
    public boolean canFill(Long tenderId, Long userId) {
        return AssignmentPermissionRules.canFill(latest(tenderId), userId);
    }

    /** 用户是否为该标讯的 latest assigned-by（可投标 / 弃标）。 */
    public boolean canDecide(Long tenderId, Long userId) {
        return AssignmentPermissionRules.canDecide(latest(tenderId), userId);
    }

    private Optional<TenderAssignmentRecord> latest(Long tenderId) {
        if (tenderId == null) return Optional.empty();
        return repository.findFirstByTenderIdOrderByAssignedAtDesc(tenderId);
    }
}
