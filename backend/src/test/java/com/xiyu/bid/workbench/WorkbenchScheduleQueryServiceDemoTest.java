package com.xiyu.bid.workbench;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkbenchScheduleQueryServiceDemoTest {

    @Mock
    private CalendarService calendarService;
    @Mock
    private DemoModeService demoModeService;

    @Test
    void shouldAppendDemoEventsWhenE2eModeEnabled() {
        when(demoModeService.isEnabled()).thenReturn(true);
        when(calendarService.getEventsByDateRange(any(), any())).thenReturn(List.of());

        WorkbenchScheduleQueryService service = new WorkbenchScheduleQueryService(
                calendarService,
                demoModeService,
                new DemoDataProvider(),
                new DemoFusionService()
        );

        var response = service.getScheduleOverview(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10), 1L);

        assertThat(response.getEvents()).isNotEmpty();
        assertThat(response.getTotal()).isGreaterThan(0);
    }
}
