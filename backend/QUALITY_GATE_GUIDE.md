# Quality Gate Governance Guide

一旦我所属的文件夹有所变化，请更新我。

## 目的
- 让质量门禁从“能跑”变成“团队知道何时跑、为什么失败、何时扩圈”。
- 固化 `quality-audit` 与 `quality-strict` 的职责，避免每次靠口头约定。

## 模式定义
- `quality-audit`
  - 用途：盘点问题、形成治理清单
  - 行为：运行 Checkstyle、PMD、SpotBugs，但不阻断构建
  - 适用场景：
    - 新模块准备纳入受保护范围前
    - 普通 PR 的例行质量盘点
    - 扩圈前的现状摸底
- `quality-strict`
  - 用途：对受保护范围进行真实阻断
  - 行为：在当前默认范围内，任何 Checkstyle / PMD / SpotBugs 违规都会失败
  - 适用场景：
    - 修改默认受保护范围内代码
    - 扩圈 PR
    - 调整质量门禁配置本身

## 当前默认受保护范围
- `marketinsight.core`
- `admin.settings.core`
- `task.core`
- `bidresult.core`
- `projectworkflow`

范围定义来源只有两处：
- `pom.xml` 中的 `quality.includes`
- `pom.xml` 中的 `quality.onlyAnalyze`

禁止在其他脚本、工作流或临时命令中维护第二份范围清单。

## 问题分级
- `L1` 机械问题
  - 示例：导入、命名、显而易见的简单坏味道
  - 要求：进入 strict 前必须清零
- `L2` 核心结构问题
  - 示例：集合暴露、不可变性缺口、边界泄漏
  - 要求：在受保护范围内必须清零
- `L3` 历史债
  - 示例：大范围旧代码复杂度、广泛分布的长期坏味道
  - 要求：先在 audit 中收集，进入专项治理，不阻断未纳入 strict 的范围

## 模块扩圈准入标准
一个模块进入 `quality-strict` 前，必须同时满足：
- Checkstyle 0 阻断问题
- PMD 0 阻断问题
- SpotBugs 0 阻断问题
- 现有架构门禁通过
- 至少有最小回归测试入口

## 扩圈操作步骤
1. 用 `quality-audit` 在候选模块范围内跑质量盘点
2. 把问题按 `L1/L2/L3` 分类
3. 清理 `L1/L2`
4. 补最小回归测试入口
5. 同时更新：
   - `pom.xml` 的 `quality.includes`
   - `pom.xml` 的 `quality.onlyAnalyze`
   - `QUALITY_GATE_PLAN.md`
6. 跑 strict 验证通过后再合入

## 覆盖率策略
- 当前全仓 JaCoCo 阈值仍维持保守值
- 下一阶段采用“受保护范围 ratchet”策略，而不是直接提高全仓阈值
- 任何覆盖率阈值提升都必须附带：
  - 适用范围
  - 当前基线
  - 新阈值
  - 回归命令

## 运行命令
```bash
# 审计模式
mvn -Pjava-quality,java-quality-spotbugs,quality-audit checkstyle:check pmd:check spotbugs:check

# 严格模式
mvn -Pjava-quality,java-quality-spotbugs,quality-strict -DforkCount=0 test checkstyle:check pmd:check spotbugs:check
```

## CI 约定
- 普通 PR：默认运行 `quality-audit`
- 修改受保护范围或质量门禁配置的 PR：额外运行 `quality-strict`
- 扩圈 PR：必须同时更新范围配置、计划文档和验证结果
