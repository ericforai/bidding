# Platform Account Management Module

> 一旦我所属的文件夹有所变化，请更新我。

## 目录功能

平台账户管理模块，负责管理各招标平台的账户信息，支持账户的借用、归还及密码加密存储。

## 文件清单

### Entity (实体层)
- `entity/PlatformAccount.java` - 平台账户实体，包含账户名、加密密码、状态、借用信息等

### Repository (数据访问层)
- `repository/PlatformAccountRepository.java` - 平台账户数据访问接口

### Service (业务层)
- `service/PlatformAccountService.java` - 平台账户业务逻辑，包含借用/归还流程

### Controller (控制器层)
- `controller/PlatformAccountController.java` - 平台账户 REST API 端点

### DTO (数据传输对象)
- `dto/PlatformAccountDTO.java` - 平台账户数据传输对象（不含密码）
- `dto/PlatformAccountCreateRequest.java` - 创建账户请求
- `dto/BorrowAccountRequest.java` - 借用账户请求
- `dto/PlatformAccountStatisticsDTO.java` - 账户统计信息

### Util (工具类)
- `util/PasswordEncryptionUtil.java` - AES-256-GCM 密码加密工具

## API 端点

- `POST /api/platform/accounts` - 创建账户
- `GET /api/platform/accounts` - 获取账户列表
- `GET /api/platform/accounts/{id}` - 获取账户详情
- `PUT /api/platform/accounts/{id}` - 更新账户
- `DELETE /api/platform/accounts/{id}` - 删除账户
- `POST /api/platform/accounts/{id}/borrow` - 借用账户
- `POST /api/platform/accounts/{id}/return` - 归还账户
- `GET /api/platform/accounts/{id}/password` - 获取解密密码（需审计）
- `GET /api/platform/accounts/statistics` - 获取统计信息

## 状态流转

```
AVAILABLE → IN_USE → AVAILABLE
              ↓
           DISABLED
```
