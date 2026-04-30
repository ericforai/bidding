// Status / file-type / amount / rate formatters shared across dashboard widgets

export function formatAmount(amount) {
  if (amount >= 10000) {
    return (amount / 10000).toFixed(1) + '亿'
  }
  return amount + '万'
}

export function formatMetricAmount(amount) {
  const numeric = Number(amount || 0)
  return `${numeric.toLocaleString('zh-CN')}万`
}

export function formatMetricRate(rate) {
  return `${Number(rate || 0).toFixed(1)}%`
}

export function formatMetricDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function getStatusType(status) {
  return {
    bidding: 'warning',
    reviewing: 'info',
    won: 'success',
    lost: 'danger',
    pending: 'info'
  }[status] || 'info'
}

export function getStatusText(status) {
  return {
    bidding: '投标中',
    reviewing: '评审中',
    won: '已中标',
    lost: '未中标',
    pending: '待处理',
    tracking: '跟踪中',
    bidded: '已投标',
    abandoned: '已放弃'
  }[status] || status
}

export function getFileType(fileName) {
  const ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  return {
    '.docx': 'Word',
    '.doc': 'Word',
    '.xlsx': 'Excel',
    '.xls': 'Excel',
    '.pdf': 'PDF',
    '.pptx': 'PPT',
    '.ppt': 'PPT',
    '.zip': '压缩包',
    '.rar': '压缩包'
  }[ext] || '其他'
}

export function getFileTypeColor(fileName) {
  const ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  return {
    '.docx': 'primary',
    '.doc': 'primary',
    '.xlsx': 'success',
    '.xls': 'success',
    '.pdf': 'danger',
    '.pptx': 'warning',
    '.ppt': 'warning',
    '.zip': 'info',
    '.rar': 'info'
  }[ext] || 'info'
}

export function getMetricColorClass(key) {
  return { bids: 'blue', winRate: 'green', amount: 'orange', cost: 'red' }[key] || 'blue'
}

export function getTrendClass(direction) {
  if (direction === 'trend-neutral') return 'neutral'
  return direction === 'trend-up' ? 'positive' : 'negative'
}

export function formatMetricCell(column, value, row = {}, metricDrawerType = '') {
  if (value == null || value === '') return '-'

  if (column.type === 'amount') return formatMetricAmount(value)
  if (column.type === 'rate') return formatMetricRate(value)
  if (column.type === 'datetime') return formatMetricDateTime(value)

  if (column.type === 'status') {
    if (metricDrawerType === 'revenue' && column.key === 'status') {
      return getStatusText(String(value).toLowerCase())
    }
    if (metricDrawerType === 'projects' && column.key === 'status') {
      return {
        INITIATED: '已启动',
        PREPARING: '准备中',
        REVIEWING: '审核中',
        SEALING: '封装中',
        BIDDING: '投标中',
        ARCHIVED: '已归档'
      }[value] || value
    }
    if (metricDrawerType === 'win-rate' && column.key === 'outcome') {
      return { WON: '已中标', LOST: '未中标', IN_PROGRESS: '进行中' }[value] || value
    }
    if (metricDrawerType === 'team' && column.key === 'role') {
      return { ADMIN: '管理员', MANAGER: '经理', STAFF: '员工' }[value] || value
    }
  }

  return value
}

export function getMetricStatusTagType(value, type) {
  if (type === 'revenue') {
    return { PENDING: 'info', TRACKING: 'warning', BIDDED: 'success', ABANDONED: 'danger' }[value] || 'info'
  }
  if (type === 'projects') {
    return {
      INITIATED: 'info',
      PREPARING: 'warning',
      REVIEWING: 'primary',
      SEALING: 'warning',
      BIDDING: 'success',
      ARCHIVED: 'info'
    }[value] || 'info'
  }
  if (type === 'win-rate') {
    return { WON: 'success', LOST: 'danger', IN_PROGRESS: 'warning' }[value] || 'info'
  }
  if (type === 'team') {
    return { ADMIN: 'danger', MANAGER: 'primary', STAFF: 'success' }[value] || 'info'
  }
  return 'info'
}
