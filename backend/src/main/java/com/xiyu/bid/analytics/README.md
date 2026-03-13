# Analytics Module

> 一旦我所属的文件夹有所变化，请更新我。

## 目录功能

数据分析模块，提供 dashboard 汇总统计、趋势分析以及 drill-down 下钻明细能力。

## 文件清单

### Service (业务层)
- `service/DashboardAnalyticsService.java` - Dashboard 聚合统计与 drill-down 业务逻辑

### Controller (控制器层)
- `controller/DashboardController.java` - Dashboard REST API 端点

### DTO (数据传输对象)
- `dto/DashboardOverviewDTO.java` - Dashboard 汇总视图
- `dto/SummaryStats.java` - 汇总统计
- `dto/TrendData.java` - 趋势数据
- `dto/CompetitorData.java` - 竞争对手数据
- `dto/RegionalData.java` - 区域数据
- `dto/AnalyticsDrillDownResponseDTO.java` - drill-down 响应体
- `dto/AnalyticsDrillDownRowDTO.java` - drill-down 行数据
- `dto/AnalyticsDrillDownSummaryDTO.java` - drill-down 汇总数据
- `dto/AnalyticsDrillDownFiltersDTO.java` - drill-down 过滤器
- `dto/AnalyticsFilterDimensionDTO.java` - 过滤维度
- `dto/AnalyticsFilterOptionDTO.java` - 过滤选项
- `dto/AnalyticsPaginationDTO.java` - 分页元数据

## API 端点

- `GET /api/analytics/overview` - 获取 dashboard 汇总
- `GET /api/analytics/summary` - 获取核心统计
- `GET /api/analytics/trends` - 获取趋势数据
- `GET /api/analytics/competitors` - 获取竞争对手分析
- `GET /api/analytics/regions` - 获取区域分布
- `GET /api/analytics/status-distribution` - 获取状态分布
- `GET /api/analytics/drilldown/revenue` - 获取金额下钻明细
- `GET /api/analytics/drilldown/win-rate` - 获取中标率下钻明细
- `GET /api/analytics/drilldown/team` - 获取团队下钻明细
- `GET /api/analytics/drilldown/projects` - 获取项目下钻明细
- `POST /api/analytics/cache/clear` - 清理 dashboard 缓存
