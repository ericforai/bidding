// Input: Vue router/stores, template API module, and template-library helpers
// Output: template page orchestration state and actions
// Pos: src/views/Knowledge/components/template/ - template page composable layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { knowledgeApi } from '@/api'
import {
  buildDocumentDraft,
  createTemplateFilters,
  createTemplateForm,
  createUseTemplateForm,
  filterTemplateCollection,
  paginateTemplates,
  patchTemplateForm
} from './templateLibraryHelpers.js'
import {
  copyTemplateRecord,
  deleteTemplateRecord,
  fetchTemplateList,
  getCurrentUserId,
  loadTemplateVersions,
  recordTemplateDownload,
  recordTemplateUse,
  saveTemplate,
  upsertTemplateInCollection
} from './templateLibraryRemote.js'

function isPersistentTemplateId(templateId) {
  return /^\d+$/.test(String(templateId))
}

export function useTemplateLibraryPage() {
  const router = useRouter()
  const projectStore = useProjectStore()
  const userStore = useUserStore()

  const activeCategory = ref('all')
  const filters = reactive(createTemplateFilters())
  const pagination = reactive({ page: 1, pageSize: 10 })
  const templates = ref([])
  const loading = ref(false)
  const featurePlaceholder = ref(null)
  const versionPlaceholder = ref(null)

  const previewDialogVisible = ref(false)
  const previewTemplate = ref(null)
  const activePreviewTab = ref('content')
  const useTemplateDialogVisible = ref(false)
  const selectedTemplate = ref(null)
  const useTemplateForm = reactive(createUseTemplateForm())
  const versionDialogVisible = ref(false)
  const versionHistory = ref([])
  const upsertDialogVisible = ref(false)
  const upsertMode = ref('create')
  const templateForm = reactive(createTemplateForm())
  const upsertSubmitting = ref(false)

  const inProgressProjects = computed(() => projectStore.inProgressProjects)
  const allTags = computed(() => Array.from(new Set(templates.value.flatMap((item) => item.tags || []))).sort())
  const filteredTemplates = computed(() => filterTemplateCollection(templates.value, {
    ...filters,
    category: activeCategory.value
  }))
  const pagedTemplates = computed(() => paginateTemplates(filteredTemplates.value, pagination.page, pagination.pageSize))

  watch(filteredTemplates, (items) => {
    const maxPage = Math.max(1, Math.ceil(items.length / pagination.pageSize))
    if (pagination.page > maxPage) pagination.page = maxPage
  })
  function buildRemoteFilters() {
    return {
      category: activeCategory.value,
      name: filters.name,
      productType: filters.productType,
      industry: filters.industry,
      documentType: filters.documentType
    }
  }

  async function loadTemplates(query = buildRemoteFilters()) {
    loading.value = true
    try {
      const result = await fetchTemplateList(query)
      templates.value = result.templates
      featurePlaceholder.value = result.featurePlaceholder
      if (result.errorMessage) ElMessage.error(result.errorMessage)
    } finally {
      loading.value = false
    }
  }
  async function handleSearch() { pagination.page = 1; await loadTemplates() }
  async function handleReset() {
    Object.assign(filters, createTemplateFilters())
    pagination.page = 1
    await loadTemplates({
      category: activeCategory.value
    })
  }

  async function handleCategoryChange() { pagination.page = 1; await loadTemplates() }
  function openCreateDialog() {
    patchTemplateForm(templateForm, {
      category: activeCategory.value === 'all' ? 'technical' : activeCategory.value
    })
    upsertMode.value = 'create'
    upsertDialogVisible.value = true
  }
  function openEditDialog(template) {
    patchTemplateForm(templateForm, template)
    upsertMode.value = 'edit'
    upsertDialogVisible.value = true
  }

  async function submitTemplate() {
    if (!templateForm.name.trim()) {
      ElMessage.warning('请输入模板名称')
      return
    }
    if (!templateForm.productType || !templateForm.industry || !templateForm.documentType) {
      ElMessage.warning('请选择产品类型、行业和文档类型')
      return
    }

    upsertSubmitting.value = true
    try {
      const result = await saveTemplate(upsertMode.value, templateForm, getCurrentUserId(userStore))
      if (!result?.success) {
        ElMessage.error(result?.message || '模板保存失败')
        return
      }
      upsertTemplateInCollection(templates.value, result.data)
      if (previewTemplate.value && String(previewTemplate.value.id) === String(result.data.id)) {
        previewTemplate.value = result.data
      }
      upsertDialogVisible.value = false
      await loadTemplates()
      ElMessage.success(upsertMode.value === 'create' ? '模板创建成功' : '模板更新成功')
    } finally {
      upsertSubmitting.value = false
    }
  }
  async function handlePreview(template) {
    const result = await knowledgeApi.templates.getDetail(template.id)
    previewTemplate.value = result?.success && result.data ? result.data : template
    previewDialogVisible.value = true
    activePreviewTab.value = 'content'
  }
  function handleUseTemplate(template) {
    selectedTemplate.value = template
    Object.assign(useTemplateForm, createUseTemplateForm(), {
      docName: `${template.name}应用`
    })
    useTemplateDialogVisible.value = true
  }
  async function confirmUseTemplate() {
    if (!selectedTemplate.value || !useTemplateForm.docName.trim()) {
      ElMessage.warning('请输入文档名称')
      return
    }

    if (isPersistentTemplateId(selectedTemplate.value.id)) {
      const result = await recordTemplateUse(
        selectedTemplate.value.id,
        useTemplateForm,
        getCurrentUserId(userStore)
      )
      if (!result?.success) {
        ElMessage.error(result?.message || '模板使用记录失败')
        return
      }
    }
    const newDocument = buildDocumentDraft(
      selectedTemplate.value,
      useTemplateForm,
      userStore.currentUser?.name
    )

    useTemplateDialogVisible.value = false
    await router.push({
      name: 'DocumentEditor',
      params: { id: newDocument.id }
    })
    ElMessage.success(`文档「${useTemplateForm.docName}」创建成功`)
  }
  async function handleVersion(template) {
    const result = await loadTemplateVersions(template.id)
    versionHistory.value = []
    versionPlaceholder.value = result.placeholder
    if (result.errorMessage) {
      ElMessage.error(result.errorMessage)
      return
    }

    versionHistory.value = result.versions.map((item, index) => ({
      id: item.id,
      version: item.version,
      date: String(item.createdAt || '').slice(0, 10) || template.updateTime,
      description: item.description || '版本变更',
      isCurrent: index === 0
    }))
    versionDialogVisible.value = true
  }
  async function handleDownload(template) {
    const detail = previewTemplate.value?.id === template.id ? previewTemplate.value : template
    const fileContent = detail.content || detail.description || detail.name
    const blob = new Blob([fileContent], { type: 'text/markdown;charset=utf-8' })
    const { triggerDownload } = await import('@/api/modules/export.js')
    triggerDownload(blob, `${detail.name}.md`)
    if (isPersistentTemplateId(template.id)) {
      const result = await recordTemplateDownload(template.id, getCurrentUserId(userStore))
      if (result?.success && result.data) {
        upsertTemplateInCollection(templates.value, result.data)
      }
    }
    ElMessage.success(`开始下载：${detail.name}`)
  }
  async function handleCopy(template) {
    const result = await copyTemplateRecord(template, getCurrentUserId(userStore))
    if (!result?.success) {
      ElMessage.error(result?.message || '复制失败')
      return
    }
    upsertTemplateInCollection(templates.value, result.data)
    ElMessage.success(`已复制模板：${template.name}`)
  }
  async function handleDelete(template) {
    await ElMessageBox.confirm(
      `确定要删除模板「${template.name}」吗？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const result = await deleteTemplateRecord(template.id)
    if (!result?.success && result !== undefined) {
      ElMessage.error(result?.message || '删除失败')
      return
    }
    templates.value = templates.value.filter((item) => String(item.id) !== String(template.id))
    ElMessage.success('删除成功')
  }
  async function handleMoreAction(command, template) {
    if (command === 'edit') return openEditDialog(template)
    if (command === 'copy') return handleCopy(template)
    if (command === 'version') return handleVersion(template)
    if (command === 'download') return handleDownload(template)
    if (command === 'delete') return handleDelete(template)
  }
  onMounted(loadTemplates)
  return {
    activeCategory,
    filters,
    pagination,
    loading,
    featurePlaceholder,
    versionPlaceholder,
    previewDialogVisible,
    previewTemplate,
    activePreviewTab,
    useTemplateDialogVisible,
    selectedTemplate,
    useTemplateForm,
    versionDialogVisible,
    versionHistory,
    upsertDialogVisible,
    upsertMode,
    templateForm,
    upsertSubmitting,
    inProgressProjects,
    allTags,
    filteredTemplates,
    pagedTemplates,
    handleSearch,
    handleReset,
    handleCategoryChange,
    openCreateDialog,
    submitTemplate,
    handlePreview,
    handleUseTemplate,
    confirmUseTemplate,
    handleMoreAction,
    handleDownload
  }
}
