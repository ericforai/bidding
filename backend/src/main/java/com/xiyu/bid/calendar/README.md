# Calendar Module (日历模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

提供投标项目的时间管理和日程提醒功能，包括项目截止日期、会议安排、里程碑跟踪和提交提醒。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `CalendarEvent.java` | Entity | 日历事件实体 |
| `EventType.java` | Enum | 事件类型枚举 (DEADLINE, MEETING, MILESTONE, REMINDER, SUBMISSION, REVIEW) |
| `CalendarEventRepository.java` | Repository | 日历事件数据访问层 |
| `CalendarService.java` | Service | 日历业务逻辑层 |
| `CalendarController.java` | Controller | REST API 端点 |
| `CalendarEventDTO.java` | DTO | 日历事件数据传输对象 |
| `CalendarEventCreateRequest.java` | DTO | 创建事件请求 |
| `CalendarEventUpdateRequest.java` | DTO | 更新事件请求 |

## API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/calendar/events` | 获取日历事件列表 |
| GET | `/api/calendar/events/{id}` | 获取单个事件 |
| POST | `/api/calendar/events` | 创建新事件 |
| PUT | `/api/calendar/events/{id}` | 更新事件 |
| DELETE | `/api/calendar/events/{id}` | 删除事件 |
| GET | `/api/calendar/project/{projectId}` | 按项目查询事件 |
| GET | `/api/calendar/urgent` | 获取紧急事件 |

## 数据库索引

- `idx_calendar_project` - 项目ID索引
- `idx_calendar_date` - 日期索引
- `idx_calendar_type` - 事件类型索引
