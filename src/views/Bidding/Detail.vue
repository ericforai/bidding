<template>
  <div class="bidding-detail-page">
    <!-- 返回导航 -->
    <div class="breadcrumb-nav">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/bidding' }">标讯中心</el-breadcrumb-item>
        <el-breadcrumb-item>标讯详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 主要内容 -->
    <div v-if="tender" class="detail-content">
      <!-- 标讯基本信息卡片 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <h2 class="tender-title">{{ tender.title }}</h2>
              <div class="tags-row">
                <el-tag v-for="tag in tender.tags" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
            </div>
            <div class="header-right">
              <div class="ai-score-large" :class="getScoreClass(tender.aiScore)">
                <div class="score-value">{{ tender.aiScore }}分</div>
                <div class="score-label">AI评分</div>
              </div>
            </div>
          </div>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="预算金额">
            <span class="amount-text">{{ tender.budget }}万元</span>
          </el-descriptions-item>
          <el-descriptions-item label="所属地区">
            {{ tender.region }}
          </el-descriptions-item>
          <el-descriptions-item label="所属行业">
            {{ tender.industry }}
          </el-descriptions-item>
          <el-descriptions-item label="发布日期">
            {{ tender.date }}
          </el-descriptions-item>
          <el-descriptions-item label="截止日期">
            <span :class="getDeadlineClass(tender.deadline)">
              {{ tender.deadline }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusType(tender.status)" size="small">
              {{ getStatusText(tender.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="中标概率" :span="3">
            <el-rate
              v-model="probabilityRate"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons">
          <el-button type="primary" size="large" @click="handleParticipate">
            <el-icon><DocumentAdd /></el-icon>
            立即投标
          </el-button>
          <el-button v-if="tender && tender.originalUrl" type="success" size="large" @click="handleViewOriginal">
            <el-icon><Link /></el-icon>
            查看官网公告
          </el-button>
          <el-button size="large" @click="handleFollow">
            <el-icon><StarFilled v-if="isFollowed" /><Star v-else /></el-icon>
            {{ isFollowed ? '已关注' : '加入关注' }}
          </el-button>
          <el-button size="large" @click="handleShare">
            <el-icon><Share /></el-icon>
            分享
          </el-button>
        </div>
      </el-card>

      <!-- AI分析卡片 -->
      <el-card v-if="showTenderAiSection" class="ai-analysis-card" shadow="never">
        <template #header>
          <div class="card-title-with-icon">
            <el-icon class="ai-icon"><MagicStick /></el-icon>
            AI智能分析
          </div>
        </template>

        <div class="analysis-content">
          <div class="analysis-section">
            <h4 class="section-title">匹配度分析</h4>
            <div v-if="aiLoading" class="match-bars">
              <el-skeleton :rows="4" animated />
            </div>
            <div v-else-if="aiDimensions.length > 0" class="match-bars">
              <div v-for="dim in aiDimensions" :key="dim.name" class="match-bar-item">
                <div class="bar-label">
                  <span>{{ dim.name }}</span>
                  <span class="bar-value">{{ dim.percentage }}</span>
                </div>
                <el-progress :percentage="dim.score" :stroke-width="10" :show-text="false" />
              </div>
            </div>
            <div v-else class="match-bars">
              <div class="match-bar-item">
                <div class="bar-label">
                  <span>综合评分</span>
                  <span class="bar-value">{{ overallScore }}%</span>
                </div>
                <el-progress :percentage="overallScore" :stroke-width="10" :show-text="false" />
              </div>
            </div>
          </div>

          <el-divider />

          <div class="analysis-section">
            <h4 class="section-title">优势分析</h4>
            <div class="advantages-list">
              <div v-for="(advantage, index) in advantages" :key="index" class="advantage-item">
                <el-icon class="advantage-icon"><CircleCheckFilled /></el-icon>
                <span>{{ advantage }}</span>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="analysis-section">
            <h4 class="section-title">AI建议</h4>
            <div class="suggestions">
              <el-alert
                v-for="(suggestion, index) in suggestions"
                :key="index"
                :title="suggestion.title"
                :type="suggestion.type"
                :closable="false"
                show-icon
              >
                <template #default>
                  <p>{{ suggestion.content }}</p>
                </template>
              </el-alert>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 相关案例推荐 -->
      <el-card class="related-cases-card" shadow="never">
        <template #header>
          <div class="card-title-with-icon">
            <el-icon><Briefcase /></el-icon>
            相关案例推荐
          </div>
        </template>

        <div class="cases-list">
          <div
            v-for="caseItem in relatedCases"
            :key="caseItem.id"
            class="case-item"
            @click="handleViewCase(caseItem.id)"
          >
            <div class="case-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="case-content">
              <h5 class="case-title">{{ caseItem.title }}</h5>
              <div class="case-meta">
                <span>{{ caseItem.customer }}</span>
                <span>{{ caseItem.amount }}万元</span>
                <span>{{ caseItem.year }}年</span>
              </div>
              <p class="case-summary">{{ caseItem.summary }}</p>
              <div class="case-highlights">
                <el-tag
                  v-for="highlight in caseItem.highlights"
                  :key="highlight"
                  size="small"
                  type="info"
                >
                  {{ highlight }}
                </el-tag>
              </div>
            </div>
            <div class="case-arrow">
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 加载状态 -->
    <div v-else class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useBiddingStore } from '@/stores/bidding'
import {
  MagicStick,
  DocumentAdd,
  StarFilled,
  Star,
  Share,
  Link,
  CircleCheckFilled,
  Briefcase,
  Document,
  ArrowRight
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { tendersApi, knowledgeApi } from '@/api'
import { normalizeAiDimensions, normalizeAiRisks } from './bidding-utils.js'

const router = useRouter()
const route = useRoute()
const biddingStore = useBiddingStore()
const showTenderAiSection = true

const tender = ref(null)
const isFollowed = ref(false)
const aiAnalysis = ref(null)
const aiLoading = ref(false)
const relatedCases = ref([])

const probabilityRate = computed(() => {
  if (!tender.value) return 0
  const score = overallScore.value
  if (score >= 90) return 5
  if (score >= 80) return 4
  if (score >= 70) return 3
  if (score >= 60) return 2
  return 1
})

const aiDimensions = computed(() => {
  if (!aiAnalysis.value?.dimensionScores) return []
  return normalizeAiDimensions(aiAnalysis.value.dimensionScores)
})

const aiRiskItems = computed(() => {
  if (!aiAnalysis.value?.risks) return []
  return normalizeAiRisks(aiAnalysis.value.risks)
})

const overallScore = computed(() => {
  return aiAnalysis.value?.winScore ?? tender.value?.aiScore ?? 0
})

const advantages = computed(() => {
  if (!tender.value) return []
  const score = overallScore.value
  const advantageList = []
  if (score >= 90) {
    advantageList.push('该客户历史合作记录良好，累计中标3次')
    advantageList.push('我司在信创领域具有较强技术优势')
    advantageList.push('拥有相关行业成功案例')
  } else if (score >= 80) {
    advantageList.push('传统优势领域，有行业经验')
    advantageList.push('前期已建立良好客户关系')
  } else {
    advantageList.push('预算充足，项目规模适中')
    advantageList.push('技术要求在现有能力范围内')
  }
  return advantageList
})

const suggestions = computed(() => {
  if (!tender.value) return []
  const suggestion = aiAnalysis.value?.suggestion
  return [
    {
      title: '投标策略建议',
      type: 'success',
      content: suggestion || tender.value.aiReason || '建议优先跟进，预计中标概率较高'
    },
    {
      title: '注意事项',
      type: 'warning',
      content: '需提前准备相关资质文件，确保符合招标要求'
    }
  ]
})

async function loadAiAnalysis(tenderId) {
  if (!tenderId) return
  aiLoading.value = true
  try {
    const result = await tendersApi.getAIAnalysis(tenderId)
    if (result?.success && result?.data) {
      aiAnalysis.value = result.data
    }
  } catch (error) {
    console.warn('AI analysis load failed:', error)
  } finally {
    aiLoading.value = false
  }
}

async function loadRelatedCases(tenderData) {
  if (!tenderData?.industry) {
    relatedCases.value = []
    return
  }
  try {
    const result = await knowledgeApi.cases.getList({ industry: tenderData.industry })
    relatedCases.value = Array.isArray(result?.data) ? result.data.slice(0, 3) : []
  } catch {
    relatedCases.value = []
  }
}

onMounted(async () => {
  const tenderId = route.params.id
  try {
    const result = await tendersApi.getDetail(tenderId)
    if (result?.success) {
      tender.value = result.data
      isFollowed.value = result.data.status === 'TRACKING'
      loadAiAnalysis(tenderId)
      loadRelatedCases(result.data)
    } else {
      ElMessage.error(result?.message || '获取标讯详情失败')
    }
  } catch (error) {
    console.warn('Failed to fetch tender detail:', error)
    ElMessage.error('网络请求失败，请稍后重试')
  }
})

const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  return 'score-normal'
}

const getStatusType = (status) => {
  const map = {
    PENDING: 'info', TRACKING: 'warning', BIDDED: 'success', ABANDONED: 'danger',
    new: '', following: 'warning', bidding: 'primary'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    PENDING: '待处理', TRACKING: '跟踪中', BIDDED: '已投标', ABANDONED: '已放弃',
    new: '新建', following: '跟进中', bidding: '投标中'
  }
  return map[status] || status
}

const getDeadlineClass = (deadline) => {
  const today = new Date()
  const deadlineDate = new Date(deadline)
  const diffDays = Math.ceil((deadlineDate - today) / (1000 * 60 * 60 * 24))
  if (diffDays <= 3) return 'deadline-urgent'
  if (diffDays <= 7) return 'deadline-warning'
  return ''
}

const handleParticipate = () => {
  ElMessage.success('正在跳转到项目创建页...')
  router.push({
    path: '/project/create',
    query: { tenderId: tender.value.id }
  })
}

const handleFollow = async () => {
  try {
    const newStatus = isFollowed.value ? 'PENDING' : 'TRACKING'
    const result = await tendersApi.update(tender.value.id, { status: newStatus })
    if (result?.success) {
      isFollowed.value = !isFollowed.value
      ElMessage.success(isFollowed.value ? '已关注' : '已取消关注')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleShare = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    ElMessage.info('请手动复制当前页面链接')
  }
}

const handleViewOriginal = () => {
  if (tender.value && tender.value.originalUrl) {
    window.open(tender.value.originalUrl, '_blank')
  } else {
    ElMessage.warning('该标讯暂无官网公告链接')
  }
}

const handleViewCase = (caseId) => {
  router.push({
    path: '/knowledge/case/detail',
    query: { id: caseId }
  })
}
</script>

<style scoped>
.bidding-detail-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.breadcrumb-nav {
  margin-bottom: 20px;
}

.detail-content {
  max-width: 1200px;
  margin: 0 auto;
}

.info-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.header-left {
  flex: 1;
}

.tender-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.tags-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.header-right {
  flex-shrink: 0;
}

.ai-score-large {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.ai-score-large.score-excellent {
  background: linear-gradient(135deg, #67c23a, #85ce61);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.ai-score-large.score-good {
  background: linear-gradient(135deg, #e6a23c, #f0c78a);
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.3);
}

.ai-score-large.score-normal {
  background: linear-gradient(135deg, #909399, #b1b3b8);
  box-shadow: 0 4px 12px rgba(144, 147, 153, 0.3);
}

.score-value {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.score-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 2px;
}

.amount-text {
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
}

.deadline-urgent {
  color: #f56c6c;
  font-weight: 600;
}

.deadline-warning {
  color: #e6a23c;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.ai-analysis-card {
  margin-bottom: 20px;
}

.card-title-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.ai-icon {
  color: #409eff;
  font-size: 18px;
}

.analysis-content {
  padding: 8px 0;
}

.analysis-section {
  margin-bottom: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.match-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.match-bar-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
}

.bar-value {
  font-weight: 600;
  color: #303133;
}

.advantages-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.advantage-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  color: #606266;
}

.advantage-icon {
  color: #67c23a;
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 2px;
}

.suggestions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestions :deep(.el-alert) {
  --el-alert-padding: 12px 16px;
}

.suggestions :deep(.el-alert__content) {
  padding: 8px 0 0 0;
}

.suggestions :deep(.el-alert__content p) {
  margin: 0;
  font-size: 13px;
  color: #606266;
}

.related-cases-card {
  margin-bottom: 20px;
}

.cases-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.case-item {
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.case-item:hover {
  background: #ecf5ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.case-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.case-icon .el-icon {
  font-size: 24px;
  color: #fff;
}

.case-content {
  flex: 1;
  min-width: 0;
}

.case-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.case-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #909399;
}

.case-summary {
  font-size: 13px;
  color: #606266;
  margin: 0 0 12px 0;
  line-height: 1.6;
}

.case-highlights {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.case-arrow {
  display: flex;
  align-items: center;
  color: #c0c4cc;
  transition: color 0.3s;
}

.case-item:hover .case-arrow {
  color: #409eff;
}

.loading-container {
  padding: 40px;
  background: #fff;
  border-radius: 8px;
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
  background-color: #fafafa;
}

:deep(.el-divider) {
  margin: 20px 0;
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .bidding-detail-page {
    padding: 12px;
  }

  .detail-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
  }

  .header-left {
    width: 100%;
  }

  .detail-title {
    font-size: 18px;
  }

  .header-actions {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .header-actions .el-button {
    flex: 1;
    min-width: 100px;
  }

  /* 详情卡片移动端优化 */
  .detail-card {
    margin-bottom: 12px;
  }

  .detail-card :deep(.el-card__header) {
    padding: 12px 16px;
  }

  .detail-card :deep(.el-card__body) {
    padding: 16px;
  }

  /* 描述列表移动端优化 */
  :deep(.el-descriptions) {
    font-size: 12px;
  }

  :deep(.el-descriptions__label) {
    width: 100px !important;
  }

  /* 内容区域移动端优化 */
  .content-section {
    margin-bottom: 16px;
  }

  .content-title {
    font-size: 14px;
  }

  .content-text {
    font-size: 13px;
  }

  /* 相关案例移动端优化 */
  .case-item {
    padding: 12px;
  }

  .case-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  /* 对话框移动端优化 */
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }

  :deep(.el-dialog__body) {
    padding: 16px;
  }

  /* 标签移动端优化 */
  .info-tags {
    flex-wrap: wrap;
  }

  .info-tags .el-tag {
    margin-bottom: 6px;
  }

  /* AI评分移动端优化 */
  .ai-score-badge {
    font-size: 14px;
    padding: 4px 8px;
  }

  /* 面包屑导航移动端优化 */
  .breadcrumb-nav :deep(.el-breadcrumb__item) {
    font-size: 12px;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .header-actions .el-button,
  .action-buttons .el-button {
    min-height: 44px;
  }

  .case-item {
    min-height: 80px;
  }

  .case-item:active {
    background: #f5f7fa;
  }
}
</style>
