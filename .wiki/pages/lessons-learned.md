---
title: 工程经验总结
space: engineering
category: guide
tags: [经验总结, 数据库迁移, PostgreSQL, MySQL, Flyway]
sources:
  - CLAUDE.md
  - 本次任务 cursor/tender-source-tag-20260509
  - conductor/tracks/tender_source_tag_20260509/index.md
backlinks:
  - _index
created: 2026-05-10
updated: 2026-05-10
health_checked: 2026-05-10
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
