// Input: CalendarController接口
// Output: Controller API测试
// Pos: Test/单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.calendar.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.calendar.controller.CalendarController;
import com.xiyu.bid.calendar.dto.CalendarEventCreateRequest;
import com.xiyu.bid.calendar.dto.CalendarEventDTO;
import com.xiyu.bid.calendar.dto.CalendarEventUpdateRequest;
import com.xiyu.bid.calendar.entity.EventType;
import com.xiyu.bid.calendar.service.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarController 单元测试")
class CalendarControllerTest {

    @Mock
    private CalendarService calendarService;

    @InjectMocks
    private CalendarController calendarController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private CalendarEventDTO testEventDTO;
    private CalendarEventCreateRequest createRequest;
    private CalendarEventUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(calendarController)
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();

        testEventDTO = CalendarEventDTO.builder()
                .id(1L)
                .eventDate(LocalDate.of(2024, 3, 15))
                .eventType(EventType.DEADLINE)
                .title("项目截止日期")
                .description("标书提交截止日期")
                .projectId(100L)
                .isUrgent(true)
                .build();

        createRequest = CalendarEventCreateRequest.builder()
                .eventDate(LocalDate.of(2024, 3, 15))
                .eventType(EventType.DEADLINE)
                .title("项目截止日期")
                .description("标书提交截止日期")
                .projectId(100L)
                .isUrgent(true)
                .build();

        updateRequest = CalendarEventUpdateRequest.builder()
                .title("更新后的标题")
                .isUrgent(false)
                .build();
    }

    @Test
    @DisplayName("GET /api/calendar - 应该成功获取日期范围内的事件")
    void shouldGetEventsByDateRangeSuccessfully() throws Exception {
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);
        List<CalendarEventDTO> events = Arrays.asList(testEventDTO);
        when(calendarService.getEventsByMonth(anyInt(), anyInt())).thenReturn(events);

        mockMvc.perform(get("/api/calendar")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/calendar/month/2024/3 - 应该成功获取指定月份的事件")
    void shouldGetEventsByMonthSuccessfully() throws Exception {
        List<CalendarEventDTO> events = Arrays.asList(testEventDTO);
        when(calendarService.getEventsByMonth(2024, 3)).thenReturn(events);

        mockMvc.perform(get("/api/calendar/month/2024/3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("项目截止日期"));
    }

    @Test
    @DisplayName("GET /api/calendar/month/2024/13 - 应该返回错误对于无效月份")
    void shouldReturnErrorForInvalidMonth() throws Exception {
        when(calendarService.getEventsByMonth(2024, 13))
                .thenThrow(new IllegalArgumentException("Invalid month: 13"));

        mockMvc.perform(get("/api/calendar/month/2024/13")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/calendar/project/100 - 应该成功获取项目事件")
    void shouldGetEventsByProjectSuccessfully() throws Exception {
        List<CalendarEventDTO> events = Arrays.asList(testEventDTO);
        when(calendarService.getEventsByProject(100L)).thenReturn(events);

        mockMvc.perform(get("/api/calendar/project/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].projectId").value(100));
    }

    @Test
    @DisplayName("GET /api/calendar/urgent - 应该成功获取紧急事件")
    void shouldGetUrgentEventsSuccessfully() throws Exception {
        List<CalendarEventDTO> events = Arrays.asList(testEventDTO);
        when(calendarService.getUrgentEvents()).thenReturn(events);

        mockMvc.perform(get("/api/calendar/urgent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].isUrgent").value(true));
    }

    @Test
    @DisplayName("POST /api/calendar - 应该成功创建事件")
    void shouldCreateEventSuccessfully() throws Exception {
        when(calendarService.createEvent(any(CalendarEventCreateRequest.class)))
                .thenReturn(testEventDTO);

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("项目截止日期"))
                .andExpect(jsonPath("$.data.eventType").value("DEADLINE"));

        verify(calendarService, times(1)).createEvent(any(CalendarEventCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/calendar - 应该拒绝创建空标题的事件")
    void shouldRejectEventWithEmptyTitle() throws Exception {
        createRequest.setTitle("");

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(calendarService, never()).createEvent(any());
    }

    @Test
    @DisplayName("POST /api/calendar - 应该拒绝创建空日期的事件")
    void shouldRejectEventWithNullDate() throws Exception {
        createRequest.setEventDate(null);

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(calendarService, never()).createEvent(any());
    }

    @Test
    @DisplayName("POST /api/calendar - 应该拒绝创建空类型的事件")
    void shouldRejectEventWithNullType() throws Exception {
        createRequest.setEventType(null);

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(calendarService, never()).createEvent(any());
    }

    @Test
    @DisplayName("PUT /api/calendar/1 - 应该成功更新事件")
    void shouldUpdateEventSuccessfully() throws Exception {
        CalendarEventDTO updatedDTO = CalendarEventDTO.builder()
                .id(1L)
                .eventDate(LocalDate.of(2024, 3, 15))
                .eventType(EventType.DEADLINE)
                .title("更新后的标题")
                .isUrgent(false)
                .build();
        when(calendarService.updateEvent(eq(1L), any(CalendarEventUpdateRequest.class)))
                .thenReturn(updatedDTO);

        mockMvc.perform(put("/api/calendar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("更新后的标题"))
                .andExpect(jsonPath("$.data.isUrgent").value(false));

        verify(calendarService, times(1)).updateEvent(eq(1L), any(CalendarEventUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /api/calendar/999 - 应该返回错误当更新不存在的事件时")
    void shouldReturnErrorWhenUpdatingNonExistentEvent() throws Exception {
        when(calendarService.updateEvent(eq(999L), any(CalendarEventUpdateRequest.class)))
                .thenThrow(new RuntimeException("CalendarEvent not found"));

        mockMvc.perform(put("/api/calendar/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(calendarService, times(1)).updateEvent(eq(999L), any(CalendarEventUpdateRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/calendar/1 - 应该成功删除事件")
    void shouldDeleteEventSuccessfully() throws Exception {
        doNothing().when(calendarService).deleteEvent(1L);

        mockMvc.perform(delete("/api/calendar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(calendarService, times(1)).deleteEvent(1L);
    }

    @Test
    @DisplayName("DELETE /api/calendar/999 - 应该返回错误当删除不存在的事件时")
    void shouldReturnErrorWhenDeletingNonExistentEvent() throws Exception {
        doThrow(new RuntimeException("CalendarEvent not found"))
                .when(calendarService).deleteEvent(999L);

        mockMvc.perform(delete("/api/calendar/999"))
                .andExpect(status().isNotFound());

        verify(calendarService, times(1)).deleteEvent(999L);
    }

    @Test
    @DisplayName("应该正确处理特殊字符")
    void shouldHandleSpecialCharacters() throws Exception {
        createRequest.setTitle("项目截止日期 <script>alert('xss')</script>");
        when(calendarService.createEvent(any(CalendarEventCreateRequest.class)))
                .thenReturn(testEventDTO);

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        verify(calendarService, times(1)).createEvent(any(CalendarEventCreateRequest.class));
    }

    @Test
    @DisplayName("应该正确处理边界日期值")
    void shouldHandleBoundaryDateValues() throws Exception {
        createRequest.setEventDate(LocalDate.of(2024, 1, 1));
        when(calendarService.createEvent(any(CalendarEventCreateRequest.class)))
                .thenReturn(testEventDTO);

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        verify(calendarService, times(1)).createEvent(any(CalendarEventCreateRequest.class));
    }
}
