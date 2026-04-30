# roleprofile 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
提供角色档案的中性支撑能力，供 admin 配置边界和根层业务 service 共同使用，避免 `admin` 与 `service` 包互相依赖。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `RoleProfileBootstrap.java` | Component | 根据 `RoleProfileCatalog` 补齐内置系统角色，并为既有系统角色追加必需种子权限 |
