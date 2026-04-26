---
title: DocInsight 文档智能引擎
space: engineering
category: module
tags: [解析, Markdown, 证据链, 结构化提取, 基础设施]
sources:
  - backend/src/main/java/com/xiyu/bid/docinsight/README.md
  - backend/src/main/java/com/xiyu/bid/docinsight/application/DocumentIntelligenceService.java
  - backend/src/main/java/com/xiyu/bid/docinsight/infrastructure/openai/BaseOpenAiDocumentAnalyzer.java
  - src/components/common/doc-insight/DocVerificationWorkbench.vue
backlinks:
  - ai-capabilities
  - modules
  - business-process
created: 2026-04-26
updated: 2026-04-26
---
# DocInsight 文档智能引擎

DocInsight 是全系统统一的文档解析、结构化提取与证据链锚定基础设施。它通过“高保真转换 -> 结构化切片 -> 证据锚定提取”三步法，解决了复杂办公文档（.doc/.pdf）在 AI 处理中易丢失上下文、易产生幻觉的核心痛点。

## 1. 核心架构：三位一体解析流

| 层次 | 技术组件 | 职责 |
|------|----------|------|
| **物理层** | Python Sidecar (MarkItDown) | 将 Office/PDF 原始字节流转换为高保真 Markdown，并计算字符偏移与标题层级。 |
| **结构层** | Java Structural Chunker | 基于标题层级（H1-H6）进行语义切片，每个分块（Chunk）携带完整的“章节路径”。 |
| **认知层** | Generic AI Pipeline | 通过 Base 类封装，注入章节背景，引导 AI 提取带证据（原文摘录）的结构化结果。 |

## 2. 核心特性

### 2.1 结构化切片 (Structural Chunking)
不同于传统的固定字符数切分，DocInsight 会识别文档的物理结构。AI 在解析“技术规范”时，会明确知道当前正文属于“第五章 > 第三节 > 5.3.2 指标要求”。

### 2.2 证据锚定 (Evidence Anchoring)
引擎强制要求 AI 在提取每个结构化字段（如：项目预算、强制资质）时，必须附带：
1.  **sourceExcerpt**：原文中能支撑该结论的摘录片段。
2.  **sectionPath**：该结论所在的章节全路径。

### 2.3 交互式证据核对台
前端提供通用的 `DocVerificationWorkbench` 组件：
*   **点击溯源**：点击左侧提取出的结构化数据，右侧 Markdown 原文自动滚动并高亮对应证据。
*   **Schema 驱动**：支持通过 JSON 定义不同的核对表单（标书模式、合同模式等）。

## 3. 业务价值 (The XiYu Edge)

1.  **消除 AI 幻觉**：每一项立项决策都有“原文实锤”，将合规风险降至最低。
2.  **极速准入**：将 80+ 页标书的人工核对时间从 1 小时缩短至 5 分钟以内。
3.  **底层能力化**：全系统（投标、法务、人事）可统一复用这一智能大脑。

## 4. 接入指南

### 后端调用
```java
// 调用通用分析服务
DocumentAnalysisResult result = docInsightService.process(
    "TENDER",       // Profile 编码
    projectId,      // 业务实体 ID
    multipartFile   // 原始文件
);
```

### 前端集成
```vue
<DocVerificationWorkbench
  title="立项深度核对"
  :schema="tenderSchema"
  :data="extractedData"
  :requirements="requirements"
  :markdown="rawMarkdown"
/>
```
