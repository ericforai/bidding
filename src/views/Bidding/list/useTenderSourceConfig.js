// Input: optional external sync API, search state, and browser storage
// Output: source config state plus safe save/sync actions
// Pos: src/views/Bidding/list/ - External tender source composable
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DEFAULT_FETCH_RESULT, DEFAULT_SOURCE_CONFIG } from './constants.js'
import {
  restoreSourceConfig,
  sanitizeSourceConfigForStorage,
  summarizeExternalSyncResult,
} from './helpers.js'

const STORAGE_KEY = 'tenderSourceConfig'
const API_NOT_READY_MESSAGE = '外部标讯聚合 API 尚未接入，暂不能同步'

export function useTenderSourceConfig({
  externalSyncApi = null,
  refreshTenderList,
  searchForm,
  canSyncExternalSource,
}) {
  const showSourceConfig = ref(false)
  const savingConfig = ref(false)
  const testingConnection = ref(false)
  const fetchingTenders = ref(false)
  const lastSyncTime = ref('暂未同步')
  const sourceConfig = ref({ ...DEFAULT_SOURCE_CONFIG })
  const fetchResult = ref({ ...DEFAULT_FETCH_RESULT })

  const loadSavedConfig = () => {
    if (typeof localStorage === 'undefined') {
      return
    }
    const restored = restoreSourceConfig(
      localStorage.getItem(STORAGE_KEY),
      (safeValue) => localStorage.setItem(STORAGE_KEY, safeValue),
    )
    sourceConfig.value = { ...sourceConfig.value, ...restored, apiKey: '' }
  }

  const saveSourceConfig = async () => {
    if (sourceConfig.value.platforms.length === 0) {
      ElMessage.warning('请至少选择一个标讯源平台')
      return false
    }

    savingConfig.value = true
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify(sanitizeSourceConfigForStorage(sourceConfig.value)),
        )
      }
      ElMessage.success('标讯源配置已保存')
      showSourceConfig.value = false
      return true
    } catch {
      ElMessage.error('保存失败，请重试')
      return false
    } finally {
      savingConfig.value = false
    }
  }

  const testConnection = async () => {
    if (sourceConfig.value.platforms.length === 0) {
      ElMessage.warning('请先选择标讯源平台')
      return
    }
    testingConnection.value = true
    try {
      if (typeof externalSyncApi !== 'function') {
        ElMessage.warning('外部标讯聚合 API 尚未接入，无法测试连接')
        return
      }
      await externalSyncApi({ keyword: searchForm.value?.keyword || '', pageSize: 1 })
      ElMessage.success('连接测试成功')
    } catch {
      ElMessage.error('连接测试失败')
    } finally {
      testingConnection.value = false
    }
  }

  const syncExternalTenders = async () => {
    if (!canSyncExternalSource.value) {
      ElMessage.error('当前账号无权同步外部标讯')
      return null
    }
    if (sourceConfig.value.platforms.length === 0) {
      ElMessage.warning('请先配置标讯源')
      showSourceConfig.value = true
      return null
    }
    if (typeof externalSyncApi !== 'function') {
      ElMessage.warning(API_NOT_READY_MESSAGE)
      return null
    }

    fetchingTenders.value = true
    try {
      const response = await externalSyncApi({
        keyword: searchForm.value?.keyword || '',
        pageSize: 20,
      })
      fetchResult.value = summarizeExternalSyncResult(response)
      lastSyncTime.value = new Date().toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      })
      await refreshTenderList()
      ElMessage.success(`标讯同步完成：新增 ${fetchResult.value.saved} 条，跳过 ${fetchResult.value.skipped} 条`)
      return response
    } catch {
      ElMessage.error('获取标讯失败，请检查网络或后端服务')
      return null
    } finally {
      fetchingTenders.value = false
    }
  }

  return {
    showSourceConfig,
    sourceConfig,
    savingConfig,
    testingConnection,
    fetchingTenders,
    lastSyncTime,
    fetchResult,
    loadSavedConfig,
    saveSourceConfig,
    testConnection,
    syncExternalTenders,
  }
}
