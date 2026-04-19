# Resources 模块 (平台资源域)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
资源模块统一管理平台账户、费用、BAR 证书和站点子资源，覆盖借用、审批、支付登记、归还和校验流程。这里是资源域的总入口，负责把多个资源子能力维持在同一套边界内。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/AccountController.java` | Controller | 平台账户接口 |
| `controller/ExpenseController.java` | Controller | 费用申请、审批、支付登记与退还接口 |
| `controller/BarAssetController.java` | Controller | BAR 证书资产接口 |
| `controller/BarCertificateController.java` | Controller | BAR 证书借还接口 |
| `controller/BarSiteSubresourceController.java` | Controller | BAR 站点子资源接口 |
| `service/AccountService.java` | Service | 账户业务逻辑 |
| `service/ExpenseService.java` | Service | 费用对外门面服务 |
| `service/expense/ExpenseCommandService.java` | Service | 费用命令编排 |
| `service/expense/ExpenseQueryService.java` | Service | 费用查询编排 |
| `service/expense/ExpensePaymentService.java` | Service | 费用支付登记与记录查询 |
| `service/BarAssetService.java` | Service | BAR 资产业务逻辑 |
| `service/BarCertificateService.java` | Service | BAR 证书业务逻辑 |
| `service/BarSiteSubresourceService.java` | Service | 站点子资源业务逻辑 |
| `entity/` | Entity | Account、BarAsset、Expense、ExpensePaymentRecord、BarCertificate、BarSite* 实体 |
| `repository/` | Repository | 资源数据访问（含支付记录仓储） |
| `dto/` | DTO | 资源请求/响应模型（含支付记录 DTO） |
