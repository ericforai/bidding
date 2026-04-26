<template>
  <!-- 跳过导航链接 - 无障碍功能 -->
  <a href="#main-content" class="skip-to-content">跳转到主内容</a>

  <el-container class="main-layout" :class="{ 'mobile': isMobile }">
    <!-- PC端侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside" v-if="!isMobile">
      <Sidebar :collapse="isCollapse" />
    </el-aside>

    <!-- 移动端侧边栏抽屉 -->
    <Sidebar v-model="mobileDrawerVisible" v-if="isMobile" />

    <el-container>
      <el-header height="56px" class="layout-header">
        <Header
          @toggle-collapse="toggleCollapse"
          @mobile-menu-click="mobileDrawerVisible = true"
        />
      </el-header>

      <el-main id="main-content" class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'

const isCollapse = ref(false)
const isMobile = ref(false)
const mobileDrawerVisible = ref(false)

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
/* 跳过导航链接样式 - 无障碍功能 */
.skip-to-content {
  position: absolute;
  top: -40px;
  left: 0;
  background: #1890ff;
  color: #ffffff;
  padding: 8px 16px;
  text-decoration: none;
  z-index: 9999;
  font-weight: 500;
  transition: top 0.3s ease;
}

.skip-to-content:focus {
  top: 0;
  outline: 2px solid #1890ff;
  outline-offset: 2px;
}

.main-layout {
  height: 100vh;
}

.layout-aside {
  background: var(--sidebar-bg, #FFFFFF);
  transition: width 0.28s ease;
  overflow: hidden;
}

.layout-header {
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.layout-main {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}

/* 页面切换动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

/* 移动端适配样式 */
@media (max-width: 768px) {
  .layout-main {
    padding: 12px;
  }

  .main-layout.mobile .layout-aside {
    display: none;
  }
}

/* 触摸优化 */
@media (hover: none) and (pointer: coarse) {
  .el-button {
    min-height: 44px;
  }

  .el-input__inner {
    min-height: 44px;
  }
}
</style>
