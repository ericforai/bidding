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
        <div v-if="selectedTemplateVersions.length > 0" class="version-list">
          <div class="version-list-title">历史版本</div>
          <div
            v-for="version in selectedTemplateVersions"
            :key="`${version.templateCode}-${version.version}`"
            class="version-row"
          >
            <div>
              <p>v{{ version.version }}</p>
              <span>{{ version.publishedAt || '-' }}</span>
            </div>
            <el-button
              type="primary"
              size="small"
              :disabled="version.version === draft.version"
              @click="rollback(version.version)"
            >
              回滚
            </el-button>
          </div>
        </div>
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
import DynamicWorkflowForm from '@/components/common/DynamicWorkflowForm.vue'
import { useWorkflowFormDesigner } from './workflow-form-designer/useWorkflowFormDesigner.js'
import './workflow-form-designer/workflow-form-designer.css'

const {
  addField,
  autoMapping,
  deleteField,
  draft,
  fieldTypes,
  loadTemplateVersions,
  loadTemplates,
  move,
  newTemplate,
  normalizeField,
  operationError,
  oa,
  businessTypes,
  previewModel,
  previewVisible,
  publish,
  rollback,
  normalizedSchema,
  trialPayload,
  trialSubmit,
  templates,
  selectedTemplateVersions,
  loading,
  saveAll
} = useWorkflowFormDesigner()
</script>
