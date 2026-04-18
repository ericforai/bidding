# Quality Gate Rollout Plan

一旦我所属的文件夹有所变化，请更新我。

## 目标
- 把质量门禁从“配置存在但默认关闭”推进到“对核心包和正在整治的模块默认可执行”。
- 先收紧低噪音、高信号的检查，再逐步扩大范围。
- 保持 ratchet 原则：已经变绿的范围不再回退。

## 当前状态
- `quality-audit` 已可执行：会运行 Checkstyle、PMD、SpotBugs，但不会阻断构建。
- `quality-strict` 已可执行：会在当前默认范围内阻断构建。
- 当前默认范围：
  - `marketinsight.core`
  - `admin.settings.core`
  - `task.core`
  - `bidresult.core`
  - `projectworkflow`

## 第一阶段：机械问题清零
- 状态：已启动
- 目标：把 Checkstyle 和低争议 PMD 命名问题清零。
- 已完成：
  - `projectworkflow` 下的 JPA 星号导入已修复。
  - `bidresult.core.FunctionalResult` 的 PMD 命名冲突已修复。
- 退出条件：
  - `mvn -Pjava-quality,java-quality-spotbugs,quality-audit checkstyle:check pmd:check spotbugs:check`
    在默认范围内只剩高价值问题。

## 第二阶段：核心值对象防御性复制
- 状态：待处理
- 当前阻塞：
  - `quality-strict` 下 SpotBugs 暴露 26 个 `EI_EXPOSE_REP / EI_EXPOSE_REP2`
- 根因：
  - 多个 `core` record/值对象直接持有并返回可变 `List`/`Map`
- 处理策略：
  - 在 canonical constructor 中做 `List.copyOf(...)` / `Map.copyOf(...)`
  - 对外 getter 继续返回不可变集合
- 首批处理文件：
  - `admin/settings/core/CoreAccessProfile`
  - `admin/settings/core/DepartmentGraph`
  - `admin/settings/core/DepartmentScopeRule`
  - `admin/settings/core/RoleAccessRule`
  - `admin/settings/core/UserScopeRule`
  - `bidresult/core/AwardRegistrationValidation`
  - `task/core/BidSubmissionPolicy`
  - `task/core/DeliverableAssociationPolicy`

## 第三阶段：默认 strict 绿灯
- 状态：待处理
- 目标：
  - 默认范围下 `quality-strict` 全绿
- 验证命令：
  - `mvn -Pjava-quality,java-quality-spotbugs,quality-strict -DforkCount=0 -Dtest=FPJavaArchitectureTest,ScoreDraftPolicyTest,ProjectWorkflowServiceTest test checkstyle:check pmd:check spotbugs:check`

## 第四阶段：逐步扩圈
- 状态：待处理
- 顺序建议：
  1. `projectworkflow` 扩到相邻纯逻辑模块
  2. `bidresult` / `task` 的非 core 但已稳定模块
  3. 其余受保护模块
- 原则：
  - 先 audit
  - 问题分类
  - 再 strict

## 第五阶段：覆盖率 ratchet
- 状态：待处理
- 当前：
  - `jacoco.minimum.coveredratio = 0.10`
- 建议：
  - 先对默认 strict 范围建立更高阈值
  - 再考虑提高全仓阈值
- 不建议：
  - 在全仓测试基线不稳定前，直接抬升全局覆盖率门槛
