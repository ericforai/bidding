# 代码格式化（Spotless + google-java-format）

后端使用 [Spotless](https://github.com/diffplug/spotless) + [google-java-format](https://github.com/google/google-java-format) 作为 Java 代码格式化工具，目标是把"代码风格"从人工 review 中彻底剥离出去——保存即合规。

## 当前生效范围（试点）

仅 `backend/src/main/java/com/xiyu/bid/biddraftagent/domain/**`。

后续按 PR 节奏分批扩到整个 `biddraftagent/`，再扩全仓。

## 常用命令

```bash
cd backend

# 检查（CI / PR 用）
mvn -Pjava-format spotless:check

# 一键修复所有格式问题（开发本地用）
mvn -Pjava-format spotless:apply

# 与 quality-strict profile 一起跑（CI 完整 verify）
mvn -Pjava-format,quality-strict verify
```

`spotless:check` 已经挂在 `verify` 阶段：CI 跑 `mvn -Pjava-format verify` 时格式不合规会自动失败。

## 风格规则

- Google Java Style（2 空格缩进、列宽 100、自动换行）
- import 顺序：`java` → `javax` → `jakarta` → `org` → `com` → `com.xiyu` → 其他
- 自动移除未使用 import
- 自动 trim 行尾空格 + 强制文件末尾换行

完整配置见 `pom.xml` 的 `java-format` profile。

## IDE 集成（建议团队统一）

- **IntelliJ IDEA**：装 `google-java-format` 插件，启用并设为默认 formatter（Settings → Tools → google-java-format Settings）。
- **VS Code**：装 `vscjava.vscode-java-pack` 后，把 `java.format.settings.url` 指向 google style，或者依赖 spotless pre-commit。
- 任何 IDE 都可以靠 `mvn -Pjava-format spotless:apply` 兜底。

## 为什么是 Spotless 而不是手补 checkstyle 违规

`maven-checkstyle-plugin` 默认带的 sun_checks 跑出来 26000 条违规，多数是 `final` 参数、Javadoc、行宽这些**机械问题** —— 这些应该由格式化器自动处理，让人写 boilerplate 是反生产力的。Checkstyle 仍然保留，用来卡 import / IllegalCatch 这种**语义级别**的检查。
