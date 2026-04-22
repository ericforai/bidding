---
title: 部署与上线
space: engineering
category: guide
tags: [部署, 上线, 发布, 运维, Docker, UAT]
sources:
  - docs/GO_LIVE_CHECKLIST.md
  - docs/ROLLBACK_RUNBOOK.md
  - docs/UAT_PLAN.md
  - README.md
  - docs/plans/2026-03-10-go-live-execution-plan.md
backlinks:
  - _index
  - architecture
  - implementation/acceptance-and-closure
  - implementation/delivery-playbook
  - implementation/milestones
  - implementation/risk-register
  - implementation/weekly-status
  - requirements
  - team-and-timeline
created: 2026-04-15
updated: 2026-04-15
health_checked: 2026-04-22
---
# 部署与上线

## 1. 运行模式

平台支持两种运行模式，通过环境变量和 `.env` 文件切换：

| 维度 | Mock 模式 | API 模式 |
|------|-----------|----------|
| 环境变量 | `VITE_API_MODE=mock` | `VITE_API_MODE=api` |
| 前端端口 | 1314 | 1314 |
| 后端 API | 无需后端 | `http://localhost:18080` |
| 数据来源 | `src/api/mock.js` 本地数据 | 真实后端服务 |
| 适用场景 | 前端开发、长期 MVP 演示 | 联调、UAT、发布演练、生产 |
| 启动命令 | `npm run dev:mock` | `npm run dev:api` 或 `npm run dev` |

**切换方法：**

```bash
# 切换到 API 模式（默认模式）
cp .env.api .env

# 切换到 Mock 模式
cp .env.mock .env
```

修改 `.env` 文件后需重启开发服务器。

---

## 2. 端口约定

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端开发服务器 | **1314** | 固定端口，vite.config.js / playwright.config.js / 演示统一使用 |
| 后端 API 服务 | **18080** | Spring Boot 应用端口 |
| PostgreSQL | **55432**（主机）-> 5432（容器） | Docker 容器 `xiyu-bid-rehearsal-postgres` |
| Redis | 6379 | 缓存与会话存储 |

> 注意：`14173` 等临时端口仅允许用于排查，排查结束后必须关闭，不作为项目约定端口。

---

## 3. 开发环境启动

### 3.1 前端

```bash
# 安装依赖
npm install

# 启动开发服务器（默认 API 模式）
npm run dev

# Mock 模式启动
npm run dev:mock

# 生产构建
npm run build

# 预览构建结果
npm run preview
```

### 3.2 后端

```bash
cd backend

# 编译
mvn clean compile

# 启动应用
mvn spring-boot:run

# 运行测试
mvn test

# 打包
mvn clean package
```

**必要环境变量：**
- `DB_PASSWORD` -- 数据库密码（必填）
- `JWT_SECRET` -- JWT 密钥，最少 32 字符（必填）
- `CORS_ALLOWED_ORIGINS` -- 允许的 CORS 源（默认：localhost:5173,5174,3000）
- `SPRING_PROFILES_ACTIVE` -- 环境配置（dev/prod，默认：dev）

### 3.3 数据库

```bash
# 方式 1：进入 Docker 容器
docker exec -it xiyu-bid-rehearsal-postgres psql -U xiyu_user -d xiyu_bid

# 方式 2：从主机连接
psql -h localhost -p 55432 -U xiyu_user -d xiyu_bid

# 查看密码
docker inspect xiyu-bid-rehearsal-postgres | grep POSTGRES_PASSWORD
```

---

## 4. 发布前检查

以下为 Go-Live Checklist 关键检查项摘要：

### 4.1 发布前（Pre-release）

- [ ] 候选版本已冻结
- [ ] `docs/COMMERCIAL_SCOPE.md` 已确认正式版白名单与 demo-only 黑名单
- [ ] `npm run build` 和 `VITE_API_MODE=api npm run build` 均通过
- [ ] `mvn -DskipTests compile` 通过
- [ ] 关键测试通过，PostgreSQL baseline Testcontainers 验证通过
- [ ] 数据库备份已执行并校验产物存在
- [ ] 监控面板与告警规则已配置
- [ ] UAT 已通过并签字
- [ ] 已知 P0 缺陷为 0
- [ ] 发布演练脚本已执行并产出报告

### 4.2 发布中

- [ ] 执行 `scripts/release/preflight.sh` 预检
- [ ] 记录当前版本号 / 提交号
- [ ] 停止流量或进入维护窗口
- [ ] 执行数据库迁移
- [ ] 部署后端应用 + 前端静态资源
- [ ] 检查 `/actuator/health` 和关键接口返回
- [ ] 检查前端首页与主链路

### 4.3 发布后 30 分钟

- [ ] 登录主流程正常
- [ ] 项目 / 标讯列表可访问
- [ ] Knowledge 主链路可访问
- [ ] 资源审批与 BAR 证书借用可访问
- [ ] 无高优先级错误告警
- [ ] 数据库连接池稳定、Prometheus 指标可采集

---

## 5. 发布流程

标准发布流程分为四个阶段：

### 5.1 preflight（预检）

```bash
bash scripts/release/preflight.sh
```

执行构建验证、测试通过性检查、环境变量完整性检查。

### 5.2 rehearse（演练）

```bash
bash scripts/release/rehearse-release.sh
```

在本地模拟完整发布流程，包括数据库迁移、应用部署、健康检查，产出演练报告。

### 5.3 deploy（部署）

```bash
bash scripts/release/deploy.sh
```

执行正式部署：停止流量 -> 数据库迁移 -> 部署后端 -> 部署前端 -> 健康检查。

### 5.4 signoff（签字确认）

```bash
node scripts/release/build-signoff-packet.mjs
```

生成正式签字包，包含 UAT 报告、版本信息、测试结果。Staging Gate 工作流通过后上传签字包。

---

## 6. 回滚策略

当发布失败或上线后出现 blocker 时，按以下步骤快速恢复：

### 6.1 触发回滚的条件

- 数据库迁移失败
- 应用无法启动
- 登录主流程失败
- 核心接口 5xx 持续出现
- 无法在 15 分钟内恢复核心业务

### 6.2 回滚步骤

1. **停止当前版本流量接入**
2. **回退前端**：将静态资源切换到上一个稳定版本
3. **回退后端**：将应用工件切换到上一个稳定版本
4. **重启服务**：重启并检查健康状态

### 6.3 数据库恢复

若数据库已被破坏性变更影响：

```bash
# 发布前备份
bash scripts/release/backup-db.sh

# 紧急恢复
CONFIRM_RESTORE=YES bash scripts/release/restore-db.sh <backup-file>
```

如果本机没有 `pg_dump/pg_restore`，可设置 `PG_CONTAINER_NAME=<postgres-container>` 使用 docker exec 回退路径。

### 6.4 回滚后验证

- `GET /actuator/health` 返回 `UP`
- 登录成功
- 项目 / 标讯列表可访问
- Knowledge 主链路可访问
- 资源主链路可访问
- 监控错误率恢复正常

### 6.5 事故记录

每次回滚需记录：发布时间、回滚时间、触发条件、影响范围、恢复耗时、后续修复 owner。

---

## 7. UAT 流程

### 7.1 角色分工

| 角色 | 职责 |
|------|------|
| 业务负责人 | 定义验收口径，签署通过 / 拒绝 |
| 销售代表 | 验证标讯、项目、结果闭环 |
| 技术代表 | 验证知识库、协作、文档能力 |
| 财务 / 资源代表 | 验证费用、BAR 证书借用、审批退还 |
| QA | 组织执行、记录问题、回归验证 |
| 技术负责人 | 修复 blocker 并给出版本说明 |

### 7.2 验收场景

1. **认证与权限**：登录、退出、未授权拦截、管理角色访问控制
2. **标讯到项目主链路**：标讯列表 -> 创建项目 -> 项目详情 -> 结果录入
3. **Knowledge 主链路**：资质 / 案例 / 模板列表查看与新增
4. **资源主链路**：费用审批、退还、BAR 证书借出与归还
5. **数据与观测**：Dashboard 聚合数据、health/prometheus 可用

### 7.3 准入与退出标准

- **准入**：前后端构建通过、关键测试通过、数据库备份完成、P0 缺陷为 0
- **退出**：所有 P0 场景通过、无 blocker 级问题、P1 问题有明确 owner、业务负责人签字确认

### 7.4 缺陷分级

| 级别 | 说明 |
|------|------|
| P0 | 阻塞上线，必须修复 |
| P1 | 影响主流程，原则上修复后再上线 |
| P2 | 不阻塞上线，需进入首个迭代 |

### 7.5 执行工具

- 自动 UAT 执行脚本：`node scripts/release/run-uat.mjs`
- Playwright API 联调栈启动：`bash scripts/test/start-api-e2e-stack.sh`
- 正式签字模板：`docs/UAT_SIGNOFF_TEMPLATE.md`
- 签字包生成：`node scripts/release/build-signoff-packet.mjs`

详细团队分工与时间线参见 [[team-and-timeline]]。

---

## 8. 监控与运维

| 维度 | 说明 |
|------|------|
| 健康检查 | `GET /actuator/health`（UP/DOWN）、`GET /actuator/prometheus`（指标采集） |
| 监控指标 | Micrometer + Prometheus：JVM、HTTP 请求、连接池、业务指标 |
| 日志 | Logback，开发 DEBUG 控制台、生产 INFO 文件 + 结构化日志，AOP 审计切面 |
| 告警 | 健康异常、5xx 持续、连接池耗尽、内存超阈值 |
