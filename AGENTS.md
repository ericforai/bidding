# AGENTS.md - 项目智能体配置

本项目为西域数智化投标管理平台 (POC)，支持双模式运行。

## 项目启动方式

### Mock 模式（前端独立运行）

```bash
cd /Users/user/xiyu/xiyu-bid-poc
npm install
npm run dev
```

- 前端独立运行，使用 `src/api/mock.js` 作为数据源
- 无需启动后端
- 端口：**1314**
- 访问地址：http://127.0.0.1:1314

### API 模式（前端 + 后端联调）

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

**切换模式：**
- 通过环境变量 `VITE_API_MODE` 控制（`mock` 或 `api`）
- 或在 `.env` 文件中配置

## 技术栈

- **前端**: Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router 4 + Axios + ECharts + Sass
- **E2E测试**: Playwright
- **后端**: Spring Boot 3.2 + Java 21 + PostgreSQL + Redis

## 端口约定

| 服务 | 端口 |
|------|------|
| 前端 (Mock) | 1314 |
| 前端 (API模式) | 1314 |
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
