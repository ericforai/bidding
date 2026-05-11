---
title: 工程经验总结
space: engineering
category: guide
tags: [经验总结, 数据库迁移, PostgreSQL, MySQL, Flyway, 多Agent并行, 工程化护栏, Git历史]
sources:
  - CLAUDE.md
  - RULES.md
  - conductor/tracks/tender_source_tag_20260509/index.md
backlinks:
  - _index
created: 2026-05-10
updated: 2026-05-11
health_checked: 2026-05-11
---
# 工程经验总结

> 记录本次 `cursor/tender-source-tag-20260509` 分支开发中积累的经验教训。

---

## 一、数据库迁移目录清理（PostgreSQL → MySQL）

### 问题背景

项目早期同时支持 PostgreSQL 和 MySQL，导致存在两套迁移脚本目录：
- `db/migration/` — PostgreSQL（已废弃）
- `db/migration-mysql/` — MySQL（活跃使用）

2026-04-29 官方移除 PostgreSQL 支持后，`migration/` 目录成为历史遗留，未及时清理。

### 经验教训

| 问题 | 教训 | 规范 |
|------|------|------|
| 双数据库目录导致维护成本翻倍 | 决策后立即清理废弃路径，防止技术债累积 | **废弃即删除**，不要保留"仅供参考"的代码 |
| `application-mysql.yml` 配置 `flyway.locations: classpath:db/migration-mysql`，但 `migration/` 仍存在 | 配置文件与实际目录结构必须保持同步 | 每次配置变更后验证对应目录状态 |
| CI 可能在历史某次配置中引用 PostgreSQL 测试 | CI 配置需与当前技术栈严格对齐 | **CI 是技术栈的真实声明**，不是可配置选项 |

### 操作规范（已固化到 CLAUDE.md）

1. **迁移脚本位置**：`backend/src/main/resources/db/migration-mysql/`
2. **禁止使用 `migration/`**：该目录已废弃
3. **命名规范**：
   - 基线版本：`B{version}_*.sql`
   - 增量版本：`V{version}_*.sql`
4. **版本号**：必须大于已有最大版本号
5. **回滚脚本**：`db/rollback/` 目录

### 验证命令

```bash
# 确认迁移目录结构正确
ls backend/src/main/resources/db/
# 期望输出：migration-mysql/  rollback/

# 确认 CI 只测试 MySQL
grep -E "Flyway.*Test" .github/workflows/ci.yml
# 期望输出：FlywayMysqlContainerTest（无 PostgreSQL 相关）
```

---

## 二、CI 配置与实际技术栈对齐

### 发现的问题

CI 配置中曾存在指向 PostgreSQL 的测试：
```yaml
# 错误的配置（已修复）
- name: Run migration and architecture gates
  run: mvn -Dtest=DualDatabaseMigrationParityTest,FlywayPostgresContainerTest,...
```

### 修复方案

```yaml
# 正确的配置
- name: Run migration and architecture gates
  run: mvn -Dtest=FlywayMysqlContainerTest,ArchitectureTest test
```

### 经验

- **CI 配置即技术栈声明**：CI 中出现的每一项都代表项目实际支持的能力
- **定期审计 CI**：使用 `git log` 和 `grep` 检查 CI 配置与代码库的一致性

---

## 三、工作流程建议

### 数据库迁移操作 Checklist

```
[ ] 确认目标数据库类型（MySQL/PostgreSQL）
[ ] 检查 flyway.locations 配置
[ ] 确认迁移脚本所在目录
[ ] 创建迁移脚本（V{version}__{desc}.sql）
[ ] 运行 Flyway 测试验证
[ ] 更新 CLAUDE.md（如有规范变更）
[ ] 提交前验证 CI 配置一致性
```

### 删除废弃代码 Checklist

```
[ ] 确认代码已被配置引用（grep 检查）
[ ] 确认 CI 配置不再使用
[ ] 确认文档不再引用
[ ] 执行删除
[ ] 更新文档（如 CLAUDE.md）
[ ] 提交并推送
[ ] 通知相关 agent（涉及共享模块时）
```

---

## 四、相关文档

- [[agent-sop-quickref]] — Agent 开发 SOP（含多 Agent 协作规范）
- [[architecture]] — 架构说明
- CLAUDE.md — 项目执行入口（含数据库迁移规范）

---

## 五、变更记录

| 日期 | 分支 | 变更内容 |
|------|------|----------|
| 2026-05-10 | cursor/tender-source-tag-20260509 | 删除废弃 migration/ 目录，更新 CLAUDE.md 数据库迁移规范 |

---

## 二、多 Agent 并行开发的故障模式与防御体系（2026-05-11）

> 一次排障涉及 V117 迁移报错 / 企微登录按钮丢失 / Flyway history 污染 / 8 把过期锁 / Gemini worktree 死循环 5 个看似独立的问题。
> 彻查后发现它们共享同一类根因：**多 Agent 并行改动在没有护栏的情况下持续退化**。本节把这次梳理出的故障模式和护栏落成文字，避免未来重复踩坑。

### 2.1 真实发生过的故障模式

| 模式 | 症状 | 根因 |
|---|---|---|
| **Git 历史撕裂** | `main` 出现 4 个 disconnected root commits；`src/views/Login.vue` 等文件被"覆盖回旧版本"，企微入口消失 | Agent 用 `filter-repo` / `checkout --orphan` / `replace --graft` / 强推 后未验证，且 `main` 没有 branch protection |
| **迁移自身 bug 溜进 main** | V117 包含 `UPDATE status='BIDDING'`，但 ENUM 里没有 `BIDDING` → fresh DB 启动全挂 | CI 只跑 H2 ddl-auto 测试，**从未把 migration 从 baseline 跑一遍**到真 MySQL |
| **失败迁移的 schema_history 污染** | 启动失败后 Flyway 留一条 `success=0`，下次启动直接被卡住；launchd 每 5s 重启一次生成新 failed 行 | watchdog 是无脑循环，没有失败计数/退避，加上多 worktree 共享同一个 `xiyu_bid_main` DB，事态会跨 worktree 传染 |
| **文件锁形同虚设** | `.agent-locks.yml` 里 8 把锁全过期 2 天；新任务改 `db/migration-mysql/` 和 `Login.vue` 没人挂锁；CI 的 `agent-locks` job 只检查冲突，**不检查是否登记** | 锁系统设计完整但 CI 不强制、没续期机制、没清理机制，导致全员默认忽略 |
| **分支长期失败无人知** | Gemini worktree 编译失败 24 小时，launchd 每 30s 重试一次，产出 tens of GB 日志 | watchdog 没有阈值停机；没有跨 worktree 的健康度聚合工具；失败信号只存在于本地日志里 |

### 2.2 一次事故里踩到 5 个坑 —— 事故时间线

```
触发: npm run dev 启动失败
  ↓ backend 报 "Data truncated for column 'status'"
发现 1: V117 有 SQL bug（UPDATE ENUM 未声明的值）
  ↓ 清 flyway_schema_history 后重启
发现 2: launchd 每 5s 又把 backend 拉起来，立即再写 failed 行
  ↓ launchctl bootout 停掉守护进程
发现 3: 另一个 worktree（Gemini）也在连 xiyu_bid_main，每 30s 抢写
  ↓ 对比 worktree 的 Login.vue
发现 4: main 上的 Login.vue 被覆盖回了无企微按钮的旧版本
  ↓ git log --all --oneline 追查
发现 5: main 有 4 个 disconnected root commits，历史被改写过
```

**教训**：单一工程问题在没有护栏的系统里**会互相放大**。每一层小漏洞独立看都可以"下次注意"，叠加起来就是几小时的 production-style 排障。

### 2.3 护栏体系（2026-05-11 已上线）

| 层 | 工具 | 功能 | 应急开关 |
|---|---|---|---|
| **Git 历史** | `.github/workflows/branch-history-guard.yml` | root commit 数 ratchet；超过 `MAX_ROOTS=4` 直接 CI 红 | 修复后降低 ratchet |
| **Git 历史** | `.githooks/pre-push` | 拒绝 non-fast-forward push 到 main/master/release/* | `FORCE_PUSH_OK=1` 一次性 |
| **Git 历史** | GitHub Branch Protection | 强制 PR / 线性历史 / 禁 force push / 禁删除 | GitHub 设置 |
| **Git 历史** | `RULES.md §10.5` | 7 条历史改写命令的红线清单 + 双人签字流程 | — |
| **迁移** | `.github/workflows/flyway-migrate-dryrun.yml` | 每个改 `migration-mysql/` 的 PR 在 MySQL 8.0 从 baseline 跑一遍 | — |
| **多 Agent** | `scripts/check-agent-locks.mjs` + `scripts/hot-paths.yml` | hot-path 改动必须有 active lock；无锁 CI 红 | 加锁再 push |
| **多 Agent** | `npm run agent:lock-renew` | 当前分支锁批量延期 +2 天 | — |
| **多 Agent** | `.github/workflows/agent-locks-janitor.yml` | 每天 2:17 AM UTC 清理已死分支的过期锁 | — |
| **运行时** | `scripts/dev-services.sh` 退避 | 30s → 2min → 10min → 30min；10 次失败写 `backend.fail-state` | `rm .runtime/dev-services/backend.fail-state` |
| **运行时** | `npm run agent:health-check` | 聚合所有 worktree 的 backend/frontend/sidecar 状态 + 最近 ERROR 行 | — |

### 2.4 工作流铁律

踩过这次坑之后，**以下行为在 `main` 上默认禁止**，违反需要先开 issue 拿签字：

- `git filter-repo` / `git filter-branch` / `git checkout --orphan`
- `git replace --graft` / `git replace -d`
- `git rebase --root`
- `git push --force` / `git push --force-with-lease`
- `git reset --hard` 后接 push
- 通过 GitHub API 直接 PATCH refs

详细清单和豁免流程见 `RULES.md §10.5`。

### 2.5 给未来自己的 checklist

如果接下来再出现 fresh DB 启动失败 / 登录页某功能消失 / worktree 疯转的情况，**先跑这 3 条**再往下查：

```bash
# 1. 有没有某个 worktree 在后台死循环？
npm run agent:health-check

# 2. 当前分支改的文件是不是 hot-path 没挂锁？
npm run agent:lock-check:changed

# 3. main 的历史是不是被改写了？
git rev-list --max-parents=0 origin/main | wc -l   # 应该是 1
```

### 2.6 待办（P1 / P2）

| Issue | 状态 | 优先级 | 真正修复依赖 |
|---|---|---|---|
| [#224](https://github.com/ericforai/bidding/issues/224) | ratchet 锁住不恶化 | P1 | ~30 min 停机窗口做历史缝合 |
| [#220](https://github.com/ericforai/bidding/issues/220) | 数据污染路径已关 | P1 | `dev-env.sh` 改造支持 per-worktree DB |
| [#221](https://github.com/ericforai/bidding/issues/221) | health-check 可见 | P2 | Gemini worktree 手动 rebase |
| [#227](https://github.com/ericforai/bidding/issues/227) | `@Disabled` 绕过 | P2 | 业务 owner 决定 fixture 策略 |

**重点**：防御到位 ≠ 问题消失。这些 P1/P2 仍是真实债务，只是当前不会继续恶化。
