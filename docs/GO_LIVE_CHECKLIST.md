# Go-Live Checklist

## 发布前
- [ ] 候选版本已冻结
- [ ] `docs/COMMERCIAL_SCOPE.md` 已确认正式版白名单与 demo-only 黑名单
- [ ] `npm run build` 通过
- [ ] `VITE_API_MODE=api npm run build` 通过
- [ ] `mvn -DskipTests compile` 通过
- [ ] 关键测试通过
- [ ] PostgreSQL baseline Testcontainers 验证通过
- [ ] 数据库备份已执行并校验产物存在
- [ ] 监控面板与告警规则已配置
- [ ] UAT 已通过并签字
- [ ] 已知 P0 缺陷为 0
- [ ] `bash scripts/release/rehearse-release.sh` 已执行并产出报告
- [ ] `Staging Gate` 工作流已通过并上传签字包

## 发布中
- [ ] 执行 `scripts/release/preflight.sh`
- [ ] 记录当前版本号/提交号
- [ ] 停止流量或进入维护窗口
- [ ] 执行数据库迁移
- [ ] 部署后端应用
- [ ] 部署前端静态资源
- [ ] 检查 `/actuator/health`
- [ ] 检查关键接口返回
- [ ] 检查前端首页与主链路

## 发布后 30 分钟
- [ ] 登录主流程正常
- [ ] 项目/标讯列表可访问
- [ ] Knowledge 主链路可访问
- [ ] 资源审批与 BAR 证书借用可访问
- [ ] 无高优先级错误告警
- [ ] 数据库连接池稳定
- [ ] Prometheus 指标可抓取

## 触发回滚条件
- [ ] 数据库迁移失败
- [ ] 应用无法启动
- [ ] 登录主流程失败
- [ ] 核心接口 5xx 持续出现
- [ ] 无法在 15 分钟内恢复核心业务
