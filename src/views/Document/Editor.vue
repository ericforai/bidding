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
                <el-radio :value="'TPL_SMARTCITY'" border>智慧城市</el-radio>
                <el-radio :value="'TPL_SOFTWARE'" border>软件开发</el-radio>
                <el-radio :value="'TPL_EQUIPMENT'" border>设备采购</el-radio>
              </el-radio-group>
            </div>

            <!-- 章节勾选 -->
            <div class="form-section">
              <h4 class="section-label">包含章节</h4>
              <el-checkbox-group v-model="assemblyForm.sections" class="section-checkboxes">
                <el-checkbox :label="'technical'">技术方案</el-checkbox>
                <el-checkbox :label="'cases'">案例展示</el-checkbox>
                <el-checkbox :label="'qualification'">资质文件</el-checkbox>
                <el-checkbox :label="'service'">服务承诺</el-checkbox>
                <el-checkbox :label="'delivery'">交付计划</el-checkbox>
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
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { collaborationApi, isMockMode, projectsApi } from '@/api'

const router = useRouter()
const route = useRoute()
const currentStructureId = ref(null)
const activeSectionId = ref(null)
const isRemoteProjectId = computed(() => /^\d+$/.test(String(route.params.id || '')))
const canUseLocalEditorActions = computed(() => isMockMode() && !isRemoteProjectId.value)
const canUseEditorExportActions = computed(() => isRemoteProjectId.value || canUseLocalEditorActions.value)
const canUseEditorArchiveActions = computed(() => isRemoteProjectId.value || canUseLocalEditorActions.value)

// 项目信息
const projectInfo = ref({
  id: 'P001',
  name: '智慧城市IOC项目'
})

// 文档信息
const documentInfo = ref({
  templateId: 'TPL_SMARTCITY',
  templateName: '智慧城市标书模板'
})

// 章节数据
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
          content: '## 1.1 项目背景\n\n本项目旨在构建一套智慧城市IOC（智能运营中心）系统，实现对城市运行状态的全面感知、实时监测和智能分析。\n\n### 建设目标\n- 实现城市数据统一汇聚\n- 构建可视化指挥调度平台\n- 建立智能决策支持体系'
        },
        {
          id: '1.2',
          name: '需求分析',
          type: 'section',
          content: '## 1.2 需求分析\n\n### 功能需求\n\n1. 数据采集与汇聚\n2. 可视化展示\n3. 智能预警\n4. 应急指挥\n\n### 非功能需求\n\n- 系统响应时间 < 2秒\n- 支持1000+并发用户\n- 系统可用性 99.9%'
        },
        {
          id: '1.3',
          name: '技术架构',
          type: 'section',
          content: '## 1.3 技术架构\n\n### 总体架构\n\n本系统采用微服务架构，分为以下几层：\n\n1. **感知层**：IoT设备、传感器、摄像头\n2. **网络层**：5G、NB-IoT、LoRa\n3. **数据层**：数据湖、数据仓库\n4. **平台层**：微服务、中间件\n5. **应用层**：各业务应用\n\n### 技术选型\n\n- 前端：Vue3 + Element Plus\n- 后端：Spring Cloud Alibaba\n- 数据库：MySQL + MongoDB + Redis\n- 大数据：Hadoop + Spark\n- 可视化：ECharts + DataV'
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
          content: '## 投标函\n\n致：[招标单位名称]\n\n根据贵方[项目名称]招标文件（编号：[招标编号]），我方经过认真研究，决定参加投标。\n\n### 投标报价\n\n人民币：[金额]万元\n\n### 投标承诺\n\n1. 我方承诺投标文件真实有效\n2. 我方承诺按要求完成项目\n3. 我方承诺提供优质服务'
        },
        {
          id: '2.2',
          name: '报价清单',
          type: 'section',
          content: '## 报价清单\n\n| 序号 | 项目名称 | 数量 | 单价 | 金额 |\n|------|---------|------|------|------|\n| 1 | IOC平台软件 | 1套 | | |\n| 2 | 大屏展示系统 | 1套 | | |\n| 3 | 数据采集服务 | 1项 | | |\n\n**合计：[金额]万元**'
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
          content: '## 成功案例\n\n### 上海XX区智慧城市IOC项目\n\n**项目规模**：500万元\n**完成时间**：2024年\n\n**项目亮点**：\n- 接入20+委办局数据\n- 实现300+指标实时监测\n- 建成指挥调度大厅\n\n### 深圳XX园区智慧管理平台\n\n**项目规模**：300万元\n**完成时间**：2023年\n\n**项目亮点**：\n- 园区资产数字化管理\n- 能耗智能分析\n- 安防联动预警'
        }
      ]
    }
  ]
})

// 树形结构配置
const treeProps = {
  children: 'children',
  label: 'name'
}

const sectionTreeRef = ref(null)

// 计算树形数据
const sectionTreeData = computed(() => sectionData.value.sections)

// 当前选中章节
const currentSection = ref(null)

// 知识库匹配数据
const knowledgeMatches = ref([])
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

// 缩放比例
const zoomLevel = ref(100)
const baseFontSize = 14

// 装配表单
const assemblyForm = ref({
  templateId: 'TPL_SMARTCITY',
  sections: []
})

// 装配状态
const isAssembling = ref(false)
const showAssemblyProgress = ref(false)
const currentStepIndex = ref(0)

// 装配步骤
const assemblySteps = ref([
  '分析评分标准...',
  '匹配技术方案模板...',
  '检索相关案例...',
  '组装资质文件...',
  '生成服务承诺...',
  '整合交付计划...',
  '合规性检查...'
])

// 装配历史
const assemblyHistory = ref([])
const exportHistory = ref([])
const archiveHistory = ref([])

// 章节对话框
const showSectionDialog = ref(false)
const sectionDialogTitle = ref('添加章节')
const sectionForm = ref({
  id: '',
  name: '',
  type: 'section',
  parentId: ''
})

// 编辑章节ID
const editingSectionId = ref('')

const resolveSectionApiId = (section) => section?.apiId || section?.id

const findFirstEditableSection = (sections) => {
  for (const item of sections || []) {
    if (item.type === 'section' || !item.children?.length) {
      return item
    }
    const nested = findFirstEditableSection(item.children)
    if (nested) return nested
  }
  return sections?.[0] || null
}

const selectSectionById = (id) => {
  const section = findSectionById(id)
  if (!section) return null
  currentSection.value = section
  activeSectionId.value = section.id
  loadKnowledgeMatches(section.id)
  return section
}

const buildSectionOrders = (sections, orders = {}) => {
  ;(sections || []).forEach((item, index) => {
    const apiId = resolveSectionApiId(item)
    if (apiId && /^\d+$/.test(String(apiId))) {
      orders[String(apiId)] = index + 1
    }
    if (item.children?.length) {
      buildSectionOrders(item.children, orders)
    }
  })
  return orders
}

const syncCurrentSectionReference = () => {
  if (!activeSectionId.value) return
  const latest = findSectionById(activeSectionId.value)
  if (latest) {
    currentSection.value = latest
  }
}

const ensureEditorStructure = async (projectId) => {
  if (!isRemoteProjectId.value) return null

  try {
    const result = await collaborationApi.editor.getStructure(projectId)
    if (result?.success && result?.data?.id) {
      return result.data
    }
  } catch (error) {
    if (error?.response?.status && error.response.status !== 404) {
      throw error
    }
  }

  const created = await collaborationApi.editor.createStructure(projectId, {
    name: `${projectInfo.value.name || '项目'} 文档结构`,
  })
  return created?.data || null
}

const loadProjectInfo = async (projectId) => {
  try {
    const result = await projectsApi.getDetail(projectId)
    if (result?.success && result?.data) {
      projectInfo.value = {
        id: result.data.id,
        name: result.data.name || projectInfo.value.name,
      }
    }
  } catch (error) {

  }
}

const loadExportArtifacts = async (projectId) => {
  if (!isRemoteProjectId.value) {
    exportHistory.value = []
    archiveHistory.value = []
    return
  }

  try {
    const [exportResult, archiveResult] = await Promise.all([
      collaborationApi.exports.getExports(projectId),
      collaborationApi.exports.getArchiveRecords(projectId),
    ])
    exportHistory.value = Array.isArray(exportResult?.data) ? exportResult.data : []
    archiveHistory.value = Array.isArray(archiveResult?.data) ? archiveResult.data : []
  } catch (error) {

    exportHistory.value = []
    archiveHistory.value = []
  }
}

const loadEditorData = async () => {
  const projectId = route.params.id
  await loadProjectInfo(projectId)
  await loadExportArtifacts(projectId)

  if (!isRemoteProjectId.value) {
    const fallbackSection = findFirstEditableSection(sectionData.value.sections)
    if (fallbackSection) {
      selectSectionById(fallbackSection.id)
    }
    return
  }

  try {
    const structure = await ensureEditorStructure(projectId)
    currentStructureId.value = structure?.id || null
    if (structure?.name) {
      documentInfo.value.templateName = structure.name
    }

    const treeResult = await collaborationApi.editor.getEditorTree(projectId)
    if (treeResult?.success) {
      sectionData.value.sections = Array.isArray(treeResult.data) ? treeResult.data : []
    }

    const preferredSection = activeSectionId.value
      ? findSectionById(activeSectionId.value)
      : findFirstEditableSection(sectionData.value.sections)

    if (preferredSection) {
      selectSectionById(preferredSection.id)
    } else {
      currentSection.value = null
      activeSectionId.value = null
    }
  } catch (error) {

    ElMessage.warning('文档结构加载失败，请检查网络连接后重试')
    const fallbackSection = findFirstEditableSection(sectionData.value.sections)
    if (fallbackSection) {
      selectSectionById(fallbackSection.id)
    }
  }
}

// 获取章节图标
const getSectionIcon = (type) => {
  return type === 'folder' ? '📁' : '📄'
}

// 检查是否允许拖拽
const checkAllowDrag = (node) => true

// 检查是否允许放置
const checkAllowDrop = (draggingNode, dropNode, type) => {
  if (isRemoteProjectId.value) {
    if (type === 'inner') return false
    return String(draggingNode.data.parentId || '') === String(dropNode.data.parentId || '')
  }
  if (type === 'inner') {
    return dropNode.data.type === 'folder'
  }
  return true
}

// 点击章节节点
const handleNodeClick = (data) => {
  selectSectionById(data.id)
}

// 加载知识库匹配
const loadKnowledgeMatches = (sectionId) => {
  const mockKnowledge = {
    '1.1': [
      {
        id: 'k1',
        type: 'case',
        title: '上海XX智慧城市IOC项目',
        summary: '项目背景与建设目标描述，可直接参考使用',
        relevance: 95,
        content: '### 项目背景\n\n上海XX区作为国家新型智慧城市试点区，亟需建设一套综合性的智能运营中心系统。\n\n### 建设目标\n\n1. 实现"一屏观全城"\n2. 建立"一网管全城"\n3. 打造"一脑慧全城"'
      },
      {
        id: 'k2',
        type: 'template',
        title: '智慧城市建设方案模板',
        summary: '标准的项目背景描述模板，含建设目标',
        relevance: 88,
        content: '## 项目背景\n\n随着智慧城市建设的深入推进，...\n\n## 建设目标\n\n本项目旨在构建...'
      }
    ],
    '1.2': [
      {
        id: 'k3',
        type: 'case',
        title: '深圳XX项目需求分析',
        summary: '详细的功能需求和非功能需求描述',
        relevance: 92,
        content: '## 功能需求\n\n### 数据采集需求\n\n支持多种数据源接入...\n\n### 可视化需求\n\n支持多种图表类型...'
      },
      {
        id: 'k4',
        type: 'template',
        title: '需求分析模板',
        summary: '标准需求分析文档结构模板',
        relevance: 85,
        content: '## 需求分析\n\n### 功能需求\n\n#### 1. 用户管理\n\n#### 2. 数据管理...'
      }
    ],
    '1.3': [
      {
        id: 'k5',
        type: 'template',
        title: '微服务架构方案模板',
        summary: '完整的微服务架构设计说明模板',
        relevance: 92,
        content: '## 技术架构\n\n### 总体架构\n\n采用微服务架构...\n\n### 技术选型\n\n- 前端框架\n- 后端框架\n- 数据库...'
      }
    ],
    '3.1': [
      {
        id: 'k6',
        type: 'case',
        title: '北京XX区智慧城市项目',
        summary: '完整的项目案例描述，含项目亮点',
        relevance: 90,
        content: '## 北京XX区智慧城市项目\n\n**项目规模**：800万元\n**完成时间**：2024年\n\n### 项目亮点\n\n1. 实现跨部门数据共享\n2. 建成城市运行指标体系\n3. 支持7×24小时运行监控'
      }
    ]
  }

  knowledgeMatches.value = mockKnowledge[sectionId] || []
}

// 拖拽节点放置
const handleNodeDrop = (draggingNode, dropNode, position) => {
  if (!isRemoteProjectId.value || !currentStructureId.value) {
    ElMessage.success('章节顺序已更新')
    return
  }

  collaborationApi.editor.reorderSections(route.params.id, {
    structureId: currentStructureId.value,
    sectionOrders: buildSectionOrders(sectionData.value.sections),
  }).then(() => {
    syncCurrentSectionReference()
    ElMessage.success('章节顺序已同步')
  }).catch((error) => {
    ElMessage.error(`章节排序同步失败: ${error.message}`)
    loadEditorData()
  })
}

// 缩放
const handleZoomIn = () => {
  if (zoomLevel.value < 150) {
    zoomLevel.value += 10
  }
}

const handleZoomOut = () => {
  if (zoomLevel.value > 70) {
    zoomLevel.value -= 10
  }
}

// 内容变化
const handleContentChange = () => {
  // 可以在这里添加自动保存逻辑
}

// 插入知识库内容
const handleInsertKnowledge = (match) => {
  if (currentSection.value) {
    const cursorPosition = currentSection.value.content.length
    const insertContent = `\n\n> 来自知识库：[${match.title}]\n\n${match.content}\n\n`
    currentSection.value.content += insertContent
    ElMessage.success('已插入知识库内容')
  }
}

// 开始装配
const handleStartAssembly = () => {
  if (assemblyForm.value.sections.length === 0) {
    ElMessage.warning('请至少选择一个章节')
    return
  }

  ElMessageBox.confirm(
    `确定要使用"${getTemplateName(assemblyForm.value.templateId)}"模板生成${assemblyForm.value.sections.length}个章节的内容吗？`,
    '确认装配',
    {
      confirmButtonText: '开始装配',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    startAssemblyProcess()
  }).catch(() => {})
}

// 获取模板名称
const getTemplateName = (templateId) => {
  const map = {
    'TPL_SMARTCITY': '智慧城市',
    'TPL_SOFTWARE': '软件开发',
    'TPL_EQUIPMENT': '设备采购'
  }
  return map[templateId] || '未知模板'
}

// 执行装配流程
const startAssemblyProcess = () => {
  isAssembling.value = true
  showAssemblyProgress.value = true
  currentStepIndex.value = 0

  const steps = assemblySteps.value
  let stepIndex = 0

  const executeNextStep = () => {
    if (stepIndex < steps.length) {
      currentStepIndex.value = stepIndex
      setTimeout(() => {
        stepIndex++
        executeNextStep()
      }, 800)
    } else {
      // 装配完成
      completeAssembly()
    }
  }

  executeNextStep()
}

// 装配完成
const completeAssembly = () => {
  setTimeout(() => {
    showAssemblyProgress.value = false
    isAssembling.value = false

    // 更新章节内容并获取填充的章节ID
    const filledSectionIds = fillAssembledContent()

    // 自动定位到第一个填充的章节
    if (filledSectionIds.length > 0) {
      const firstSectionId = filledSectionIds[0]
      const section = findSectionById(firstSectionId)
      if (section) {
        currentSection.value = section
        loadKnowledgeMatches(firstSectionId)

        // 展开父节点并高亮
        nextTick(() => {
          if (sectionTreeRef.value) {
            sectionTreeRef.value.setCurrentKey(firstSectionId)
          }
        })
      }
    }

    // 添加到历史
    assemblyHistory.value.unshift({
      id: Date.now(),
      templateName: getTemplateName(assemblyForm.value.templateId),
      time: new Date().toLocaleString()
    })

    ElMessage.success(`智能装配完成！已填充 ${filledSectionIds.length} 个章节`)
  }, 500)
}

// 填充装配内容
const fillAssembledContent = () => {
  const filledIds = []

  const templateContents = {
    'TPL_SMARTCITY': {
      technical: {
        '1.1': '## 1.1 项目背景\n\n本项目立足于智慧城市建设的实际需求，旨在打造一个集数据汇聚、智能分析、联动指挥于一体的智能运营中心(IOC)。\n\n### 建设背景\n\n随着城市化进程加快，城市管理面临诸多挑战：数据分散、管理滞后、决策缺乏支撑。亟需建设统一的城市智能运营平台。\n\n### 建设目标\n\n1. **数据融合**：整合各部门数据资源，实现"一湖汇全城"\n2. **智能感知**：建立城市运行指标体系，实现"一屏观全城"\n3. **联动指挥**：构建应急指挥体系，实现"一网管全城"',
        '1.2': '## 1.2 需求分析\n\n### 业务需求\n\n**数据汇聚需求**\n- 接入不少于20个委办局数据\n- 支持结构化、非结构化数据\n- 实时数据接入能力\n\n**可视化需求**\n- 支持大屏、PC、移动端多终端展示\n- 提供2D/3D可视化能力\n- 自定义仪表盘\n\n### 技术需求\n\n- 系统响应时间<2秒\n- 支持1000+并发用户\n- 系统可用性≥99.9%\n- 数据安全等保三级',
        '1.3': '## 1.3 技术架构\n\n### 总体架构\n\n```\n┌─────────────────────────────────────┐\n│          应用层 (SaaS)              │\n├─────────────────────────────────────┤\n│          平台层 (PaaS)              │\n│  微服务 | 中间件 | API网关           │\n├─────────────────────────────────────┤\n│          数据层 (DaaS)              │\n│  数据湖 | 数据仓库 | 数据治理         │\n├─────────────────────────────────────┤\n│          感知层 (IoT)               │\n│  传感器 | 摄像头 | 智能设备          │\n└─────────────────────────────────────┘\n```\n\n### 技术选型\n\n| 类别 | 技术选型 | 说明 |\n|------|---------|------|\n| 前端 | Vue3 + Element Plus | 响应式UI框架 |\n| 后端 | Spring Cloud Alibaba | 微服务框架 |\n| 数据库 | MySQL + MongoDB | 关系型+文档型 |\n| 缓存 | Redis Cluster | 分布式缓存 |\n| 大数据 | Hadoop + Spark | 数据处理 |'
      },
      cases: {
        '3.1': '## 成功案例\n\n### 案例1：上海XX区智慧城市IOC\n\n**项目概况**\n- 项目规模：500万元\n- 完成时间：2024年6月\n- 服务周期：3年\n\n**建设内容**\n1. 建设800平指挥大厅\n2. 接入23个委办局数据\n3. 实现300+城市指标监测\n\n**项目成效**\n- 城市事件发现效率提升60%\n- 跨部门协同效率提升50%\n- 领导决策支持满意度95%\n\n### 案例2：深圳XX园区智慧管理\n\n**项目概况**\n- 项目规模：300万元\n- 完成时间：2023年12月\n\n**建设内容**\n- 园区资产数字化管理\n- 能耗智能分析与优化\n- 安防联动预警系统\n\n**项目成效**\n- 园区能耗降低15%\n- 安防事件响应时间缩短70%'
      },
      qualification: {
        '2.1': '## 公司资质\n\n### 基础资质\n\n- 营业执照（注册资本5000万元）\n- ISO9001质量管理体系认证\n- ISO27001信息安全管理体系认证\n- CMMI5级认证\n\n### 行业资质\n\n- 电子与智能化工程专业承包一级\n- 信息系统集成及服务资质一级\n- 安全技术防范工程设计施工一级\n\n### 软件著作权\n\n- 智慧城市综合管理平台V1.0\n- IOC智能运营中心系统V2.0\n- 城市数据中台系统V1.0\n- 可视化大屏展示系统V3.0'
      },
      service: {
        '2.2': '## 服务承诺\n\n### 质量承诺\n\n1. **系统质量**：符合国家及行业标准，通过第三方测评\n2. **数据质量**：数据准确率≥99.5%\n3. **服务响应**：7×24小时技术支持热线\n\n### 培训承诺\n\n- 现场培训：不少于10个工作日\n- 培训人数：不少于20人\n- 培训内容：系统操作、维护、管理\n\n ### 售后服务\n\n**质保期**：3年免费质保\n\n**响应时间**：\n- 严重故障：2小时内响应，24小时内解决\n- 一般故障：4小时内响应，48小时内解决\n\n**定期巡检**：每季度一次现场巡检服务'
      },
      delivery: {
        '2.3': '## 交付计划\n\n### 项目周期\n\n总工期：6个月\n\n### 里程碑计划\n\n| 阶段 | 工作内容 | 周期 | 交付物 |\n|------|---------|------|--------|\n| 需求调研 | 需求分析、方案设计 | 1个月 | 需求规格说明书 |\n| 系统开发 | 平台开发、功能实现 | 3个月 | 系统源码 |\n| 测试验收 | 系统测试、用户验收 | 1个月 | 测试报告 |\n| 上线运行 | 部署上线、培训移交 | 1个月 | 操作手册 |\n\n### 交付标准\n\n1. 完整的系统源代码\n2. 系统设计文档、技术文档\n3. 用户操作手册、维护手册\n4. 测试报告、验收报告'
      }
    }
  }

  const contents = templateContents[assemblyForm.value.templateId]
  if (contents) {
    Object.keys(contents).forEach(key => {
      if (assemblyForm.value.sections.includes(key)) {
        const sectionContents = contents[key]
        Object.keys(sectionContents).forEach(sectionId => {
          const section = findSectionById(sectionId)
          if (section) {
            section.content = sectionContents[sectionId]
            filledIds.push(sectionId)
          }
        })
      }
    })
  }

  return filledIds
}

// 根据ID查找章节
const findSectionById = (id) => {
  const findInArray = (arr) => {
    for (const item of arr) {
      if (item.id === id) return item
      if (item.children) {
        const found = findInArray(item.children)
        if (found) return found
      }
    }
    return null
  }
  return findInArray(sectionData.value.sections)
}

// 添加章节
const handleAddSection = () => {
  sectionDialogTitle.value = '添加章节'
  sectionForm.value = {
    id: '',
    name: '',
    type: 'section',
    parentId: ''
  }
  editingSectionId.value = ''
  showSectionDialog.value = true
}

// 节点命令
const handleNodeCommand = (command, data) => {
  switch (command) {
    case 'add':
      sectionDialogTitle.value = '添加子章节'
      sectionForm.value = {
        id: '',
        name: '',
        type: 'section',
        parentId: data.id
      }
      editingSectionId.value = ''
      showSectionDialog.value = true
      break
    case 'rename':
      sectionDialogTitle.value = '重命名章节'
      sectionForm.value = {
        id: data.id,
        name: data.name,
        type: data.type,
        parentId: ''
      }
      editingSectionId.value = data.id
      showSectionDialog.value = true
      break
    case 'delete':
      handleDeleteSection(data)
      break
  }
}

// 删除章节
const handleDeleteSection = (data) => {
  ElMessageBox.confirm('确定要删除该章节吗？', '确认删除', {
    type: 'warning'
  }).then(() => {
    if (isRemoteProjectId.value && /^\d+$/.test(String(resolveSectionApiId(data)))) {
      collaborationApi.editor.deleteSection(route.params.id, resolveSectionApiId(data))
        .then(() => {
          deleteSectionById(data.id)
          ElMessage.success('章节已删除')
          if (currentSection.value?.id === data.id) {
            currentSection.value = findFirstEditableSection(sectionData.value.sections)
            activeSectionId.value = currentSection.value?.id || null
          }
        })
        .catch((error) => {
          ElMessage.error(`删除章节失败: ${error.message}`)
        })
      return
    }

    deleteSectionById(data.id)
    ElMessage.success('章节已删除')
    if (currentSection.value?.id === data.id) {
      currentSection.value = null
    }
  }).catch(() => {})
}

// 根据ID删除章节
const deleteSectionById = (id) => {
  const deleteInArray = (arr) => {
    const index = arr.findIndex(item => item.id === id)
    if (index > -1) {
      arr.splice(index, 1)
      return true
    }
    for (const item of arr) {
      if (item.children && deleteInArray(item.children)) {
        return true
      }
    }
    return false
  }
  deleteInArray(sectionData.value.sections)
}

// 确认章节操作
const handleConfirmSection = () => {
  if (!sectionForm.value.name) {
    ElMessage.warning('请输入章节名称')
    return
  }

  if (editingSectionId.value && isRemoteProjectId.value) {
    const targetSection = findSectionById(editingSectionId.value)
    collaborationApi.editor.updateSection(route.params.id, resolveSectionApiId(targetSection), {
      title: sectionForm.value.name,
    }).then((result) => {
      if (targetSection) {
        targetSection.name = result?.data?.name || sectionForm.value.name
      }
      syncCurrentSectionReference()
      ElMessage.success('章节已重命名')
      showSectionDialog.value = false
    }).catch((error) => {
      ElMessage.error(`重命名失败: ${error.message}`)
    })
    return
  }

  if (editingSectionId.value) {
    // 重命名
    const section = findSectionById(editingSectionId.value)
    if (section) {
      section.name = sectionForm.value.name
      ElMessage.success('章节已重命名')
    }
  } else if (isRemoteProjectId.value && currentStructureId.value) {
    const parent = sectionForm.value.parentId ? findSectionById(sectionForm.value.parentId) : null
    collaborationApi.editor.createSection(route.params.id, {
      structureId: currentStructureId.value,
      parentId: parent ? resolveSectionApiId(parent) : null,
      sectionType: parent ? 'SECTION' : 'CHAPTER',
      title: sectionForm.value.name,
      content: sectionForm.value.type === 'section' ? `## ${sectionForm.value.name}\n\n在此处添加内容...` : '',
      orderIndex: parent?.children?.length ? parent.children.length + 1 : sectionData.value.sections.length + 1,
    }).then((result) => {
      const newSection = result?.data
      if (!newSection) return

      if (sectionForm.value.parentId && parent) {
        if (!parent.children) parent.children = []
        parent.children.push(newSection)
      } else {
        sectionData.value.sections.push(newSection)
      }
      selectSectionById(newSection.id)
      ElMessage.success('章节已添加')
      showSectionDialog.value = false
    }).catch((error) => {
      ElMessage.error(`添加章节失败: ${error.message}`)
    })
    return
  } else {
    // 添加新章节
    const newSection = {
      id: Date.now().toString(),
      name: sectionForm.value.name,
      type: sectionForm.value.type,
      content: sectionForm.value.type === 'section' ? '## ' + sectionForm.value.name + '\n\n在此处添加内容...' : ''
    }

    if (sectionForm.value.parentId) {
      const parent = findSectionById(sectionForm.value.parentId)
      if (parent) {
        if (!parent.children) {
          parent.children = []
        }
        parent.children.push(newSection)
      }
    } else {
      sectionData.value.sections.push(newSection)
    }

    ElMessage.success('章节已添加')
  }

  showSectionDialog.value = false
}

// 顶部操作
const handleGoBack = () => {
  router.back()
}

const handlePreview = () => {
  const previewContent = sectionData.value.sections
    .map((section) => `${section.name}\n${section.content || ''}`)
    .join('\n\n')
  downloadTextFile(`${projectInfo.value.name}_预览.txt`, previewContent)
  ElMessage.success('已生成本地预览文件')
}

const handleExport = () => {
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
    exportedByName: '当前用户',
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

const handleArchive = () => {
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

const handleSave = () => {
  if (!currentSection.value) {
    ElMessage.warning('请先选择要保存的章节')
    return
  }

  if (!isRemoteProjectId.value || !/^\d+$/.test(String(resolveSectionApiId(currentSection.value)))) {
    ElMessage.success('保存成功')
    return
  }

  collaborationApi.editor.updateSection(route.params.id, resolveSectionApiId(currentSection.value), {
    title: currentSection.value.name,
    content: currentSection.value.content,
    metadata: currentSection.value.metadata || '',
    orderIndex: currentSection.value.orderIndex ?? 0,
  }).then((result) => {
    currentSection.value = {
      ...currentSection.value,
      ...(result?.data || {}),
    }
    activeSectionId.value = currentSection.value.id
    syncCurrentSectionReference()
    ElMessage.success('保存成功')
  }).catch((error) => {
    ElMessage.error(`保存失败: ${error.message}`)
  })
}

onMounted(() => {
  loadEditorData()
})

watch(() => route.params.id, () => {
  loadEditorData()
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
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.content-textarea:focus {
  outline: none;
  border-color: #0369a1;
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1);
}
</style>
