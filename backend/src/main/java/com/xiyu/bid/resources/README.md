# Resources Module (平台资源模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

管理第三方投标平台的账户和 CA 数字证书资源，支持账户借用和归还。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `entity/Account.java` | Entity | 平台账户实体 |
| `entity/BarAsset.java` | Entity | CA 数字证书资产实体 |
| `repository/AccountRepository.java` | Repository | 账户数据访问 |
| `repository/BarAssetRepository.java` | Repository | CA 证书数据访问 |
| `dto/AccountCreateRequest.java` | DTO | 创建账户请求 |
| `dto/AccountUpdateRequest.java` | DTO | 更新账户请求 |
| `dto/BarAssetCreateRequest.java` | DTO | 创建 CA 证书请求 |
| `dto/BarAssetUpdateRequest.java` | DTO | 更新 CA 证书请求 |
| `service/AccountService.java` | Service | 账户业务逻辑 |

## API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/platform/accounts` | 获取平台账户列表 |
| POST | `/api/platform/accounts` | 添加平台账户 |
| PUT | `/api/platform/accounts/{id}` | 更新账户信息 |
| DELETE | `/api/platform/accounts/{id}` | 删除账户 |
| POST | `/api/platform/accounts/{id}/borrow` | 借用账户 |
| POST | `/api/platform/accounts/{id}/return` | 归还账户 |
| GET | `/api/bar/assets` | 获取 CA 证书列表 |
| POST | `/api/bar/assets` | 添加 CA 证书 |
| PUT | `/api/bar/assets/{id}` | 更新 CA 证书 |
| DELETE | `/api/bar/assets/{id}` | 删除 CA 证书 |
| GET | `/api/bar/statistics` | 获取资产统计 |
