import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowFormApi } from '@/api/modules/workflowForm.js'
import {
  FIELD_TYPES,
  buildDefaultTemplate,
  buildMappingFromFields,
  buildSelectedTemplateState,
  createField,
  extractWorkflowFormError,
  moveField,
  removeField
} from './workflowFormDesignerCore.js'

export function useWorkflowFormDesigner() {
  const templates = ref([])
  const selectedTemplateVersions = ref([])
  const businessTypes = ref(['GENERAL_WORKFLOW', 'QUALIFICATION_BORROW'])
  const draft = reactive(buildDefaultTemplate())
  const oa = reactive({ provider: 'WEAVER', workflowCode: '', fieldMapping: { workflowCode: '', mainFields: [] } })
  const fieldTypes = FIELD_TYPES
  const previewVisible = ref(false)
  const previewModel = ref({})
  const trialPayload = ref('')
  const operationError = ref('')
  const loading = reactive({ templates: false, save: false, publish: false, trial: false })

  const normalizedSchema = computed(() => ({
    fields: draft.schema.fields.map(normalizeFieldOptions)
  }))

  function normalizeFieldOptions(field) {
    if (field.type !== 'select') return field
    const optionsText = field.optionsText || field.options?.map((option) => `${option.label}=${option.value}`).join('\n') || ''
    return {
      ...field,
      options: optionsText.split('\n').map((line) => {
        const [label, value] = line.split('=')
        return { label: label?.trim(), value: (value || label || '').trim() }
      }).filter((option) => option.label)
    }
  }

  async function loadTemplates() {
    loading.templates = true
    operationError.value = ''
    try {
      const [templateResponse, typeResponse] = await Promise.all([
        workflowFormApi.listAdminTemplates(),
        workflowFormApi.listBusinessTypes()
      ])
      templates.value = templateResponse.data || []
      businessTypes.value = typeResponse.data || businessTypes.value
      if (templates.value.length > 0) {
        await selectTemplate(templates.value[0])
      } else {
        selectedTemplateVersions.value = []
      }
    } catch (error) {
      operationError.value = extractWorkflowFormError(error, '流程表单模板加载失败')
      ElMessage.error(operationError.value)
    } finally {
      loading.templates = false
    }
  }

  async function selectTemplate(template) {
    const selected = buildSelectedTemplateState(template)
    Object.assign(draft, selected.draft)
    Object.assign(oa, selected.oa)
    if (template?.templateCode) {
      await loadTemplateVersions(template.templateCode)
    } else {
      selectedTemplateVersions.value = []
    }
  }

  async function loadTemplateVersions(templateCode) {
    try {
      const versionResponse = await workflowFormApi.listTemplateVersions(templateCode)
      selectedTemplateVersions.value = versionResponse.data || []
    } catch (error) {
      selectedTemplateVersions.value = []
      ElMessage.error(extractWorkflowFormError(error, '历史版本加载失败'))
    }
  }

  function newTemplate() {
    Object.assign(draft, buildDefaultTemplate())
    Object.assign(oa, { provider: 'WEAVER', workflowCode: '', fieldMapping: { workflowCode: '', mainFields: [] } })
    selectedTemplateVersions.value = []
  }

  function addField() {
    draft.schema.fields.push(createField(`field${draft.schema.fields.length + 1}`, '新字段', 'text'))
  }

  function deleteField(key) {
    draft.schema.fields = removeField(draft.schema.fields, key)
  }

  function move(index, direction) {
    draft.schema.fields = moveField(draft.schema.fields, index, direction)
  }

  function normalizeField(field) {
    if (field.type === 'select' && !field.optionsText) field.optionsText = '选项一=option_1'
    if (field.type === 'info') field.required = false
  }

  function autoMapping() {
    oa.fieldMapping = buildMappingFromFields(oa.workflowCode || `WF_${draft.templateCode}`, normalizedSchema.value.fields)
    oa.workflowCode = oa.fieldMapping.workflowCode
  }

  async function saveAll() {
    loading.save = true
    operationError.value = ''
    try {
      const payload = { ...draft, schema: normalizedSchema.value }
      await workflowFormApi.createTemplateDraft(payload)
      await workflowFormApi.saveOaBinding(draft.templateCode, {
        provider: oa.provider,
        workflowCode: oa.workflowCode,
        fieldMapping: oa.fieldMapping,
        enabled: true
      })
      ElMessage.success('流程表单草稿已保存')
      await loadTemplates()
    } catch (error) {
      operationError.value = extractWorkflowFormError(error, '流程表单草稿保存失败')
      ElMessage.error(operationError.value)
      throw error
    } finally {
      loading.save = false
    }
  }

  async function publish() {
    loading.publish = true
    operationError.value = ''
    try {
      await saveAll()
      await workflowFormApi.publishTemplate(draft.templateCode)
      ElMessage.success('流程表单已发布')
      await loadTemplates()
    } catch (error) {
      operationError.value = extractWorkflowFormError(error, '流程表单发布失败')
      ElMessage.error(operationError.value)
    } finally {
      loading.publish = false
    }
  }

  async function rollback(version) {
    if (!draft.templateCode) return
    loading.save = true
    operationError.value = ''
    try {
      await workflowFormApi.rollbackTemplateVersion(draft.templateCode, version)
      ElMessage.success(`已回滚到 v${version}`)
      await loadTemplates()
    } catch (error) {
      operationError.value = extractWorkflowFormError(error, '回滚历史版本失败')
      ElMessage.error(operationError.value)
    } finally {
      loading.save = false
    }
  }

  async function trialSubmit() {
    loading.trial = true
    operationError.value = ''
    try {
      const response = await workflowFormApi.testSubmitTemplate(draft.templateCode, {
        applicantName: '测试管理员',
        formData: Object.fromEntries(
          normalizedSchema.value.fields.map((field) => [field.key, previewModel.value[field.key] || `测试${field.label}`])
        )
      })
      trialPayload.value = JSON.stringify(response.data, null, 2)
      if (response.data?.oaStarted) {
        ElMessage.success('OA 测试流程已发起')
      }
    } catch (error) {
      operationError.value = extractWorkflowFormError(error, '流程表单试提交失败')
      ElMessage.error(operationError.value)
    } finally {
      loading.trial = false
    }
  }

  onMounted(loadTemplates)

  return {
    templates,
    selectedTemplateVersions,
    businessTypes,
    draft,
    oa,
    fieldTypes,
    previewVisible,
    previewModel,
    trialPayload,
    operationError,
    loading,
    normalizedSchema,
    addField,
    autoMapping,
    deleteField,
    loadTemplateVersions,
    loadTemplates,
    move,
    newTemplate,
    normalizeField,
    normalizeFieldOptions,
    publish,
    rollback,
    saveAll,
    selectTemplate,
    trialSubmit
  }
}
