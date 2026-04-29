# QA 决策记录（流程表单中心）

更新日期：2026-04-29  
模块：`workflowform`（流程表单中心）  
关联 PR：#99

## 1. 开发与提交路径确认

- 已新建独立 Worktree：`/Users/user/xiyu/worktrees/codex-workflow-form-versioning`
- 分支：`codex/workflow-form-versioning`
- 提交方式：`feat: add workflow form version history and rollback`（`2952df04`）
- PR 仍为 **OPEN**，未合并到 `main` 前可继续补充文档或补测。

## 2. 关于历史版本能力（TODO Proposal 1）

针对“表单历史版本查看/回滚”提案，结论是：

1. 记录到待办：是（`TODO.md` 已补充为已完成项）
2. 本次不在同一 PR 中做额外运维增强（diff 对比、审批链路扩展），仅聚焦历史快照与回滚发布闭环。

## 3. 问题清单与状态（便于后续开发追溯）

- `todo.md` / `TODOs`：确认存在，优先查看 `TODO.md`（项目内主待办），`TODOS.md` 当前为空。
- 前端环境验证：部分本地命令（`mvn`、`vite build`）在当前工作区受系统权限影响存在 `Operation not permitted`/`EPERM`，请在可写环境复跑门禁。
- 文档层面：已补充 `TODO.md` 及 `src/views/System/workflow-form-designer/README.md`，新建本决策记录文件用于追踪。
- PR 状态：先前 review 建议均未引入新功能阻断，版本功能已在当前分支实现并提交，当前仅待你确认是否允许合并。

## 4. 用户可复用路径

- 直接查看：`TODO.md`（功能交付）  
- 回滚能力入口：`/api/admin/workflow-forms/templates/{templateCode}/versions`  
- 回滚动作：`POST /api/admin/workflow-forms/templates/{templateCode}/versions/{version}/rollback`

