// Input: approval repositories, policies, and DTO assemblers
// Output: approval query results and read models
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.approval.service;

import com.xiyu.bid.approval.core.ApprovalPermissionPolicy;
import com.xiyu.bid.approval.core.ApprovalRuleResult;
import com.xiyu.bid.approval.dto.ApprovalDetailDTO;
import com.xiyu.bid.approval.dto.ApprovalStatisticsDTO;
import com.xiyu.bid.approval.entity.ApprovalRequest;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import com.xiyu.bid.approval.repository.ApprovalRequestRepository;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审批查询服务
 */
@Service
@RequiredArgsConstructor
public class ApprovalQueryService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalPermissionPolicy permissionPolicy;
    private final ApprovalDetailAssembler detailAssembler;

    public Page<ApprovalDetailDTO> getPendingApprovals(
            Long currentUserId,
            User.Role currentUserRole,
            Long approverId,
            Pageable pageable
    ) {
        assertAllowed(permissionPolicy.canViewPendingQueue(currentUserId, currentUserRole, approverId));

        List<ApprovalRequest> requests = loadPendingApprovals(currentUserId, currentUserRole, approverId);
        return buildPage(requests.stream().map(detailAssembler::toDetailDTO).toList(), pageable);
    }

    public ApprovalStatisticsDTO getStatistics() {
        Long totalCount = requestRepository.count();
        Map<String, Long> statusCounts = mapRowsToStringLong(requestRepository.countByStatus());

        Long approvedCount = statusCounts.getOrDefault(ApprovalStatus.APPROVED.name(), 0L);
        Long rejectedCount = statusCounts.getOrDefault(ApprovalStatus.REJECTED.name(), 0L);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        Long totalDecisions = approvedCount + rejectedCount;
        Double approvalRate = totalDecisions > 0
                ? (approvedCount.doubleValue() / totalDecisions) * 100
                : null;

        return ApprovalStatisticsDTO.builder()
                .totalCount(totalCount)
                .pendingCount(statusCounts.getOrDefault(ApprovalStatus.PENDING.name(), 0L))
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .cancelledCount(statusCounts.getOrDefault(ApprovalStatus.CANCELLED.name(), 0L))
                .todaySubmitted(requestRepository.countBySubmittedAtBetween(todayStart, todayStart.plusDays(1)))
                .monthSubmitted(requestRepository.countBySubmittedAtBetweenAndStatusIn(
                        monthStart,
                        monthStart.plusMonths(1),
                        List.of(ApprovalStatus.PENDING, ApprovalStatus.APPROVED, ApprovalStatus.REJECTED, ApprovalStatus.CANCELLED)
                ))
                .overdueCount(requestRepository.countByStatusAndDueDateBefore(ApprovalStatus.PENDING, now))
                .nearDueCount(requestRepository.countByStatusAndDueDateBetween(ApprovalStatus.PENDING, now, now.plusHours(24)))
                .avgProcessingHours(calculateAverageProcessingHours())
                .approvalRate(approvalRate)
                .byType(mapRowsToStringLong(requestRepository.countByType()))
                .byPriority(mapRowsToIntegerLong(requestRepository.countByPriority()))
                .build();
    }

    public ApprovalDetailDTO getApprovalDetail(UUID requestId, Long currentUserId, User.Role currentUserRole) {
        ApprovalRequest request = getApprovalRequestEntity(requestId);
        assertAllowed(permissionPolicy.canViewApprovalRequest(request, currentUserId, currentUserRole));
        return detailAssembler.toDetailDTO(request);
    }

    public Page<ApprovalDetailDTO> getMyApprovals(Long userId, ApprovalStatus status, Pageable pageable) {
        List<ApprovalDetailDTO> dtos = requestRepository.findByRequesterIdOrderByCreatedAtDesc(userId).stream()
                .filter(request -> status == null || request.getStatus() == status)
                .map(detailAssembler::toDetailDTO)
                .toList();
        return buildPage(dtos, pageable);
    }

    private List<ApprovalRequest> loadPendingApprovals(Long currentUserId, User.Role currentUserRole, Long approverId) {
        if (approverId != null) {
            return requestRepository.findByStatusAndCurrentApproverIdOrderByPriorityDescCreatedAtDesc(
                    ApprovalStatus.PENDING, approverId
            );
        }
        if (permissionPolicy.isPrivileged(currentUserRole)) {
            return requestRepository.findByStatusOrderByPriorityDescCreatedAtDesc(ApprovalStatus.PENDING);
        }
        return requestRepository.findByStatusAndCurrentApproverIdOrderByPriorityDescCreatedAtDesc(
                ApprovalStatus.PENDING, currentUserId
        );
    }

    private ApprovalRequest getApprovalRequestEntity(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("审批请求不存在: " + requestId));
    }

    private Double calculateAverageProcessingHours() {
        return requestRepository.findByStatusInAndCompletedAtIsNotNull(
                        List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED)
                ).stream()
                .filter(item -> item.getSubmittedAt() != null && item.getCompletedAt() != null)
                .mapToLong(item -> ChronoUnit.MINUTES.between(item.getSubmittedAt(), item.getCompletedAt()))
                .average()
                .stream()
                .map(avgMinutes -> avgMinutes / 60.0)
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private Map<String, Long> mapRowsToStringLong(List<Object[]> rows) {
        Map<String, Long> mapped = new HashMap<>();
        for (Object[] row : rows) {
            mapped.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return mapped;
    }

    private Map<Integer, Long> mapRowsToIntegerLong(List<Object[]> rows) {
        Map<Integer, Long> mapped = new HashMap<>();
        for (Object[] row : rows) {
            mapped.put((Integer) row[0], (Long) row[1]);
        }
        return mapped;
    }

    private Page<ApprovalDetailDTO> buildPage(List<ApprovalDetailDTO> dtos, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<ApprovalDetailDTO> pageContent = start < dtos.size() ? dtos.subList(start, end) : List.of();
        return new PageImpl<>(pageContent, pageable, dtos.size());
    }

    private void assertAllowed(ApprovalRuleResult result) {
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
    }
}
