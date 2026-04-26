<!--
  Input: requirementProfile (Object, required), markdown (String)
  Output: emits 'cancel' and 'confirm' (with profile)
  Pos: Components/Project Detail (tender 证据驱动核对面板)
  一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
-->
<template>
  <div class="evidence-conversion-workbench">
    <div class="workbench-header">
      <div class="header-left">
        <h3>项目立项核对 (AI 证据驱动)</h3>
        <p class="subtitle">基于招标文件深度解析，请核对关键信息并确认立项</p>
      </div>
      <div class="header-right">
        <el-button @click="$emit('cancel')">放弃</el-button>
        <el-button type="primary" @click="$emit('confirm', profile)">确认立项并生成初稿</el-button>
      </div>
    </div>

    <el-row :gutter="24" class="workbench-body">
      <!-- Left: Extracted Data -->
      <el-col :span="10" class="scroll-panel">
        <el-form label-position="top">
          <el-card shadow="never" class="info-group">
            <template #header>基本信息</template>
            <el-form-item label="项目名称">
              <el-input v-model="profile.projectName" />
            </el-form-item>
            <el-form-item label="采购人">
              <el-input v-model="profile.purchaserName" />
            </el-form-item>
            <el-form-item label="项目预算">
              <el-input-number v-model="profile.budget" :precision="2" :step="10000" style="width: 100%" />
            </el-form-item>
          </el-card>

          <el-card shadow="never" class="info-group">
            <template #header>关键节点</template>
            <el-form-item label="发布日期">
              <el-date-picker v-model="profile.publishDate" type="date" style="width: 100%" />
            </el-form-item>
            <el-form-item label="投标截止">
              <el-date-picker v-model="profile.deadline" type="datetime" style="width: 100%" />
            </el-form-item>
          </el-card>

          <el-card shadow="never" class="info-group">
            <template #header>解析出的关键要求 ({{ profile.items?.length || 0 }})</template>
            <div class="requirement-list">
              <div 
                v-for="(item, index) in profile.items" 
                :key="index" 
                class="req-item"
                @click="highlightInMarkdown(item)"
              >
                <div class="req-header">
                  <el-tag :type="getCategoryTag(item.category)" size="small">{{ item.category }}</el-tag>
                  <span class="req-title">{{ item.title }}</span>
                  <el-tag v-if="item.mandatory" type="danger" size="small" effect="dark">强制</el-tag>
                </div>
                <div class="req-content">{{ item.content }}</div>
                <div class="req-evidence" v-if="item.sectionPath">
                  <el-icon><Location /></el-icon>
                  {{ item.sectionPath }}
                </div>
              </div>
            </div>
          </el-card>
        </el-form>
      </el-col>

      <!-- Right: Markdown with Evidence Links -->
      <el-col :span="14" class="scroll-panel">
        <el-card shadow="never" class="markdown-card">
          <template #header>
            <div class="card-header">
              <span>标讯原文</span>
              <el-tag size="small" type="info">已转为结构化 Markdown</el-tag>
            </div>
          </template>
          <div class="markdown-container" ref="mdContainer" v-html="safeMarkdownHtml"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Location } from '@element-plus/icons-vue'
import { renderSafeMarkdown } from './tender-conversion/markdownSanitizer.js'

const props = defineProps({
  requirementProfile: { type: Object, required: true },
  markdown: { type: String, default: '' }
})

const profile = ref({ ...props.requirementProfile })
const mdContainer = ref(null)

const safeMarkdownHtml = computed(() => renderSafeMarkdown(props.markdown))

const getCategoryTag = (cat) => {
  const map = {
    qualification: 'warning',
    technical: 'primary',
    commercial: 'success',
    scoring: 'danger'
  }
  return map[cat] || 'info'
}

const highlightInMarkdown = (item) => {
  const container = mdContainer.value
  if (!container || !item?.sourceExcerpt) return
  const txt = container.innerText || ''
  if (txt.includes(item.sourceExcerpt)) {
    container.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}
</script>

<style scoped>
.evidence-conversion-workbench {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.workbench-header {
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.workbench-header h3 { margin: 0; font-size: 18px; color: #303133; }
.subtitle { margin: 4px 0 0; font-size: 13px; color: #909399; }

.workbench-body {
  flex: 1;
  overflow: hidden;
  padding: 24px;
}

.scroll-panel {
  height: 100%;
  overflow-y: auto;
}

.info-group {
  margin-bottom: 20px;
}

.requirement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.req-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
}

.req-item:hover {
  border-color: #409eff;
  background: #fff;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.req-header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
}

.req-title {
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.req-content {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}

.req-evidence {
  font-size: 12px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 4px;
}

.markdown-card {
  height: 100%;
}

.markdown-container {
  height: calc(100vh - 250px);
  overflow-y: auto;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.8;
  padding: 16px;
  background: #fff;
}

.md-line {
  padding: 0 8px;
  white-space: pre-wrap;
  transition: background 0.3s;
}

.md-line.is-highlighted {
  background: #fff8e1;
  border-left: 3px solid #ffc107;
}
</style>
