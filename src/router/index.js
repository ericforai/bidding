import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { hasMenuAccessForRole } from '@/api/modules/settings'

const DEFAULT_AUTHENTICATED_HOME = '/dashboard'

const getStoredUser = () => {
  const raw = localStorage.getItem('user') || sessionStorage.getItem('user')
  if (!raw) return null

  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

const getNormalizedRole = (userStore) => {
  const role = userStore.currentUser?.role || getStoredUser()?.role || ''
  return String(role).toLowerCase()
}

const getRequiredRoles = (to) => {
  const roles = to.matched.flatMap((record) => record.meta?.roles || [])
  return [...new Set(roles)]
}

const getRequiredPermissionKeys = (to) => {
  const permissionKeys = to.matched.flatMap((record) => record.meta?.permissionKeys || [])
  return [...new Set(permissionKeys)]
}

const hasRouteAccess = (to, role) => {
  const requiredPermissionKeys = getRequiredPermissionKeys(to)
  const runtimePermissionDecision = hasMenuAccessForRole(role, requiredPermissionKeys)
  if (runtimePermissionDecision !== null) {
    return runtimePermissionDecision
  }

  const requiredRoles = getRequiredRoles(to)
  return requiredRoles.length === 0 || requiredRoles.includes(role)
}

const DEFAULT_AUTHENTICATED_HOME = '/dashboard'

const getStoredUser = () => {
  const raw = localStorage.getItem('user') || sessionStorage.getItem('user')
  if (!raw) return null

  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

const getNormalizedRole = (userStore) => {
  const role = userStore.currentUser?.role || getStoredUser()?.role || ''
  return String(role).toLowerCase()
}

const getRequiredRoles = (to) => {
  const roles = to.matched.flatMap((record) => record.meta?.roles || [])
  return [...new Set(roles)]
}

const hasRouteAccess = (to, role) => {
  const requiredRoles = getRequiredRoles(to)
  return requiredRoles.length === 0 || requiredRoles.includes(role)
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard/Workbench.vue'),
        meta: { title: '工作台', permissionKeys: ['dashboard'] }
      },
      {
        path: 'bidding',
        name: 'Bidding',
        component: () => import('@/views/Bidding/List.vue'),
        meta: { title: '标讯中心', permissionKeys: ['bidding', 'bidding-list'] }
      },
      {
        path: 'bidding/customer-opportunities',
        name: 'CustomerOpportunityCenter',
        component: () => import('@/views/Bidding/CustomerOpportunityCenter.vue'),
        meta: { title: '客户商机中心', icon: 'bidding', permissionKeys: ['bidding'] }
      },
      {
        path: 'bidding/:id',
        name: 'BiddingDetail',
        component: () => import('@/views/Bidding/Detail.vue'),
        meta: { title: '标讯详情', permissionKeys: ['bidding', 'bidding-detail'] }
      },
      {
        path: 'bidding/ai-analysis/:id',
        name: 'BiddingAIAnalysis',
        component: () => import('@/views/Bidding/AIAnalysis.vue'),
        meta: { title: 'AI分析', requiresAuth: true, permissionKeys: ['bidding'] }
      },
      {
        path: 'ai-center',
        name: 'AICenter',
        component: () => import('@/views/AI/Center.vue'),
        meta: { title: 'AI 智能中心', icon: 'MagicStick' }
      },
      {
        path: 'project',
        name: 'ProjectList',
        component: () => import('@/views/Project/List.vue'),
        meta: { title: '投标项目', permissionKeys: ['project', 'project-list'] }
      },
      {
        path: 'project/create',
        name: 'ProjectCreate',
        component: () => import('@/views/Project/Create.vue'),
        meta: { title: '创建项目', permissionKeys: ['project', 'project-create'] }
      },
      {
        path: 'project/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/Project/Detail.vue'),
        meta: { title: '项目详情', permissionKeys: ['project', 'project-detail'] }
      },
      {
        path: 'knowledge/qualification',
        name: 'Qualification',
        component: () => import('@/views/Knowledge/Qualification.vue'),
        meta: { title: '资质库', permissionKeys: ['knowledge', 'knowledge-qualification'] }
      },
      {
        path: 'knowledge/case',
        name: 'Case',
        component: () => import('@/views/Knowledge/Case.vue'),
        meta: { title: '案例库', permissionKeys: ['knowledge', 'knowledge-case'] }
      },
      {
        path: 'knowledge/case/detail',
        name: 'CaseDetail',
        component: () => import('@/views/Knowledge/CaseDetail.vue'),
        meta: { title: '案例详情', permissionKeys: ['knowledge', 'knowledge-case'] }
      },
      {
        path: 'knowledge/template',
        name: 'Template',
        component: () => import('@/views/Knowledge/Template.vue'),
        meta: { title: '模板库', permissionKeys: ['knowledge', 'knowledge-template'] }
      },
      {
        path: 'resource/expense',
        name: 'Expense',
        component: () => import('@/views/Resource/Expense.vue'),
        meta: { title: '费用管理', permissionKeys: ['resource', 'resource-expense'] }
      },
      {
        path: 'resource/account',
        name: 'Account',
        component: () => import('@/views/Resource/Account.vue'),
        meta: { title: '账户管理', permissionKeys: ['resource', 'resource-account'] }
      },
      {
        path: 'resource/bid-result',
        name: 'BidResult',
        component: () => import('@/views/Resource/BidResult.vue'),
        meta: { title: '投标结果闭环', permissionKeys: ['resource'] }
      },
      // BAR 投标资产台账
      {
        path: 'resource/bar',
        name: 'BAR',
        component: () => import('@/views/Resource/BAR/CheckPanel.vue'),
        meta: { title: '可投标能力检查', permissionKeys: ['resource', 'resource-bar'] }
      },
      {
        path: 'resource/bar/sites',
        name: 'BAR_SiteList',
        component: () => import('@/views/Resource/BAR/SiteList.vue'),
        meta: { title: '站点台账', permissionKeys: ['resource', 'resource-bar'] }
      },
      {
        path: 'resource/bar/site/:id',
        name: 'BAR_SiteDetail',
        component: () => import('@/views/Resource/BAR/SiteDetail.vue'),
        meta: { title: '站点详情', permissionKeys: ['resource', 'resource-bar'] }
      },
      {
        path: 'resource/bar/sop/:siteId',
        name: 'BAR_SOPDetail',
        component: () => import('@/views/Resource/BAR/SOPDetail.vue'),
        meta: { title: '找回SOP', permissionKeys: ['resource', 'resource-bar'] }
      },
      {
        path: 'analytics/dashboard',
        name: 'AnalyticsDashboard',
        component: () => import('@/views/Analytics/Dashboard.vue'),
        meta: { title: '数据分析', roles: ['admin', 'manager'], permissionKeys: ['analytics', 'analytics-dashboard'] }
      },
      {
        path: 'analytics',
        redirect: '/analytics/dashboard'
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/System/Settings.vue'),
        meta: { title: '系统设置', roles: ['admin'], permissionKeys: ['settings'] }
      },
      {
        path: 'document/editor/:id',
        name: 'DocumentEditor',
        component: () => import('@/views/Document/Editor.vue'),
        meta: { title: '标书编辑器', requiresAuth: true, permissionKeys: ['project', 'project-detail'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  let hasAuthState =
    userStore.token ||
    localStorage.getItem('token') ||
    sessionStorage.getItem('token') ||
    userStore.currentUser ||
    localStorage.getItem('user') ||
    sessionStorage.getItem('user')

  if (hasAuthState && !userStore.currentUser && userStore.token) {
    await userStore.restoreSession()
    hasAuthState =
      userStore.token ||
      localStorage.getItem('token') ||
      sessionStorage.getItem('token') ||
      userStore.currentUser ||
      localStorage.getItem('user') ||
      sessionStorage.getItem('user')
  }

  if (hasAuthState && userStore.currentUser && !userStore.permissionProfileLoaded) {
    await userStore.refreshPermissionProfile()
  }

  if (to.meta.requiresAuth && !hasAuthState) {
    next('/login')
  } else if (to.path === '/login' && hasAuthState) {
    next(DEFAULT_AUTHENTICATED_HOME)
  } else if (hasAuthState && !hasRouteAccess(to, getNormalizedRole(userStore))) {
    next(DEFAULT_AUTHENTICATED_HOME)
  } else {
    next()
  }
})

export default router
