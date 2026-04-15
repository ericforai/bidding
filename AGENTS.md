# AGENTS.md - 项目智能体配置

本项目为正式交付版本的西域数智化投标管理平台，目标在2个月内交付给用户正式使用。

## 协作口径规则

- **协作语言**: 默认使用中文进行沟通、代码注释编写与包含Git Commits在内的变更描述。
- **项目品牌**: 在对外输出与需求交流中，统一使用“西域数智化投标管理平台”作为项目全局名称，禁止随意简写。
- **开场约定**: AI 代码代理在开启新任务对话或接收复杂长任务时，必须在回复开场：1) 确认当前处于真实的正式API交付开发模式（全面放弃Mock，推进正式交付），2) 承诺将严格遵循 `RULES.md` 的 Everything Claude Code 四阶段作业流程（plan → tdd → code-review → refactor-clean）展开工作。

## 自动化测试与自我校对 (Mandatory)

为了确保交付质量，所有 AI 代理必须遵守以下**“军令状”**：

1. **落地 TDD 闭环**: 禁止“先改代码再补测试”。在执行功能变更前，必须先更新/编写对应的测试用例（后端 JUnit 或前端 Vitest），验证其失败，再编写功能代码使其通过。
2. **提交前自证清白**: 在宣布任务完成前，AI 代理必须**主动运行**相关测试并输出结果作为证据：
   - **前端变更**: 运行 `npx vitest run <相关测试文件>`。
   - **后端变更**: 运行 `mvn test -Dtest=<相关测试类>`。
   - **核心链路变更**: 运行 `npm run test:e2e`。
3. **自我修复 (Self-Correction)**: 如果测试运行失败，AI 代理必须在不打扰用户的情况下，自行分析报错原因并修复代码，直到测试全绿。
4. **严禁破坏覆盖率**: 禁止通过删除测试或降低断言标准来使 CI 通过。新功能代码必须附带对应的单元测试。

## 项目启动

**方式一：一键启动（推荐）**
```bash
npm run dev:all
```

**方式二：手动启动**
```bash
# 终端1 - 启动后端
cd /Users/user/xiyu/xiyu-bid-poc/backend
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=18080"

# 终端2 - 启动前端
cd /Users/user/xiyu/xiyu-bid-poc
VITE_API_MODE=api VITE_API_BASE_URL=http://127.0.0.1:18080 npm run dev
```



## 技术栈

- **前端**: Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router 4 + Axios + ECharts + Sass
- **E2E测试**: Playwright
- **后端**: Spring Boot 3.2 + Java 21 + PostgreSQL + Redis

## 端口约定

| 服务 | 端口 |
| 前端 | 1314 |
| 后端 | 18080 |

## 演示账号

| 用户名 | 角色 | 说明 |
|--------|------|------|
| 小王 | 销售(sales) | 普通销售人员 |
| 张经理 | 经理(manager) | 部门经理 |
| 李总 | 管理员(admin) | 系统管理员 |

## 开发命令

```bash
# 前端
npm run dev          # 启动开发服务器
npm run build        # 生产环境构建
npm run preview      # 预览构建结果
npm run dev:all      # 前端+后端一键启动

# 后端
cd backend
mvn spring-boot:run  # 启动后端服务
mvn test            # 运行测试

# E2E测试
npm run test:e2e
```
