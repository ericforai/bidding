export const hiddenApiMenuNames = new Set(['CustomerOpportunityCenter', 'OperationLogs', 'AuditLogs'])

export const sidebarMenuConfig = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    meta: { title: '工作台', icon: 'workbench', permissionKeys: ['dashboard'] }
  },
  {
    path: '/bidding',
    name: 'Bidding',
    meta: { title: '标讯中心', icon: 'bidding', permissionKeys: ['bidding', 'bidding-list'] }
  },
  {
    path: '/bidding/customer-opportunities',
    name: 'CustomerOpportunityCenter',
    meta: { title: '客户商机中心', icon: 'bidding', permissionKeys: ['bidding'] }
  },
  {
    path: '/project',
    name: 'Project',
    meta: { title: '投标项目', icon: 'project', permissionKeys: ['project'] },
    children: [
      {
        path: '/project',
        name: 'ProjectList',
        meta: { title: '项目列表', permissionKeys: ['project', 'project-list'] }
      },
      {
        path: '/project/create',
        name: 'ProjectCreate',
        meta: { title: '创建项目', permissionKeys: ['project', 'project-create'] }
      }
    ]
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    meta: { title: '知识库', icon: 'knowledge', permissionKeys: ['knowledge'] },
    children: [
      {
        path: '/knowledge/qualification',
        name: 'Qualification',
        meta: { title: '资质库', permissionKeys: ['knowledge', 'knowledge-qualification'] }
      },
      {
        path: '/knowledge/case',
        name: 'Case',
        meta: { title: '案例库', permissionKeys: ['knowledge', 'knowledge-case'] }
      },
      {
        path: '/knowledge/template',
        name: 'Template',
        meta: { title: '模板库', permissionKeys: ['knowledge', 'knowledge-template'] }
      }
    ]
  },
  {
    path: '/resource',
    name: 'Resource',
    meta: { title: '资源管理', icon: 'resource', permissionKeys: ['resource'] },
    children: [
      {
        path: '/resource/bar',
        name: 'BAR',
        meta: { title: '资产台账', permissionKeys: ['resource', 'resource-bar'] }
      },
      {
        path: '/resource/bar/sites',
        name: 'BAR_SiteList',
        meta: { title: '站点台账', permissionKeys: ['resource', 'resource-bar'] }
      },
      {
        path: '/resource/expense',
        name: 'Expense',
        meta: { title: '费用管理', permissionKeys: ['resource', 'resource-expense'] }
      },
      {
        path: '/resource/account',
        name: 'Account',
        meta: { title: '账户管理', permissionKeys: ['resource', 'resource-account'] }
      },
      {
        path: '/resource/contract-borrow',
        name: 'ContractBorrow',
        meta: { title: '合同借阅', permissionKeys: ['resource'] }
      },
      {
        path: '/resource/bid-result',
        name: 'BidResult',
        meta: { title: '结果闭环', permissionKeys: ['resource'] }
      }
    ]
  },
  {
    path: '/ai-center',
    name: 'AICenter',
    meta: { title: 'AI 智能中心', icon: 'ai-center', permissionKeys: ['ai-center'] }
  },
  {
    path: '/analytics/dashboard',
    name: 'AnalyticsDashboard',
    meta: { title: '数据分析', icon: 'analytics', roles: ['admin', 'manager'], permissionKeys: ['analytics', 'analytics-dashboard'] }
  },
  {
    path: '/operation-logs',
    name: 'OperationLogs',
    meta: { title: '操作日志', icon: 'history', permissionKeys: ['operation-logs'] }
  },
  {
    path: '/audit-logs',
    name: 'AuditLogs',
    meta: { title: '审计日志', icon: 'lock', roles: ['admin', 'auditor'], permissionKeys: ['audit-logs'] }
  },
  {
    path: '/settings',
    name: 'Settings',
    meta: { title: '系统设置', icon: 'settings', roles: ['admin'], permissionKeys: ['settings'] },
    children: [
      {
        path: '/settings',
        name: 'SettingsRoot',
        meta: { title: '组织设置', roles: ['admin'], permissionKeys: ['settings'] }
      },
      {
        path: '/settings/workflow-forms',
        name: 'WorkflowFormDesigner',
        meta: { title: '流程表单配置', roles: ['admin'], permissionKeys: ['settings', 'settings-workflow-forms'] }
      },
      {
        path: '/settings/alert-rules',
        name: 'AlertRules',
        meta: { title: '告警规则', roles: ['admin', 'manager'], permissionKeys: ['settings'] }
      },
      {
        path: '/settings/alert-history',
        name: 'AlertHistory',
        meta: { title: '告警历史', roles: ['admin', 'manager', 'staff'], permissionKeys: ['settings'] }
      }
    ]
  }
]

export const roleMenuOptions = [
  ...sidebarMenuConfig
    .filter((menu) => !hiddenApiMenuNames.has(menu.name))
    .map((menu) => ({
      value: menu.meta.permissionKeys[0],
      label: menu.meta.title
    })),
  { value: 'dashboard.quickStart', label: '工作台快速发起' },
  { value: 'dashboard:view_tender_list', label: '工作台：标讯列表' },
  { value: 'dashboard:view_project_list', label: '工作台：负责项目' },
  { value: 'dashboard:view_technical_task', label: '工作台：技术任务' },
  { value: 'dashboard:view_review_list', label: '工作台：待评审列表' },
  { value: 'dashboard:view_team_task', label: '工作台：团队任务' },
  { value: 'dashboard:view_global_projects', label: '工作台：全院重点项目' }
]
