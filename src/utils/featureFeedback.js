import { ElMessage } from 'element-plus'
import { getFeaturePlaceholder, isFeatureUnavailableResponse } from '@/api'

export function resolveFeaturePlaceholder(response, fallback = {}) {
  if (!isFeatureUnavailableResponse(response)) return null
  return getFeaturePlaceholder(response, fallback)
}

export function notifyFeatureUnavailable(response, options = {}) {
  const {
    fallback = {},
    level = 'info',
  } = options

  const placeholder = resolveFeaturePlaceholder(response, fallback)
  if (!placeholder) return null

  const messageType = typeof ElMessage[level] === 'function' ? level : 'info'
  ElMessage[messageType](placeholder.message)
  return placeholder
}
