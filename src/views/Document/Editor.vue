<template>
  <div class="document-editor-page">
    <!-- 顶部导航栏 -->
    <div class="editor-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" @click="handleGoBack">返回</el-button>
        <div class="title-section">
          <h2 class="document-title">{{ projectInfo.name }} - 标书编辑器</h2>
          <el-tag size="small" type="info">{{ documentInfo.templateName }}</el-tag>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="View" @click="handlePreview">预览</el-button>
        <el-button v-if="canUseEditorExportActions" :icon="Download" @click="handleExport">导出</el-button>
        <el-button v-if="canUseEditorArchiveActions" :icon="DocumentChecked" @click="handleArchive">归档</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave">保存</el-button>
      </div>
    </div>

    <!-- 三栏布局 -->
    <div class="editor-container">
      <!-- 左侧章节树 -->
      <div class="left-panel">
        <el-card shadow="never" class="section-tree-card">
          <template #header>
            <div class="card-header">
              <span>章节目录</span>
              <el-button :icon="Plus" size="small" text @click="handleAddSection">添加章节</el-button>
            </div>
          </template>
          <el-tree
            ref="sectionTreeRef"
            :data="sectionTreeData"
            :props="treeProps"
            :highlight-current="true"
            :allow-drag="checkAllowDrag"
            :allow-drop="checkAllowDrop"
            node-key="id"
            draggable
            @node-click="handleNodeClick"
            @node-drop="handleNodeDrop"
          >
            <template #default="{ node, data }">
              <div class="tree-node-content">
                <span class="node-icon">{{ getSectionIcon(data.type) }}</span>
                <span class="node-label">{{ node.label }}</span>
                <el-dropdown trigger="click" @command="(cmd) => handleNodeCommand(cmd, data)">
                  <el-icon :size="14" class="node-more-icon"><MoreFilled /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="add">添加子章节</el-dropdown-item>
                      <el-dropdown-item command="rename">重命名</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-tree>
        </el-card>
      </div>

      <!-- 中间编辑区 -->
      <div class="center-panel">
        <el-card shadow="never" class="editor-card">
          <template #header>
            <div class="editor-header-bar">
              <span class="section-title">{{ currentSection?.name || '请选择章节' }}</span>
              <div class="editor-tools">
                <el-button-group size="small">
                  <el-button :icon="ZoomOut" @click="handleZoomOut" />
                  <el-button>{{ zoomLevel }}%</el-button>
                  <el-button :icon="ZoomIn" @click="handleZoomIn" />
                </el-button-group>
              </div>
            </div>
          </template>

          <div v-if="currentSection" class="editor-content">
            <textarea
              v-model="currentSection.content"
              class="content-textarea"
              :style="{ fontSize: baseFontSize * zoomLevel / 100 + 'px' }"
              placeholder="在此处编辑内容..."
              @input="handleContentChange"
            />

            <div v-if="currentSectionSources.length > 0" class="source-records">
              <div class="source-records-header">来源记录</div>
              <div class="source-record-list">
                <el-tag
                  v-for="(source, index) in currentSectionSources"
                  :key="`${source.kind || 'source'}-${index}`"
                  class="source-record-tag"
                  type="info"
                  effect="plain"
                >
                  {{ source.sourceLabel || source.kind || '来源' }} · {{ source.title }}
                </el-tag>
              </div>
            </div>

            <!-- 知识库推荐浮层 -->
            <div v-if="knowledgeMatches.length > 0" class="knowledge-float-panel">
              <div class="panel-header">
                <el-icon><MagicStick /></el-icon>
                <span>知识库推荐</span>
              </div>
              <div class="knowledge-list">
                <div
                  v-for="match in knowledgeMatches"
                  :key="match.id"
                  class="knowledge-item"
                  @click="handleInsertKnowledge(match)"
                >
                  <div class="knowledge-type">
                    <el-tag :type="match.type === 'case' ? 'success' : 'primary'" size="small">
                      {{ match.type === 'case' ? '案例' : '模板' }}
                    </el-tag>
                    <span class="relevance">匹配度: {{ match.relevance }}%</span>
                  </div>
                  <div class="knowledge-title">{{ match.title }}</div>
                  <div class="knowledge-summary">{{ match.summary }}</div>
                  <div class="insert-hint">点击插入</div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-state">
            <el-icon :size="48" color="#c0c4cc"><Document /></el-icon>
            <p>请从左侧选择章节进行编辑</p>
          </div>
        </el-card>
      </div>

      <!-- 右侧智能装配面板 -->
      <div class="right-panel">
        <el-card shadow="never" class="assembly-card">
          <template #header>
            <div class="card-header with-ai">
              <el-icon class="ai-icon"><MagicStick /></el-icon>
              <span>智能装配</span>
            </div>
          </template>

          <div class="assembly-content">
            <!-- 模板选择 -->
            <div class="form-section">
              <h4 class="section-label">选择模板</h4>
              <el-radio-group v-model="assemblyForm.templateId" class="template-options">
                <el-radio
                  v-for="template in assemblyTemplates"
                  :key="template.id"
                  :value="template.id"
                  border
                >
                  <div class="template-option">
                    <div class="template-name">{{ template.name }}</div>
                    <div class="template-meta">{{ template.category || 'OTHER' }}</div>
                  </div>
                </el-radio>
              </el-radio-group>
              <el-empty v-if="assemblyTemplates.length === 0" description="暂无可用模板" />
            </div>

            <!-- 章节勾选 -->
            <div class="form-section">
              <h4 class="section-label">包含章节</h4>
              <el-checkbox-group v-model="assemblyForm.sections" class="section-checkboxes">
                <el-checkbox value="technical">技术方案</el-checkbox>
                <el-checkbox value="cases">案例展示</el-checkbox>
                <el-checkbox value="qualification">资质文件</el-checkbox>
                <el-checkbox value="service">服务承诺</el-checkbox>
                <el-checkbox value="delivery">交付计划</el-checkbox>
              </el-checkbox-group>
            </div>

            <!-- 装配按钮 -->
            <el-button
              type="primary"
              size="large"
              :loading="isAssembling"
              :disabled="assemblyForm.sections.length === 0"
              class="assembly-btn"
              @click="handleStartAssembly"
            >
              <el-icon v-if="!isAssembling"><MagicStick /></el-icon>
              {{ isAssembling ? '装配中...' : '开始装配' }}
            </el-button>

            <!-- 装配历史 -->
            <div v-if="assemblyHistory.length > 0" class="history-section">
              <el-divider>装配历史</el-divider>
              <div class="history-list">
                <div v-for="item in assemblyHistory" :key="item.id" class="history-item">
                  <el-icon color="#67c23a"><CircleCheckFilled /></el-icon>
                  <div class="history-content">
                    <div class="history-title">{{ item.templateName }}</div>
                    <div class="history-time">{{ item.time }}</div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="exportHistory.length > 0" class="history-section">
              <el-divider>导出历史</el-divider>
              <div class="history-list">
                <div v-for="item in exportHistory" :key="item.id" class="history-item">
                  <el-icon color="#409eff"><Download /></el-icon>
                  <div class="history-content">
                    <div class="history-title">{{ item.fileName }}</div>
                    <div class="history-time">{{ item.exportedAt }}</div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="archiveHistory.length > 0" class="history-section">
              <el-divider>归档记录</el-divider>
              <div class="history-list">
                <div v-for="item in archiveHistory" :key="item.id" class="history-item">
                  <el-icon color="#67c23a"><DocumentChecked /></el-icon>
                  <div class="history-content">
                    <div class="history-title">{{ item.archiveReason }}</div>
                    <div class="history-time">{{ item.archivedAt }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 装配进度对话框 -->
    <el-dialog
      v-model="showAssemblyProgress"
      title="智能装配中"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="assembly-progress">
        <div class="progress-icon">
          <el-icon :size="48" class="rotating"><Loading /></el-icon>
        </div>
        <div class="progress-steps">
          <div
            v-for="(step, index) in assemblySteps"
            :key="index"
            class="step-item"
            :class="{
              'step-active': index === currentStepIndex,
              'step-done': index < currentStepIndex,
              'step-pending': index > currentStepIndex
            }"
          >
            <div class="step-icon">
              <el-icon v-if="index < currentStepIndex"><CircleCheckFilled /></el-icon>
              <el-icon v-else-if="index === currentStepIndex"><Loading /></el-icon>
              <span v-else>{{ index + 1 }}</span>
            </div>
            <div class="step-text">{{ step }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <span></span>
      </template>
    </el-dialog>

    <!-- 章节编辑对话框 -->
    <el-dialog v-model="showSectionDialog" :title="sectionDialogTitle" width="500px">
      <el-form :model="sectionForm" label-width="80px">
        <el-form-item label="章节名称">
          <el-input v-model="sectionForm.name" placeholder="请输入章节名称" />
        </el-form-item>
        <el-form-item label="章节类型">
          <el-radio-group v-model="sectionForm.type">
            <el-radio value="section">章节</el-radio>
            <el-radio value="folder">文件夹</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSectionDialog = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmSection">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  View,
  Download,
  DocumentChecked,
  Check,
  Plus,
  ZoomIn,
  ZoomOut,
  Document,
  MagicStick,
  MoreFilled,
  CircleCheckFilled,
  Loading
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { collaborationApi } from '@/api'
import { parseSectionMetadata } from './documentEditorHelpers.js'
import { useDocumentAssembly } from './useDocumentAssembly.js'
import { useDocumentKnowledge } from './useDocumentKnowledge.js'
import { useDocumentSidebar } from './useDocumentSidebar.js'

const router = useRouter()
const route = useRoute()

const isRemoteProjectId = computed(() => /^\d+$/.test(String(route.params.id || '')))
const canUseLocalEditorActions = computed(() => false && !isRemoteProjectId.value)
const canUseEditorExportActions = computed(() => isRemoteProjectId.value || canUseLocalEditorActions.value)
const canUseEditorArchiveActions = computed(() => isRemoteProjectId.value || canUseLocalEditorActions.value)

const projectInfo = ref({
  id: 'P001',
  name: '智慧城市IOC项目'
})

const documentInfo = ref({
  templateId: 'TPL_SMARTCITY',
  templateName: '智慧城市标书模板'
})

const sectionData = ref({
  sections: [
    {
      id: 'cover',
      name: '封面',
      type: 'section',
      content: '# 智慧城市IOC项目\n\n投标文件\n\n投标单位：西域科技股份有限公司\n投标日期：2025年2月'
    },
    {
      id: '1',
      name: '技术方案',
      type: 'folder',
      children: [
        {
          id: '1.1',
          name: '项目背景',
          type: 'section',
          content: '## 项目背景\n\n在此处编辑项目背景...'
        },
        {
          id: '1.2',
          name: '需求分析',
          type: 'section',
          content: '## 需求分析\n\n在此处编辑需求分析...'
        }
      ]
    },
    {
      id: '2',
      name: '商务文件',
      type: 'folder',
      children: [
        {
          id: '2.1',
          name: '投标函',
          type: 'section',
          content: '## 投标函\n\n在此处编辑投标函...'
        },
        {
          id: '2.2',
          name: '报价清单',
          type: 'section',
          content: '## 报价清单\n\n在此处编辑报价清单...'
        },
        {
          id: '2.3',
          name: '交付计划',
          type: 'section',
          content: '## 交付计划\n\n在此处编辑交付计划...'
        }
      ]
    },
    {
      id: '3',
      name: '案例展示',
      type: 'folder',
      children: [
        {
          id: '3.1',
          name: '智慧城市案例',
          type: 'section',
          content: '## 案例展示\n\n在此处编辑案例展示...'
        }
      ]
    }
  ]
})

const currentSection = ref(null)
const currentStructureId = ref(null)
const activeSectionId = ref(null)
const sectionTreeRef = ref(null)
const zoomLevel = ref(100)
const baseFontSize = 14
const exportHistory = ref([])
const archiveHistory = ref([])

const treeProps = {
  children: 'children',
  label: 'name'
}

const downloadTextFile = (filename, content, mimeType = 'text/plain;charset=utf-8') => {
  const blob = new Blob([content], { type: mimeType })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

const handleContentChange = () => {}

const {
  knowledgeMatches,
  loadKnowledgeMatches,
  handleInsertKnowledge
} = useDocumentKnowledge({
  currentSection,
  projectInfo,
  documentInfo,
  isRemoteProjectId
})

const sidebar = useDocumentSidebar({
  route,
  router,
  projectInfo,
  documentInfo,
  sectionData,
  currentSection,
  currentStructureId,
  activeSectionId,
  sectionTreeRef,
  isRemoteProjectId,
  onSectionSelected: (section) => loadKnowledgeMatches(section)
})

const {
  sectionTreeData,
  showSectionDialog,
  sectionDialogTitle,
  sectionForm,
  editingSectionId,
  loadEditorData,
  handleGoBack,
  handleNodeClick,
  handleNodeDrop,
  handleAddSection,
  handleNodeCommand,
  handleConfirmSection,
  handleSave,
  getSectionIcon,
  checkAllowDrag,
  checkAllowDrop,
  selectSectionById
} = sidebar

const {
  assemblyTemplates,
  assemblyHistory,
  assemblyForm,
  assemblySteps,
  currentStepIndex,
  isAssembling,
  showAssemblyProgress,
  loadAssemblyTemplates,
  loadAssemblyHistory,
  handleStartAssembly
} = useDocumentAssembly({
  sectionData,
  currentSection,
  projectInfo,
  documentInfo,
  isRemoteProjectId,
  onSectionSelected: (section) => selectSectionById(section.id)
})

const currentSectionSources = computed(() => parseSectionMetadata(currentSection.value?.metadata).sources || [])
const currentSectionLabel = computed(() => currentSection.value?.name || '请选择章节')

function handlePreview() {
  const previewContent = sectionData.value.sections
    .map((section) => `${section.name}\n${section.content || ''}`)
    .join('\n\n')
  downloadTextFile(`${projectInfo.value.name}_预览.txt`, previewContent)
  ElMessage.success('已生成本地预览文件')
}

function handleExport() {
  if (!isRemoteProjectId.value) {
    const exportContent = JSON.stringify({
      project: projectInfo.value,
      document: documentInfo.value,
      sections: sectionData.value.sections,
      exportedAt: new Date().toISOString()
    }, null, 2)
    downloadTextFile(`${projectInfo.value.name}_标书导出.json`, exportContent, 'application/json;charset=utf-8')
    ElMessage.success('已生成本地导出文件')
    return
  }

  collaborationApi.exports.createExport(route.params.id, {
    format: 'json',
    exportedBy: null,
    exportedByName: '当前用户'
  }).then(async (result) => {
    if (!result?.success || !result?.data) {
      ElMessage.error(result?.message || '导出失败')
      return
    }
    if (!result.data.content) {
      ElMessage.error('导出失败：后端未返回可下载内容')
      return
    }
    downloadTextFile(
      result.data.fileName,
      result.data.content || '',
      result.data.contentType || 'application/json;charset=utf-8'
    )
    await loadExportArtifacts(route.params.id)
    ElMessage.success('文档导出成功')
  }).catch(() => {
    ElMessage.error('导出失败')
  })
}

async function loadExportArtifacts(projectId) {
  if (!isRemoteProjectId.value) {
    exportHistory.value = []
    archiveHistory.value = []
    return
  }

  try {
    const [exportResult, archiveResult] = await Promise.all([
      collaborationApi.exports.getExports(projectId),
      collaborationApi.exports.getArchiveRecords(projectId)
    ])
    exportHistory.value = Array.isArray(exportResult?.data) ? exportResult.data : []
    archiveHistory.value = Array.isArray(archiveResult?.data) ? archiveResult.data : []
  } catch (error) {
    exportHistory.value = []
    archiveHistory.value = []
  }
}

function handleArchive() {
  if (!isRemoteProjectId.value) {
    ElMessage.success('已完成本地归档记录')
    return
  }

  collaborationApi.exports.archive(route.params.id, {
    archivedBy: null,
    archivedByName: '当前用户',
    archiveReason: '标书编制完成，归档留存'
  }).then(async (result) => {
    if (!result?.success || !result?.data) {
      ElMessage.error(result?.message || '归档失败')
      return
    }
    await loadExportArtifacts(route.params.id)
    ElMessage.success('文档归档成功')
  }).catch(() => {
    ElMessage.error('归档失败')
  })
}

async function loadDocumentData() {
  await loadEditorData()
  await loadKnowledgeMatches(currentSection.value)
  await loadAssemblyTemplates()
  await loadAssemblyHistory()
  await loadExportArtifacts(route.params.id)
}

function handleZoomIn() {
  if (zoomLevel.value < 150) {
    zoomLevel.value += 10
  }
}

function handleZoomOut() {
  if (zoomLevel.value > 70) {
    zoomLevel.value -= 10
  }
}

onMounted(() => {
  loadDocumentData()
})

watch(() => route.params.id, () => {
  loadDocumentData()
})
</script>

<style scoped>
.document-editor-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

/* 顶部导航栏 */
.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.document-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 三栏布局容器 */
.editor-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 16px;
  padding: 16px;
}

/* 左侧章节树面板 */
.left-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.section-tree-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-tree-card :deep(.el-card__body) {
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.tree-node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding-right: 8px;
}

.node-icon {
  flex-shrink: 0;
}

.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-more-icon {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node-content:hover .node-more-icon {
  opacity: 1;
}

/* 中间编辑面板 */
.center-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.editor-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-card :deep(.el-card__body) {
  flex: 1;
  overflow: hidden;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.editor-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-weight: 600;
  color: #303133;
}

.editor-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.content-textarea {
  flex: 1;
  width: 100%;
  border: none;
  resize: none;
  outline: none;
  padding: 20px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  line-height: 1.8;
  color: #303133;
  background: #fff;
}

.content-textarea::placeholder {
  color: #c0c4cc;
}

.source-records {
  padding: 0 20px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.source-records-header {
  padding: 12px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.source-record-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.source-record-tag {
  max-width: 100%;
}

/* 知识库推荐浮层 */
.knowledge-float-panel {
  position: absolute;
  right: 20px;
  top: 20px;
  width: 280px;
  max-height: 400px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 10;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: 600;
}

.knowledge-list {
  max-height: 320px;
  overflow-y: auto;
}

.knowledge-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.knowledge-item:hover {
  background: #f5f7fa;
}

.knowledge-item:last-child {
  border-bottom: none;
}

.knowledge-type {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.relevance {
  font-size: 12px;
  color: #67c23a;
  font-weight: 600;
}

.knowledge-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.knowledge-summary {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.insert-hint {
  font-size: 12px;
  color: #409eff;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.empty-state p {
  margin-top: 16px;
  font-size: 14px;
}

/* 右侧装配面板 */
.right-panel {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.assembly-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.assembly-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
}

.card-header.with-ai {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-icon {
  color: #409eff;
  font-size: 18px;
}

.assembly-content {
  padding: 8px 0;
}

.form-section {
  margin-bottom: 24px;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.template-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.template-options :deep(.el-radio) {
  margin-right: 0;
  margin-bottom: 0;
}

.template-options :deep(.el-radio.is-bordered) {
  width: 100%;
  margin-right: 0;
}

.template-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.template-name {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
}

.template-meta {
  font-size: 12px;
  color: #909399;
}

.section-checkboxes {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-checkboxes :deep(.el-checkbox) {
  margin-right: 0;
  margin-bottom: 0;
}

.assembly-btn {
  width: 100%;
  margin-bottom: 24px;
}

.history-section {
  margin-top: 16px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.history-content {
  flex: 1;
}

.history-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.history-time {
  font-size: 12px;
  color: #909399;
}

/* 装配进度对话框 */
.assembly-progress {
  padding: 20px 0;
}

.progress-icon {
  text-align: center;
  margin-bottom: 24px;
  color: #409eff;
}

.progress-steps {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s;
}

.step-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #f0f0f0;
  color: #909399;
}

.step-text {
  font-size: 14px;
  color: #909399;
}

.step-active {
  background: #ecf5ff;
}

.step-active .step-icon {
  background: #409eff;
  color: #fff;
}

.step-active .step-text {
  color: #409eff;
  font-weight: 600;
}

.step-done .step-icon {
  background: #67c23a;
  color: #fff;
}

.step-done .step-text {
  color: #67c23a;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 响应式 */
@media (max-width: 1200px) {
  .editor-container {
    flex-direction: column;
    overflow-y: auto;
  }

  .left-panel,
  .right-panel {
    width: 100%;
    height: auto;
  }

  .left-panel,
  .right-panel {
    max-height: 300px;
  }

  .center-panel {
    min-height: 500px;
  }
}

/* ==================== Button Enhancements ==================== */

.header-actions .el-button,
.card-header .el-button {
  min-width: 90px;
  height: 36px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.header-actions .el-button--primary {
  background: linear-gradient(135deg, #0369a1, #0284c7);
  border: none;
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.2);
}

.header-actions .el-button--primary:hover {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.3);
}

.header-actions .el-button--primary:active {
  transform: translateY(0);
}

.header-actions .el-button--default {
  border: 1.5px solid #e5e7eb;
  color: #64748b;
}

.header-actions .el-button--default:hover {
  border-color: #94a3b8;
  color: #1e293b;
  background: #f8fafc;
}

.editor-header .el-button {
  height: 36px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
}

/* Button group styling */
:deep(.el-button-group) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-button-group .el-button) {
  border-radius: 0;
}

:deep(.el-button-group .el-button:first-child) {
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}

:deep(.el-button-group .el-button:last-child) {
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
}

/* ==================== Tree Node Enhancements ==================== */

.tree-node-content {
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 6px;
  padding: 4px 8px;
}

.tree-node-content:hover {
  background: #f1f5f9;
}

.node-more-icon {
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 4px;
  padding: 4px;
  cursor: pointer;
}

.node-more-icon:hover {
  background: #e5e7eb;
  color: #0369a1;
}

/* ==================== Knowledge Panel Enhancements ==================== */

.knowledge-item {
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  border: 1.5px solid #e5e7eb;
  cursor: pointer;
}

.knowledge-item:hover {
  border-color: #0369a1;
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.1);
  transform: translateY(-1px);
}

.knowledge-item:active {
  transform: translateY(0);
}

/* ==================== Input/Radio/Checkbox Enhancements ==================== */

:deep(.el-radio.is-bordered) {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-radio.is-bordered:hover) {
  border-color: #94a3b8;
}

:deep(.el-radio.is-bordered.is-checked) {
  border-color: #0369a1;
  background: #f0f9ff;
}

:deep(.el-checkbox) {
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-checkbox:hover) {
  color: #0369a1;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: linear-gradient(135deg, #0369a1, #0284c7);
  border-color: #0369a1;
}

/* ==================== Assembly Button ==================== */

.assembly-btn {
  height: 42px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #0369a1, #0284c7);
  border: none;
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.2);
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.assembly-btn:hover {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.3);
}

.assembly-btn:active {
  transform: translateY(0);
}

/* ==================== History Item Enhancement ==================== */

.history-item {
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.history-item:hover {
  background: #edf2f7;
  transform: translateX(4px);
}

/* ==================== Tag Enhancements ==================== */

:deep(.el-tag) {
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border: none;
}

:deep(.el-tag--primary) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #ffffff;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #ffffff;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #64748b, #475569);
  color: #ffffff;
}

/* ==================== Textarea Enhancement ==================== */

.content-textarea {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
}

.content-textarea:focus {
  outline: none;
  border-color: #e5e7eb;
  box-shadow: none;
}
</style>
