// Input: qualification store, user store, export/download helpers
// Output: qualification page state and handlers for the qualification view
// Pos: src/views/Knowledge/components/qualification/ - View composition logic

import { computed, onMounted, reactive, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { isFeatureUnavailableResponse } from '@/api'
import { triggerDownload } from '@/api/modules/export'
import { getAccessToken } from '@/api/session.js'
import { useQualificationStore } from '@/stores/qualification'
import { useUserStore } from '@/stores/user'

export function useQualificationPage() {
  const qualificationStore = useQualificationStore()
  const userStore = useUserStore()
  const {
    borrowFeaturePlaceholder,
    borrowLoading,
    borrowRecords,
    listFeaturePlaceholder,
    listLoading,
    qualifications
  } = storeToRefs(qualificationStore)

  const isAdmin = computed(() => userStore.currentUser?.role === 'admin')

  const searchForm = reactive({
    name: '',
    type: '',
    status: ''
  })

  const pagination = reactive({
    page: 1,
    pageSize: 10
  })

  const uploadDialogVisible = ref(false)
  const detailDialogVisible = ref(false)
  const borrowDialogVisible = ref(false)
  const currentQualification = ref(null)

  const uploadForm = reactive({
    name: '',
    type: '',
    subjectType: 'company',
    subjectName: '',
    certificateNo: '',
    issuer: '',
    holderName: '',
    issueDate: '',
    expiryDate: '',
    file: null
  })

  const borrowForm = reactive({
    borrower: '',
    department: '',
    purpose: '',
    returnDate: '',
    remark: ''
  })

  const filteredQualifications = computed(() => {
    const result = qualifications.value
      .filter((item) => !searchForm.name || item.name.toLowerCase().includes(searchForm.name.toLowerCase()))
      .filter((item) => !searchForm.type || item.type === searchForm.type)
      .filter((item) => !searchForm.status || item.status === searchForm.status)

    return [...result].sort((a, b) => a.remainingDays - b.remainingDays)
  })

  const pagedQualifications = computed(() => {
    const start = (pagination.page - 1) * pagination.pageSize
    return filteredQualifications.value.slice(start, start + pagination.pageSize)
  })

  function resetUploadForm() {
    Object.assign(uploadForm, {
      name: '',
      type: '',
      subjectType: 'company',
      subjectName: '',
      certificateNo: '',
      issuer: '',
      holderName: '',
      issueDate: '',
      expiryDate: '',
      file: null
    })
  }

  function resetBorrowForm() {
    Object.assign(borrowForm, {
      borrower: '',
      department: '',
      purpose: '',
      returnDate: '',
      remark: ''
    })
  }

  async function loadPageData() {
    try {
      await qualificationStore.loadQualifications()
      await qualificationStore.loadBorrowRecords()
    } catch (error) {
      console.error('Failed to load qualification page data:', error)
      ElMessage.error('资质页面数据加载失败，请稍后重试')
    }
  }

  function handleSearch() {
    pagination.page = 1
  }

  function handleReset() {
    searchForm.name = ''
    searchForm.type = ''
    searchForm.status = ''
    pagination.page = 1
  }

  async function handleExportList() {
    const [{ useExport }, { ExportType }] = await Promise.all([
      import('@/composables/useExport'),
      import('@/api')
    ])

    useExport().exportExcel(ExportType.QUALIFICATIONS, {
      name: searchForm.name || undefined,
      type: searchForm.type || undefined,
      status: searchForm.status || undefined
    }, '资质列表导出成功')
  }

  function handleUpload() {
    resetUploadForm()
    uploadDialogVisible.value = true
  }

  function handleFileChange(file) {
    uploadForm.file = file
  }

  async function handleConfirmUpload() {
    if (!uploadForm.name || !uploadForm.type || !uploadForm.expiryDate) {
      ElMessage.warning('请填写必填项')
      return
    }

    const result = await qualificationStore.createQualification({
      name: uploadForm.name,
      type: uploadForm.type,
      subjectType: uploadForm.subjectType,
      subjectName: uploadForm.subjectName,
      certificateNo: uploadForm.certificateNo,
      issueDate: uploadForm.issueDate,
      expiryDate: uploadForm.expiryDate,
      issuer: uploadForm.issuer,
      holderName: uploadForm.holderName,
      fileUrl: uploadForm.file?.name || ''
    })

    if (!result?.success) {
      ElMessage.error(result?.message || '上传失败')
      return
    }

    uploadDialogVisible.value = false
    ElMessage.success('资质元数据已创建')
  }

  function handleView(row) {
    currentQualification.value = row
    detailDialogVisible.value = true
  }

  function openBorrowDialog(row) {
    currentQualification.value = row
    resetBorrowForm()
    borrowDialogVisible.value = true
  }

  async function handleConfirmBorrow() {
    if (!currentQualification.value?.id) {
      ElMessage.warning('请先从资质列表选择待借阅资质')
      return
    }
    if (!borrowForm.borrower || !borrowForm.purpose) {
      ElMessage.warning('请填写必填项')
      return
    }

    const result = await qualificationStore.submitBorrow(currentQualification.value.id, borrowForm)

    if (result?.success) {
      borrowDialogVisible.value = false
      ElMessage.success('借阅申请已提交')
      return
    }

    if (isFeatureUnavailableResponse(result)) {
      ElMessage.warning(result.message || '资质借阅接口暂未接入')
      return
    }

    ElMessage.error(result?.message || '借阅申请提交失败')
  }

  async function handleReturn(row) {
    ElMessageBox.confirm(
      `确认「${row.qualificationName}」已归还吗？`,
      '归还确认',
      {
        confirmButtonText: '确认归还',
        cancelButtonText: '取消',
        type: 'success'
      }
    ).then(async () => {
      const result = await qualificationStore.returnBorrow(row.id)
      if (result?.success) {
        ElMessage.success('归还成功')
        return
      }
      if (isFeatureUnavailableResponse(result)) {
        ElMessage.warning(result.message || '资质归还接口暂未接入')
        return
      }
      ElMessage.error(result?.message || '归还失败')
    }).catch(() => {})
  }

  function handleDownload(row) {
    if (!row?.fileUrl) {
      ElMessage.warning('当前资质暂无可下载附件')
      return
    }

    const token = getAccessToken()
    const filename = `${row.name}.${row.fileUrl.split('.').pop() || 'pdf'}`

    fetch(row.fileUrl, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
      .then((response) => {
        if (!response.ok) throw new Error('下载失败')
        return response.blob()
      })
      .then((blob) => {
        triggerDownload(blob, filename)
        ElMessage.success(`下载成功：${row.name}`)
      })
      .catch((error) => {
        console.error('Download error:', error)
        ElMessage.error(`下载失败：${error.message}`)
      })
  }

  function handleDelete(row) {
    ElMessageBox.confirm(
      `确定要删除「${row.name}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      const result = await qualificationStore.deleteQualification(row.id)
      if (result?.success === false) {
        ElMessage.error(result?.message || '删除失败')
        return
      }
      ElMessage.success('删除成功')
    }).catch(() => {})
  }

  function handlePageChange(page) {
    pagination.page = page
  }

  function handleSizeChange(size) {
    pagination.pageSize = size
    pagination.page = 1
  }

  onMounted(loadPageData)

  return {
    borrowDialogVisible,
    borrowFeaturePlaceholder,
    borrowForm,
    borrowLoading,
    borrowRecords,
    currentQualification,
    detailDialogVisible,
    filteredQualifications,
    handleConfirmBorrow,
    handleConfirmUpload,
    handleDelete,
    handleDownload,
    handleExportList,
    handleFileChange,
    handlePageChange,
    handleReset,
    handleReturn,
    handleSearch,
    handleSizeChange,
    handleUpload,
    handleView,
    isAdmin,
    listFeaturePlaceholder,
    listLoading,
    openBorrowDialog,
    pagedQualifications,
    pagination,
    searchForm,
    uploadDialogVisible,
    uploadForm
  }
}

export default useQualificationPage
