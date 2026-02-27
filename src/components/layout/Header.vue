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
            <el-dropdown-item command="settings">
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
      :width="isMobile ? '90%' : '400px'"
    >
      <el-radio-group v-model="selectedUserId" class="role-selector">
        <el-radio
          v-for="user in allUsers"
          :key="user.id"
          :label="user.id"
          class="role-option"
        >
          <div class="role-option-content">
            <span class="role-avatar">{{ user.avatar }}</span>
            <div class="role-info">
              <div class="role-name">{{ user.name }}</div>
              <div class="role-detail">{{ user.dept }} · {{ roleTextMap[user.role] }}</div>
            </div>
          </div>
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSwitchRole">确认切换</el-button>
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
  SwitchButton, Expand, Fold, Refresh, Menu
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

const userAvatar = computed(() => userStore.currentUser?.avatar || '👤')
const userName = computed(() => userStore.currentUser?.name || '游客')
const userRoleText = computed(() => roleTextMap[userStore.userRole] || '游客')
const allUsers = computed(() => userStore.users || [])

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
      router.push('/settings')
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
  transition: color 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.collapse-icon:hover,
.mobile-menu-icon:hover {
  color: var(--brand-primary);
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
  border-radius: var(--radius-xl);
  background: var(--gray-50);
  box-shadow: none;
  display: flex;
  align-items: center;
}

.search-input :deep(.el-input__wrapper:hover),
.search-input :deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  box-shadow: 0 0 0 1px var(--brand-primary) inset;
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
}

.header-icon {
  font-size: 20px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon:hover {
  color: var(--brand-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xs) 12px var(--space-xs) var(--space-xs);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: background 0.3s;
  flex-shrink: 0;
}

.user-info:hover {
  background: var(--gray-50);
}

.user-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  background: #e6f7ff;
  border-radius: var(--radius-full);
  flex-shrink: 0;
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
  font-size: 20px;
  background: #e6f7ff;
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

/* ========== 角色选择器 ========== */
.role-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.role-option {
  width: 100%;
  margin-right: 0;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--gray-200);
  display: flex;
  align-items: center;
}

.role-option:hover {
  background: var(--gray-50);
}

.role-option.is-checked {
  background: #e6f7ff;
  border-color: var(--brand-primary);
}

.role-option-content {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.role-avatar {
  font-size: 24px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.role-info {
  flex: 1;
  min-width: 0;
}

.role-name {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.role-detail {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
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
    font-size: 14px;
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
