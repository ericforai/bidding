---
title: 实施风险台账
space: implementation
category: guide
tags: [implementation, delivery]
sources:
  - docs/DELIVERY_BLOCKERS_SCHEDULE.md
  - docs/UAT_PLAN.md
backlinks:
  - _index
  - implementation/delivery-playbook
  - implementation/weekly-status
created: 2026-04-21
updated: 2026-04-21
health_checked: 2026-04-22
---
# 实施风险台账

| 风险 | 等级 | 触发信号 | 应对策略 | 追溯页面 |
|---|---|---|---|---|
| 集成链路不稳定 | 高 | API 联调失败率升高 | 预演 + 回滚演练 + 降级开关 | [[deployment]] |
| 范围漂移 | 高 | 非白名单需求插入 | 严格按正式范围核对 | [[requirements]] |
| 验收证据不足 | 中 | UAT 记录缺失 | 固化报告模板 + 每周补齐 | [[implementation/acceptance-and-closure]] |

## 关联

- [[implementation/weekly-status]]
- [[team-and-timeline]]

