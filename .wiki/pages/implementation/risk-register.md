---
title: 实施风险台账
space: implementation
category: guide
tags: [implementation, delivery]
sources:
  - docs/DELIVERY_BLOCKERS_SCHEDULE.md
  - docs/UAT_PLAN.md
  - .wiki/sources/contract/西域数智化投标管理平台建设项目合同-V1 0420.docx
  - .wiki/sources/contract/附件3-合同报价清单人工摘录.md
backlinks:
  - _index
  - contract-constraints
  - implementation/delivery-playbook
  - implementation/weekly-status
created: 2026-04-21
updated: 2026-04-23
health_checked: 2026-04-24
---
# 实施风险台账

| 风险 | 等级 | 触发信号 | 应对策略 | 追溯页面 |
|---|---|---|---|---|
| 集成链路不稳定 | 高 | API 联调失败率升高 | 预演 + 回滚演练 + 降级开关 | [[deployment]] |
| 范围漂移 | 高 | 非白名单需求插入 | 严格按正式范围核对 | [[requirements]] |
| 验收证据不足 | 中 | UAT 记录缺失 | 固化报告模板 + 每周补齐 | [[implementation/acceptance-and-closure]] |
| 合同阶段延期 | 高 | 任一阶段文档、测试上线或上线确认延期 | 每周核对合同里程碑，提前升级并形成书面计划变更 | [[contract-constraints]] |
| 第三方前置条件不足 | 高 | API 文档、账号、授权、白名单、测试环境未就绪 | 由甲方或第三方负责事项单独建台账，阻塞项进入周会升级 | [[contract-constraints]] |
| 人员配置不满足合同 | 高 | 项目经理/方案负责人/骨干顾问未按要求驻场或变更 | 人员变更需甲方审查和现场交接，首付款前必须确认人员配置 | [[team-and-timeline]] |
| 报价用户数口径冲突 | 中 | “包含 200 注册用户”与“使用人数 100 人”同时存在 | 按主合同和最终盖章报价单确认，必要时补充书面说明 | [[contract-constraints]] |

## 关联

- [[implementation/weekly-status]]
- [[team-and-timeline]]
- [[contract-constraints]]
