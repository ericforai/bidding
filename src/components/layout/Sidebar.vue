<template>
  <!-- 移动端抽屉菜单 -->
  <el-drawer
    v-model="drawerVisible"
    direction="ltr"
    :size="260"
    :with-header="false"
    class="mobile-drawer"
    v-if="isMobile"
  >
    <div class="drawer-sidebar">
      <div class="sidebar-logo">
        <span class="logo-icon">西域MRO</span>
        <CommonIcon name="close" class="close-icon" @click="drawerVisible = false" />
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="false"
        class="sidebar-menu"
        background-color="#001529"
        text-color="rgba(255, 255, 255, 0.85)"
        active-text-color="#FFFFFF"
        router
        @select="handleMenuSelect"
      >
        <!-- 直接按配置顺序渲染菜单项 -->
        <template v-for="item in filteredMenus" :key="item.path">
          <!-- 单级菜单 -->
          <el-menu-item
            v-if="!item.children || item.children.length === 0"
            :index="item.path"
          >
            <CommonIcon :name="item.meta?.icon" size="md" />
            <template #title>{{ item.meta?.title }}</template>
          </el-menu-item>

          <!-- 多级菜单 -->
          <el-sub-menu v-else :index="item.path">
            <template #title>
              <CommonIcon :name="item.meta?.icon" size="md" />
              <span>{{ item.meta?.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
              class="sub-menu-item"
            >
              <template #title>{{ child.meta?.title }}</template>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </div>
  </el-drawer>

  <!-- PC端侧边栏 -->
  <div class="sidebar-container" v-else>
    <div class="sidebar-logo">
      <span class="logo-icon" v-if="!collapse">西域MRO</span>
      <span class="logo-icon-small" v-else>西域</span>
    </div>

    <el-menu
      :default-active="activeMenu"
      :collapse="collapse"
      :collapse-transition="false"
      class="sidebar-menu"
      background-color="#001529"
      text-color="rgba(255, 255, 255, 0.85)"
      active-text-color="#FFFFFF"
      router
      @select="handleMenuSelect"
    >
      <!-- 直接按配置顺序渲染菜单项 -->
      <template v-for="item in filteredMenus" :key="item.path">
        <!-- 单级菜单 -->
        <el-menu-item
          v-if="!item.children || item.children.length === 0"
          :index="item.path"
        >
          <CommonIcon :name="item.meta?.icon" size="md" />
          <template #title>{{ item.meta?.title }}</template>
        </el-menu-item>

        <!-- 多级菜单 -->
        <el-sub-menu v-else :index="item.path">
          <template #title>
            <CommonIcon :name="item.meta?.icon" size="md" />
            <span>{{ item.meta?.title }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.path"
            :index="child.path"
            class="sub-menu-item"
          >
            <template #title>{{ child.meta?.title }}</template>
          </el-menu-item>
        </el-sub-menu>
      </template>
    </el-menu>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CommonIcon from '@/components/common/CommonIcon.vue'
import { useUserStore } from '@/stores/user'
import { hasMenuAccessForRole } from '@/api/modules/settings'

const props = defineProps({
  collapse: {
    type: Boolean,
    default: false
  },
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

// 移动端检测
const isMobile = ref(false)
const drawerVisible = ref(false)

// 监听父组件传入的 modelValue
watch(() => props.modelValue, (val) => {
  drawerVisible.value = val
})

// 监听抽屉状态变化
watch(drawerVisible, (val) => {
  emit('update:modelValue', val)
})

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

// 菜单选择后关闭抽屉
const handleMenuSelect = (index) => {
  console.log('[Sidebar] Menu selected:', index)
  console.log('[Sidebar] Current route:', route.path)
  console.log('[Sidebar] Target route exists:', router.hasRoute('AICenter'))

  // 手动导航到 /ai-center
  if (index === '/ai-center' || index.includes('ai-center')) {
    console.log('[Sidebar] Navigating to /ai-center')
    router.push('/ai-center')
  }

  if (isMobile.value) {
    drawerVisible.value = false
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const hasRoleAccess = (roles) => !roles || roles.length === 0 || roles.includes(userStore.userRole)
const hasPermissionAccess = (permissionKeys) => {
  const decision = hasMenuAccessForRole(userStore.userRole, permissionKeys)
  return decision === null ? true : decision
}

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta?.activeMenu) {
    return meta.activeMenu
  }
  return path
})

// 所有菜单配置（使用业务图标名称，通过 iconMap 映射）
// 按照业务流程排序：发现商机→创建项目→知识支持→资源管理→AI辅助→数据分析→系统设置
const menuConfig = [
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
        path: '/resource/bid-result',
        name: 'BidResult',
        meta: { title: '结果闭环', permissionKeys: ['resource'] }
      }
    ]
  },
  {
    path: '/ai-center',
    name: 'AICenter',
    meta: { title: 'AI 智能中心', icon: 'ai-center' }
  },
  {
    path: '/analytics/dashboard',
    name: 'AnalyticsDashboard',
    meta: { title: '数据分析', icon: 'analytics', roles: ['admin', 'manager'], permissionKeys: ['analytics', 'analytics-dashboard'] }
  },
  {
    path: '/settings',
    name: 'Settings',
    meta: { title: '系统设置', icon: 'settings', roles: ['admin'], permissionKeys: ['settings'] }
  }
]

// 根据角色过滤菜单
const filteredMenus = computed(() => {
  return menuConfig
    .map(menu => {
      if (!hasRoleAccess(menu.meta?.roles) || !hasPermissionAccess(menu.meta?.permissionKeys)) {
        return null
      }

      if (menu.children) {
        const visibleChildren = menu.children.filter(
          child => hasRoleAccess(child.meta?.roles) && hasPermissionAccess(child.meta?.permissionKeys)
        )

        if (visibleChildren.length === 0) {
          return null
        }

        return {
          ...menu,
          children: visibleChildren
        }
      }

      return menu
    })
    .filter(Boolean)
})
</script>

<style scoped>
.sidebar-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #001529;
}

/* ========== Logo 区域 ========== */
.sidebar-logo {
  height: var(--header-height);
  min-height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 20px;
  font-weight: 700;
  color: #409EFF;
  letter-spacing: 2px;
  white-space: nowrap;
}

.logo-icon-small {
  font-size: 18px;
  font-weight: 700;
  color: #409EFF;
  white-space: nowrap;
}

/* ========== 菜单区域 ========== */
.sidebar-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: var(--sidebar-width);
}

/* 滚动条样式 */
.sidebar-menu::-webkit-scrollbar {
  width: 6px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* ========== 菜单项对齐优化 ========== */
/* 统一菜单项高度 - 使用 flexbox 确保内容居中 */
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: var(--menu-item-height);
  display: flex;
  align-items: center;
  padding-right: var(--space-md);
}

/* 图标和文字完美对齐 - 使用 inline-flex */
:deep(.el-menu-item .el-menu-tooltip__trigger),
:deep(.el-sub-menu__title .el-sub-menu__title-arrow),
:deep(.el-menu-item span),
:deep(.el-sub-menu__title span) {
  display: inline-flex;
  align-items: center;
}

/* 图标尺寸和对齐 */
:deep(.el-menu-item .el-icon),
:deep(.el-sub-menu__title .el-icon) {
  font-size: 18px;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* ========== 子菜单缩进统一 ========== */
:deep(.el-sub-menu .el-menu-item) {
  padding-left: var(--space-lg) !important; /* 24px */
  min-height: var(--menu-item-height);
  display: flex;
  align-items: center;
}

/* ========== 菜单交互状态 ========== */
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.el-menu-item:hover)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 0 2px 2px 0;
}

:deep(.el-menu-item:focus-visible) {
  outline: 2px solid rgba(64, 158, 255, 0.5);
  outline-offset: -2px;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, var(--brand-primary) 0%, rgba(64, 158, 255, 0.8) 100%) !important;
  border-right: 3px solid var(--brand-primary-hover);
  color: #FFFFFF !important;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

:deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: #fff;
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
}

:deep(.el-sub-menu .el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.2) !important;
  color: #fff !important;
}

:deep(.el-sub-menu .el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 2px;
  height: 16px;
  background: #fff;
  border-radius: 0 2px 2px 0;
}

/* Icon animation on hover */
:deep(.el-menu-item:hover .el-icon),
:deep(.el-sub-menu__title:hover .el-icon) {
  transform: scale(1.08);
}

:deep(.el-menu-item .el-icon),
:deep(.el-sub-menu__title .el-icon) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ========== 子菜单箭头动画 ========== */
:deep(.el-sub-menu__title-arrow) {
  transition: transform 300ms cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title .el-sub-menu__title-arrow) {
  transform: rotate(180deg);
}

/* ========== 子菜单展开动画 ========== */
:deep(.el-menu--inline) {
  animation: slideDown 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 折叠状态 ========== */
:deep(.el-menu--collapse .el-menu-item) {
  padding: 0;
  justify-content: center;
}

:deep(.el-menu--collapse .el-sub-menu__title) {
  padding: 0;
  justify-content: center;
}

:deep(.el-menu--collapse) .el-menu-item span,
:deep(.el-menu--collapse) .el-sub-menu__title span {
  display: none;
}

:deep(.el-menu--collapse) .el-menu-item .el-icon,
:deep(.el-menu--collapse) .el-sub-menu__title .el-icon {
  margin-right: 0;
}

/* ========== 移动端抽屉 ========== */
.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #001529;
}

.drawer-sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #001529;
}

.drawer-sidebar .sidebar-logo {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--space-md);
}

.close-icon {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
}

.close-icon:hover {
  color: #409EFF;
  background: rgba(64, 158, 255, 0.15);
}

.close-icon:active {
  transform: scale(0.95);
}

.close-icon :deep(.el-icon) {
  font-size: 20px;
}

/* ========== 移动端抽屉菜单触摸目标优化 ========== */
.mobile-drawer :deep(.el-menu-item),
.mobile-drawer :deep(.el-sub-menu__title) {
  min-height: 48px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
}

.mobile-drawer :deep(.el-sub-menu .el-menu-item) {
  min-height: 44px;
  padding: 10px 20px 10px 32px;
}

/* 移动端菜单项触摸反馈 */
.mobile-drawer :deep(.el-menu-item):active,
.mobile-drawer :deep(.el-sub-menu__title):active {
  background: rgba(64, 158, 255, 0.2) !important;
  transition: background-color 0.1s ease;
}

/* ========== 移动端响应式 ========== */
@media (max-width: 768px) {
  .sidebar-container {
    display: none;
  }
}
</style>
