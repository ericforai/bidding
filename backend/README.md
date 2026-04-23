# Backend - 西域智慧供应链投标管理平台后端

一旦我所属的文件夹有所变化，请更新我。

## 功能概述
基于Spring Boot 3.2 + Java 21的投标管理平台后端服务，提供标讯管理、项目管理、任务协作、知识库、资源管理和智能预警等核心业务功能。

## 技术栈
- Spring Boot 3.2.0
- Java 21
- Spring Security + JWT认证
- Spring Data JPA
- MySQL 8.0
- Redis
- Lombok

## 目录结构
```
src/main/java/com/xiyu/bid/
├── annotation/        # 注解定义（@Auditable等）
├── aspect/            # AOP切面（审计日志）
├── auth/              # 认证授权（JWT过滤器、用户详情服务）
├── config/            # 配置类（Security、JWT、CORS、Async、RateLimit）
├── controller/        # REST控制器（API端点）
├── dto/               # 数据传输对象
├── entity/            # JPA实体
├── exception/         # 异常定义
├── repository/        # 数据访问层
├── service/           # 业务逻辑层
├── util/              # 工具类
├── alerts/            # 智能预警模块
└── resources/        # 资源管理模块
```

## 运行命令
```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 质量审计（只报问题，不阻断）
mvn -Pjava-quality,java-quality-spotbugs,quality-audit checkstyle:check pmd:check spotbugs:check

# 渐进收严（当前默认只扫 core 包和 projectworkflow）
mvn -Pjava-quality,java-quality-spotbugs,quality-strict -DforkCount=0 test checkstyle:check pmd:check spotbugs:check

# 启动应用
mvn spring-boot:run

# 打包
mvn clean package
```

## 质量门禁策略
- `quality-audit`: 审计模式，打开质量插件但不阻断构建，适合先盘点问题。
- `quality-strict`: 严格模式，默认由 Checkstyle + SpotBugs 对受保护范围做真实阻断。
- PMD 当前定位为“复杂度和坏味道提醒层”，会持续输出问题，但默认不作为阻断条件。
- SpotBugs 当前定位为“真实风险门禁”，先只阻断受保护范围内的高价值问题。
- 默认扫描范围收敛到 `core` 包和 `projectworkflow`，避免一开始全仓爆炸。
- 需要扩大或缩小范围时，可覆盖 `-Dquality.includes=...` 和 `-Dquality.onlyAnalyze=...`。
- 覆盖率门槛由 `jacoco.minimum.coveredratio` 控制；审计模式默认跳过覆盖率阻断。
- SpotBugs 豁免统一收口到 `config/spotbugs/exclude.xml`，只允许少量、带理由的例外。
- 治理规则、扩圈标准和 CI 约定见 `backend/QUALITY_GATE_GUIDE.md`。

## 与 CI 对齐的门禁命令
```bash
# 静态质量门禁
mvn -Pjava-quality,java-quality-spotbugs,quality-strict -DskipTests -Djacoco.skip=true checkstyle:check pmd:check spotbugs:check

# 架构门禁
mvn test -Dtest=FPJavaArchitectureTest,MaintainabilityArchitectureTest,ArchitectureTest

# Flyway / 资源链路门禁
mvn test -Dtest=FlywayBaselineContextTest,FlywayMysqlContainerTest,ExpenseControllerIntegrationTest,BarCertificateControllerIntegrationTest
```

## 环境变量
- `DB_PASSWORD` - 数据库密码（必填）
- `JWT_SECRET` - JWT密钥，最少32字符（必填）
- `CORS_ALLOWED_ORIGINS` - 允许的CORS源（默认：localhost:5173,5174,3000）
- `SPRING_PROFILES_ACTIVE` - 环境配置（常用：`e2e`、`dev,mysql`、`prod,mysql`）

## e2e(H2) Demo 融合约定
- 当 `SPRING_PROFILES_ACTIVE` 包含 `e2e` 时，读取类 API 会返回“真实数据 + 内存 Demo 数据”的融合结果。
- Demo 记录统一使用负数 ID，避免与真实数据库主键冲突。
- 对 Demo 负数 ID 的写操作（新增/修改/删除）会返回受控失败，语义为“演示数据只读”。
- 非 `e2e` 环境不启用上述融合逻辑。
