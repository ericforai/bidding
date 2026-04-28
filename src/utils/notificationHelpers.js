// Input: notification item type / createdAt string / sourceEntity
// Output: shared icon map, route map, formatter, and safe navigation target
// Pos: src/utils/ - Shared notification UI helpers
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import {
  Bell,
  Warning,
  Document,
  ChatDotRound,
  InfoFilled
} from '@element-plus/icons-vue'

export const NOTIFICATION_ICON_BY_TYPE = {
  DEADLINE: Warning,
  DOCUMENT_CHANGE: Document,
  MENTION: ChatDotRound,
  SYSTEM: InfoFilled,
  DEFAULT: Bell
}

export const NOTIFICATION_ENTITY_ROUTE_MAP = {
  PROJECT: '/project/',
  BIDDING: '/bidding/',
  TENDER: '/bidding/',
  DOCUMENT: '/document/editor/'
}

export const NOTIFICATION_TYPE_LABELS = {
  INFO: '通知',
  SYSTEM: '系统',
  MENTION: '提及',
  APPROVAL: '审批',
  DEADLINE: '截止',
  TASK_UPDATE: '任务',
  DOCUMENT_CHANGE: '文档'
}

export const getNotificationIcon = (type) =>
  NOTIFICATION_ICON_BY_TYPE[type] || NOTIFICATION_ICON_BY_TYPE.DEFAULT

export const getNotificationTypeLabel = (type) =>
  NOTIFICATION_TYPE_LABELS[type] || type

export const formatNotificationTime = (dateStr) => {
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

export const resolveNotificationRoute = (item) => {
  const prefix = NOTIFICATION_ENTITY_ROUTE_MAP[item?.sourceEntityType]
  if (!prefix || item?.sourceEntityId == null) return null
  const safeId = Number(item.sourceEntityId)
  if (!Number.isFinite(safeId) || safeId <= 0) return null
  return `${prefix}${safeId}`
}
