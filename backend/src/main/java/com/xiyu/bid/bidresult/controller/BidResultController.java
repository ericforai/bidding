package com.xiyu.bid.bidresult.controller;

import com.xiyu.bid.bidresult.dto.BidResultActionRequest;
import com.xiyu.bid.bidresult.dto.BidResultCompetitorReportRowDTO;
import com.xiyu.bid.bidresult.dto.BidResultDetailDTO;
import com.xiyu.bid.bidresult.dto.BidResultFetchResultDTO;
import com.xiyu.bid.bidresult.dto.BidResultOverviewDTO;
import com.xiyu.bid.bidresult.dto.BidResultRegisterRequest;
import com.xiyu.bid.bidresult.dto.BidResultReminderDTO;
import com.xiyu.bid.bidresult.dto.BidResultSyncResponseDTO;
import com.xiyu.bid.bidresult.dto.BidResultUpdateRequest;
import com.xiyu.bid.bidresult.dto.CompetitorWinDTO;
import com.xiyu.bid.bidresult.dto.CompetitorWinRequest;
import com.xiyu.bid.bidresult.service.BidResultQueryService;
import com.xiyu.bid.bidresult.service.BidResultRegistrationService;
import com.xiyu.bid.bidresult.service.BidResultReminderService;
import com.xiyu.bid.bidresult.service.BidResultSyncService;
import com.xiyu.bid.bidresult.service.CompetitorReportService;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Input: HTTP 请求
 * Output: ApiResponse 包装的 BidResult 视图 DTO
 * Pos: backend/bidresult/controller - 薄 HTTP 层，只做授权 + 派发，不做业务决策。
 */
@RestController
@RequestMapping("/api/bid-results")
@RequiredArgsConstructor
public class BidResultController {
    private static final String ADMIN_MANAGER_STAFF_EXPR = "hasAnyRole('ADMIN', 'MANAGER', 'STAFF')";
    private static final String ADMIN_MANAGER_EXPR = "hasAnyRole('ADMIN', 'MANAGER')";

    private final BidResultQueryService queryService;
    private final BidResultRegistrationService registrationService;
    private final BidResultReminderService reminderService;
    private final BidResultSyncService syncService;
    private final CompetitorReportService competitorReportService;
    private final AuthService authService;

    @GetMapping("/overview")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultOverviewDTO> getOverview() {
        return ApiResponse.success(queryService.getOverview(competitorReportService.countCompetitorsWithRecords()));
    }

    @PostMapping("/sync")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultSyncResponseDTO> sync(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success(syncService.syncInternal(user.getId(), user.getFullName()));
    }

    @PostMapping("/fetch")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultSyncResponseDTO> fetch(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success(syncService.fetchPublicResults(user.getId(), user.getFullName()));
    }

    @GetMapping("/fetch-results")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<List<BidResultFetchResultDTO>> getFetchResults() {
        return ApiResponse.success(queryService.getFetchResults());
    }

    @PostMapping("/register")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultFetchResultDTO> register(@RequestBody BidResultRegisterRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success("已登记投标结果", registrationService.registerAward(request, user.getId(), user.getFullName()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultFetchResultDTO> update(@PathVariable Long id, @RequestBody BidResultUpdateRequest request) {
        return ApiResponse.success("已更新投标结果", registrationService.updateAward(id, request));
    }

    @PostMapping("/fetch-results/{id}/confirm")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultFetchResultDTO> confirm(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success(registrationService.confirmFetchResult(id, user.getId(), user.getFullName()));
    }

    @PostMapping("/fetch-results/{id}/ignore")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<Void> ignore(@PathVariable Long id, @RequestBody(required = false) BidResultActionRequest request) {
        registrationService.ignoreFetchResult(id, request == null ? null : request.getComment());
        return ApiResponse.success("已忽略该记录", null);
    }

    @PostMapping("/fetch-results/confirm-batch")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultSyncResponseDTO> confirmBatch(@RequestBody BidResultActionRequest request,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        int count = registrationService.confirmBatch(request.getIds(), user.getId(), user.getFullName());
        return ApiResponse.success(BidResultSyncResponseDTO.builder().affectedCount(count).message("批量确认完成").build());
    }

    @GetMapping("/reminders")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<List<BidResultReminderDTO>> getReminders() {
        return ApiResponse.success(queryService.getReminders());
    }

    @PostMapping("/reminders/send")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultReminderDTO> sendReminder(@RequestBody BidResultActionRequest request,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success(
                reminderService.sendReminder(request.getResultId(), request.getComment(), user.getId(), user.getFullName())
        );
    }

    @PostMapping("/reminders/send-batch")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultSyncResponseDTO> sendReminderBatch(@RequestBody BidResultActionRequest request,
                                                                   @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        int count = reminderService.sendReminders(request.getIds(), request.getComment(), user.getId(), user.getFullName());
        return ApiResponse.success(BidResultSyncResponseDTO.builder().affectedCount(count).message("批量提醒完成").build());
    }

    @GetMapping("/{id}")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultDetailDTO> getDetail(@PathVariable Long id) {
        return ApiResponse.success(queryService.getDetail(id));
    }

    @GetMapping("/competitor-report")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<List<BidResultCompetitorReportRowDTO>> getCompetitorReport() {
        return ApiResponse.success(competitorReportService.getCompetitorReport());
    }

    @GetMapping("/competitor-wins")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<List<CompetitorWinDTO>> listCompetitorWins() {
        return ApiResponse.success(competitorReportService.listCompetitorWins());
    }

    @PostMapping("/competitor-wins")
    @PreAuthorize(ADMIN_MANAGER_EXPR)
    public ApiResponse<CompetitorWinDTO> registerCompetitorWin(@RequestBody CompetitorWinRequest request,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success("已登记竞争对手中标记录",
                competitorReportService.registerCompetitorWin(request, user.getId(), user.getFullName()));
    }

    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("Authenticated user context is missing");
        }
        try {
            return authService.resolveUserByUsername(userDetails.getUsername());
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            throw new IllegalStateException("Authenticated user not found", ex);
        }
    }
}
