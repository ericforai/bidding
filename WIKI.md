# WIKI.md — LLM Wiki 治理规范 / LLM Wiki Governance Schema

> 本文件定义项目 Wiki 知识库的三层架构、维护规范和工作流。
> LLM 在操作 Wiki 时必须遵循此文件的约定。

## 1. 三层架构 / Three-Layer Architecture

```
Layer 1 — 原始资料（Raw Sources）
  存放位置：.wiki/sources/（外部资料：文章、论文、招标文件、图片等）
  项目内源：docs/、backend/、src/ 等（已有的代码库文档）
  索引：.wiki/INDEX.md（对以上两类来源统一编目）
  性质：不可变。LLM 只读不写。这是事实的唯一来源。

Layer 2 — Wiki 页面（Wiki Pages）
  位置：.wiki/pages/*.md
  性质：LLM 拥有此层。创建、更新、维护交叉引用、保持一致性。

Layer 3 — Schema（本文件）
  位置：WIKI.md（项目根目录）
  性质：治理权威。人和 LLM 共同演进。
```

## 2. 目录结构 / Directory Structure

```
.wiki/
├── INDEX.md                    # Layer 1: 源文档分类索引（编目所有来源）
├── index.md                    # 页面索引：按分类组织的知识页面目录
├── log.md                      # 操作日志：按时间倒序的所有 Wiki 操作记录
├── sources/                    # Layer 1: 原始资料存放目录
│   ├── README.md               # 存放规则与目录结构说明
│   ├── bidding/                # 招标文件、投标要求、评分标准
│   ├── industry/               # 行业资料、政策法规、市场报告
│   ├── competitor/             # 竞对资料、竞品分析
│   ├── customer/               # 客户需求、会议纪要、沟通记录
│   ├── technical/              # 技术参考、架构文献、最佳实践
│   └── internal/               # 内部文档、培训资料、知识沉淀
└── pages/                      # Layer 2: Wiki 合成知识页面
    ├── _index.md               # Wiki 首页 / 知识导航
    ├── overview.md             # 项目综述
    ├── architecture.md         # 架构合成
    ├── business-process.md     # 业务流程
    ├── modules.md              # 模块目录
    ├── ai-capabilities.md      # AI 能力
    ├── data-model.md           # 数据模型
    ├── roles-and-permissions.md # 角色权限
    ├── glossary.md             # 术语表
    ├── team-and-timeline.md    # 团队与排期
    └── deployment.md           # 部署与上线
```

## 3. 页面 Frontmatter 规范 / Page Frontmatter Format

每个 Wiki 页面必须以 YAML frontmatter 开头：

```yaml
---
title: 页面标题
category: architecture | business | module | guide | reference | decision
tags: [标签1, 标签2]
sources:
  - docs/技术架构方案.md
  - backend/README.md
created: 2026-04-15
updated: 2026-04-15
---
```

字段说明：
- **title**: 页面中文标题
- **category**: 分类（见第 4 节）
- **tags**: 关键词标签，用于检索
- **sources**: 本页合成内容所依据的源文件路径（相对于项目根）
- **created**: 创建日期
- **updated**: 最后更新日期

## 4. 分类体系 / Categories

| 分类 | 英文 | 适用范围 |
|------|------|---------|
| 技术架构 | architecture | 系统架构、技术选型、分层设计 |
| 业务知识 | business | 业务流程、业务规则、领域概念 |
| 功能模块 | module | 模块说明、模块边界、接口契约 |
| 操作指南 | guide | 操作指南、开发指南、流程说明 |
| 参考资料 | reference | 术语表、数据字典、API 参考 |
| 架构决策 | decision | 技术选型记录、架构决策记录 |

## 5. 交叉引用约定 / Cross-Reference Conventions

- 使用 `[[page-name]]` 语法引用其他 Wiki 页面
- `page-name` 为不含 `.md` 扩展名的文件名
- 示例：`详见 [[architecture]] 中的前端架构部分`
- 每个页面的 frontmatter `sources` 字段列出所依据的源文件路径
- 术语首次出现时应链接到 `[[glossary]]`

## 6. 工作流 / Workflows

### 6.1 回答问题工作流 / Query Workflow

1. 阅读 `.wiki/pages/_index.md` 了解 Wiki 已有覆盖范围
2. 查找最相关的 Wiki 页面，优先使用合成知识
3. 必要时追溯到 `.wiki/INDEX.md` 中列出的源文件获取细节
4. 回答时注明引用来源（Wiki 页面或源文件）

### 6.2 摄入新源文档工作流 / Ingest Workflow

1. 用户将原始资料放入 `.wiki/sources/` 对应子目录（按主题分类）
2. LLM 阅读新文档，提取关键知识
3. 更新 `.wiki/INDEX.md`，将新文档归入合适分类
4. 判断是否需要更新现有 Wiki 页面或新建页面
5. 更新受影响页面的 frontmatter（sources、updated）
6. 添加或更新 `[[wiki-link]]` 交叉引用
7. 更新 `.wiki/pages/_index.md` 目录
8. 更新 `.wiki/index.md` 页面索引（新增或修改页面条目）
9. 追加操作记录到 `.wiki/log.md`

### 6.3 维护页面工作流 / Maintenance Workflow

1. 检测源文件变更（git diff 或人工触发）
2. 比对 Wiki 页面的 sources 列表与实际源文件内容
3. 重新合成过时页面
4. 更新 `updated` 日期
5. 验证交叉引用完整性
6. 同步更新 `.wiki/index.md` 中受影响页面的摘要和更新日期
7. 追加操作记录到 `.wiki/log.md`

## 7. 更新触发规则 / Update Triggers

| 触发事件 | 需更新的页面 |
|---------|------------|
| 源文件修改 | 检查引用该源的所有 Wiki 页面 |
| 新增 backend 模块 | `[[modules]]`、INDEX.md |
| 新增 docs/ 文件 | INDEX.md，评估是否需要新页面 |
| 架构变更 | `[[architecture]]` |
| 新增 AI 功能 | `[[ai-capabilities]]` |
| 角色/权限变更 | `[[roles-and-permissions]]` |
| 数据模型变更 | `[[data-model]]` |
| 里程碑推进 | `[[team-and-timeline]]` |

## 8. 质量规则 / Quality Rules

- 每个事实陈述应可追溯到至少一个 source
- 页面不超过 300 行（超过应拆分）
- 每个页面至少有 1 个 source
- 交叉引用 `[[page-name]]` 的目标页面必须存在
- 页面 `updated` 日期不应落后于源文件最后修改日 30 天以上
- INDEX.md 只编目和分类，不摘录内容

## 9. 约束 / Constraints

- **源文件不可变**：Layer 1 的源文件永远不可被 Wiki 系统修改
- **合成非复制**：Wiki 页面是合成知识，不是源文件的副本或摘录
- **Schema 变更需确认**：本文件（WIKI.md）的变更需要人工确认
- **INDEX 只编目**：INDEX.md 只记录文件路径和一行概要，不摘录内容
- **敏感信息**：标注为"内部机密"的源文档，Wiki 页面只合成结构性信息，不照搬商务细节
