一旦我所属的文件夹有所变化，请更新我。

# 脚本目录

这里放仓库级校验、清理和维护脚本。
脚本优先服务构建门禁、治理收口和本地环境整理，不承载业务逻辑。

| 文件 | 地位 | 功能 |
|------|------|------|
| `check-doc-consistency.sh` | 兼容入口脚本 | 保留旧命令入口，转调新的文档治理检查器 |
| `check-doc-governance.mjs` | 门禁脚本 | 检查强制目录 README 和强制文件头注释是否符合规范 |
| `check-front-data-boundaries.mjs` | 门禁脚本 | 检查前端业务层是否越过 mock 数据边界 |
| `clean-local-artifacts.sh` | 清理脚本 | 删除本地产生的测试、报告和演练产物 |
| `test/` | 测试基线目录 | Playwright 与 API 联调测试的启动、停止和说明脚本 |
