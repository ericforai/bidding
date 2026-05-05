# 发布回滚 SOP（ROLLBACK.md）

> 一旦本目录文件发生变化，请更新本 SOP 与上层 `docs/` 索引。

本文件覆盖前端、后端、数据库三类回滚的标准动作。SLA 目标：发现问题后 30 分钟内完成回滚。

## 0. 部署快照（每次上线前必须留存）

每次部署前，由发布人记录三类锚点：

1. **Git 锚点**：`git tag release-<YYYYMMDD>-<NN>` 推到 origin。
2. **前端 dist 归档**：把上一版与本次 dist 各保留一份命名 `dist-<git_tag>.tar.gz` 到部署机 `/srv/xiyu-bid-poc/releases/`。
3. **DB 备份**：`mysqldump xiyu_bid | gzip > /srv/xiyu-bid-poc/db-backups/xiyu_bid-<git_tag>.sql.gz`。Flyway version 一并 `select * from flyway_schema_history` 落到该备份元信息。
4. **环境变量快照**：`/srv/xiyu-bid-poc/env/<git_tag>.env`，至少含 `JWT_SECRET / DB_URL / CORS_ALLOWED_ORIGINS / ADMIN_PASSWORD`。

## 1. 前端 dist 回滚（最快，2 分钟内）

部署机用 nginx symlink 切换：

```bash
cd /srv/xiyu-bid-poc/frontend
ln -sfn releases/dist-<上一版 tag> current
nginx -s reload
```

验证：`curl https://<domain>/` 返回 200，且页面顶部 `<meta name="version">` 与上一版一致。

## 2. 后端服务回滚（5 分钟内）

```bash
cd /srv/xiyu-bid-poc/backend
ln -sfn releases/bid-poc-<上一版 tag>.jar current.jar
systemctl restart xiyu-bid-backend
```

启动后立刻检查：

```bash
curl -fsS http://127.0.0.1:8080/actuator/health/liveness
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
```

`status` 必须为 `UP`。如果 `down`，立即查看 `/var/log/xiyu-bid/application.log` 最新栈。

## 3. Flyway 数据库回滚（最关键、最危险）

**首要原则**：**生产环境严禁 `flyway clean`**。`application-prod.yml` 已设 `baseline-on-migrate: false`，禁止自动 baseline。

### 3.1 迁移失败但部分应用

1. `flyway info -url=<DB_URL> -user=<user> -password=<pwd>`，定位 `Pending`/`Failed` 的版本号。
2. 如果当次失败的迁移有显式 down 脚本（位于 `backend/src/main/resources/db/migration-mysql-rollback/V<NN>__down.sql`），执行该脚本将 schema 回退到该版本之前。
3. 执行 `flyway repair`，把失败行从 `flyway_schema_history` 中清理。
4. 重启后端验证。

### 3.2 没有 down 脚本的迁移

> **目前仓库 `db/migration-mysql/` 下大部分迁移没有配套 down 脚本**——这是已知技术债，列入 `TECHNICAL_DEBT.md` 跟踪。

应急路径：
1. `mysqldump` 出当前 DB 全量备份（防止下一步出问题）。
2. 用上线前 `db-backups/<git_tag>.sql.gz` **整库恢复**：

   ```bash
   gunzip < db-backups/xiyu_bid-<git_tag>.sql.gz | mysql -u root -p xiyu_bid
   ```
3. 跑后端 jar 的上一版（不要跑新版，否则 Flyway 又会推到新版本）。
4. 事后由 DBA + 后端 owner 一起补 down 脚本，进 `migration-mysql-rollback/`。

### 3.3 应急下线（破坏性）

若仅业务异常但 DB 正常，可通过轮换 `JWT_SECRET` 让所有 access/refresh token 立即失效，全员被踢下线（用作"快速止血"，非常规回滚）：

```bash
# 在部署环境
export JWT_SECRET=$(openssl rand -base64 48)
systemctl restart xiyu-bid-backend
```

注意：用户必须重新登录；正在进行中的业务会丢失。

## 4. 配置与密钥回滚

`/srv/xiyu-bid-poc/env/<git_tag>.env` → 恢复对应版本：

```bash
cp /srv/xiyu-bid-poc/env/<上一版 tag>.env /etc/xiyu-bid/backend.env
systemctl restart xiyu-bid-backend
```

`CORS_ALLOWED_ORIGINS` 一旦变错，前端控制台会立即看到 CORS 失败；这是 5 分钟内可发现的故障。

## 5. 回滚演练 Checklist

每月一次在 STG 环境执行下列演练，回归 SLA：

- [ ] **DEV 演练**：跑前后端回滚 + Flyway repair，记录耗时（应 < 10 分钟）
- [ ] **STG 演练**：包含 dist 回滚 + jar 回滚 + 整库恢复，记录耗时（应 < 30 分钟）
- [ ] **生产**：每个季度做一次只读演练（不切流量），验证脚本可执行
- [ ] **JWT_SECRET 轮换**：演练在生产做一次，确认登录界面、refresh、logout 都不出错（除强制下线本身）

## 6. 关键联系人 / 责任人

| 角色 | 责任 | 联系 |
|---|---|---|
| 发布负责人 | 触发回滚决策 | （TBD） |
| 后端 owner | 跑后端回滚命令 | （TBD） |
| DBA | DB 恢复 | （TBD） |
| 前端 owner | dist 切换、缓存清理 | （TBD） |

## 7. 回滚后必做

1. 把回滚原因 + 耗时 + 痛点写入 `docs/release/incidents/<date>.md`（目录可惰性创建）。
2. 出问题的版本 git tag 加 `-broken` 后缀。
3. 7 个工作日内补 RCA 与修复方案。
