<template>
  <div class="evidence-conversion-workbench">
    <el-row :gutter="20" class="workbench-layout">
      <!-- Left Panel: Extracted Structured Data & Validation Guards -->
      <el-col :span="12" class="data-panel">
        <el-card shadow="hover">
          <template #header>
            <div class="panel-header">
              <span class="title">项目转化核对 (AI 智能提取)</span>
              <el-tag type="success" size="small" effect="plain">解析完成</el-tag>
            </div>
          </template>

          <el-form label-width="120px" class="conversion-form">
            <el-divider content-position="left">基础与财务信息</el-divider>
            <el-form-item label="项目名称">
              <el-input v-model="projectData.projectName" />
            </el-form-item>
            <el-form-item label="项目预算">
              <el-input v-model="projectData.budget" />
              <el-alert
                v-if="validationWarnings.budget"
                :title="validationWarnings.budget"
                type="warning"
                show-icon
                :closable="false"
                class="mt-8"
              />
            </el-form-item>

            <el-divider content-position="left">关键时间节点</el-divider>
            <el-form-item label="发布日期">
              <el-date-picker v-model="projectData.publishDate" type="date" />
            </el-form-item>
            <el-form-item label="投标截止">
              <el-date-picker v-model="projectData.deadline" type="datetime" />
              <el-alert
                v-if="validationWarnings.timeline"
                :title="validationWarnings.timeline"
                type="warning"
                show-icon
                :closable="false"
                class="mt-8"
              />
            </el-form-item>

            <el-divider content-position="left">资质对账</el-divider>
            <div class="qualifications-list">
              <div v-for="(qual, index) in projectData.qualifications" :key="index" class="qual-item">
                <span class="qual-name">{{ qual.requirementText }}</span>
                <el-tag v-if="qual.matched" type="success" size="small">内部已有</el-tag>
                <el-tag v-else type="danger" size="small">资质缺失</el-tag>
              </div>
            </div>
          </el-form>

          <div class="workbench-actions">
            <el-button @click="onCancel">取消</el-button>
            <el-button type="primary" @click="onConfirmConversion">确认立项</el-button>
          </div>
        </el-card>
      </el-col>

      <!-- Right Panel: Markdown Evidence Source -->
      <el-col :span="12" class="evidence-panel">
        <el-card shadow="hover">
          <template #header>
            <div class="panel-header">
              <span class="title">标讯原文证据链 (Markdown)</span>
              <el-tooltip content="点击左侧字段高亮显示对应原文" placement="top">
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          
          <div class="markdown-preview" v-html="formattedMarkdown" ref="markdownContainer">
            <!-- Rendered Markdown from Sidecar goes here -->
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'

// Mocked data for the evidence workbench layout
const projectData = ref({
  projectName: '市级智慧城市招标项目',
  budget: '8500000',
  publishDate: '2026-04-20',
  deadline: '2026-04-21 14:00:00',
  qualifications: [
    { requirementText: 'ISO9001质量管理体系认证', matched: true },
    { requirementText: '涉密信息系统集成资质', matched: false }
  ]
})

const validationWarnings = ref({
  budget: '',
  timeline: '注意：发布日期与截止日期过近，不足3天！'
})

const rawMarkdown = ref(`
# 1. 项目基本情况
本项目为市级智慧城市招标项目，总预算约为 850 万元人民币。

# 2. 投标要求
**资质要求**：
- 投标人必须具备 ISO9001质量管理体系认证。
- 必须具备涉密信息系统集成资质。

**时间节点**：
- 发布日期：2026-04-20
- 投标截止：2026-04-21 14:00:00
`)

// Simple markdown formatter for preview
const formattedMarkdown = computed(() => {
  let md = rawMarkdown.value
  md = md.replace(/^# (.*$)/gim, '<h3>$1</h3>')
  md = md.replace(/^\*\*([^*]+)\*\*/gim, '<strong>$1</strong>')
  md = md.replace(/^- (.*$)/gim, '<li>$1</li>')
  return md.replace(/\n/g, '<br>')
})

const emit = defineEmits(['cancel', 'confirm'])

const onCancel = () => emit('cancel')
const onConfirmConversion = () => {
  emit('confirm', projectData.value)
}
</script>

<style scoped>
.evidence-conversion-workbench {
  padding: 20px;
  background-color: var(--el-bg-color-page);
  height: 100vh;
}
.workbench-layout {
  height: 100%;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  font-weight: 600;
  font-size: 16px;
}
.mt-8 {
  margin-top: 8px;
}
.qual-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.workbench-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
.markdown-preview {
  height: calc(100vh - 160px);
  overflow-y: auto;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 4px;
  font-family: monospace;
  line-height: 1.6;
}
</style>
