<template>
  <div class="workflow-designer-page">
    <header class="designer-header">
      <div>
        <p class="eyebrow">Workflow Forms</p>
        <h1>流程表单配置</h1>
      </div>
      <div class="header-actions">
        <el-button :loading="loading.templates" @click="loadTemplates">刷新</el-button>
        <el-button type="primary" @click="newTemplate">新建表单</el-button>
      </div>
    </header>

    <main class="designer-shell">
      <aside class="template-list">
        <button
          v-for="template in templates"
          :key="template.templateCode"
          class="template-row"
          :class="{ active: template.templateCode === draft.templateCode }"
          type="button"
          @click="selectTemplate(template)"
        >
          <strong>{{ template.name }}</strong>
          <span>{{ template.templateCode }} · v{{ template.version || 0 }} · {{ template.status }}</span>
        </button>
      </aside>

      <section class="designer-main">
        <div class="form-grid">
          <el-form label-width="96px" class="template-form">
            <el-form-item label="模板编码">
              <el-input v-model="draft.templateCode" placeholder="例如 SEAL_APPLY" />
            </el-form-item>
            <el-form-item label="表单名称">
              <el-input v-model="draft.name" placeholder="例如 用章申请" />
            </el-form-item>
            <el-form-item label="业务类型">
              <el-select v-model="draft.businessType">
                <el-option v-for="type in businessTypes" :key="type" :label="type" :value="type" />
              </el-select>
            </el-form-item>
            <el-form-item label="启用">
              <el-switch v-model="draft.enabled" />
            </el-form-item>
          </el-form>

          <section class="oa-panel">
            <h2>OA 流程绑定</h2>
            <el-input v-model="oa.workflowCode" placeholder="泛微流程 ID / workflowCode" />
            <el-input v-model="oa.provider" placeholder="Provider" />
            <el-button @click="autoMapping">按字段生成映射</el-button>
          </section>
        </div>

        <section class="field-editor">
          <div class="section-title">
            <h2>字段配置器</h2>
            <el-button @click="addField">添加字段</el-button>
          </div>

          <div v-for="(field, index) in draft.schema.fields" :key="field.key" class="field-row">
            <el-input v-model="field.key" placeholder="字段 key" />
            <el-input v-model="field.label" placeholder="字段名称" />
            <el-select v-model="field.type" @change="normalizeField(field)">
              <el-option v-for="type in fieldTypes" :key="type.value" :label="type.label" :value="type.value" />
            </el-select>
            <el-checkbox v-model="field.required" :disabled="field.type === 'info'">必填</el-checkbox>
            <el-button :disabled="index === 0" @click="move(index, -1)">上移</el-button>
            <el-button :disabled="index === draft.schema.fields.length - 1" @click="move(index, 1)">下移</el-button>
            <el-button type="danger" @click="deleteField(field.key)">删除</el-button>
            <el-input
              v-if="field.type === 'select'"
              v-model="field.optionsText"
              class="field-wide"
              placeholder="选项，格式：显示名=值，每行一个"
              type="textarea"
              :rows="2"
            />
            <el-input
              v-if="field.type === 'info'"
              v-model="field.content"
              class="field-wide"
              placeholder="说明文本"
              type="textarea"
              :rows="2"
            />
          </div>
        </section>

        <section class="preview-area">
          <div class="section-title">
            <h2>预览和试提交</h2>
            <div>
              <el-button @click="previewVisible = true">预览表单</el-button>
              <el-button :loading="loading.trial" @click="trialSubmit">试提交</el-button>
              <el-button :loading="loading.save" type="primary" @click="saveAll">保存草稿</el-button>
              <el-button :loading="loading.publish" type="success" @click="publish">发布</el-button>
            </div>
          </div>
          <el-alert v-if="operationError" :title="operationError" type="error" show-icon :closable="false" />
          <pre v-if="trialPayload">{{ trialPayload }}</pre>
        </section>
      </section>
    </main>

    <el-drawer v-model="previewVisible" title="表单预览" size="520px">
      <DynamicWorkflowForm :schema="normalizedSchema" v-model="previewModel" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowFormApi } from '@/api/modules/workflowForm.js'
import DynamicWorkflowForm from '@/components/common/DynamicWorkflowForm.vue'
import {
  FIELD_TYPES,
  buildDefaultTemplate,
  buildMappingFromFields,
  buildSelectedTemplateState,
  createField,
  extractWorkflowFormError,
  moveField,
  removeField
} from './workflow-form-designer/workflowFormDesignerCore.js'
import './workflow-form-designer/workflow-form-designer.css'

const templates = ref([])
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
    if (templates.value.length > 0) selectTemplate(templates.value[0])
  } catch (error) {
    operationError.value = extractWorkflowFormError(error, '流程表单模板加载失败')
    ElMessage.error(operationError.value)
  } finally {
    loading.templates = false
  }
}

function selectTemplate(template) {
  const selected = buildSelectedTemplateState(template)
  Object.assign(draft, selected.draft)
  Object.assign(oa, selected.oa)
}

function newTemplate() {
  Object.assign(draft, buildDefaultTemplate())
  Object.assign(oa, { provider: 'WEAVER', workflowCode: '', fieldMapping: { workflowCode: '', mainFields: [] } })
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

async function trialSubmit() {
  loading.trial = true
  operationError.value = ''
  try {
    const response = await workflowFormApi.testSubmitTemplate(draft.templateCode, {
      applicantName: '测试管理员',
      formData: Object.fromEntries(normalizedSchema.value.fields.map((field) => [field.key, previewModel.value[field.key] || `测试${field.label}`]))
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
</script>
