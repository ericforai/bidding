/**
 * 导出功能 API
 * 支持 Excel 和 PDF 导出，包含文件下载处理
 */
import httpClient from '../client.js'
import { isMockMode } from '../config.js'
import { ElMessage } from 'element-plus'

/**
 * 导出类型枚举
 */
export const ExportType = {
  // 标讯模块
  TENDERS: 'tenders',
  TENDER_DETAIL: 'tender_detail',

  // 项目模块
  PROJECTS: 'projects',
  PROJECT_DETAIL: 'project_detail',
  PROJECT_TASKS: 'project_tasks',
  PROJECT_SCORE_DRAFTS: 'project_score_drafts',

  // 知识库模块
  QUALIFICATIONS: 'qualifications',
  CASES: 'cases',
  TEMPLATES: 'templates',

  // AI分析模块
  AI_SCORE_ANALYSIS: 'ai_score_analysis',
  AI_COMPETITION: 'ai_competition',
  AI_ROI: 'ai_roi',
  AI_COMPLIANCE: 'ai_compliance',

  // 数据看板
  DASHBOARD_OVERVIEW: 'dashboard_overview',
  DASHBOARD_DRILLDOWN: 'dashboard_drilldown',

  // 费用模块
  FEES: 'fees',
  FEE_REPORT: 'fee_report',

  // 资源模块
  PLATFORM_ACCOUNTS: 'platform_accounts',
  BAR_ASSETS: 'bar_assets',
}

/**
 * 导出格式枚举
 */
export const ExportFormat = {
  EXCEL: 'excel',
  PDF: 'pdf',
  CSV: 'csv',
}

/**
 * 导出状态枚举
 */
export const ExportStatus = {
  PENDING: 'pending',
  PROCESSING: 'processing',
  COMPLETED: 'completed',
  FAILED: 'failed',
}

/**
 * 触发文件下载
 * @param {Blob|string} data - Blob数据或URL
 * @param {string} filename - 文件名
 * @param {string} mimeType - MIME类型
 */
export function triggerDownload(data, filename, mimeType = 'application/octet-stream') {
  let url = data

  // 如果是 Blob，创建对象 URL
  if (data instanceof Blob) {
    url = URL.createObjectURL(data)
  }

  // 创建隐藏的下载链接
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = url
  link.download = filename

  // 添加到 DOM 并触发点击
  document.body.appendChild(link)
  link.click()

  // 清理
  if (data instanceof Blob) {
    // 延迟释放 URL 对象，确保下载开始
    setTimeout(() => URL.revokeObjectURL(url), 100)
  }
  document.body.removeChild(link)
}

/**
 * 从响应头获取文件名
 * @param {Headers} headers - 响应头
 * @returns {string} 文件名
 */
function extractFilenameFromHeaders(headers) {
  const contentDisposition = headers?.get('content-disposition') || ''
  const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)

  if (filenameMatch && filenameMatch[1]) {
    let filename = filenameMatch[1].replace(/['"]/g, '')
    // 处理 UTF-8 编码的文件名
    if (filename.startsWith('UTF-8')) {
      const utf8Match = filename.match(/UTF-8''(.+)/)
      if (utf8Match) {
        filename = decodeURIComponent(utf8Match[1])
      }
    }
    return filename
  }

  return null
}

/**
 * 生成默认文件名
 * @param {string} type - 导出类型
 * @param {string} format - 导出格式
 * @returns {string} 文件名
 */
function generateDefaultFilename(type, format) {
  const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  const typeLabels = {
    tenders: '标讯列表',
    tender_detail: '标讯详情',
    projects: '项目列表',
    project_detail: '项目详情',
    project_tasks: '项目任务',
    project_score_drafts: '评分草稿',
    qualifications: '资质库',
    cases: '案例库',
    templates: '模板库',
    ai_score_analysis: '评分分析',
    ai_competition: '竞争情报',
    ai_roi: 'ROI分析',
    ai_compliance: '合规检查',
    dashboard_overview: '数据概览',
    dashboard_drilldown: '数据明细',
    fees: '费用明细',
    fee_report: '费用报表',
    platform_accounts: '平台账户',
    bar_assets: 'BAR资产',
  }
  const label = typeLabels[type] || '导出数据'
  const ext = format === ExportFormat.PDF ? '.pdf' : format === ExportFormat.CSV ? '.csv' : '.xlsx'

  return `${label}_${timestamp}${ext}`
}

/**
 * 执行导出（立即下载模式）
 * @param {string} type - 导出类型
 * @param {Object} params - 导出参数
 * @param {string} format - 导出格式
 * @param {Function} onProgress - 进度回调
 * @returns {Promise<{success: boolean, filename?: string, error?: string}>}
 */
async function executeExport(type, params = {}, format = ExportFormat.EXCEL, onProgress = null) {
  if (isMockMode()) {
    // Mock 模式：模拟导出
    onProgress?.(30)
    await new Promise(resolve => setTimeout(resolve, 500))
    onProgress?.(60)
    await new Promise(resolve => setTimeout(resolve, 500))
    onProgress?.(100)

    // 创建模拟文件
    const mockContent = JSON.stringify({ type, params, exportedAt: new Date().toISOString() }, null, 2)
    const blob = new Blob([mockContent], { type: 'application/json' })
    const filename = generateDefaultFilename(type, format)

    triggerDownload(blob, filename)

    return {
      success: true,
      filename
    }
  }

  // API 模式
  try {
    onProgress?.(10)

    // 确定端点和格式
    const formatMap = {
      [ExportFormat.EXCEL]: 'xlsx',
      [ExportFormat.PDF]: 'pdf',
      [ExportFormat.CSV]: 'csv',
    }
    const fileFormat = formatMap[format] || 'xlsx'

    // 构建请求参数
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        if (Array.isArray(value)) {
          value.forEach(v => queryParams.append(key, String(v)))
        } else {
          queryParams.append(key, String(value))
        }
      }
    })
    queryParams.append('format', fileFormat)

    onProgress?.(30)

    // 发起请求
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const headers = {}
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const response = await fetch(`${httpClient.defaults.baseURL}/api/export/${type}?${queryParams.toString()}`, {
      method: 'GET',
      headers
    })

    onProgress?.(60)

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: '导出失败' }))
      throw new Error(errorData.message || `导出失败: ${response.status}`)
    }

    onProgress?.(80)

    // 获取文件名
    const filename = extractFilenameFromHeaders(response.headers) || generateDefaultFilename(type, format)

    // 获取 Blob 并触发下载
    const blob = await response.blob()
    triggerDownload(blob, filename)

    onProgress?.(100)

    return {
      success: true,
      filename
    }
  } catch (error) {
    console.error('Export error:', error)
    return {
      success: false,
      error: error.message || '导出失败，请稍后重试'
    }
  }
}

/**
 * 创建异步导出任务（适用于大数据量）
 * @param {string} type - 导出类型
 * @param {Object} params - 导出参数
 * @param {string} format - 导出格式
 * @returns {Promise<{success: boolean, taskId?: string, error?: string}>}
 */
async function createExportTask(type, params = {}, format = ExportFormat.EXCEL) {
  if (isMockMode()) {
    return {
      success: true,
      taskId: `TASK_${Date.now()}`,
      estimatedTime: 5
    }
  }

  try {
    const formatMap = {
      [ExportFormat.EXCEL]: 'xlsx',
      [ExportFormat.PDF]: 'pdf',
      [ExportFormat.CSV]: 'csv',
    }

    const response = await httpClient.post('/api/export/tasks', {
      type,
      format: formatMap[format] || 'xlsx',
      params
    })

    return {
      success: true,
      taskId: response?.data?.taskId,
      estimatedTime: response?.data?.estimatedTime
    }
  } catch (error) {
    return {
      success: false,
      error: error.message || '创建导出任务失败'
    }
  }
}

/**
 * 查询导出任务状态
 * @param {string} taskId - 任务ID
 * @returns {Promise<{success: boolean, status?: string, downloadUrl?: string, error?: string}>}
 */
async function getExportTaskStatus(taskId) {
  if (isMockMode()) {
    return {
      success: true,
      status: ExportStatus.COMPLETED,
      downloadUrl: '#',
      progress: 100
    }
  }

  try {
    const response = await httpClient.get(`/api/export/tasks/${taskId}`)

    return {
      success: true,
      status: response?.data?.status,
      downloadUrl: response?.data?.downloadUrl,
      progress: response?.data?.progress || 0,
      filename: response?.data?.filename
    }
  } catch (error) {
    return {
      success: false,
      error: error.message || '查询任务状态失败'
    }
  }
}

/**
 * 下载导出任务文件
 * @param {string} taskId - 任务ID
 * @param {string} filename - 文件名（可选）
 * @returns {Promise<{success: boolean, error?: string}>}
 */
async function downloadExportTask(taskId, filename = null) {
  if (isMockMode()) {
    ElMessage.success('模拟下载成功')
    return { success: true }
  }

  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const headers = {}
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const response = await fetch(`${httpClient.defaults.baseURL}/api/export/tasks/${taskId}/download`, {
      method: 'GET',
      headers
    })

    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }

    const actualFilename = filename || extractFilenameFromHeaders(response.headers) || `export_${taskId}.xlsx`
    const blob = await response.blob()
    triggerDownload(blob, actualFilename)

    return { success: true }
  } catch (error) {
    return {
      success: false,
      error: error.message || '下载失败'
    }
  }
}

/**
 * 导出 API 对象
 */
export const exportApi = {
  /**
   * 导出 Excel
   * @param {string} type - 导出类型
   * @param {Object} params - 导出参数
   * @param {Function} onProgress - 进度回调
   */
  async exportExcel(type, params, onProgress) {
    return executeExport(type, params, ExportFormat.EXCEL, onProgress)
  },

  /**
   * 导出 PDF
   * @param {string} type - 导出类型
   * @param {Object} params - 导出参数
   * @param {Function} onProgress - 进度回调
   */
  async exportPdf(type, params, onProgress) {
    return executeExport(type, params, ExportFormat.PDF, onProgress)
  },

  /**
   * 导出 CSV
   * @param {string} type - 导出类型
   * @param {Object} params - 导出参数
   * @param {Function} onProgress - 进度回调
   */
  async exportCsv(type, params, onProgress) {
    return executeExport(type, params, ExportFormat.CSV, onProgress)
  },

  /**
   * 创建异步导出任务
   * @param {string} type - 导出类型
   * @param {Object} params - 导出参数
   * @param {string} format - 导出格式
   */
  async createTask(type, params, format) {
    return createExportTask(type, params, format)
  },

  /**
   * 查询导出任务状态
   * @param {string} taskId - 任务ID
   */
  async getTaskStatus(taskId) {
    return getExportTaskStatus(taskId)
  },

  /**
   * 下载导出任务文件
   * @param {string} taskId - 任务ID
   * @param {string} filename - 文件名
   */
  async downloadTask(taskId, filename) {
    return downloadExportTask(taskId, filename)
  },

  /**
   * 便捷方法：导出标讯列表
   */
  async exportTenders(params = {}, onProgress) {
    return this.exportExcel(ExportType.TENDERS, params, onProgress)
  },

  /**
   * 便捷方法：导出项目列表
   */
  async exportProjects(params = {}, onProgress) {
    return this.exportExcel(ExportType.PROJECTS, params, onProgress)
  },

  /**
   * 便捷方法：导出资质库
   */
  async exportQualifications(params = {}, onProgress) {
    return this.exportExcel(ExportType.QUALIFICATIONS, params, onProgress)
  },

  /**
   * 便捷方法：导出案例库
   */
  async exportCases(params = {}, onProgress) {
    return this.exportExcel(ExportType.CASES, params, onProgress)
  },

  /**
   * 便捷方法：导出模板库
   */
  async exportTemplates(params = {}, onProgress) {
    return this.exportExcel(ExportType.TEMPLATES, params, onProgress)
  },

  /**
   * 便捷方法：导出AI评分分析
   */
  async exportAiScoreAnalysis(projectId, onProgress) {
    return this.exportPdf(ExportType.AI_SCORE_ANALYSIS, { projectId }, onProgress)
  },

  /**
   * 便捷方法：导出竞争情报
   */
  async exportCompetition(projectId, onProgress) {
    return this.exportPdf(ExportType.AI_COMPETITION, { projectId }, onProgress)
  },

  /**
   * 便捷方法：导出ROI分析
   */
  async exportRoi(projectId, onProgress) {
    return this.exportPdf(ExportType.AI_ROI, { projectId }, onProgress)
  },

  /**
   * 便捷方法：导出费用报表
   */
  async exportFeeReport(params = {}, onProgress) {
    return this.exportExcel(ExportType.FEE_REPORT, params, onProgress)
  },
}

export default exportApi
