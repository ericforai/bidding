# MySQL 8.0 部署说明

## 范围

MySQL 8.0 是客户专项的新库部署路径。PostgreSQL 仍是默认和主线数据库。

本路径不包含 PostgreSQL 历史数据迁移到 MySQL。

## 运行配置

必须显式启用 MySQL profile：

```bash
SPRING_PROFILES_ACTIVE=prod,mysql
DB_ENGINE=mysql
DB_HOST=<mysql-host>
DB_PORT=3306
DB_NAME=xiyu_bid
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
```

`DB_URL` 可覆盖完整 JDBC URL。未设置时，MySQL profile 会从标准数据库变量拼接 URL。发布脚本也兼容历史变量 `DB_USER`，未设置 `DB_USER` 时会读取 `DB_USERNAME`。

## Flyway

- PostgreSQL 继续使用 `classpath:db/migration`。
- MySQL 使用 `classpath:db/migration-mysql`。
- 当前 MySQL baseline 为 `B73__full_schema_baseline.sql`。
- 从 `V74` 开始，新业务迁移必须同时提交 PostgreSQL 与 MySQL 两条路径。

## 本地演练

使用现有发布演练跑 MySQL：

```bash
DB_ENGINE=mysql bash scripts/release/rehearse-release.sh
```

演练会启动 `mysql:8.0`、Redis、`prod,mysql` 后端、前端 preview，并执行 UAT、E2E、备份、恢复和恢复后健康检查。

## 备份与恢复

MySQL 环境使用：

```bash
DB_ENGINE=mysql bash scripts/release/backup-db.sh
CONFIRM_RESTORE=YES DB_ENGINE=mysql bash scripts/release/restore-db.sh <backup-file.sql>
```

如果本机没有 MySQL 客户端工具，可设置 `MYSQL_CONTAINER_NAME=<mysql-container>` 走 Docker fallback。
