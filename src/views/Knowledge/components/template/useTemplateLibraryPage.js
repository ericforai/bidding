// Input: Vue router/stores, template API module, and template-library helpers
// Output: template page orchestration state and actions
// Pos: src/views/Knowledge/components/template/ - template page composable layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { isFeatureUnavailableResponse, knowledgeApi } from '@/api'
import { notifyFeatureUnavailable } from '@/utils/featureFeedback'
import {
  buildDocumentDraft,
  createTemplateFilters,
  createTemplateForm,
  createUseTemplateForm,
  extractTags,
  filterTemplateCollection,
  paginateTemplates,
  patchTemplateForm
} from './templateLibraryHelpers.js'

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
    if (pagination.page > maxPage) {
      pagination.page = maxPage
    }
  })

  watch(() => templateForm.category, (category) => {
    if (!templateForm.documentType) return
    if (upsertMode.value === 'create') {
      patchTemplateForm(templateForm, { ...templateForm, category })
    }
  })

  function getCurrentUserId() {
    const rawId = userStore.currentUser?.id
    if (rawId === undefined || rawId === null || rawId === '') return null
    const numericId = Number(rawId)
    return Number.isFinite(numericId) ? numericId : null
  }

  function upsertTemplateRow(template) {
    const index = templates.value.findIndex((item) => String(item.id) === String(template.id))
    if (index > -1) {
      templates.value.splice(index, 1, template)
    } else {
      templates.value.unshift(template)
    }
  }

  async function loadTemplates() {
    loading.value = true
    try {
      const result = await knowledgeApi.templates.getList()
      if (result?.success) {
        templates.value = Array.isArray(result.data) ? result.data : []
        featurePlaceholder.value = null
      } else {
        templates.value = []
        featurePlaceholder.value = notifyFeatureUnavailable(result, {
          fallback: {
            title: '模板库当前不可用',
            hint: '当前无法加载模板列表，请稍后重试或联系管理员检查知识库服务。'
          }
        })
        if (!featurePlaceholder.value && result?.message) {
          ElMessage.error(result.message)
        }
      }
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    pagination.page = 1
  }

  function handleReset() {
    Object.assign(filters, createTemplateFilters())
    pagination.page = 1
  }

  function handleCategoryChange() {
    pagination.page = 1
  }

  function openCreateDialog() {
    patchTemplateForm(templateForm, { category: activeCategory.value === 'all' ? 'technical' : activeCategory.value })
    upsertMode.value = 'create'
    upsertDialogVisible.value = true
  }

  function openEditDialog(template) {
    patchTemplateForm(templateForm, template)
    upsertMode.value = 'edit'
    upsertDialogVisible.value = true
  }

  async function submitTemplate() {
    upsertSubmitting.value = true
    try {
      const payload = {
        ...templateForm,
        tags: extractTags(templateForm.tagsText),
        createdBy: getCurrentUserId()
      }
      const result = upsertMode.value === 'create'
        ? await knowledgeApi.templates.create(payload)
        : await knowledgeApi.templates.update(templateForm.id, payload)

      if (!result?.success) {
        ElMessage.error(result?.message || '模板保存失败')
        return
      }

      upsertTemplateRow(result.data)
      if (previewTemplate.value && String(previewTemplate.value.id) === String(result.data.id)) {
        previewTemplate.value = result.data
      }
      upsertDialogVisible.value = false
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
      const result = await knowledgeApi.templates.recordUse(selectedTemplate.value.id, {
        documentName: useTemplateForm.docName,
        docType: useTemplateForm.docType,
        projectId: useTemplateForm.projectId || null,
        applyOptions: useTemplateForm.applyOptions,
        usedBy: getCurrentUserId()
      })
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
    versionPlaceholder.value = null
    const result = await knowledgeApi.templates.getVersions(template.id)
    if (isFeatureUnavailableResponse(result)) {
      versionHistory.value = []
      versionPlaceholder.value = notifyFeatureUnavailable(result, {
        fallback: {
          title: '版本历史当前不可用',
          hint: '当前无法加载模板版本轨迹，可继续使用模板主体能力。'
        }
      })
      versionDialogVisible.value = true
      return
    }
    if (!result?.success) {
      ElMessage.error(result?.message || '获取版本历史失败')
      return
    }

    versionHistory.value = (result.data || []).map((item, index) => ({
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
      const result = await knowledgeApi.templates.recordDownload(template.id, {
        downloadedBy: getCurrentUserId()
      })
      if (result?.success && result.data) {
        upsertTemplateRow(result.data)
      }
    }
    ElMessage.success(`开始下载：${detail.name}`)
  }

  async function handleCopy(template) {
    const result = await knowledgeApi.templates.copy(template.id, {
      name: `${template.name}（副本）`,
      createdBy: getCurrentUserId()
    })
    if (!result?.success) {
      ElMessage.error(result?.message || '复制失败')
      return
    }
    upsertTemplateRow(result.data)
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
    const result = await knowledgeApi.templates.delete(template.id)
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
    templates,
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
    loadTemplates,
    handleSearch,
    handleReset,
    handleCategoryChange,
    openCreateDialog,
    openEditDialog,
    submitTemplate,
    handlePreview,
    handleUseTemplate,
    confirmUseTemplate,
    handleMoreAction,
    handleDownload
  }
}
