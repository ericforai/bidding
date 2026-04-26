# TODO

- [ ] 对接外部标讯聚合 API（替换 `src/views/Bidding/List.vue` 中 `fetchExternalTendersFromApi` 的占位实现，打通“一键获取标讯”真实链路）
- [ ] 补跑 Microsoft Edge 浏览器兼容测试：当前 Chrome、Firefox 29 个页面巡检已通过；Edge 因测试机未安装 `/Applications/Microsoft Edge.app` 且自动安装需要 sudo 密码暂未完成。安装 Edge 后按真实 API 模式补跑同一套页面兼容巡检，并更新验收结论。

## Bid Agent 优化

- [ ] 将 `RequirementProfile` 进一步做成原子化条目拆分，避免长句聚合，便于逐条响应与章节映射
- [ ] 在项目详情抽屉增加“招标拆解预览”，支持人工快速核查结构化结果后再生成初稿
- [ ] 为扫描版 PDF 增加 OCR 流程，并区分文本 PDF / 扫描 PDF 的处理策略与错误提示
- [ ] 优化知识库信号召回，按行业、项目上下文、标签做更精准的资质/模板/案例筛选
- [ ] 继续增强 OpenAI structured output 清洗，对列表字段做更细的语义去噪和分段规整
- [ ] 支持基于解析快照的多版本管理与人工激活，允许选择“哪个招标版本”参与后续生成
- [ ] 补全文档编辑器里的来源追踪展示，让章节可直接查看引用的招标条款与知识库来源
- [ ] 增加生成阶段的异步任务化与进度持久化，支持长文档生成的可靠重试 and 状态恢复

## 系统集成中心

> 2026-04-25 已交付：系统设置「系统集成」Tab + 企业微信配置入口（CorpID/AgentID/Secret + SSO/消息推送启用开关 + 连接测试）。详见 [[integration-wecom]]。以下为后续尚未完成工作。

### 企业微信 — 运行时实现（本次仅做配置）

- [ ] **真实连通性 Probe**：替换 `WeComMockConnectivityProbe` 为调用 `https://qyapi.weixin.qq.com/cgi-bin/gettoken` 的真实实现；缓存 access_token（约 7200s 有效），失败时给出具体错误码与建议处置
- [ ] **SSO OAuth2 回调落地**：
  - 新增 `/api/auth/wecom/callback` 处理 `state + code`
  - 实现 state token 校验（防 CSRF）
  - 通过 OAuth2 接口 `snsapi_base` / `snsapi_privateinfo` 拉用户 openid/userid
  - 与本地 user 表按 userid 或手机号对齐建立联系；首次登录弹绑定窗
  - 登录成功签发 JWT，复用现有 `JwtUtil` + 登录流程
  - 前端在登录页增加"企业微信扫码登录"按钮，未启用时隐藏
- [ ] **应用消息实际推送**：
  - 新增 `WeComMessagePublisher` 服务（`application/` 层）
  - 封装 `https://qyapi.weixin.qq.com/cgi-bin/message/send` 调用
  - 接入三个事件触发点：标讯入库（`TenderCreatedEvent`）、审批结果（`ApprovalDecidedEvent`）、代办提醒（定时任务）
  - 支持按用户开关订阅（在「用户偏好」页增加订阅矩阵）
- [ ] **异常重试与限流**：接 Spring Retry 或自建 backoff，记录最近 N 次失败到独立 `wecom_push_log` 表供排查
- [ ] **配置变更审计**：PUT /api/admin/integrations/wecom 成功后写入 `audit_log`，保留修改前后 diff（Secret 字段脱敏）

### 通讯录同步（二期）

- [ ] 新增 `WeComContactSyncAppService`，周期性或手动触发拉取部门树与成员
- [ ] 落盘到 `department`、`user` 表（保留映射字段 `external_department_id` / `external_user_id`）
- [ ] 前端在「系统集成 → 企业微信」卡增加"立即同步"按钮与最近同步状态
- [ ] 同步冲突策略：企微为准 vs 本地保留，提供开关
- [ ] 离职/调岗增量事件订阅（企微通讯录回调）

### 占位系统落地（等接口规范）

- [ ] **CRM 系统**：客户提供 CRM 接口规范后，按本次企微同样模式实现（domain/application/controller/infrastructure 分层），占位卡替换为真实配置卡
- [ ] **OA / 审批流**：等待客户选定 OA 厂商后对接（用印、合同评审、付款申请三类流程模板映射 + 回调状态同步）
- [ ] **组织架构系统**：如客户有独立组织架构系统（非 OA），需单独对接；否则与 OA 合并

### 开放 API 接口补齐（跟 [[api-openapi]] 配套）

- [ ] 增加 `/api/v1/...` 路径前缀与版本化策略
- [ ] 机器身份认证（API Key / Client Credentials）：新增 `api_client` 表 + `ApiKeyAuthenticationFilter`
- [ ] Webhook 出站事件回调框架：`webhook_subscription` 表 + 事件总线 + 重试机制
- [ ] 在每个 Controller 上补齐 `@Operation` / `@Tag` / `@Schema` 注解，让 Swagger UI 文档更友好

## 架构与性能优化

### 后端数据权限范围查询下推
- [ ] **Goal**: 将统计与导出的可见项目范围计算从 `projectRepository.findAll()` 后内存过滤，下推为数据库层的当前用户可见项目 ID 查询。
- [ ] **Impact**: 降低非管理员统计和导出的查询成本，避免大租户/大项目量下出现慢接口。
- [ ] **Next Step**: 把 `filterAccessibleProjects(projectRepository.findAll())` 替换为 `currentUserAccessibleProjectIds()`。

## QA 遗留事项 (QA Deferred)

- [ ] **非管理员演示账号不可登录**: 登录页展示 `lizong` 等演示账号但密码 401。需明确移除提示或在本地 profile 中种子化。 (Found: 2026-04-25)
- [ ] **导出响应 recordCount 始终为 0**: 已在分支修复，需确认 `ExportController` 已使用 `ExcelExportService` 返回的结构化元数据。 (Found: 2026-04-25)

## 工程质量

- [ ] `ManualTenderDialog.spec.js` 中临时 `.skip` 的 `emits file changes to the parent workflow` 用例需要原维护者重写
- [ ] 评估把 `integration/domain/ValidationResult.java` 与 `bidmatch/domain/ValidationResult.java` 抽取到 `common/domain/` 共享
- [ ] 补一个 `@SpringBootTest` 级别的 integration 测试，验证 `WeComIntegrationController` 真实路径 + Flyway 迁移端到端
