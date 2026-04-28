<template>
  <div class="notification-inbox">
    <div class="inbox-header">
      <h2 class="inbox-title">通知中心</h2>
      <el-button
        v-if="store.unreadCount > 0"
        type="primary"
        plain
        size="small"
        @click="handleMarkAllRead"
      >
        全部已读
      </el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="系统通知" name="SYSTEM" />
      <el-tab-pane label="任务更新" name="TASK_UPDATE" />
      <el-tab-pane label="截止提醒" name="DEADLINE" />
    </el-tabs>

    <div v-if="store.loading" class="inbox-loading">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="store.notifications.length === 0" class="inbox-empty">
      <el-empty description="暂无通知" />
    </div>

    <div v-else class="inbox-list">
      <div
        v-for="item in store.notifications"
        :key="item.id"
        class="inbox-item"
        :class="{ 'inbox-item--unread': !item.read }"
        @click="handleClick(item)"
      >
        <div class="inbox-item-left">
          <div class="inbox-item-icon">
            <el-icon :size="18">
              <component :is="getIconByType(item.type)" />
            </el-icon>
          </div>
        </div>
        <div class="inbox-item-body">
          <div class="inbox-item-title">{{ item.title }}</div>
          <div v-if="item.body" class="inbox-item-desc">{{ item.body }}</div>
          <div class="inbox-item-meta">
            <el-tag v-if="item.type" size="small" type="info">{{ typeLabel(item.type) }}</el-tag>
            <span class="inbox-item-time">{{ formatTime(item.createdAt) }}</span>
          </div>
        </div>
        <div class="inbox-item-right">
          <div v-if="!item.read" class="inbox-item-dot" />
        </div>
      </div>
    </div>

    <div v-if="store.totalPages > 1" class="inbox-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="store.totalElements"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  Warning,
  Document,
  ChatDotRound,
  InfoFilled
} from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notifications'

const router = useRouter()
const store = useNotificationStore()

const activeTab = ref('all')
const currentPage = ref(1)
const pageSize = 20

const TYPE_LABELS = {
  INFO: '通知',
  SYSTEM: '系统',
  MENTION: '提及',
  APPROVAL: '审批',
  DEADLINE: '截止',
  TASK_UPDATE: '任务',
  DOCUMENT_CHANGE: '文档'
}

const ICON_BY_TYPE = {
  DEADLINE: Warning,
  DOCUMENT_CHANGE: Document,
  MENTION: ChatDotRound,
  SYSTEM: InfoFilled,
  DEFAULT: Bell
}

const ENTITY_ROUTE_MAP = {
  PROJECT: '/project/',
  BIDDING: '/bidding/',
  TENDER: '/bidding/',
  DOCUMENT: '/document/editor/'
}

const typeLabel = (type) => TYPE_LABELS[type] || type
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

const fetchData = () => {
  const params = { page: currentPage.value - 1, size: pageSize }
  if (activeTab.value !== 'all') {
    params.type = activeTab.value
  }
  store.fetchNotifications(params)
}

const handleTabChange = () => {
  currentPage.value = 1
  fetchData()
}

const handlePageChange = () => {
  fetchData()
}

const handleClick = async (item) => {
  if (!item.read) {
    await store.markAsRead({ userNotificationId: item.id, notificationId: item.notificationId })
  }
  const prefix = ENTITY_ROUTE_MAP[item.sourceEntityType]
  if (prefix && item.sourceEntityId) {
    router.push(`${prefix}${item.sourceEntityId}`)
  }
}

const handleMarkAllRead = async () => {
  await store.markAllAsRead()
}

onMounted(fetchData)
</script>

<style scoped>
.notification-inbox {
  max-width: 800px;
  margin: 0 auto;
}

.inbox-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.inbox-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
  margin: 0;
}

.inbox-loading,
.inbox-empty {
  padding: 40px 0;
}

.inbox-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--border-color, #f1f5f9);
  border-radius: 8px;
  overflow: hidden;
}

.inbox-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: #ffffff;
  cursor: pointer;
  transition: background 150ms ease;
}

.inbox-item:hover {
  background: var(--surface-hover, #f8fafc);
}

.inbox-item--unread {
  background: rgba(46, 118, 89, 0.03);
}

.inbox-item-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--surface-hover, #f1f5f9);
  color: var(--brand-xiyu-logo, #2E7659);
  flex-shrink: 0;
}

.inbox-item-body {
  flex: 1;
  min-width: 0;
}

.inbox-item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary, #1e293b);
  line-height: 1.4;
}

.inbox-item-desc {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary, #64748b);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.inbox-item-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.inbox-item-time {
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
}

.inbox-item-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding-top: 4px;
}

.inbox-item-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
}

.inbox-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
