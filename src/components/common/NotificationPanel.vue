<template>
  <div class="notification-panel">
    <div class="notification-panel-header">
      <span class="notification-panel-title">通知</span>
      <el-button
        v-if="store.unreadCount > 0"
        link
        type="primary"
        size="small"
        @click="handleMarkAllRead"
      >
        全部已读
      </el-button>
    </div>

    <div class="notification-panel-body">
      <div v-if="store.loading" class="notification-panel-loading">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="store.notifications.length === 0" class="notification-panel-empty">
        <el-empty description="暂无通知" :image-size="60" />
      </div>
      <div v-else class="notification-list">
        <div
          v-for="item in store.notifications"
          :key="item.id"
          class="notification-item"
          :class="{ 'notification-item--unread': !item.read }"
          @click="handleClick(item)"
        >
          <div class="notification-item-icon">
            <el-icon :size="16">
              <component :is="getIconByType(item.type)" />
            </el-icon>
          </div>
          <div class="notification-item-content">
            <div class="notification-item-title">{{ item.title }}</div>
            <div class="notification-item-time">{{ formatTime(item.createdAt) }}</div>
          </div>
          <div v-if="!item.read" class="notification-item-dot" />
        </div>
      </div>
    </div>

    <div class="notification-panel-footer">
      <el-button link type="primary" @click="handleViewAll">查看全部</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  Warning,
  Document,
  ChatDotRound,
  InfoFilled
} from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notifications'

const emit = defineEmits(['close'])
const router = useRouter()
const store = useNotificationStore()

const ENTITY_ROUTE_MAP = {
  PROJECT: '/project/',
  BIDDING: '/bidding/',
  TENDER: '/bidding/',
  DOCUMENT: '/document/editor/'
}

const ICON_BY_TYPE = {
  DEADLINE: Warning,
  DOCUMENT_CHANGE: Document,
  MENTION: ChatDotRound,
  SYSTEM: InfoFilled,
  DEFAULT: Bell
}

const getIconByType = (type) => ICON_BY_TYPE[type] || ICON_BY_TYPE.DEFAULT

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return ''
  const diffMs = Date.now() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay === 1) return '昨天'
  if (diffDay < 7) return `${diffDay}天前`
  return date.toLocaleDateString('zh-CN')
}

const navigateToSource = (item) => {
  const prefix = ENTITY_ROUTE_MAP[item.sourceEntityType]
  if (prefix && item.sourceEntityId) {
    router.push(`${prefix}${item.sourceEntityId}`)
    emit('close')
  }
}

const handleClick = async (item) => {
  if (!item.read) {
    await store.markAsRead({ userNotificationId: item.id, notificationId: item.notificationId })
  }
  if (item.sourceEntityType) {
    navigateToSource(item)
  }
}

const handleMarkAllRead = async () => {
  await store.markAllAsRead()
}

const handleViewAll = () => {
  router.push('/inbox')
  emit('close')
}

onMounted(() => {
  store.fetchNotifications({ page: 0, size: 10 })
})
</script>

<style scoped>
.notification-panel {
  width: 360px;
  max-width: 100vw;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.notification-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color, #f1f5f9);
}

.notification-panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}

.notification-panel-body {
  max-height: 420px;
  overflow-y: auto;
}

.notification-panel-loading,
.notification-panel-empty {
  padding: 24px 16px;
}

.notification-list {
  display: flex;
  flex-direction: column;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-color, #f1f5f9);
  transition: background 150ms ease;
  position: relative;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: var(--surface-hover, #f8fafc);
}

.notification-item--unread {
  background: rgba(46, 118, 89, 0.04);
}

.notification-item-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--surface-hover, #f1f5f9);
  color: var(--brand-xiyu-logo, #2E7659);
}

.notification-item-content {
  flex: 1;
  min-width: 0;
}

.notification-item-title {
  font-size: 13px;
  color: var(--text-primary, #1e293b);
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.notification-item-time {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
}

.notification-item-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  margin-top: 8px;
}

.notification-panel-footer {
  padding: 8px 16px;
  border-top: 1px solid var(--border-color, #f1f5f9);
  text-align: center;
}
</style>
