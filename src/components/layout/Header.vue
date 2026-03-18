<template>
  <div class="header-container">
    <div class="header-left">
      <!-- PC端折叠按钮 -->
      <el-icon class="collapse-icon" @click="handleToggle" v-if="!isMobile">
        <Expand v-if="collapse" />
        <Fold v-else />
      </el-icon>
      <!-- 移动端菜单按钮 -->
      <el-icon class="mobile-menu-icon" @click="handleMobileMenuClick" v-else>
        <Menu />
      </el-icon>
      <div class="logo">
        <span class="logo-icon">西域MRO</span>
        <span class="logo-text">投标管理平台</span>
      </div>
    </div>

    <div class="header-center" v-if="!isMobile">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索标讯、项目、知识..."
        class="search-input"
        :prefix-icon="Search"
        clearable
        @keyup.enter="handleSearch"
      />
    </div>

    <!-- 移动端搜索按钮 -->
    <div class="header-center-mobile" v-else>
      <el-icon class="mobile-search-icon" @click="showMobileSearch = true">
        <Search />
      </el-icon>
    </div>

    <div class="header-right">
      <el-tooltip content="通知" placement="bottom">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
          <el-icon class="header-icon" @click="handleNotification">
            <Bell />
          </el-icon>
        </el-badge>
      </el-tooltip>

      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <span class="user-avatar">{{ userAvatar }}</span>
          <span class="user-name" v-if="!isMobile">{{ userName }}</span>
          <el-icon class="dropdown-icon">
            <ArrowDown />
          </el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled class="user-info-dropdown">
              <div class="dropdown-user-detail">
                <span class="dropdown-avatar">{{ userAvatar }}</span>
                <div class="dropdown-user-text">
                  <div class="dropdown-user-name">{{ userName }}</div>
                  <div class="dropdown-user-role">{{ userRoleText }}</div>
                </div>
              </div>
            </el-dropdown-item>
            <el-dropdown-item divided command="switchRole">
              <el-icon><Refresh /></el-icon>
              切换角色
            </el-dropdown-item>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人中心
            </el-dropdown-item>
            <el-dropdown-item v-if="canAccessSettings" command="settings">
              <el-icon><Setting /></el-icon>
              系统设置
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 切换角色对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      title="切换角色"
      :width="isMobile ? '90%' : '440px'"
      class="role-switch-dialog"
      :close-on-click-modal="false"
    >
      <div class="role-dialog-content">
        <p class="role-dialog-hint">选择要切换的角色身份</p>
        <div class="role-selector">
          <div
            v-for="user in allUsers"
            :key="user.id"
            :class="['role-option-card', { 'is-selected': selectedUserId === user.id }]"
            @click="selectedUserId = user.id"
          >
            <div class="role-radio-indicator">
              <div class="radio-inner"></div>
            </div>
            <span class="role-avatar">{{ user.avatar }}</span>
            <div class="role-info">
              <div class="role-name">{{ user.name }}</div>
              <div class="role-detail">{{ user.dept }} · {{ roleTextMap[user.role] }}</div>
            </div>
            <div class="role-check-icon" v-if="selectedUserId === user.id">
              <el-icon><Check /></el-icon>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="role-dialog-footer">
          <el-button class="btn-cancel" @click="roleDialogVisible = false">
            <el-icon><Close /></el-icon>
            取消
          </el-button>
          <el-button class="btn-confirm" type="primary" @click="confirmSwitchRole" :disabled="!selectedUserId">
            <el-icon><Check /></el-icon>
            确认切换
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 移动端搜索弹窗 -->
    <el-dialog
      v-model="showMobileSearch"
      title="搜索"
      :width="isMobile ? '90%' : '500px'"
      class="mobile-search-dialog"
    >
      <el-input
        v-model="searchKeyword"
        placeholder="搜索标讯、项目、知识..."
        size="large"
        clearable
        @keyup.enter="handleMobileSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <template #footer>
        <el-button @click="showMobileSearch = false">取消</el-button>
        <el-button type="primary" @click="handleMobileSearch">搜索</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Bell, ArrowDown, User, Setting,
  SwitchButton, Expand, Fold, Refresh, Menu, Check, Close
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  collapse: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggleCollapse', 'mobileMenuClick'])

const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')
const unreadCount = ref(3)
const roleDialogVisible = ref(false)
const selectedUserId = ref('')
const showMobileSearch = ref(false)

// 移动端检测
const isMobile = ref(false)

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const roleTextMap = {
  admin: '管理员',
  manager: '经理',
  sales: '销售人员',
  staff: '员工',
  guest: '游客'
}

const userAvatar = computed(() => {
  const name = userStore.currentUser?.name || '游客'
  return name.charAt(0).toUpperCase()
})
const userName = computed(() => userStore.currentUser?.name || '游客')
const userRoleText = computed(() => roleTextMap[userStore.userRole] || '游客')
const allUsers = computed(() => userStore.users || [])
const canAccessSettings = computed(() => userStore.userRole === 'admin')

const handleToggle = () => {
  emit('toggleCollapse')
}

const handleMobileMenuClick = () => {
  emit('mobileMenuClick')
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    ElMessage.info(`搜索: ${searchKeyword.value}`)
    // TODO: 实现全局搜索功能
  }
}

const handleMobileSearch = () => {
  if (searchKeyword.value.trim()) {
    ElMessage.info(`搜索: ${searchKeyword.value}`)
    showMobileSearch.value = false
    // TODO: 实现全局搜索功能
  }
}

const handleNotification = () => {
  ElMessage.info('通知中心')
  unreadCount.value = 0
}

const handleCommand = async (command) => {
  switch (command) {
    case 'switchRole':
      roleDialogVisible.value = true
      selectedUserId.value = userStore.currentUser?.id || ''
      break
    case 'profile':
      ElMessage.info('个人中心')
      break
    case 'settings':
      if (canAccessSettings.value) {
        router.push('/settings')
      } else {
        ElMessage.warning('当前角色无权访问系统设置')
      }
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        userStore.logout()
        router.push('/login')
        ElMessage.success('已退出登录')
      } catch {
        // 用户取消
      }
      break
  }
}

const confirmSwitchRole = () => {
  if (selectedUserId.value) {
    userStore.switchUser(selectedUserId.value)
    roleDialogVisible.value = false
    ElMessage.success(`已切换为: ${userStore.currentUser?.name}`)
  }
}
</script>

<style scoped>
/* ========== 主容器 ========== */
.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 var(--space-md);
  gap: var(--space-md);
}

/* ========== 左侧区域 ========== */
.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-shrink: 0;
  min-width: 0;
}

.collapse-icon,
.mobile-menu-icon {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md, 8px);
}

.collapse-icon:hover,
.mobile-menu-icon:hover {
  color: var(--brand-primary);
  background: var(--surface-hover, #f1f5f9);
}

.collapse-icon:active,
.mobile-menu-icon:active {
  transform: scale(0.95);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  white-space: nowrap;
}

.logo-icon {
  font-size: 18px;
  font-weight: 700;
  color: var(--brand-primary);
  letter-spacing: 1px;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

/* ========== 中间搜索区 ========== */
.header-center {
  flex: 1;
  max-width: 500px;
  margin: 0 var(--space-lg);
  min-width: 0;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-xl, 20px);
  background: var(--surface-hover, #f8fafc);
  box-shadow: none;
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border: 1.5px solid transparent;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.search-input :deep(.el-input__wrapper:hover) {
  background: #ffffff;
  border-color: var(--border-color, #e5e7eb);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: var(--brand-primary, #0369a1);
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1), 0 2px 8px rgba(0, 0, 0, 0.05);
}

.search-input :deep(.el-input__inner) {
  font-size: 14px;
  color: var(--text-primary, #1e293b);
}

.search-input :deep(.el-input__inner::placeholder) {
  color: var(--text-tertiary, #94a3b8);
}

.search-input :deep(.el-input__prefix) {
  color: var(--text-secondary, #64748b);
}

/* ========== 右侧用户区 ========== */
.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  flex-shrink: 0;
}

.notification-badge {
  cursor: pointer;
  display: flex;
  align-items: center;
  position: relative;
}

.header-icon {
  font-size: 20px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-full, 9999px);
}

.header-icon:hover {
  color: var(--brand-primary);
  background: var(--surface-hover, #f1f5f9);
}

.header-icon:active {
  transform: scale(0.95);
}

/* 徽标样式优化 */
:deep(.el-badge__content) {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border: 2px solid #ffffff;
  box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3);
  font-weight: 600;
  font-size: 11px;
  height: 18px;
  line-height: 18px;
  min-width: 18px;
  padding: 0 5px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 6px 12px 6px 6px;
  border-radius: var(--radius-xl, 16px);
  cursor: pointer;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  border: 1.5px solid transparent;
}

.user-info:hover {
  background: var(--surface-hover, #f8fafc);
  border-color: var(--border-color, #e5e7eb);
}

.user-info:active {
  transform: scale(0.98);
}

.user-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #0369a1, #0ea5e9);
  border-radius: var(--radius-full, 9999px);
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.2);
  transition: all 200ms ease;
}

.user-info:hover .user-avatar {
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.3);
  transform: scale(1.05);
}

.user-name {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-icon {
  font-size: 12px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* ========== 下拉菜单 ========== */
.user-info-dropdown {
  cursor: default;
  padding: var(--space-sm) 12px;
  background: transparent;
}

/* 下拉菜单项样式优化 */
:deep(.el-dropdown-menu) {
  padding: 8px;
  border-radius: 12px;
  border: 1px solid var(--border-color, #f1f5f9);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

:deep(.el-dropdown-menu__item) {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary, #1e293b);
  transition: all 150ms ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-dropdown-menu__item:hover) {
  background: var(--surface-hover, #f1f5f9);
  color: var(--brand-primary, #0369a1);
}

:deep(.el-dropdown-menu__item.is-disabled) {
  background: transparent;
  cursor: default;
}

:deep(.el-dropdown-menu__item.is-disabled:hover) {
  background: transparent;
  color: var(--text-secondary, #64748b);
}

:deep(.el-dropdown-menu__item .el-icon) {
  font-size: 16px;
  color: currentColor;
}

/* 分割线样式 */
:deep(.el-dropdown-menu__item--divided::before) {
  height: 1px;
  margin: 0;
  background: var(--border-color, #f1f5f9);
}

.dropdown-user-detail {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dropdown-avatar {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #0066CC, #3388DD);
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.dropdown-user-text {
  flex: 1;
  min-width: 0;
}

.dropdown-user-name {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.dropdown-user-role {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

/* ========== 角色切换对话框 ========== */
.role-dialog-content {
  padding: var(--space-md) 0;
}

.role-dialog-hint {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: var(--space-md);
  padding-left: var(--space-xs);
}

.role-selector {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  width: 100%;
}

.role-option-card {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  border-radius: 10px;
  border: 1.5px solid var(--border-color, #e5e7eb);
  background: var(--surface-primary, #ffffff);
  cursor: pointer;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.role-option-card:hover {
  border-color: var(--brand-primary, #0369a1);
  background: var(--surface-hover, #f0f9ff);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.1);
}

.role-option-card:active {
  transform: translateY(0);
}

.role-option-card.is-selected {
  border-color: var(--brand-primary, #0369a1);
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1);
}

.role-radio-indicator {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid var(--border-color, #d1d5db);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 200ms ease;
  background: #ffffff;
}

.role-option-card:hover .role-radio-indicator {
  border-color: var(--brand-primary, #0369a1);
}

.role-option-card.is-selected .role-radio-indicator {
  border-color: var(--brand-primary, #0369a1);
  background: var(--brand-primary, #0369a1);
}

.radio-inner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ffffff;
  opacity: 0;
  transform: scale(0);
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.role-option-card.is-selected .radio-inner {
  opacity: 1;
  transform: scale(1);
}

.role-avatar {
  width: 40px;
  height: 40px;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #0369a1, #0ea5e9);
  border-radius: var(--radius-full, 9999px);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.2);
}

.role-info {
  flex: 1;
  min-width: 0;
}

.role-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
  line-height: 1.4;
}

.role-detail {
  font-size: 12px;
  color: var(--text-secondary, #64748b);
  margin-top: 2px;
  line-height: 1.3;
}

.role-check-icon {
  width: 20px;
  height: 20px;
  color: var(--brand-primary, #0369a1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  animation: checkSlideIn 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes checkSlideIn {
  from {
    opacity: 0;
    transform: scale(0.5) translateX(-8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateX(0);
  }
}

.role-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  padding-top: var(--space-sm);
  border-top: 1px solid var(--border-color, #f1f5f9);
  margin-top: var(--space-md);
}

.role-dialog-footer .el-button {
  min-width: 100px;
  height: 38px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-cancel {
  border: 1.5px solid var(--border-color, #e5e7eb);
  color: var(--text-secondary, #64748b);
  background: #ffffff;
}

.btn-cancel:hover {
  border-color: var(--text-secondary, #94a3b8);
  color: var(--text-primary, #1e293b);
  background: var(--surface-hover, #f8fafc);
}

.btn-confirm {
  background: linear-gradient(135deg, #0369a1, #0284c7);
  border: none;
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.25);
}

.btn-confirm:hover:not(:disabled) {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.35);
  transform: translateY(-1px);
}

.btn-confirm:active:not(:disabled) {
  transform: translateY(0);
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 对话框样式优化 */
.role-switch-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-color, #f1f5f9);
}

.role-switch-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}

.role-switch-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.role-switch-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 20px;
}

/* ========== 移动端搜索区 ========== */
.header-center-mobile {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.mobile-search-icon {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ========== 移动端响应式 ========== */
@media (max-width: 768px) {
  .header-container {
    padding: 0 var(--space-sm);
    gap: var(--space-sm);
  }

  .header-center {
    display: none;
  }

  .logo-text {
    display: none;
  }

  .user-name {
    display: none;
  }

  .header-right {
    gap: var(--space-md);
  }

  .user-avatar {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }

  .header-icon {
    font-size: 18px;
  }

  .user-info {
    padding: 0;
  }
}

/* ========== 移动端搜索对话框 ========== */
.mobile-search-dialog :deep(.el-dialog__body) {
  padding: 20px;
}
</style>
