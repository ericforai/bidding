一旦我所属的文件夹有所变化，请更新我。

# 脚本目录

这里放仓库级校验、清理和维护脚本。
脚本优先服务构建门禁、治理收口和本地环境整理，不承载业务逻辑。

| 文件 | 地位 | 功能 |
|------|------|------|
| `check-doc-consistency.sh` | 兼容入口脚本 | 保留旧命令入口，转调新的文档治理检查器 |
| `check-doc-governance.mjs` | 门禁脚本 | 检查强制目录 README 和强制文件头注释是否符合规范 |
| `check-front-data-boundaries.mjs` | 门禁脚本 | 检查业务层是否发生遗留 Demo 回退代码污染 |
| `check-line-budgets.mjs` | 门禁脚本 | 对核心源码目录执行 300 行棘轮门禁：默认检查当前工作区；pre-commit 走 staged，CI 走显式 diff 范围 |
| `check-version-sync.mjs` | 门禁脚本 | 校验根目录 `VERSION`、`package.json` 与 `backend/pom.xml` 是否保持一致 |
| `line-budget.config.json` | 配置文件 | 声明 300 行门禁的纳入目录、文件类型与排除规则，供 pre-commit 与 CI 共用 |
| `wiki-common.mjs` | 基础库脚本 | 提供 Wiki ingest/build/check 共用的 frontmatter、目录、索引与链接处理能力 |
| `wiki-ingest.mjs` | 摄入脚本 | 扫描 `.wiki/sources/` 原始资料（含 `bidding/`、`contract/` 等分类），抽取到 `.wiki/extracts/`，并更新 Source Catalog |
| `wiki-build.mjs` | 编译脚本 | 规范化 `.wiki/pages/`，补齐实施空间页面，生成 Page Catalog 与 backlinks |
| `wiki-check.mjs` | 门禁脚本 | 校验 frontmatter、链接完整性、双索引一致性、抽取产物与时效健康度 |
| `check-java-coding-standards.sh` | 门禁脚本 | 检查暂存区 Java 代码规范（如 `catch(Exception)`、`Optional.get()`、原始泛型），并执行 changed-code 质量门禁：Checkstyle 默认全启；PMD 支持 `off|report|on` 分阶段只检查目标服务包；SpotBugs 支持 `auto|off|report|on`，并通过 `quality.includes` / `quality.onlyAnalyze` 缩圈到目标改动类 |
| `install-java-standards-hook.sh` | 安装脚本 | 将仓库 `.githooks/pre-commit` 安装到本地 `.git/hooks/pre-commit` |
| `local-ci.sh` | 门禁脚本 | GitHub Actions 账单受限期间的本地验收入口；提供 `quick`、`full`、`release` 三档真实 API 模式门禁，后端门禁前会清理构建输出，不替代、不修改远端 Actions 定义 |
| `clean-local-artifacts.sh` | 清理脚本 | 删除本地产生的测试、报告和演练产物 |
| `dev-frontend.sh` | 启动脚本 | 统一前端本地启动入口，强制 Vite 使用真实 API 模式和默认后端地址 |
| `dev-frontend-health.sh` | 健康检查脚本 | 校验 `1314` 前端服务是否来自当前仓库，并确认运行时 API 模式和后端地址正确 |
| `sync-version.mjs` | 维护脚本 | 以根目录 `VERSION` 为单一版本源，同步前端 `package.json` 和后端 `backend/pom.xml` |
| `release/` | 发布脚本目录 | 管理发布演练、后端预编译与启动诊断、复用演练栈的 E2E 门禁、产物打包、远端激活、备份恢复和生产 smoke 验活；默认数据库口径为 MySQL 8.0，历史 PostgreSQL 仅保留显式兼容路径 |
| `test/` | 测试基线目录 | Playwright 与 API 联调测试的启动、停止和说明脚本 |
