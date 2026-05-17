package com.xiyu.bid.workbench.service;

import com.xiyu.bid.fees.repository.FeeRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import com.xiyu.bid.workbench.domain.WorkbenchDeadlinePolicy;
import com.xiyu.bid.workbench.dto.WorkbenchDeadlineStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkbenchDeadlineQueryService {

    private final TenderRepository tenderRepository;
    private final FeeRepository feeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessScopeService projectAccessScopeService;

    @Transactional(readOnly = true)
    public WorkbenchDeadlineStatsDTO getDeadlineStats(LocalDate today) {
        List<Long> allowedProjectIds = projectAccessScopeService.getAllowedProjectIdsForCurrentUser();

        var monthStart = today.withDayOfMonth(1).atStartOfDay();
        var monthEnd = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59);

        List<LocalDateTime> regDeadlines;
        List<LocalDateTime> openingTimes;
        List<LocalDateTime> depositDeadlines;

        if (allowedProjectIds.isEmpty()) {
            // Admin: 全量
            regDeadlines = tenderRepository.findRegistrationDeadlinesBetween(monthStart, monthEnd);
            openingTimes = tenderRepository.findBidOpeningTimesBetween(monthStart, monthEnd);
            depositDeadlines = feeRepository.findDepositDeadlinesBetween(monthStart, monthEnd);
        } else {
            // 非 Admin: 按项目范围过滤
            List<Long> allowedTenderIds = projectRepository.findTenderIdsByProjectIds(allowedProjectIds);
            regDeadlines = allowedTenderIds.isEmpty()
                    ? List.of()
                    : tenderRepository.findRegistrationDeadlinesByTenderIds(allowedTenderIds, monthStart, monthEnd);
            openingTimes = allowedTenderIds.isEmpty()
                    ? List.of()
                    : tenderRepository.findBidOpeningTimesByTenderIds(allowedTenderIds, monthStart, monthEnd);
            depositDeadlines = feeRepository.findDepositDeadlinesByProjectIds(allowedProjectIds, monthStart, monthEnd);
        }

        log.debug("Workbench deadline stats: reg={}, opening={}, deposit={}",
                regDeadlines.size(), openingTimes.size(), depositDeadlines.size());

        var stats = WorkbenchDeadlinePolicy.buildDeadlineStats(today, regDeadlines, openingTimes, depositDeadlines);

        return new WorkbenchDeadlineStatsDTO(
                new WorkbenchDeadlineStatsDTO.DeadlinePeriodStatsDTO(
                        stats.registrationDeadline().counts().todayCount(),
                        stats.registrationDeadline().counts().weekCount(),
                        stats.registrationDeadline().counts().monthCount()
                ),
                new WorkbenchDeadlineStatsDTO.DeadlinePeriodStatsDTO(
                        stats.bidOpening().counts().todayCount(),
                        stats.bidOpening().counts().weekCount(),
                        stats.bidOpening().counts().monthCount()
                ),
                new WorkbenchDeadlineStatsDTO.DeadlinePeriodStatsDTO(
                        stats.depositDeadline().counts().todayCount(),
                        stats.depositDeadline().counts().weekCount(),
                        stats.depositDeadline().counts().monthCount()
                )
        );
    }
}
