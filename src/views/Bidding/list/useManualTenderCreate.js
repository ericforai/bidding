// Input: tenders API, manual form validation ref, document parse API, and refresh callback
// Output: manual tender dialog state, document backfill, and create action
// Pos: src/views/Bidding/list/ - Manual tender creation composable
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createManualTenderForm } from './constants.js'
import { buildManualTenderPayload, normalizeManualTenderParseResult } from './helpers.js'

const SUPPORTED_PARSE_EXTENSIONS = new Set(['.doc', '.docx', '.pdf'])

function resolveUploadFile(file) {
  if (file instanceof File || file instanceof Blob) return file
  if (file?.raw instanceof File || file?.raw instanceof Blob) return file.raw
  return null
}

function isSupportedParseFile(file) {
  const name = String(file?.name || '').toLowerCase()
  return [...SUPPORTED_PARSE_EXTENSIONS].some((extension) => name.endsWith(extension))
}

function applyParsedFields(form, parsedFields) {
  for (const [key, value] of Object.entries(parsedFields)) {
    if (Array.isArray(value)) {
      if (value.length > 0) form[key] = value
      continue
    }
    if (value !== '' && value !== null && value !== undefined) {
      form[key] = value
    }
  }
}

export function useManualTenderCreate({ tendersApi, refreshTenderList, canCreateTender }) {
  const showManualAdd = ref(false)
  const manualFormRef = ref(null)
  const uploadRef = ref(null)
  const savingManual = ref(false)
  const parsingManualDocument = ref(false)
  const manualForm = ref(createManualTenderForm())

  const resetManualForm = () => {
    manualForm.value = createManualTenderForm()
  }

  const handleFileChange = async (file, fileList) => {
    manualForm.value.attachments = fileList
    const uploadFile = resolveUploadFile(file)
    if (!uploadFile || !isSupportedParseFile(uploadFile)) return

    parsingManualDocument.value = true
    try {
      const response = await tendersApi.parseTenderIntakeDocument(uploadFile, { entityId: 'manual-tender' })
      if (!response?.success) {
        throw new Error(response?.message || '文档自动识别失败')
      }
      applyParsedFields(manualForm.value, normalizeManualTenderParseResult(response.data))
      ElMessage.success('DeepSeek/AI 已识别附件内容，可继续编辑后保存')
    } catch (error) {
      const timedOut = error?.code === 'ECONNABORTED'
      ElMessage.warning(timedOut ? 'AI 解析超时，可继续手动填写' : '自动识别失败，可继续手动填写')
    } finally {
      parsingManualDocument.value = false
    }
  }

  const saveManualTender = async () => {
    if (!canCreateTender.value) {
      ElMessage.error('当前账号无权人工录入标讯')
      return false
    }

    try {
      await manualFormRef.value?.validate()
      savingManual.value = true
      const response = await tendersApi.create(buildManualTenderPayload(manualForm.value))
      if (!response?.success) {
        throw new Error(response?.message || '标讯入库失败')
      }

      ElMessage.success('标讯已成功入库')
      showManualAdd.value = false
      resetManualForm()
      await refreshTenderList()
      return true
    } catch (error) {
      if (error) {
        ElMessage.error(error.message || '标讯入库失败')
      }
      return false
    } finally {
      savingManual.value = false
    }
  }

  return {
    showManualAdd,
    manualFormRef,
    uploadRef,
    manualForm,
    savingManual,
    parsingManualDocument,
    resetManualForm,
    handleFileChange,
    saveManualTender,
  }
}
