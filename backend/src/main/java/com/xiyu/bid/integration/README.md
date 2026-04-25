# integration 模块（系统集成）

> 一旦我所属的文件夹有所变化，请更新我。

## 职责

负责第三方系统集成的领域逻辑、凭证管理和连通性探测。当前支持企业微信（WeCom）集成。遵循六边形架构：domain 定义纯业务规则，application 编排用例，infrastructure 实现外部 I/O，controller 暴露 HTTP 接口。

## 目录结构

```
integration/
├── application/          # 用例编排：连通性探测、凭证加密
│   ├── WeComConnectivityProbe.java
│   ├── WeComCredentialCipher.java
│   └── WeComMockConnectivityProbe.java
├── controller/           # HTTP 控制器（/api/admin/integrations/wecom）
├── domain/               # 纯业务模型，无框架依赖
│   ├── ValidationResult.java
│   ├── WeComConnectivityResult.java
│   ├── WeComCredential.java
│   └── WeComCredentialValidation.java
├── dto/                  # 请求/响应 DTO
│   ├── WeComConnectivityResponse.java
│   ├── WeComIntegrationRequest.java
│   └── WeComIntegrationResponse.java
└── infrastructure/       # JPA 持久化实现
    └── persistence/
        ├── entity/WeComIntegrationEntity.java
        └── repository/WeComIntegrationJpaRepository.java
```
