一旦我所属的文件夹有所变化，请更新我。

# 文档目录

这里沉淀仓库级规范、上线检查、迁移清单和治理约束。
本目录只存放需要多人协作长期维护的文档，不存放临时调试记录。

| 文件 | 地位 | 功能 |
|------|------|------|
| `DOCUMENTATION_GOVERNANCE.md` | 团队规范 | 定义目录 README、源码头注释、更新流程和检查门禁 |
| `FRONTEND_REAL_DATA_GOVERNANCE.md` | 前端治理规范 | 约束前端真实数据源、mock adapter 和边界扫描 |
| `GO_LIVE_CHECKLIST.md` | 发布清单 | 记录联调、环境、验收和发布前检查项 |
| `MYSQL_8_DEPLOYMENT.md` | 数据库部署口径 | 记录客户 MySQL 8.0 新库部署 profile、Flyway 路径、演练与备份恢复方式 |
| `MOCK_MIGRATION_BACKLOG.md` | 迁移清单 | 记录 mock 迁移剩余项和模块治理 backlog |
| `PRODUCTION_RELEASE_PIPELINE.md` | 生产发布口径 | 定义 `main` 合并后 CI、远端部署、生产 smoke 验活和 GitHub 环境变量约定 |
| `ROLLBACK_RUNBOOK.md` | 回滚手册 | 记录应用回滚、数据库备份恢复和 PostgreSQL/MySQL 工具回退路径 |
| `需求闭环完成说明-2026-04-21.md` | 验收证明 | 记录非集成缺口从 `22/25` 到 `25/25` 的前端、API、controller、service、DB 和测试证据 |
| `reports/` | 报告目录 | 存放检查脚本生成的报告和临时产物 |
