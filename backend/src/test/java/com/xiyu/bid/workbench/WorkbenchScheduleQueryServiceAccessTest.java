package com.xiyu.bid.workbench;

import com.xiyu.bid.calendar.dto.CalendarEventDTO;
import com.xiyu.bid.calendar.entity.EventType;
import com.xiyu.bid.calendar.service.CalendarService;
import com.xiyu.bid.demo.service.DemoDataProvider;
import com.xiyu.bid.demo.service.DemoFusionService;
import com.xiyu.bid.demo.service.DemoModeService;
import com.xiyu.bid.workbench.service.WorkbenchScheduleQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkbenchScheduleQueryServiceAccessTest {

    @Mock
    private CalendarService calendarService;
    @Mock
    private DemoModeService demoModeService;

    @Test
    void shouldInheritCalendarServiceProjectFilteringWithoutOwnAccessPolicy() {
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);
        CalendarEventDTO visible = event(1L, 100L, LocalDate.of(2024, 3, 2));
        CalendarEventDTO global = event(2L, null, LocalDate.of(2024, 3, 1));
        when(calendarService.getEventsByDateRange(start, end)).thenReturn(List.of(visible, global));
        when(demoModeService.isEnabled()).thenReturn(false);

        WorkbenchScheduleQueryService service = new WorkbenchScheduleQueryService(
                calendarService,
                demoModeService,
                new DemoDataProvider(),
                new DemoFusionService()
        );

        var response = service.getScheduleOverview(start, end, 99L);

        assertThat(response.getEvents()).extracting(CalendarEventDTO::getId).containsExactly(2L, 1L);
        assertThat(response.getTotal()).isEqualTo(2);
        verify(calendarService).getEventsByDateRange(start, end);
    }

    private CalendarEventDTO event(Long id, Long projectId, LocalDate eventDate) {
        return CalendarEventDTO.builder()
                .id(id)
                .eventDate(eventDate)
                .eventType(EventType.MEETING)
                .title("工作台事件" + id)
                .projectId(projectId)
                .isUrgent(false)
                .build();
    }
}
