// Input: CalendarService接口
// Output: Service业务逻辑测试
// Pos: Test/单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.calendar.unit;

import com.xiyu.bid.calendar.dto.CalendarEventCreateRequest;
import com.xiyu.bid.calendar.dto.CalendarEventDTO;
import com.xiyu.bid.calendar.dto.CalendarEventUpdateRequest;
import com.xiyu.bid.calendar.entity.CalendarEvent;
import com.xiyu.bid.calendar.entity.EventType;
import com.xiyu.bid.calendar.repository.CalendarEventRepository;
import com.xiyu.bid.calendar.service.CalendarService;
import com.xiyu.bid.service.IAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarService 单元测试")
class CalendarServiceTest {

    @Mock
    private CalendarEventRepository repository;

    @Mock
    private IAuditLogService auditLogService;

    @InjectMocks
    private CalendarService calendarService;

    private CalendarEvent testEvent;
    private CalendarEventCreateRequest createRequest;
    private CalendarEventUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testEvent = CalendarEvent.builder()
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
    @DisplayName("应该成功创建日历事件")
    void shouldCreateEventSuccessfully() {
        // Given
        when(repository.save(any(CalendarEvent.class))).thenReturn(testEvent);

        // When
        CalendarEventDTO result = calendarService.createEvent(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("项目截止日期");
        verify(repository, times(1)).save(any(CalendarEvent.class));
        verify(auditLogService, times(1)).log(any());
    }

    @Test
    @DisplayName("应该拒绝创建空标题的事件")
    void shouldRejectEventWithEmptyTitle() {
        // Given
        createRequest.setTitle("");

        // When & Then
        assertThatThrownBy(() -> calendarService.createEvent(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title is required");
        verify(repository, never()).save(any());
        verify(auditLogService, never()).log(any());
    }

    @Test
    @DisplayName("应该拒绝创建空日期的事件")
    void shouldRejectEventWithNullDate() {
        // Given
        createRequest.setEventDate(null);

        // When & Then
        assertThatThrownBy(() -> calendarService.createEvent(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event date is required");
        verify(repository, never()).save(any());
        verify(auditLogService, never()).log(any());
    }

    @Test
    @DisplayName("应该拒绝创建空类型的事件")
    void shouldRejectEventWithNullType() {
        // Given
        createRequest.setEventType(null);

        // When & Then
        assertThatThrownBy(() -> calendarService.createEvent(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event type is required");
        verify(repository, never()).save(any());
        verify(auditLogService, never()).log(any());
    }

    @Test
    @DisplayName("应该成功更新日历事件")
    void shouldUpdateEventSuccessfully() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(repository.save(any(CalendarEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CalendarEventDTO result = calendarService.updateEvent(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("更新后的标题");
        assertThat(result.getIsUrgent()).isFalse();
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(any(CalendarEvent.class));
        verify(auditLogService, times(1)).log(any());
    }

    @Test
    @DisplayName("应该抛出异常当更新不存在的事件时")
    void shouldThrowExceptionWhenUpdatingNonExistentEvent() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> calendarService.updateEvent(999L, updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CalendarEvent not found");
        verify(repository, times(1)).findById(999L);
        verify(repository, never()).save(any());
        verify(auditLogService, never()).log(any());
    }

    @Test
    @DisplayName("应该成功删除日历事件")
    void shouldDeleteEventSuccessfully() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(testEvent));
        doNothing().when(repository).deleteById(1L);

        // When
        calendarService.deleteEvent(1L);

        // Then
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).deleteById(1L);
        verify(auditLogService, times(1)).log(any());
    }

    @Test
    @DisplayName("应该抛出异常当删除不存在的事件时")
    void shouldThrowExceptionWhenDeletingNonExistentEvent() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> calendarService.deleteEvent(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CalendarEvent not found");
        verify(repository, times(1)).findById(999L);
        verify(repository, never()).deleteById(anyLong());
        verify(auditLogService, never()).log(any());
    }

    @Test
    @DisplayName("应该成功获取指定月份的事件")
    void shouldGetEventsByMonthSuccessfully() {
        // Given
        List<CalendarEvent> events = Arrays.asList(
                testEvent,
                CalendarEvent.builder()
                        .id(2L)
                        .eventDate(LocalDate.of(2024, 3, 20))
                        .eventType(EventType.MEETING)
                        .title("项目会议")
                        .projectId(100L)
                        .build()
        );
        when(repository.findByEventDateBetween(
                any(LocalDate.class), any(LocalDate.class))).thenReturn(events);

        // When
        List<CalendarEventDTO> result = calendarService.getEventsByMonth(2024, 3);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("项目截止日期");
        assertThat(result.get(1).getTitle()).isEqualTo("项目会议");
        verify(repository, times(1)).findByEventDateBetween(
                any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("应该成功获取空列表当月份没有事件时")
    void shouldReturnEmptyListWhenMonthHasNoEvents() {
        // Given
        when(repository.findByEventDateBetween(
                any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        // When
        List<CalendarEventDTO> result = calendarService.getEventsByMonth(2024, 12);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("应该成功根据项目ID获取事件")
    void shouldGetEventsByProjectSuccessfully() {
        // Given
        List<CalendarEvent> events = Arrays.asList(
                testEvent,
                CalendarEvent.builder()
                        .id(2L)
                        .eventDate(LocalDate.of(2024, 3, 20))
                        .eventType(EventType.MEETING)
                        .title("项目会议")
                        .projectId(100L)
                        .build()
        );
        when(repository.findByProjectId(100L)).thenReturn(events);

        // When
        List<CalendarEventDTO> result = calendarService.getEventsByProject(100L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getProjectId().equals(100L));
        verify(repository, times(1)).findByProjectId(100L);
    }

    @Test
    @DisplayName("应该成功返回空列表当项目没有事件时")
    void shouldReturnEmptyListWhenProjectHasNoEvents() {
        // Given
        when(repository.findByProjectId(999L)).thenReturn(List.of());

        // When
        List<CalendarEventDTO> result = calendarService.getEventsByProject(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("应该成功获取所有紧急事件")
    void shouldGetUrgentEventsSuccessfully() {
        // Given
        List<CalendarEvent> events = Arrays.asList(
                testEvent,
                CalendarEvent.builder()
                        .id(2L)
                        .eventDate(LocalDate.of(2024, 3, 20))
                        .eventType(EventType.MILESTONE)
                        .title("里程碑")
                        .isUrgent(true)
                        .build()
        );
        when(repository.findByIsUrgentTrue()).thenReturn(events);

        // When
        List<CalendarEventDTO> result = calendarService.getUrgentEvents();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getIsUrgent());
        verify(repository, times(1)).findByIsUrgentTrue();
    }

    @Test
    @DisplayName("应该成功返回空列表当没有紧急事件时")
    void shouldReturnEmptyListWhenNoUrgentEvents() {
        // Given
        when(repository.findByIsUrgentTrue()).thenReturn(List.of());

        // When
        List<CalendarEventDTO> result = calendarService.getUrgentEvents();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("应该成功获取即将到来的事件")
    void shouldGetUpcomingEventsSuccessfully() {
        // Given
        List<CalendarEvent> events = Arrays.asList(
                CalendarEvent.builder()
                        .id(1L)
                        .eventDate(LocalDate.now().plusDays(7))
                        .eventType(EventType.MEETING)
                        .title("项目会议")
                        .build(),
                CalendarEvent.builder()
                        .id(2L)
                        .eventDate(LocalDate.now().plusDays(14))
                        .eventType(EventType.MILESTONE)
                        .title("里程碑")
                        .build()
        );
        when(repository.findUpcomingEvents(any(LocalDate.class))).thenReturn(events);

        // When
        List<CalendarEventDTO> result = calendarService.getUpcomingEvents();

        // Then
        assertThat(result).hasSize(2);
        verify(repository, times(1)).findUpcomingEvents(any(LocalDate.class));
    }

    @Test
    @DisplayName("应该成功返回空列表当没有即将到来的事件时")
    void shouldReturnEmptyListWhenNoUpcomingEvents() {
        // Given
        when(repository.findUpcomingEvents(any(LocalDate.class))).thenReturn(List.of());

        // When
        List<CalendarEventDTO> result = calendarService.getUpcomingEvents();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("应该正确处理无效的月份参数")
    void shouldHandleInvalidMonthParameters() {
        // When & Then
        assertThatThrownBy(() -> calendarService.getEventsByMonth(2024, 13))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid month");

        assertThatThrownBy(() -> calendarService.getEventsByMonth(2024, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid month");
    }

    @Test
    @DisplayName("应该正确处理负数年份")
    void shouldHandleNegativeYear() {
        // When & Then
        assertThatThrownBy(() -> calendarService.getEventsByMonth(-2024, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid year");
    }

    @Test
    @DisplayName("应该正确处理空字符串输入")
    void shouldHandleEmptyStringInput() {
        // Given
        updateRequest.setTitle("");

        // When
        when(repository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(repository.save(any(CalendarEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Then - 应该允许更新为空字符串（使用验证时才拒绝）
        assertThatCode(() -> calendarService.updateEvent(1L, updateRequest))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该正确处理边界值")
    void shouldHandleBoundaryValues() {
        // Given
        CalendarEventCreateRequest boundaryRequest = CalendarEventCreateRequest.builder()
                .eventDate(LocalDate.of(2024, 1, 1))
                .eventType(EventType.DEADLINE)
                .title("边界测试")
                .build();

        CalendarEvent boundaryEvent = CalendarEvent.builder()
                .id(1L)
                .eventDate(LocalDate.of(2024, 1, 1))
                .eventType(EventType.DEADLINE)
                .title("边界测试")
                .build();

        when(repository.save(any(CalendarEvent.class))).thenReturn(boundaryEvent);

        // When
        CalendarEventDTO result = calendarService.createEvent(boundaryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    }
}
