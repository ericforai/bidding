// Input: API config, HTTP client, feature availability helpers, business API modules
// Output: @/api public exports and lazy module accessors
// Pos: src/api/ - Frontend data access public entry
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * API 统一导出
 * 支持双模式切换: Mock 数据 / 真实后端 API
 *
 * 使用方式:
 * import { authApi, projectsApi, tendersApi } from '@/api'
 *
 * 环境变量控制模式:
 * - VITE_API_MODE=mock    使用 Mock 数据
 * - VITE_API_MODE=api     使用真实后端 API
 * - VITE_API_BASE_URL     后端 API 地址 (默认: http://localhost:8080)
 */

// 导出配置
export { API_CONFIG, isMockMode, isCommercialMode, getApiUrl } from './config.js'
export { buildFeatureUnavailableResponse, getFeaturePlaceholder, isFeatureUnavailableResponse } from './featureAvailability.js'

// 导出 HTTP 客户端
export { default as httpClient } from './client.js'

// 导出各模块 API
export { authApi } from './modules/auth.js'
export { tendersApi } from './modules/tenders.js'
export { projectsApi } from './modules/projects.js'
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

// 统一导出对象 (方便解构使用)
export default {
  // 认证
  auth: () => import('./modules/auth.js').then(m => m.authApi),

  // 标讯
  tenders: () => import('./modules/tenders.js').then(m => m.tendersApi),

  // 项目
  projects: () => import('./modules/projects.js').then(m => m.projectsApi),

  // 知识库
  knowledge: () => import('./modules/knowledge.js').then(m => m.default),

  // 费用
  fees: () => import('./modules/fees.js').then(m => m.feesApi),

  // AI 分析
  ai: () => import('./modules/ai.js').then(m => m.default),

  // 资源
  resources: () => import('./modules/resources.js').then(m => m.default),

  // 协作
  collaboration: () => import('./modules/collaboration.js').then(m => m.default),

  // 看板
  dashboard: () => import('./modules/dashboard.js').then(m => m.default),

  // 审批
  approval: () => import('./modules/approval.js').then(m => m.approvalApi),

  // 导出
  export: () => import('./modules/export.js').then(m => m.exportApi),

  // 审计
  audit: () => import('./modules/audit.js').then(m => m.default),

  // 系统设置
  settings: () => import('./modules/settings.js').then(m => m.settingsApi)
}
