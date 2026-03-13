/**
 * 导出功能 Composable
 * 提供统一的导出操作处理，包括进度显示和错误处理
 */
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exportApi, ExportFormat } from '@/api'

/**
 * 导出状态管理
 */
export function useExport() {
  const exporting = ref(false)
  const exportProgress = ref(0)
  const currentTaskId = ref(null)

  /**
   * 显示导出进度对话框
   * @param {string} title - 对话框标题
   * @returns {Object} 包含 close 和 update 方法的对象
   */
  const showProgressDialog = (title = '导出中') => {
    return ElMessageBox.alert(
      `<div class="export-progress-container">
        <el-progress :percentage="${exportProgress.value}" :stroke-width="12" />
        <p style="margin-top: 12px; color: #606266;">正在准备导出文件，请稍候...</p>
      </div>`,
      title,
      {
        confirmButtonText: '后台运行',
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        dangerouslyUseHTMLString: true,
        customClass: 'export-progress-dialog'
      }
    )
  }

  /**
   * 执行导出操作
   * @param {Function} exportFn - 导出函数
   * @param {Object} params - 导出参数
   * @param {string} format - 导出格式
   * @param {string} successMsg - 成功提示
   * @returns {Promise<boolean>} 是否成功
   */
  const doExport = async (exportFn, params = {}, format = ExportFormat.EXCEL, successMsg = '导出成功') => {
    if (exporting.value) {
      ElMessage.warning('正在导出中，请稍候...')
      return false
    }

    exporting.value = true
    exportProgress.value = 0

    try {
      // 显示进度对话框
      const progressBox = showProgressDialog()

      // 进度回调
      const onProgress = (progress) => {
        exportProgress.value = progress
        // 更新对话框内容
        const dialog = document.querySelector('.export-progress-dialog .el-message-box__message')
        if (dialog) {
          dialog.innerHTML = `
            <div class="export-progress-container">
              <el-progress :percentage="${progress}" :stroke-width="12" />
              <p style="margin-top: 12px; color: #606266;">正在准备导出文件，请稍候...</p>
            </div>
          `
        }
      }

      // 执行导出
      const result = await exportFn(params, onProgress)

      // 关闭进度对话框
      progressBox.close()

      if (result.success) {
        ElMessage.success({
          message: successMsg,
          duration: 3000
        })
        return true
      } else {
        ElMessage.error({
          message: result.error || '导出失败',
          duration: 5000
        })
        return false
      }
    } catch (error) {
      console.error('Export error:', error)
      ElMessage.error({
        message: error.message || '导出失败，请稍后重试',
        duration: 5000
      })
      return false
    } finally {
      exporting.value = false
      exportProgress.value = 0
    }
  }

  /**
   * 导出 Excel
   */
  const exportExcel = async (type, params = {}, successMsg = 'Excel 导出成功') => {
    return doExport(
      (p, onProgress) => exportApi.exportExcel(type, p, onProgress),
      params,
      ExportFormat.EXCEL,
      successMsg
    )
  }

  /**
   * 导出 PDF
   */
  const exportPdf = async (type, params = {}, successMsg = 'PDF 导出成功') => {
    return doExport(
      (p, onProgress) => exportApi.exportPdf(type, p, onProgress),
      params,
      ExportFormat.PDF,
      successMsg
    )
  }

  /**
   * 导出 CSV
   */
  const exportCsv = async (type, params = {}, successMsg = 'CSV 导出成功') => {
    return doExport(
      (p, onProgress) => exportApi.exportCsv(type, p, onProgress),
      params,
      ExportFormat.CSV,
      successMsg
    )
  }

  /**
   * 创建异步导出任务
   * @param {string} type - 导出类型
   * @param {Object} params - 导出参数
   * @param {string} format - 导出格式
   * @returns {Promise<string|null>} 任务ID
   */
  const createExportTask = async (type, params = {}, format = ExportFormat.EXCEL) => {
    try {
      const result = await exportApi.createTask(type, params, format)
      if (result.success) {
        currentTaskId.value = result.taskId
        ElMessage.info(`导出任务已创建，预计需要 ${result.estimatedTime || 5} 秒`)
        return result.taskId
      } else {
        ElMessage.error(result.error || '创建导出任务失败')
        return null
      }
    } catch (error) {
      ElMessage.error(error.message || '创建导出任务失败')
      return null
    }
  }

  /**
   * 查询任务状态
   * @param {string} taskId - 任务ID
   * @returns {Promise<Object|null>} 任务状态
   */
  const getTaskStatus = async (taskId) => {
    try {
      const result = await exportApi.getTaskStatus(taskId)
      if (result.success) {
        return {
          status: result.status,
          downloadUrl: result.downloadUrl,
          progress: result.progress,
          filename: result.filename
        }
      }
      return null
    } catch (error) {
      console.error('Get task status error:', error)
      return null
    }
  }

  /**
   * 下载任务文件
   * @param {string} taskId - 任务ID
   * @param {string} filename - 文件名
   */
  const downloadTask = async (taskId, filename = null) => {
    try {
      const result = await exportApi.downloadTask(taskId, filename)
      if (result.success) {
        ElMessage.success('下载成功')
        return true
      } else {
        ElMessage.error(result.error || '下载失败')
        return false
      }
    } catch (error) {
      ElMessage.error(error.message || '下载失败')
      return false
    }
  }

  return {
    exporting,
    exportProgress,
    currentTaskId,
    exportExcel,
    exportPdf,
    exportCsv,
    createExportTask,
    getTaskStatus,
    downloadTask,
    doExport
  }
}

export default useExport
