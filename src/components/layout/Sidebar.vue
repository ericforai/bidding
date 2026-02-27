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
        text-color="rgba(255, 255, 255, 0.65)"
        active-text-color="#409EFF"
        router
        @select="handleMenuSelect"
      >
        <!-- 单级菜单项 -->
        <el-menu-item
          v-for="item in singleMenus"
          :key="item.path"
          :index="item.path"
        >
          <CommonIcon :name="item.meta?.icon" size="md" />
          <template #title>{{ item.meta?.title }}</template>
        </el-menu-item>

        <!-- 多级菜单项 -->
        <el-sub-menu
          v-for="item in multiMenus"
          :key="item.path"
          :index="item.path"
        >
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
      text-color="rgba(255, 255, 255, 0.65)"
      active-text-color="#409EFF"
      router
    >
      <!-- 单级菜单项 -->
      <el-menu-item
        v-for="item in singleMenus"
        :key="item.path"
        :index="item.path"
      >
        <CommonIcon :name="item.meta?.icon" size="md" />
        <template #title>{{ item.meta?.title }}</template>
      </el-menu-item>

      <!-- 多级菜单项 -->
      <el-sub-menu
        v-for="item in multiMenus"
        :key="item.path"
        :index="item.path"
      >
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
    </el-menu>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import CommonIcon from '@/components/common/CommonIcon.vue'
import { useUserStore } from '@/stores/user'

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
const handleMenuSelect = () => {
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
const userStore = useUserStore()

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta?.activeMenu) {
    return meta.activeMenu
  }
  return path
})

// 所有菜单配置（使用业务图标名称，通过 iconMap 映射）
const menuConfig = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    meta: { title: '工作台', icon: 'workbench' }
  },
  {
    path: '/bidding',
    name: 'Bidding',
    meta: { title: '标讯中心', icon: 'bidding' }
  },
  {
    path: '/ai-center',
    name: 'AICenter',
    meta: { title: 'AI 智能中心', icon: 'ai-center' }
  },
  {
    path: '/project',
    name: 'Project',
    meta: { title: '投标项目', icon: 'project' },
    children: [
      {
        path: '/project',
        name: 'ProjectList',
        meta: { title: '项目列表' }
      },
      {
        path: '/project/create',
        name: 'ProjectCreate',
        meta: { title: '创建项目' }
      }
    ]
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    meta: { title: '知识库', icon: 'knowledge' },
    children: [
      {
        path: '/knowledge/qualification',
        name: 'Qualification',
        meta: { title: '资质库' }
      },
      {
        path: '/knowledge/case',
        name: 'Case',
        meta: { title: '案例库' }
      },
      {
        path: '/knowledge/template',
        name: 'Template',
        meta: { title: '模板库' }
      }
    ]
  },
  {
    path: '/resource',
    name: 'Resource',
    meta: { title: '资源管理', icon: 'resource' },
    children: [
      {
        path: '/resource/expense',
        name: 'Expense',
        meta: { title: '费用管理' }
      },
      {
        path: '/resource/account',
        name: 'Account',
        meta: { title: '账户管理' }
      }
    ]
  },
  {
    path: '/analytics',
    name: 'Analytics',
    meta: { title: '数据分析', icon: 'analytics', roles: ['admin', 'manager'] }
  },
  {
    path: '/settings',
    name: 'Settings',
    meta: { title: '系统设置', icon: 'settings', roles: ['admin'] }
  }
]

// 根据角色过滤菜单
const filteredMenus = computed(() => {
  const userRole = userStore.userRole

  return menuConfig
    .map(menu => {
      // 检查菜单权限
      if (menu.meta?.roles && !menu.meta.roles.includes(userRole)) {
        return null
      }

      // 处理子菜单
      if (menu.children) {
        const visibleChildren = menu.children.filter(child => {
          if (child.meta?.roles && !child.meta.roles.includes(userRole)) {
            return false
          }
          return true
        })

        return {
          ...menu,
          children: visibleChildren
        }
      }

      return menu
    })
    .filter(Boolean)
})

// 单级菜单
const singleMenus = computed(() => {
  return filteredMenus.value.filter(menu => !menu.children || menu.children.length === 0)
})

// 多级菜单
const multiMenus = computed(() => {
  return filteredMenus.value.filter(menu => menu.children && menu.children.length > 0)
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
:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.el-menu-item.is-active) {
  background: #409EFF !important;
  border-right: 3px solid #1890ff;
  color: #fff !important;
}

:deep(.el-sub-menu .el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.2) !important;
  color: #fff !important;
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
  color: rgba(255, 255, 255, 0.65);
  cursor: pointer;
  transition: color 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-icon:hover {
  color: #409EFF;
}

.close-icon :deep(.el-icon) {
  font-size: 20px;
}

/* ========== 移动端响应式 ========== */
@media (max-width: 768px) {
  .sidebar-container {
    display: none;
  }
}
</style>
