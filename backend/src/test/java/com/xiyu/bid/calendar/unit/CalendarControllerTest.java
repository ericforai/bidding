// Input: CalendarController接口
// Output: Controller API测试
// Pos: Test/单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.calendar.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.calendar.dto.CalendarEventCreateRequest;
import com.xiyu.bid.calendar.dto.CalendarEventDTO;
import com.xiyu.bid.calendar.dto.CalendarEventUpdateRequest;
import com.xiyu.bid.calendar.entity.EventType;
import com.xiyu.bid.calendar.controller.CalendarController;
import com.xiyu.bid.calendar.service.CalendarService;
import com.xiyu.bid.XiyuBidApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CalendarController.class)
@ActiveProfiles("test")
@DisplayName("CalendarController 单元测试")
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalendarService calendarService;

    @Autowired
    private ObjectMapper objectMapper;

    private CalendarEventDTO testEventDTO;
    private CalendarEventCreateRequest createRequest;
    private CalendarEventUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
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
    @WithMockUser
    @DisplayName("GET /api/calendar - 应该成功获取日期范围内的事件")
    void shouldGetEventsByDateRangeSuccessfully() throws Exception {
        // Given
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);
        List<CalendarEventDTO> events = Arrays.asList(testEventDTO);

        // Mock the service to return empty list since we don't have this endpoint in service yet
        when(calendarService.getEventsByMonth(anyInt(), anyInt())).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/calendar")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/calendar/month/2024/3 - 应该成功获取指定月份的事件")
    void shouldGetEventsByMonthSuccessfully() throws Exception {
        // Given
        List<CalendarEventDTO> events = Arrays.asList(testEventDTO);
        when(calendarService.getEventsByMonth(2024, 3)).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/calendar/month/2024/3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("项目截止日期"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/calendar/month/2024/13 - 应该返回错误对于无效月份")
    void shouldReturnErrorForInvalidMonth() throws Exception {
        // Given
        when(calendarService.getEventsByMonth(2024, 13))
                .thenThrow(new IllegalArgumentException("Invalid month: 13"));

        // When & Then
        mockMvc.perform(get("/api/calendar/month/2024/13")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/calendar/project/100 - 应该成功获取项目事件")
    void shouldGetEventsByProjectSuccessfully() throws Exception {
        // Given
        List<CalendarEventDTO> events = Arrays.asList(testEventDTO);
        when(calendarService.getEventsByProject(100L)).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/calendar/project/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].projectId").value(100));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/calendar/urgent - 应该成功获取紧急事件")
    void shouldGetUrgentEventsSuccessfully() throws Exception {
        // Given
        List<CalendarEventDTO> events = Arrays.asList(testEventDTO);
        when(calendarService.getUrgentEvents()).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/calendar/urgent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].isUrgent").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/calendar - 应该成功创建事件")
    void shouldCreateEventSuccessfully() throws Exception {
        // Given
        when(calendarService.createEvent(any(CalendarEventCreateRequest.class)))
                .thenReturn(testEventDTO);

        // When & Then
        mockMvc.perform(post("/api/calendar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("项目截止日期"))
                .andExpect(jsonPath("$.data.eventType").value("DEADLINE"));

        verify(calendarService, times(1)).createEvent(any(CalendarEventCreateRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/calendar - 应该拒绝创建空标题的事件")
    void shouldRejectEventWithEmptyTitle() throws Exception {
        // Given
        createRequest.setTitle("");

        // When & Then
        mockMvc.perform(post("/api/calendar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(calendarService, never()).createEvent(any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/calendar - 应该拒绝创建空日期的事件")
    void shouldRejectEventWithNullDate() throws Exception {
        // Given
        createRequest.setEventDate(null);

        // When & Then
        mockMvc.perform(post("/api/calendar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(calendarService, never()).createEvent(any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/calendar - 应该拒绝创建空类型的事件")
    void shouldRejectEventWithNullType() throws Exception {
        // Given
        createRequest.setEventType(null);

        // When & Then
        mockMvc.perform(post("/api/calendar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(calendarService, never()).createEvent(any());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/calendar/1 - 应该成功更新事件")
    void shouldUpdateEventSuccessfully() throws Exception {
        // Given
        CalendarEventDTO updatedDTO = CalendarEventDTO.builder()
                .id(1L)
                .eventDate(LocalDate.of(2024, 3, 15))
                .eventType(EventType.DEADLINE)
                .title("更新后的标题")
                .isUrgent(false)
                .build();
        when(calendarService.updateEvent(eq(1L), any(CalendarEventUpdateRequest.class)))
                .thenReturn(updatedDTO);

        // When & Then
        mockMvc.perform(put("/api/calendar/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("更新后的标题"))
                .andExpect(jsonPath("$.data.isUrgent").value(false));

        verify(calendarService, times(1)).updateEvent(eq(1L), any(CalendarEventUpdateRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/calendar/999 - 应该返回错误当更新不存在的事件时")
    void shouldReturnErrorWhenUpdatingNonExistentEvent() throws Exception {
        // Given
        when(calendarService.updateEvent(eq(999L), any(CalendarEventUpdateRequest.class)))
                .thenThrow(new RuntimeException("CalendarEvent not found"));

        // When & Then
        mockMvc.perform(put("/api/calendar/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(calendarService, times(1)).updateEvent(eq(999L), any(CalendarEventUpdateRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/calendar/1 - 应该成功删除事件")
    void shouldDeleteEventSuccessfully() throws Exception {
        // Given
        doNothing().when(calendarService).deleteEvent(1L);

        // When & Then
        mockMvc.perform(delete("/api/calendar/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(calendarService, times(1)).deleteEvent(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/calendar/999 - 应该返回错误当删除不存在的事件时")
    void shouldReturnErrorWhenDeletingNonExistentEvent() throws Exception {
        // Given
        doThrow(new RuntimeException("CalendarEvent not found"))
                .when(calendarService).deleteEvent(999L);

        // When & Then
        mockMvc.perform(delete("/api/calendar/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(calendarService, times(1)).deleteEvent(999L);
    }

    @Test
    @DisplayName("未认证用户应该被拒绝访问")
    void shouldRejectUnauthenticatedAccess() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/calendar/month/2024/3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(calendarService, never()).getEventsByMonth(anyInt(), anyInt());
    }

    @Test
    @WithMockUser
    @DisplayName("应该正确处理特殊字符")
    void shouldHandleSpecialCharacters() throws Exception {
        // Given
        createRequest.setTitle("项目截止日期 <script>alert('xss')</script>");
        when(calendarService.createEvent(any(CalendarEventCreateRequest.class)))
                .thenReturn(testEventDTO);

        // When & Then
        mockMvc.perform(post("/api/calendar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        verify(calendarService, times(1)).createEvent(any(CalendarEventCreateRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("应该正确处理边界日期值")
    void shouldHandleBoundaryDateValues() throws Exception {
        // Given
        createRequest.setEventDate(LocalDate.of(2024, 1, 1));
        when(calendarService.createEvent(any(CalendarEventCreateRequest.class)))
                .thenReturn(testEventDTO);

        // When & Then
        mockMvc.perform(post("/api/calendar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        verify(calendarService, times(1)).createEvent(any(CalendarEventCreateRequest.class));
    }
}
