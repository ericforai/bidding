package com.xiyu.bid.workbench.controller;

import com.xiyu.bid.calendar.dto.CalendarEventDTO;
import com.xiyu.bid.calendar.dto.ScheduleOverviewDTO;
import com.xiyu.bid.calendar.service.CalendarService;
import com.xiyu.bid.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/workbench")
@RequiredArgsConstructor
@Slf4j
public class WorkbenchScheduleController {

    private final CalendarService calendarService;

    @GetMapping("/schedule-overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ScheduleOverviewDTO>> getScheduleOverview(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @RequestParam(required = false) Long assigneeId) {
        log.info("GET /api/workbench/schedule-overview - Fetching schedule overview from {} to {}, assignee={}", start, end, assigneeId);

        List<CalendarEventDTO> events = calendarService.getEventsByMonth(start.getYear(), start.getMonthValue()).stream()
                .filter(item -> !item.getEventDate().isBefore(start) && !item.getEventDate().isAfter(end))
                .sorted(Comparator.comparing(CalendarEventDTO::getEventDate))
                .toList();

        ScheduleOverviewDTO response = ScheduleOverviewDTO.builder()
                .start(start)
                .end(end)
                .total(events.size())
                .urgent(events.stream().filter(item -> Boolean.TRUE.equals(item.getIsUrgent())).count())
                .events(events)
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
