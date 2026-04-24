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
- [ ] 增加生成阶段的异步任务化与进度持久化，支持长文档生成的可靠重试和状态恢复
