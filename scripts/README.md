一旦我所属的文件夹有所变化，请更新我。

# 脚本目录

这里放仓库级校验、清理和维护脚本。
脚本优先服务构建门禁、治理收口和本地环境整理，不承载业务逻辑。

| 文件 | 地位 | 功能 |
|------|------|------|
| `check-doc-consistency.sh` | 兼容入口脚本 | 保留旧命令入口，转调新的文档治理检查器 |
| `check-doc-governance.mjs` | 门禁脚本 | 检查强制目录 README 和强制文件头注释是否符合规范 |
| `check-front-data-boundaries.mjs` | 门禁脚本 | 检查前端业务层是否越过 mock 数据边界 |
| `check-java-coding-standards.sh` | 门禁脚本 | 检查暂存区 Java 代码规范（如 `catch(Exception)`、`Optional.get()`、原始泛型），并执行 `java-quality` 质量门禁（Checkstyle/PMD；网络可用时自动启用 SpotBugs，可通过 `JAVA_STANDARDS_SPOTBUGS=auto|on|off` 控制） |
| `install-java-standards-hook.sh` | 安装脚本 | 将仓库 `.githooks/pre-commit` 安装到本地 `.git/hooks/pre-commit` |
| `clean-local-artifacts.sh` | 清理脚本 | 删除本地产生的测试、报告和演练产物 |
| `test/` | 测试基线目录 | Playwright 与 API 联调测试的启动、停止和说明脚本 |
