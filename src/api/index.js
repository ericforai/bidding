// Input: API config, HTTP client, feature availability helpers, business API modules
// Output: @/api public exports and lazy module accessors
// Pos: src/api/ - Frontend data access public entry
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * API 统一导出
 * 真实 API 为唯一数据源
 *
 * 使用方式:
 * import { authApi, projectsApi, tendersApi } from '@/api'
 *
 * 环境变量:
 * - VITE_API_BASE_URL     后端 API 地址 (默认: http://localhost:8080)
 */

export { API_CONFIG, isCommercialMode, getApiUrl } from './config.js'
export { buildFeatureUnavailableResponse, getFeaturePlaceholder, isFeatureUnavailableResponse } from './featureAvailability.js'

export { default as httpClient } from './client.js'

export { authApi } from './modules/auth.js'
export { tendersApi } from './modules/tenders.js'
export { batchTendersApi } from './modules/tenders/batch.js'
export { projectsApi } from './modules/projects.js'
export { default as qualificationsApi } from './modules/qualification.js'
export { templatesApi } from './modules/templates.js'
export { default as knowledgeApi } from './modules/knowledge.js'
export { feesApi } from './modules/fees.js'
export { default as aiApi } from './modules/ai.js'
export { default as resourcesApi } from './modules/resources.js'
export { default as collaborationApi } from './modules/collaboration.js'
export { dashboardApi } from './modules/dashboard.js'
export { approvalApi } from './modules/approval.js'
export { exportApi, ExportType, ExportFormat, ExportStatus } from './modules/export.js'
export { auditApi } from './modules/audit.js'
export { settingsApi } from './modules/settings.js'
export { projectGroupsApi } from './modules/projectGroups.js'
export { bidResultsApi } from './modules/bidResults.js'
export { workbenchApi } from './modules/workbench.js'

export default {
  auth: () => import('./modules/auth.js').then(m => m.authApi),
  tenders: () => import('./modules/tenders.js').then(m => m.tendersApi),
  tenderBatch: () => import('./modules/tenders/batch.js').then(m => m.batchTendersApi),
  projects: () => import('./modules/projects.js').then(m => m.projectsApi),
  qualifications: () => import('./modules/qualification.js').then(m => m.default),
  knowledge: () => import('./modules/knowledge.js').then(m => m.default),
  fees: () => import('./modules/fees.js').then(m => m.feesApi),
  ai: () => import('./modules/ai.js').then(m => m.default),
  resources: () => import('./modules/resources.js').then(m => m.default),
  collaboration: () => import('./modules/collaboration.js').then(m => m.default),
  dashboard: () => import('./modules/dashboard.js').then(m => m.default),
  approval: () => import('./modules/approval.js').then(m => m.approvalApi),
  export: () => import('./modules/export.js').then(m => m.exportApi),
  audit: () => import('./modules/audit.js').then(m => m.default),
  settings: () => import('./modules/settings.js').then(m => m.settingsApi),
  projectGroups: () => import('./modules/projectGroups.js').then(m => m.projectGroupsApi),
  workbench: () => import('./modules/workbench.js').then(m => m.workbenchApi)
}
