package com.xiyu.bid.bidresult.service;

import com.xiyu.bid.bidresult.core.BidResultReminderLogic;
import com.xiyu.bid.bidresult.dto.BidResultAssembler;
import com.xiyu.bid.bidresult.dto.BidResultReminderDTO;
import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import com.xiyu.bid.bidresult.entity.BidResultReminder;
import com.xiyu.bid.bidresult.repository.BidResultFetchResultRepository;
import com.xiyu.bid.bidresult.repository.BidResultReminderRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.BusinessException;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 提醒服务：销售上传中标通知书 / 分析报告的提醒。
 * Functional Shell: Handles I/O and orchestrates data for pure logic.
 */
@Service
@RequiredArgsConstructor
public class BidResultReminderService {

    private static final int MAX_BATCH_SIZE = 200;

    private final BidResultReminderRepository reminderRepository;
    private final BidResultFetchResultRepository fetchResultRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public BidResultReminder ensurePendingReminderForResult(BidResultFetchResult result, String comment, Long operatorId, String operatorName) {
        BidResultReminder.ReminderType reminderType = BidResultReminderLogic.determineType(result.getResult());
        BidResultReminder existing = findExistingReminder(result.getProjectId(), reminderType);

        // Fetch owners only if we are creating a new reminder
        OwnerInfo owner = existing == null ? resolveOwnerInfo(result.getProjectId()) : null;

        BidResultReminder updated = BidResultReminderLogic.calculateReminderState(
                result,
                existing,
                owner != null ? owner.id : null,
                owner != null ? owner.name : "待分配",
                comment,
                operatorId,
                operatorName,
                LocalDateTime.now(),
                false
        );

        return reminderRepository.save(updated);
    }

    @Transactional
    public BidResultReminderDTO sendReminder(Long resultId, String comment, Long operatorId, String operatorName) {
        BidResultFetchResult result = fetchResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid result fetch record not found: " + resultId));

        BidResultReminder.ReminderType reminderType = BidResultReminderLogic.determineType(result.getResult());
        BidResultReminder existing = findExistingReminder(result.getProjectId(), reminderType);

        OwnerInfo owner = existing == null ? resolveOwnerInfo(result.getProjectId()) : null;

        BidResultReminder updated = BidResultReminderLogic.calculateReminderState(
                result,
                existing,
                owner != null ? owner.id : null,
                owner != null ? owner.name : "待分配",
                comment,
                operatorId,
                operatorName,
                LocalDateTime.now(),
                true
        );

        return BidResultAssembler.toReminderDTO(reminderRepository.save(updated));
    }

    @Transactional
    public int sendReminders(List<Long> resultIds, String comment, Long operatorId, String operatorName) {
        List<Long> safeIds = Optional.ofNullable(resultIds).orElse(List.of());
        if (safeIds.size() > MAX_BATCH_SIZE) {
            throw new BusinessException("批量数量不得超过 " + MAX_BATCH_SIZE);
        }
        int count = 0;
        for (Long resultId : safeIds) {
            sendReminder(resultId, comment, operatorId, operatorName);
            count++;
        }
        return count;
    }

    private BidResultReminder findExistingReminder(Long projectId, BidResultReminder.ReminderType type) {
        if (projectId == null) return null;
        return reminderRepository
                .findFirstByProjectIdAndReminderTypeOrderByRemindTimeDesc(projectId, type)
                .orElse(null);
    }

    private OwnerInfo resolveOwnerInfo(Long projectId) {
        if (projectId == null) return new OwnerInfo(null, "待分配");
        
        return projectRepository.findById(projectId)
                .map(p -> {
                    Long mid = p.getManagerId();
                    String name = mid == null ? "待分配" : userRepository.findById(mid)
                            .map(User::getFullName)
                            .orElse("待分配");
                    return new OwnerInfo(mid, name);
                })
                .orElse(new OwnerInfo(null, "待分配"));
    }

    @RequiredArgsConstructor
    private static class OwnerInfo {
        final Long id;
        final String name;
    }
}
