package com.xiyu.bid.workbench.service;

import com.xiyu.bid.fees.repository.FeeRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import com.xiyu.bid.workbench.dto.WorkbenchDeadlineStatsDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkbenchDeadlineQueryServiceTest {

    @Mock TenderRepository tenderRepository;
    @Mock FeeRepository feeRepository;
    @Mock ProjectRepository projectRepository;
    @Mock ProjectAccessScopeService projectAccessScopeService;
    @InjectMocks WorkbenchDeadlineQueryService service;

    @Test
    void adminShouldSeeAllDeadlines() {
        var today = LocalDate.of(2026, 5, 17);
        when(projectAccessScopeService.getAllowedProjectIdsForCurrentUser()).thenReturn(List.of());
        when(tenderRepository.findRegistrationDeadlinesBetween(any(), any()))
                .thenReturn(List.of(LocalDateTime.of(2026, 5, 17, 10, 0)));
        when(tenderRepository.findBidOpeningTimesBetween(any(), any())).thenReturn(List.of());
        when(feeRepository.findDepositDeadlinesBetween(any(), any())).thenReturn(List.of());

        WorkbenchDeadlineStatsDTO result = service.getDeadlineStats(today);

        assertThat(result.registrationDeadline().todayCount()).isEqualTo(1);
        assertThat(result.bidOpening().todayCount()).isZero();
        assertThat(result.depositDeadline().todayCount()).isZero();
        verifyNoInteractions(projectRepository);
    }

    @Test
    void managerShouldSeeOnlyOwnProjects() {
        var today = LocalDate.of(2026, 5, 17);
        when(projectAccessScopeService.getAllowedProjectIdsForCurrentUser()).thenReturn(List.of(1L, 2L));
        when(projectRepository.findTenderIdsByProjectIds(List.of(1L, 2L))).thenReturn(List.of(10L));
        when(tenderRepository.findRegistrationDeadlinesByTenderIds(eq(List.of(10L)), any(), any()))
                .thenReturn(List.of(LocalDateTime.of(2026, 5, 17, 10, 0)));
        when(tenderRepository.findBidOpeningTimesByTenderIds(eq(List.of(10L)), any(), any()))
                .thenReturn(List.of());
        when(feeRepository.findDepositDeadlinesByProjectIds(eq(List.of(1L, 2L)), any(), any()))
                .thenReturn(List.of());

        WorkbenchDeadlineStatsDTO result = service.getDeadlineStats(today);
        assertThat(result.registrationDeadline().todayCount()).isEqualTo(1);
    }

    @Test
    void staffWithNoAccessShouldGetZeroCounts() {
        var today = LocalDate.of(2026, 5, 17);
        when(projectAccessScopeService.getAllowedProjectIdsForCurrentUser()).thenReturn(List.of(99L));
        when(projectRepository.findTenderIdsByProjectIds(List.of(99L))).thenReturn(List.of());
        when(feeRepository.findDepositDeadlinesByProjectIds(eq(List.of(99L)), any(), any()))
                .thenReturn(List.of());

        WorkbenchDeadlineStatsDTO result = service.getDeadlineStats(today);
        assertThat(result.registrationDeadline().todayCount()).isZero();
        assertThat(result.bidOpening().todayCount()).isZero();
        assertThat(result.depositDeadline().todayCount()).isZero();
    }

    @Test
    void managerWithEmptyTenderIdsShouldGetZeroCounts() {
        var today = LocalDate.of(2026, 5, 17);
        when(projectAccessScopeService.getAllowedProjectIdsForCurrentUser()).thenReturn(List.of(1L));
        when(projectRepository.findTenderIdsByProjectIds(List.of(1L))).thenReturn(List.of());
        when(feeRepository.findDepositDeadlinesByProjectIds(eq(List.of(1L)), any(), any()))
                .thenReturn(List.of());

        WorkbenchDeadlineStatsDTO result = service.getDeadlineStats(today);
        assertThat(result.registrationDeadline().todayCount()).isZero();
    }
}
