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
| `mock-adapters/` | 目录 | 隔离 demo 数据读取和 demoPersistence 适配 |
| `modules/` | 目录 | 按业务模块拆分的 API 调用函数 |
| `examples.js` | 示例 | API 使用示例代码 |
| `trendradar.js` | 趋势雷达 | 趋势雷达相关 API |

## modules/ 目录

| 文件 | 功能 |
|------|------|
| `auth.js` | 认证授权 |
| `settings.js` | 系统设置与数据权限 |
| `tenders.js` | 标讯管理 |
| `projects.js` | 项目管理 |
| `tasks.js` | 任务管理 |
| `fees.js` | 费用管理 |
| `ai.js` | AI 智能分析 |
| ... | 其他业务模块 |

## 治理约束

- 业务页面、组件、store 不得直接依赖 `mock.js`
- demo 数据只能通过 `mock-adapters/` 或 API module 间接暴露
- 目录和文件维护规则见 `docs/DOCUMENTATION_GOVERNANCE.md`

## 更新记录

- 2026-03-11: `mock.js` 修复语法错误（删除多余的 `}`）
- 2026-03-18: 认证会话快照开始承载 `allowedProjectIds`，用于项目级数据权限恢复
- 2026-03-19: 新增 `settings.js`，用于读取/保存真实数据权限配置，并让会话快照同时承载 `allowedDepts`
