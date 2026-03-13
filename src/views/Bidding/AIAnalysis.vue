<template>
  <div class="ai-analysis-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="handleGoBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="title-section">
          <h2 class="page-title">{{ tenderInfo?.title || '加载中...' }}</h2>
          <el-tag type="success" size="small">AI分析报告</el-tag>
        </div>
      </div>
      <div class="header-actions">
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出报告
        </el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div v-if="analysisData" class="main-content">
      <!-- 左侧：雷达图和赢面分 -->
      <div class="left-section">
        <el-card class="score-card" shadow="never">
          <!-- 赢面分圆形进度 -->
          <div class="win-score-display">
            <div class="score-circle" :class="getScoreLevelClass(analysisData.winScore)">
              <div class="score-inner">
                <span class="score-value">{{ analysisData.winScore }}</span>
                <span class="score-label">赢面分</span>
              </div>
            </div>
            <div class="score-suggestion">
              <el-icon><InfoFilled /></el-icon>
              {{ analysisData.suggestion }}
            </div>
          </div>

          <!-- 雷达图 -->
          <div class="radar-section">
            <h4 class="section-title">维度分析</h4>
            <WinScoreChart :dimension-scores="analysisData.dimensionScores" />
          </div>

          <!-- 维度得分列表 -->
          <div class="dimension-list">
            <div
              v-for="dim in analysisData.dimensionScores"
              :key="dim.name"
              class="dimension-item"
            >
              <div class="dim-header">
                <span class="dim-name">{{ dim.name }}</span>
                <el-tag :type="getDimensionTagType(dim.score)" size="small">
                  {{ dim.score }}分
                </el-tag>
              </div>
              <div class="dim-bar">
                <div
                  class="dim-bar-fill"
                  :style="{
                    width: dim.score + '%',
                    backgroundColor: getScoreColor(dim.score)
                  }"
                ></div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：详情和风险 -->
      <div class="right-section">
        <!-- 维度详情卡片 -->
        <el-card class="detail-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>维度详情</span>
              <el-button link type="primary" size="small" @click="expandAll = !expandAll">
                {{ expandAll ? '收起全部' : '展开全部' }}
              </el-button>
            </div>
          </template>
          <el-collapse v-model="activeDimensions" accordion>
            <el-collapse-item
              v-for="dim in dimensionDetails"
              :key="dim.name"
              :name="dim.name"
            >
              <template #title>
                <div class="collapse-title">
                  <span>{{ dim.name }}</span>
                  <el-tag :type="getDimensionTagType(dim.score)" size="small">
                    {{ dim.score }}分
                  </el-tag>
                </div>
              </template>
              <div class="collapse-content">
                <div class="detail-item">
                  <span class="detail-label">评估说明：</span>
                  <span class="detail-value">{{ dim.description }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">改进建议：</span>
                  <span class="detail-value suggestion">{{ dim.suggestion }}</span>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-card>

        <!-- 关键风险列表 -->
        <el-card class="risk-card" shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Warning /></el-icon>
              <span>关键风险</span>
              <el-badge :value="analysisData.risks.length" type="danger" />
            </div>
          </template>
          <div class="risk-list">
            <div
              v-for="(risk, index) in analysisData.risks"
              :key="index"
              class="risk-item"
              :class="'risk-' + risk.level"
            >
              <div class="risk-header">
                <el-tag :type="risk.level === 'high' ? 'danger' : 'warning'" size="small">
                  {{ risk.level === 'high' ? '高风险' : '中风险' }}
                </el-tag>
                <span class="risk-desc">{{ risk.desc }}</span>
              </div>
              <div class="risk-action">
                <el-icon><Guide /></el-icon>
                建议操作：{{ risk.action }}
              </div>
            </div>
          </div>
        </el-card>

        <!-- 自动生成的任务清单 -->
        <el-card class="task-card" shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><List /></el-icon>
              <span>任务清单 (同步到日程)</span>
              <el-button link type="primary" size="small" @click="handleSyncTasks">
                <el-icon><Refresh /></el-icon>
                同步到日程
              </el-button>
            </div>
          </template>
          <div class="task-list">
            <div
              v-for="task in analysisData.autoTasks"
              :key="task.id"
              class="task-item"
            >
              <el-checkbox
                v-model="task.completed"
                @change="handleTaskCheck(task)"
              >
                <span class="task-title" :class="{ completed: task.completed }">
                  {{ task.title }}
                </span>
              </el-checkbox>
              <div class="task-meta">
                <el-tag :type="getPriorityTagType(task.priority)" size="small">
                  {{ task.priority === 'high' ? '高优先级' : '中优先级' }}
                </el-tag>
                <span class="task-owner">
                  <el-icon><User /></el-icon>
                  {{ task.owner }}
                </span>
                <span class="task-due">
                  <el-icon><Calendar /></el-icon>
                  {{ task.dueDate }}
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <el-card v-else class="empty-card" shadow="never">
      <FeaturePlaceholder
        v-if="analysisPlaceholder"
        :title="analysisPlaceholder.title"
        :message="analysisPlaceholder.message"
        :hint="analysisPlaceholder.hint"
      />
      <el-empty v-else description="当前模式下暂无可用的 AI 分析报告" />
    </el-card>

    <!-- 底部操作栏 -->
    <div class="bottom-actions">
      <el-button size="large" @click="handleAddToPool">
        <el-icon><Star /></el-icon>
        加入意向池
      </el-button>
      <el-button type="primary" size="large" @click="handleCreateProject">
        <el-icon><Plus /></el-icon>
        创建投标项目
      </el-button>
    </div>

    <!-- AI解析进度弹窗 -->
    <el-dialog
      v-model="showParsingDialog"
      title="AI分析中"
      width="480px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="parsing-content">
        <div class="parsing-animation">
          <div class="parsing-spinner"></div>
        </div>
        <p class="parsing-text">正在解析招标文件...</p>
        <el-progress
          :percentage="parseProgress"
          :stroke-width="12"
          :color="progressColors"
        />
        <p class="parsing-hint">AI正在分析标书文档，提取关键信息</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Download,
  InfoFilled,
  Warning,
  Guide,
  List,
  Refresh,
  User,
  Calendar,
  Star,
  Plus
} from '@element-plus/icons-vue'
import WinScoreChart from '@/components/ai/WinScoreChart.vue'
import FeaturePlaceholder from '@/components/common/FeaturePlaceholder.vue'
import { aiApi, isMockMode, tendersApi } from '@/api'
import { notifyFeatureUnavailable } from '@/utils/featureFeedback'

const router = useRouter()
const route = useRoute()

// 维度详情数据
const dimensionDetailsMap = {
  '客户关系': {
    score: 80,
    description: '与客户有历史合作记录，关系维护良好，但近期高层互动较少',
    suggestion: '建议安排分管领导或高层进行技术交流，加深客户印象'
  },
  '需求匹配': {
    score: 70,
    description: '核心业务领域匹配，但部分新技术要求需要补充技术储备',
    suggestion: '梳理现有技术方案，结合新要求进行针对性优化'
  },
  '资质满足': {
    score: 60,
    description: '基础资质齐全，但部分高等级资质缺失或即将过期',
    suggestion: '及时更新即将过期的资质，考虑与合作方联合投标'
  },
  '交付能力': {
    score: 85,
    description: '具备完善的交付团队和成功案例，交付风险较低',
    suggestion: '保持优势，提前组建项目团队进行资源准备'
  },
  '竞争态势': {
    score: 50,
    description: '竞争对手实力较强，预计至少3-5家优质竞争对手',
    suggestion: '分析竞争对手特点，制定差异化竞争策略'
  }
}

const tenderId = ref(route.params.id || 'T001')
const analysisData = ref(null)
const tenderInfo = ref(null)
const analysisPlaceholder = ref(null)
const expandAll = ref(false)
const activeDimensions = ref([])
const showParsingDialog = ref(false)
const parseProgress = ref(0)

const progressColors = [
  { color: '#f56c6c', percentage: 30 },
  { color: '#e6a23c', percentage: 60 },
  { color: '#409eff', percentage: 90 },
  { color: '#67c23a', percentage: 100 }
]

const isApiMode = !isMockMode()

const dimensionDetails = computed(() => {
  if (!analysisData.value) return []
  return analysisData.value.dimensionScores.map(dim => ({
    name: dim.name,
    score: dim.score,
    ...(dimensionDetailsMap[dim.name] || {
      description: '暂无维度说明',
      suggestion: '暂无改进建议'
    })
  }))
})

// 获取分数颜色
const getScoreColor = (score) => {
  if (score >= 71) return '#67c23a'
  if (score >= 41) return '#e6a23c'
  return '#f56c6c'
}

// 获取分数等级样式
const getScoreLevelClass = (score) => {
  if (score >= 80) return 'score-excellent'
  if (score >= 60) return 'score-good'
  return 'score-normal'
}

// 获取维度标签类型
const getDimensionTagType = (score) => {
  if (score >= 71) return 'success'
  if (score >= 41) return 'warning'
  return 'danger'
}

// 获取优先级标签类型
const getPriorityTagType = (priority) => {
  return priority === 'high' ? 'danger' : 'warning'
}

// 返回
const handleGoBack = () => {
  router.back()
}

// 导出报告
const handleExport = () => {
  ElMessage.success('报告导出中，请稍候...')
  // 实际导出逻辑
  setTimeout(() => {
    ElMessage.success('报告导出成功！')
  }, 1500)
}

// 同步任务到日程
const handleSyncTasks = () => {
  ElMessage.success('任务已同步到日程')
}

// 任务勾选
const handleTaskCheck = (task) => {
  if (task.completed) {
    ElMessage.success(`任务"${task.title}"已完成`)
  }
}

// 加入意向池
const handleAddToPool = () => {
  ElMessageBox.confirm(
    '确定要加入意向池吗？加入后可以在投标项目列表中查看。',
    '加入意向池',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(() => {
    ElMessage.success('已加入意向池')
    router.push('/bidding')
  }).catch(() => {})
}

// 创建投标项目
const handleCreateProject = () => {
  ElMessageBox.confirm(
    '确定要创建投标项目吗？创建后将进入项目立项流程。',
    '创建投标项目',
    {
      confirmButtonText: '确定创建',
      cancelButtonText: '取消',
      type: 'success'
    }
  ).then(() => {
    ElMessage.success('正在跳转到项目创建页...')
    router.push({
      path: '/project/create',
      query: { tenderId: tenderId.value }
    })
  }).catch(() => {})
}

// 开始AI解析动画
const startParsingAnimation = () => {
  showParsingDialog.value = true
  parseProgress.value = 0

  const interval = setInterval(() => {
    if (parseProgress.value < 100) {
      parseProgress.value += Math.random() * 15 + 5
      if (parseProgress.value > 100) {
        parseProgress.value = 100
      }
    } else {
      clearInterval(interval)
      setTimeout(() => {
        showParsingDialog.value = false
      }, 500)
    }
  }, 800)
}

const loadTenderInfo = async () => {
  const response = await tendersApi.getDetail(tenderId.value)
  if (response?.success && response.data) {
    tenderInfo.value = response.data
    return
  }

  tenderInfo.value = null
  ElMessage.error(response?.message || '标讯信息加载失败')
}

const loadAnalysis = async () => {
  const response = await aiApi.bid.getAnalysis(tenderId.value)
  if (response?.success && response.data) {
    analysisData.value = response.data
    analysisPlaceholder.value = null
    return
  }

  analysisData.value = null
  analysisPlaceholder.value = notifyFeatureUnavailable(response, {
    fallback: {
      title: 'AI 分析暂未接入',
      hint: '当前仅支持查看标讯详情，AI 报告生成尚未完成真实后端接入。',
    },
  })

  if (analysisPlaceholder.value) {
  } else if (isApiMode) {
    ElMessage.warning(response?.message || '该标讯 AI 分析暂不可用')
  } else {
    ElMessage.error(response?.message || 'AI 分析数据加载失败')
  }
}

const initializePage = async () => {
  const showParsing = !route.params.fromList

  if (showParsing) {
    startParsingAnimation()
  }

  await loadTenderInfo()

  if (showParsing) {
    await new Promise((resolve) => setTimeout(resolve, 1000))
  }

  await loadAnalysis()
}

onMounted(() => {
  initializePage()
})
</script>

<style scoped>
.ai-analysis-page {
  min-height: calc(100vh - 120px);
  padding-bottom: 80px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 主内容区 */
.main-content {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 20px;
}

.empty-card {
  min-height: 320px;
}

/* 左侧区域 */
.left-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.score-card {
  position: sticky;
  top: 20px;
}

/* 赢面分展示 */
.win-score-display {
  text-align: center;
  padding: 24px 0;
  border-bottom: 1px solid #e4e7ed;
}

.score-circle {
  width: 140px;
  height: 140px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.score-circle::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.1;
}

.score-excellent {
  color: #67c23a;
  background: conic-gradient(#67c23a 0deg, #67c23a calc(var(--score) * 3.6deg), #e4e7ed calc(var(--score) * 3.6deg), #e4e7ed 360deg);
  --score: 75;
}

.score-good {
  color: #e6a23c;
  background: conic-gradient(#e6a23c 0deg, #e6a23c calc(var(--score) * 3.6deg), #e4e7ed calc(var(--score) * 3.6deg), #e4e7ed 360deg);
  --score: 60;
}

.score-normal {
  color: #f56c6c;
  background: conic-gradient(#f56c6c 0deg, #f56c6c calc(var(--score) * 3.6deg), #e4e7ed calc(var(--score) * 3.6deg), #e4e7ed 360deg);
  --score: 40;
}

.score-inner {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-value {
  font-size: 42px;
  font-weight: 700;
  line-height: 1;
}

.score-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.score-suggestion {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  background: #f0f9ff;
  border-radius: 6px;
  color: #409eff;
  font-size: 14px;
}

/* 雷达图区域 */
.radar-section {
  padding: 16px 0;
  border-bottom: 1px solid #e4e7ed;
}

.section-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

/* 维度列表 */
.dimension-list {
  padding: 16px 0;
}

.dimension-item {
  margin-bottom: 16px;
}

.dimension-item:last-child {
  margin-bottom: 0;
}

.dim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.dim-name {
  font-size: 14px;
  color: #606266;
}

.dim-bar {
  height: 8px;
  background: #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.dim-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

/* 右侧区域 */
.right-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-card,
.risk-card,
.task-card {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.header-icon {
  color: #409eff;
}

.collapse-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
  padding-right: 16px;
}

.collapse-content {
  padding: 12px 0;
}

.detail-item {
  display: flex;
  margin-bottom: 12px;
  font-size: 14px;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-label {
  color: #909399;
  min-width: 80px;
}

.detail-value {
  color: #606266;
  flex: 1;
}

.detail-value.suggestion {
  color: #409eff;
}

/* 风险列表 */
.risk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.risk-item {
  padding: 12px 16px;
  border-radius: 8px;
  border-left: 3px solid;
}

.risk-high {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.risk-medium {
  background: #fdf6ec;
  border-left-color: #e6a23c;
}

.risk-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.risk-desc {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.risk-action {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  padding-left: 4px;
}

/* 任务列表 */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  padding: 12px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: background 0.3s;
}

.task-item:hover {
  background: #ecf5ff;
}

.task-title {
  font-size: 14px;
  color: #303133;
}

.task-title.completed {
  text-decoration: line-through;
  color: #c0c4cc;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.task-owner,
.task-due {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 底部操作栏 */
.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 240px;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 16px 32px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.05);
}

/* 解析动画 */
.parsing-content {
  text-align: center;
  padding: 20px;
}

.parsing-animation {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.parsing-spinner {
  width: 60px;
  height: 60px;
  border: 4px solid #e4e7ed;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.parsing-text {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 20px;
}

.parsing-hint {
  margin-top: 16px;
  font-size: 13px;
  color: #909399;
}

/* 响应式 */
@media (max-width: 1400px) {
  .main-content {
    grid-template-columns: 350px 1fr;
  }
}

@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .left-section {
    position: static;
  }

  .score-card {
    position: static;
  }
}

/* 深度样式 */
:deep(.el-collapse-item__header) {
  height: 48px;
  padding: 0 16px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #f5f7fa;
}

:deep(.el-collapse-item__content) {
  padding: 0 16px;
}

:deep(.el-checkbox__label) {
  font-size: 14px;
}

:deep(.el-progress__text) {
  display: none;
}
</style>
