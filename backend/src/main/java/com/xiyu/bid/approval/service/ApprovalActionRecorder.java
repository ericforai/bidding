// Input: approval action repository and operation metadata
// Output: persisted approval action records
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.approval.service;

import com.xiyu.bid.approval.entity.ApprovalAction;
import com.xiyu.bid.approval.enums.ApprovalActionType;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import com.xiyu.bid.approval.repository.ApprovalActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审批操作记录写入器
 */
@Component
@RequiredArgsConstructor
public class ApprovalActionRecorder {

    private final ApprovalActionRepository actionRepository;

    public void record(
            UUID requestId,
            ApprovalActionType actionType,
            Long actorId,
            String actorName,
            String comment,
            ApprovalStatus previousStatus,
            ApprovalStatus newStatus
    ) {
        ApprovalAction action = ApprovalAction.builder()
                .approvalRequestId(requestId)
                .actionType(actionType)
                .actorId(actorId)
                .actorName(actorName)
                .comment(comment)
                .actionTime(LocalDateTime.now())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .build();
        actionRepository.save(action);
    }
}
