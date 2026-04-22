# Wiki 操作日志 / Operation Log

> 按时间倒序记录所有 Wiki 操作。每条记录以 `## [日期] 操作类型 | 说明` 格式开头。
> 可用 `grep "^## \[" .wiki/log.md | tail -5` 查看最近 5 条。

## [2026-04-22] build | 设计系统知识页入库与总览口径更新
- 新增页面：`pages/design-system.md`（正式 DESIGN.md 基线、落地策略、实施回链）
- 更新页面：`pages/overview.md`（切换为真实 API 唯一路径口径，补充设计系统建制信息）
- 自动重编：`pages/_index.md`、`PAGE_INDEX.md`、`catalog/page-catalog.json`
- 校验结果：`npm run wiki:build` 与 `npm run wiki:check` 均通过（pages=19）

## [2026-04-15] ingest | 附件5：需求任务书 + 附件6：功能清单
- 来源：`.wiki/sources/bidding/` 下 2 个文件（.docx + .xlsx）
- 新建页面：`requirements.md`（需求追溯，29 功能点追溯矩阵）
- 更新页面：`_index.md`（新增 requirements 导航）
- 更新文件：`INDEX.md`（新增"招标需求文档"分类，重编章节号）

## [2026-04-15] init | Wiki 知识库初始化
- 创建三层架构：`WIKI.md`（Schema）+ `.wiki/INDEX.md`（源索引）+ `.wiki/pages/`（知识页面）
- 创建源文档目录：`.wiki/sources/{bidding,industry,competitor,customer,technical,internal}/`
- 源文档编目：11 个分类，70+ 源文件
- 生成 11 个 Wiki 页面：overview, architecture, business-process, modules, ai-capabilities, data-model, roles-and-permissions, glossary, team-and-timeline, deployment, _index
- 交叉引用校验通过：所有 `[[wiki-link]]` 指向有效页面
- 更新根文件：CLAUDE.md, README.md

## [2026-04-22] upgrade | 双栈 Wiki 升级（研发 + 实施）
- 新增自动化脚本：`scripts/wiki-ingest.mjs`、`scripts/wiki-build.mjs`、`scripts/wiki-check.mjs`
- 新增目录：`.wiki/extracts/`、`.wiki/outputs/`、`.wiki/catalog/`
- 双索引落地：`.wiki/INDEX.md`（Source Catalog）+ `.wiki/index.md`（Page Catalog）
- 新增 Implementation Space 页面：`implementation/{delivery-playbook,milestones,risk-register,weekly-status,acceptance-and-closure}.md`
- 执行真实增量演示：摄入 `docx + xlsx` 源文件并生成抽取结果与 catalog
- pre-commit 门禁新增 `npm run wiki:check`
