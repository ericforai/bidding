# Implementation Plan: 立体矩阵式协作权限体系

## Phase 1: 基础设施与 CRM 同步补全
本阶段重点在于建立持久化表结构和外部同步通道。

- [~] **Task: 数据库 Schema 建立**
    - [ ] 创建 `CrmCustomerPermission` 实体与镜像表。
    - [ ] 创建 `ProjectMember` 实体与协作表（支持 VIEWER/EDITOR 角色）。
- [ ] **Task: CRM Webhook 接收端实现**
    - [ ] 实现 `/api/webhooks/crm/permissions` 接口。
    - [ ] 实现幂等同步逻辑（根据 CRM 推送更新本地镜像）。
- [ ] **Task: TDD - 验证同步与持久化**
    - [ ] 编写测试用例，模拟 Webhook 推送并断言数据库状态。
- [ ] **Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)**

## Phase 2: 后端 AOP 拦截器深度重构
本阶段是核心，将静态权限升级为矩阵动态并集。

- [ ] **Task: 升级 `DataScopeContext` 载体**
    - [ ] 扩展上下文对象，使其能承载协作成员 ID 集合和 CRM 客户权限镜像。
- [ ] **Task: 重构 `DataScopeAspect` 切面逻辑**
    - [ ] 注入协作表与 CRM 镜像表的 SQL 关联判断。
    - [ ] 实现“穿透式”只读权限判定（项目 -> 标讯）。
- [ ] **Task: TDD - 矩阵权限并集测试**
    - [ ] 编写复杂权限测试：验证用户 A 即使不在部门，作为项目成员也能查到数据。
- [ ] **Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)**

## Phase 3: 前端协作交互与工作台补强
本阶段交付用户操作入口。

- [ ] **Task: 协作成员管理 API 交付**
    - [ ] 提供项目的成员添加、删除、列表查询接口。
- [ ] **Task: 项目卡片“分享/协作”UI 开发**
    - [ ] 在项目组件中增加“协作”按钮，实现人员搜索与权限分配弹窗。
- [ ] **Task: 全流程手动验收**
    - [ ] 演示“跨部门分享”后受邀人工作台实时同步。
- [ ] **Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)**
