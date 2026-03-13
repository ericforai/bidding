export function buildFeatureUnavailableResponse({
  feature,
  title,
  message,
  hint = '',
  scope = 'section',
  data = [],
}) {
  return {
    success: false,
    featureUnavailable: true,
    message,
    data,
    placeholder: {
      feature,
      title: title || '功能暂未接入',
      message,
      hint,
      scope,
    },
  }
}

export function isFeatureUnavailableResponse(response) {
  return Boolean(response?.featureUnavailable || response?.placeholder?.feature)
}

export function getFeaturePlaceholder(response, fallback = {}) {
  if (!isFeatureUnavailableResponse(response)) return null

  return {
    feature: response?.placeholder?.feature || fallback.feature || 'unknown',
    title: response?.placeholder?.title || fallback.title || '功能暂未接入',
    message: response?.placeholder?.message || response?.message || fallback.message || '后端暂未提供该能力',
    hint: response?.placeholder?.hint || fallback.hint || '',
    scope: response?.placeholder?.scope || fallback.scope || 'section',
  }
}
