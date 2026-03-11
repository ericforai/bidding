一旦我所属的文件夹有所变化，请更新我。

# API 数据层

**位置**: `src/api/`

**功能**: 封装后端 API 调用和 Mock 数据，提供统一的数据访问接口。

## 文件清单

| 文件 | 类型 | 功能 |
|------|------|------|
| `client.js` | HTTP 客户端 | Axios 封装，请求/响应拦截器 |
| `config.js` | 配置 | API 基础配置，Mock 模式开关 |
| `mock.js` | Mock 数据 | POC 项目所有静态 Mock 数据源 |
| `index.js` | 导出入口 | 统一导出所有 API 模块 |
| `modules/` | 目录 | 按业务模块拆分的 API 调用函数 |
| `examples.js` | 示例 | API 使用示例代码 |
| `trendradar.js` | 趋势雷达 | 趋势雷达相关 API |

## modules/ 目录

| 文件 | 功能 |
|------|------|
| `auth.js` | 认证授权 |
| `tenders.js` | 标讯管理 |
| `projects.js` | 项目管理 |
| `tasks.js` | 任务管理 |
| `fees.js` | 费用管理 |
| `ai.js` | AI 智能分析 |
| ... | 其他业务模块 |

## 更新记录

- 2026-03-11: `mock.js` 修复语法错误（删除多余的 `}`）
