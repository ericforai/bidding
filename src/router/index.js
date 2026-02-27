import { createRouter, createWebHistory } from 'vue-router'

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
        path: 'knowledge/qualification',
        name: 'Qualification',
        component: () => import('@/views/Knowledge/Qualification.vue'),
        meta: { title: '资质库' }
      },
      {
        path: 'knowledge/case',
        name: 'Case',
        component: () => import('@/views/Knowledge/Case.vue'),
        meta: { title: '案例库' }
      },
      {
        path: 'knowledge/template',
        name: 'Template',
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
        path: 'analytics',
        name: 'Analytics',
        component: () => import('@/views/Analytics/Dashboard.vue'),
        meta: { title: '数据分析', roles: ['admin', 'manager'] }
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
router.beforeEach((to, from, next) => {
  // 从 localStorage 获取登录状态
  const hasToken = localStorage.getItem('user') || sessionStorage.getItem('user')

  if (to.meta.requiresAuth && !hasToken) {
    next('/login')
  } else if (to.path === '/login' && hasToken) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
