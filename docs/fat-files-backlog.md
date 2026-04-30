# 存量胖文件治理 Backlog

**最后盘点时间**: 2026-04-30
**盘点范围**: `src/**/*.{vue,js}` 中 > 500 行的文件
**盘点命令**:
```bash
find src -name "*.vue" -o -name "*.js" | xargs wc -l 2>/dev/null \
  | awk '$1 > 500 && $2 != "total" {print $1, $2}' | sort -rn
```

## 背景

`npm run check:line-budgets` 是 **ratcheting** 门禁：它只拦 **新增** ≤ 300 行 + **历史胖文件不能再长**，但对存量 > 300 行的文件没有主动告警。

真实案例：`src/views/Analytics/Dashboard.vue` 在 2153 行的规模下存活到 2026-04-30（PR #106 才拆完），期间没触发任何 CI 告警。这份 backlog 就是为了**把存量风险显式化**，让它不再隐身。

治理目标：**不要求一次全拆**，而是按 ROI 分 sprint 消化。本 backlog 每季度重新盘点一次。

---

## 当前清单（16 个）

> "90d commits" = 过去 90 天内该文件被修改的 commit 数，反映**热度**；热度越高，拆分 ROI 越大（每次 CR/merge 都受益）。

### 🔴 P0 — 胖 + 高频改动（最该先拆）

| 行数 | 文件 | 90d commits | 拆分切入点 |
|---:|---|---:|---|
| 526 | `src/components/layout/Sidebar.vue` | **27** | 路由菜单配置 + 权限过滤 + 折叠态管理可独立为 composable |
| 608 | `src/components/layout/Header.vue` | 14 | 用户下拉 / 通知 / 全局搜索 / 角色切换可分别抽组件 |
| 568 | `src/views/Login.vue` | 12 | 登录表单 / 凭据提示 / 社交登录 按块拆 |
| 596 | `src/views/Resource/BAR/SiteList.vue` | 10 | 筛选条 + 表格 + 批量操作 + 弹窗（本次已顺手拆 pagination，但主体仍在） |
| 572 | `src/api/modules/knowledge.js` | 9 | API 层，按 endpoint 组（case / template / qualification / score）拆子文件 |
| 510 | `src/api/modules/collaboration.js` | 9 | 同上（comments / mentions / exports / assignments 拆子文件） |

### 🟡 P1 — 中等胖度 + 中频（中等 ROI）

| 行数 | 文件 | 90d commits | 备注 |
|---:|---|---:|---|
| 711 | `src/views/Resource/BAR/SiteDetail.vue` | 6 | 最胖，但改动不高频；建议与 `SiteList.vue` 一起拆成一个域 |
| 563 | `src/views/Resource/Account.vue` | 7 | 账号管理页 |
| 525 | `src/views/Project/List.vue` | 7 | 本次已顺手拆 pagination |
| 635 | `src/components/ai/VersionControl.vue` | 4 | AI 版本控制组件 |
| 584 | `src/components/ai/CollaborationCenter.vue` | 4 | 协作中心 |
| 503 | `src/components/common/TaskBoard.vue` | 4 | 任务看板 |
| 533 | `src/components/ai/MobileCard.vue` | 5 | 移动端 AI 卡片 |

### 🟢 P2 — 稳定胖文件（低 ROI，暂缓）

| 行数 | 文件 | 90d commits | 备注 |
|---:|---|---:|---|
| 700 | `src/components/ai/ConfigDialog.vue` | 2 | 几乎不改 |
| 515 | `src/components/ai/ComplianceCheck.vue` | 3 | 合规检查 |
| 565 | `src/api/trendradar.js` | 1 | 基本没人碰 |

---

## 建议 Sprint 节奏

> 每个 Sprint 开一个 tracking issue；实际拆分开 1 个 PR / 文件，方便 review。

### Sprint 1 — 布局基建层（~1700 行，P0 核心）
- [ ] `Sidebar.vue`（526 → 目标 ≤ 300）
- [ ] `Header.vue`（608 → ≤ 300）
- [ ] `Login.vue`（568 → ≤ 300）

**理由**：改动最频繁的三个文件；拆了立刻受益；全站可见。

### Sprint 2 — 资产管理（BAR）模块（~1310 行）
- [ ] `SiteList.vue`（596 → ≤ 300）
- [ ] `SiteDetail.vue`（711 → ≤ 300）

**理由**：两兄弟同模块，可抽共享子组件 + composable；一次拆完整域。

### Sprint 3 — API 模块层（~1650 行）
- [ ] `api/modules/knowledge.js`（572 → ≤ 300）
- [ ] `api/modules/collaboration.js`（510 → ≤ 300）
- [ ] `api/trendradar.js`（565 → ≤ 300）— 顺手，几乎没人碰

**理由**：API 层拆分模式固定（按 endpoint 分文件），机械工作，CR 快。

### Sprint 4 — 视图杂项（~1500 行）
- [ ] `views/Project/List.vue`（525 → ≤ 300）
- [ ] `views/Resource/Account.vue`（563 → ≤ 300）
- [ ] `components/common/TaskBoard.vue`（503 → ≤ 300）

### Sprint 5（可选）— AI 组件族（~3000 行）
- [ ] `components/ai/VersionControl.vue`
- [ ] `components/ai/CollaborationCenter.vue`
- [ ] `components/ai/MobileCard.vue`
- [ ] `components/ai/ConfigDialog.vue`
- [ ] `components/ai/ComplianceCheck.vue`

**理由**：构成一个 AI UI 子系统，可以统一抽 `ai-core/` composable 层。

---

## 拆分 Playbook（参考 PR #106）

以 Editor/Create/Dashboard 三大拆分为范本：

1. **先读完整个目标文件**，识别 3 类边界：
   - **UI 区块**（Header / Panel / Dialog）→ 抽子组件
   - **业务逻辑块**（数据加载 / 过滤 / 下钻）→ 抽 `composables/`
   - **纯函数**（formatter / option builder）→ 抽 `utils/`
2. **主壳保留路由挂载点**，只做"排布 + 连线"
3. **props/emits 严格契约**，父子之间用 `v-model` 或 `defineModel`，不让子组件直接改 prop
4. **每抽一个，跑一次** `npm run lint && npm run build && npm run test:unit`
5. **按 ≤ 300 行的配额写**，CSS 超 200 行考虑独立 scoped 样式或移给子组件
6. **风险点优先处理**：有 URL 同步 / 跨组件事件 / 第三方库挂载的区块先抽，避免最后留坑

**自动化门禁**：`check:line-budgets` 仍会拦"新增文件 > 300" + "历史胖文件再长"，拆的过程中自动保护。

---

## 后续动作

- 本 backlog 每季度重新 `find | wc -l | awk` 一次，更新行数与 commit 频率
- 新增 > 300 行的胖文件一上来就触发告警（已由 `check:line-budgets` 覆盖）
- 超 500 行的存量文件**建议加到 CI warning**（不 fail），让每个 PR 都看到
- Sprint 完成后打勾 `[x]`；全部完成后废弃本文档
