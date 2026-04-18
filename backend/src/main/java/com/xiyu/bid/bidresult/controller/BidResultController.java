package com.xiyu.bid.bidresult.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.bidresult.dto.BidResultActionRequest;
import com.xiyu.bid.bidresult.dto.BidResultCompetitorReportRowDTO;
import com.xiyu.bid.bidresult.dto.BidResultDetailDTO;
import com.xiyu.bid.bidresult.dto.BidResultFetchResultDTO;
import com.xiyu.bid.bidresult.dto.BidResultOverviewDTO;
import com.xiyu.bid.bidresult.dto.BidResultReminderDTO;
import com.xiyu.bid.bidresult.dto.BidResultSyncResponseDTO;
import com.xiyu.bid.bidresult.service.BidResultService;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/bid-results")
@RequiredArgsConstructor
public class BidResultController {
    private static final String ADMIN_MANAGER_STAFF_EXPR = "hasAnyRole('ADMIN', 'MANAGER', 'STAFF')";

    private final BidResultService bidResultService;
    private final AuthService authService;

    @GetMapping("/overview")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultOverviewDTO> getOverview() {
        return ApiResponse.success(bidResultService.getOverview());
    }

    @PostMapping("/sync")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultSyncResponseDTO> sync(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success(bidResultService.syncInternal(user.getId(), user.getFullName()));
    }

    @PostMapping("/fetch")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultSyncResponseDTO> fetch(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success(bidResultService.fetchPublicResults(user.getId(), user.getFullName()));
    }

    @GetMapping("/fetch-results")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<List<BidResultFetchResultDTO>> getFetchResults() {
        return ApiResponse.success(bidResultService.getFetchResults());
    }

    @PostMapping("/fetch-results/{id}/confirm")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultFetchResultDTO> confirm(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success(bidResultService.confirmFetchResult(id, user.getId(), user.getRole()));
    }

    @PostMapping("/fetch-results/{id}/ignore")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<Void> ignore(@PathVariable Long id,
                                    @RequestBody(required = false) BidResultActionRequest request,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        bidResultService.ignoreFetchResult(id, request == null ? null : request.getComment(), user.getId(), user.getRole());
        return ApiResponse.success("已忽略该记录", null);
    }

    @PostMapping("/fetch-results/confirm-batch")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultSyncResponseDTO> confirmBatch(@RequestBody BidResultActionRequest request,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        int count = bidResultService.confirmBatch(request.getIds(), user.getId(), user.getRole());
        return ApiResponse.success(BidResultSyncResponseDTO.builder().affectedCount(count).message("批量确认完成").build());
    }

    @GetMapping("/reminders")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<List<BidResultReminderDTO>> getReminders() {
        return ApiResponse.success(bidResultService.getReminders());
    }

    @PostMapping("/reminders/send")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultReminderDTO> sendReminder(@RequestBody BidResultActionRequest request,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success(
                bidResultService.sendReminder(request.getResultId(), request.getComment(), user.getId(), user.getFullName())
        );
    }

    @PostMapping("/reminders/send-batch")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultSyncResponseDTO> sendReminderBatch(@RequestBody BidResultActionRequest request,
                                                                   @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        int count = bidResultService.sendReminders(request.getIds(), request.getComment(), user.getId(), user.getFullName());
        return ApiResponse.success(BidResultSyncResponseDTO.builder().affectedCount(count).message("批量提醒完成").build());
    }

    @GetMapping("/{id}")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<BidResultDetailDTO> getDetail(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ApiResponse.success(bidResultService.getDetail(id, user.getId(), user.getRole()));
    }

    @GetMapping("/competitor-report")
    @PreAuthorize(ADMIN_MANAGER_STAFF_EXPR)
    public ApiResponse<List<BidResultCompetitorReportRowDTO>> getCompetitorReport() {
        return ApiResponse.success(bidResultService.getCompetitorReport());
    }

    private User getCurrentUser(UserDetails userDetails) {
        try {
            return authService.resolveUserByUsername(userDetails.getUsername());
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found", ex);
        }
    }
}
