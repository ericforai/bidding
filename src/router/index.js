import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { hasStoredUserHint } from '@/api/session.js'

const DEFAULT_AUTHENTICATED_HOME = '/dashboard'
const HIDDEN_API_ROUTES = new Set([
  'CustomerOpportunityCenter'
])

const getNormalizedRole = (userStore) => {
  const role = userStore.currentUser?.role || ''
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
        meta: { title: '工作台' }
      },
      {
        path: 'bidding',
        name: 'Bidding',
        component: () => import('@/views/Bidding/List.vue'),
        meta: { title: '标讯中心' }
      },
      {
        path: 'bidding/customer-opportunities',
        name: 'CustomerOpportunityCenter',
        component: () => import('@/views/Bidding/CustomerOpportunityCenter.vue'),
        meta: { title: '客户商机中心', icon: 'bidding' }
      },
      {
        path: 'bidding/:id',
        name: 'BiddingDetail',
        component: () => import('@/views/Bidding/Detail.vue'),
        meta: { title: '标讯详情' }
      },
      {
        path: 'bidding/ai-analysis/:id',
        name: 'BiddingAIAnalysis',
        component: () => import('@/views/Bidding/AIAnalysis.vue'),
        meta: { title: 'AI分析', requiresAuth: true }
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
        meta: { title: '投标项目' }
      },
      {
        path: 'project/create',
        name: 'ProjectCreate',
        component: () => import('@/views/Project/Create.vue'),
        meta: { title: '创建项目' }
      },
      {
        path: 'project/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/Project/Detail.vue'),
        meta: { title: '项目详情' }
      },
      {
        path: 'knowledge',
        redirect: '/knowledge/qualification'
      },
      {
        path: 'knowledge/qualification',
        name: 'Qualification',
        alias: 'knowledge/qualifications',
        component: () => import('@/views/Knowledge/Qualification.vue'),
        meta: { title: '资质库' }
      },
      {
        path: 'knowledge/case',
        name: 'Case',
        alias: 'knowledge/cases',
        component: () => import('@/views/Knowledge/Case.vue'),
        meta: { title: '案例库' }
      },
      {
        path: 'knowledge/case/detail',
        name: 'CaseDetail',
        component: () => import('@/views/Knowledge/CaseDetail.vue'),
        meta: { title: '案例详情' }
      },
      {
        path: 'knowledge/template',
        name: 'Template',
        alias: 'knowledge/templates',
        component: () => import('@/views/Knowledge/Template.vue'),
        meta: { title: '模板库' }
      },
      {
        path: 'resource/expense',
        name: 'Expense',
        component: () => import('@/views/Resource/Expense.vue'),
        meta: { title: '费用管理' }
      },
      {
        path: 'resource/account',
        name: 'Account',
        component: () => import('@/views/Resource/Account.vue'),
        meta: { title: '账户管理' }
      },
      {
        path: 'resource/bid-result',
        name: 'BidResult',
        component: () => import('@/views/Resource/BidResult.vue'),
        meta: { title: '投标结果闭环' }
      },
      // BAR 投标资产台账
      {
        path: 'resource/bar',
        name: 'BAR',
        component: () => import('@/views/Resource/BAR/CheckPanel.vue'),
        meta: { title: '可投标能力检查' }
      },
      {
        path: 'resource/bar/sites',
        name: 'BAR_SiteList',
        component: () => import('@/views/Resource/BAR/SiteList.vue'),
        meta: { title: '站点台账' }
      },
      {
        path: 'resource/bar/site/:id',
        name: 'BAR_SiteDetail',
        component: () => import('@/views/Resource/BAR/SiteDetail.vue'),
        meta: { title: '站点详情' }
      },
      {
        path: 'resource/bar/sop/:siteId',
        name: 'BAR_SOPDetail',
        component: () => import('@/views/Resource/BAR/SOPDetail.vue'),
        meta: { title: '找回SOP' }
      },
      {
        path: 'analytics/dashboard',
        name: 'AnalyticsDashboard',
        component: () => import('@/views/Analytics/Dashboard.vue'),
        meta: { title: '数据分析', roles: ['admin', 'manager'] }
      },
      {
        path: 'analytics',
        redirect: '/analytics/dashboard'
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/System/Settings.vue'),
        meta: { title: '系统设置', roles: ['admin'] }
      },
      {
        path: 'document/editor/:id',
        name: 'DocumentEditor',
        component: () => import('@/views/Document/Editor.vue'),
        meta: { title: '标书编辑器', requiresAuth: true }
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
  let hasAuthState = Boolean(userStore.currentUser && userStore.token)
  const shouldAttemptRestore = to.meta.requiresAuth || hasAuthState || (to.path === '/login' && hasStoredUserHint())

  if (!userStore.hasRestoredSession && shouldAttemptRestore) {
    await userStore.restoreSession()
    hasAuthState = Boolean(userStore.currentUser && userStore.token)
  }

  if (to.meta.requiresAuth && !hasAuthState) {
    next('/login')
  } else if (to.path === '/login' && hasAuthState) {
    next(DEFAULT_AUTHENTICATED_HOME)
  } else if (HIDDEN_API_ROUTES.has(String(to.name || ''))) {
    next(
      to.name === 'CustomerOpportunityCenter'
        ? '/bidding'
        : to.name === 'BiddingAIAnalysis'
            ? '/bidding'
            : '/knowledge/qualification'
    )
  } else if (hasAuthState && !hasRouteAccess(to, getNormalizedRole(userStore))) {
    next(DEFAULT_AUTHENTICATED_HOME)
  } else {
    next()
  }
})

export default router
