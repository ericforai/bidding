// Input: tenders API, manual form validation ref, and refresh callback
// Output: manual tender dialog state and create action
// Pos: src/views/Bidding/list/ - Manual tender creation composable
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createManualTenderForm } from './constants.js'
import { buildManualTenderPayload } from './helpers.js'

export function useManualTenderCreate({ tendersApi, refreshTenderList, canCreateTender }) {
  const showManualAdd = ref(false)
  const manualFormRef = ref(null)
  const uploadRef = ref(null)
  const savingManual = ref(false)
  const manualForm = ref(createManualTenderForm())

  const resetManualForm = () => {
    manualForm.value = createManualTenderForm()
  }

  const handleFileChange = (file, fileList) => {
    manualForm.value.attachments = fileList
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
    resetManualForm,
    handleFileChange,
    saveManualTender,
  }
}
