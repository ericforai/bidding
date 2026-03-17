<template>
  <div class="case-detail-page">
    <!-- 返回导航 -->
    <div class="breadcrumb-nav">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/knowledge/case' }">案例库</el-breadcrumb-item>
        <el-breadcrumb-item>案例详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 主要内容 -->
    <div v-if="caseData" class="detail-content">
      <!-- 案例基本信息卡片 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <h2 class="case-title">{{ caseData.title }}</h2>
              <div class="tags-row">
                <el-tag v-for="tag in caseData.tags" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
            </div>
            <div class="header-right">
              <el-button type="primary" @click="handleUseCase">
                <el-icon><DocumentCopy /></el-icon>
                引用此案例
              </el-button>
            </div>
          </div>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="客户名称">
            {{ caseData.customer }}
          </el-descriptions-item>
          <el-descriptions-item label="项目金额">
            <span class="amount-text">{{ caseData.amount }}万元</span>
          </el-descriptions-item>
          <el-descriptions-item label="项目年份">
            {{ caseData.year }}年
          </el-descriptions-item>
          <el-descriptions-item label="所属地区">
            {{ caseData.location }}
          </el-descriptions-item>
          <el-descriptions-item label="所属行业">
            {{ caseData.industry }}
          </el-descriptions-item>
          <el-descriptions-item label="实施周期">
            {{ caseData.period }}
          </el-descriptions-item>
          <el-descriptions-item label="引用次数" :span="3">
            <span class="use-count-text">
              <el-icon><View /></el-icon>
              已被引用 {{ caseData.useCount }} 次
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons">
          <el-button @click="handleEdit">
            <el-icon><Edit /></el-icon>
            编辑案例
          </el-button>
          <el-button @click="handleShare">
            <el-icon><Share /></el-icon>
            分享
          </el-button>
        </div>
      </el-card>

      <!-- 项目概述 -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <div class="card-title-with-icon">
            <el-icon><Document /></el-icon>
            项目概述
          </div>
        </template>
        <div class="content-text">
          {{ caseData.summary }}
        </div>
      </el-card>

      <!-- 项目亮点 -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <div class="card-title-with-icon">
            <el-icon><Star /></el-icon>
            项目亮点
          </div>
        </template>
        <div class="highlights-list">
          <div v-for="(highlight, index) in caseData.highlights" :key="index" class="highlight-item">
            <el-icon class="highlight-icon"><CircleCheckFilled /></el-icon>
            <span>{{ highlight }}</span>
          </div>
        </div>
      </el-card>

      <!-- 技术架构 -->
      <el-card v-if="caseData.technologies" class="section-card" shadow="never">
        <template #header>
          <div class="card-title-with-icon">
            <el-icon><Cpu /></el-icon>
            技术架构
          </div>
        </template>
        <div class="tech-tags">
          <el-tag
            v-for="tech in caseData.technologies"
            :key="tech"
            type="info"
            effect="plain"
            size="large"
          >
            {{ tech }}
          </el-tag>
        </div>
      </el-card>

      <!-- 相关案例推荐 -->
      <el-card class="related-cases-card" shadow="never">
        <template #header>
          <div class="card-title-with-icon">
            <el-icon><Briefcase /></el-icon>
            相关案例
          </div>
        </template>
        <div class="related-cases-list">
          <div
            v-for="relatedCase in relatedCases"
            :key="relatedCase.id"
            class="related-case-item"
            @click="handleViewRelated(relatedCase.id)"
          >
            <div class="related-case-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="related-case-content">
              <h5 class="related-case-title">{{ relatedCase.title }}</h5>
              <div class="related-case-meta">
                <span>{{ relatedCase.customer }}</span>
                <span>{{ relatedCase.amount }}万元</span>
              </div>
            </div>
            <div class="related-case-arrow">
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

    <!-- 空状态 -->
    <div v-if="!caseData && !loading" class="empty-container">
      <el-empty description="案例不存在或已删除">
        <el-button type="primary" @click="router.push('/knowledge/case')">
          返回案例列表
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DocumentCopy,
  Edit,
  Share,
  Document,
  Star,
  Cpu,
  Briefcase,
  ArrowRight,
  View,
  CircleCheckFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { knowledgeApi, isMockMode } from '@/api'
import { loadDemoState, saveDemoState } from '@/utils/demoPersistence'

const router = useRouter()
const route = useRoute()
const CASE_STORAGE_KEY = 'knowledge-case-overrides'

const caseData = ref(null)
const loading = ref(false)
const relatedCasePool = ref([])
const isEdited = ref(false)
const shareRecords = ref([])
const referenceRecords = ref([])

// 相关案例推荐
const relatedCases = computed(() => {
  if (!caseData.value) return []

  return relatedCasePool.value
    .filter(c => String(c.id) !== String(caseData.value.id) && c.industry === caseData.value.industry)
    .slice(0, 3)
    .map(c => ({
      id: c.id,
      title: c.title,
      customer: c.customer,
      amount: c.amount,
      year: c.year
    }))
})

const loadCasePersistence = () => loadDemoState(CASE_STORAGE_KEY, {})

const applyCasePersistence = (data) => {
  if (!isMockMode()) {
    return data
  }
  const persisted = loadCasePersistence()[String(data.id)]
  return persisted ? { ...data, ...persisted } : data
}

const persistCasePatch = (caseId, patch) => {
  const state = loadCasePersistence()
  state[String(caseId)] = {
    ...(state[String(caseId)] || {}),
    ...patch,
  }
  saveDemoState(CASE_STORAGE_KEY, state)
}

const getCurrentUser = () => {
  try {
    const raw = sessionStorage.getItem('user') || localStorage.getItem('user')
    return raw ? JSON.parse(raw) : null
  } catch (error) {
    return null
  }
}

onMounted(() => {
  const caseId = route.query.id || route.params.id
  if (caseId) {
    loadCaseDetail(caseId)
  } else {
    ElMessage.warning('缺少案例ID参数')
    router.push('/knowledge/case')
  }
})

const loadCaseDetail = async (caseId) => {
  loading.value = true
  try {
    const [detailResult, listResult] = await Promise.all([
      knowledgeApi.cases.getDetail(caseId),
      knowledgeApi.cases.getList()
    ])

    if (!detailResult?.success || !detailResult?.data) {
      caseData.value = null
      relatedCasePool.value = []
      return
    }

    const found = detailResult.data
    caseData.value = applyCasePersistence({
      ...found,
      location: found.location || '北京',
      period: found.period || '6个月',
      useCount: found.useCount || (isMockMode() ? Math.floor(Math.random() * 20) + 1 : 0),
      viewCount: found.viewCount || (isMockMode() ? Math.floor(Math.random() * 100) + 50 : 0),
      technologies: found.technologies || ['Vue.js', 'Spring Boot', 'PostgreSQL', 'Redis']
    })
    isEdited.value = caseData.value.title?.includes('（已编辑）') || false

    relatedCasePool.value = listResult?.success && Array.isArray(listResult.data)
      ? listResult.data
      : []

    if (!isMockMode() && /^\d+$/.test(String(caseId))) {
      const [shareResult, referenceResult] = await Promise.all([
        knowledgeApi.cases.getShareRecords(caseId),
        knowledgeApi.cases.getReferenceRecords(caseId)
      ])
      shareRecords.value = shareResult?.success && Array.isArray(shareResult.data) ? shareResult.data : []
      referenceRecords.value = referenceResult?.success && Array.isArray(referenceResult.data) ? referenceResult.data : []
    } else {
      shareRecords.value = []
      referenceRecords.value = []
    }
  } catch (error) {
    console.error('Failed to load case detail:', error)
    ElMessage.error('加载案例详情失败')
    caseData.value = null
    relatedCasePool.value = []
  } finally {
    loading.value = false
  }
}

const handleUseCase = () => {
  if (!caseData.value) return

  const currentUser = getCurrentUser()
  const payload = {
    referencedBy: currentUser?.id ?? null,
    referencedByName: currentUser?.name || currentUser?.username || '当前用户',
    referenceTarget: '案例详情页手动引用',
    referenceContext: '从案例详情页发起引用'
  }

  knowledgeApi.cases.createReferenceRecord(caseData.value.id, payload).then((result) => {
    if (!result?.success) {
      ElMessage.error(result?.message || '案例引用失败')
      return
    }

    referenceRecords.value = [result.data, ...referenceRecords.value]
    caseData.value = {
      ...caseData.value,
      useCount: Number(caseData.value.useCount || 0) + 1
    }
    if (isMockMode()) {
      persistCasePatch(caseData.value.id, { useCount: caseData.value.useCount })
    }
    ElMessage.success('案例已添加到引用列表')
  }).catch(() => {
    ElMessage.error('案例引用失败')
  })
}

const handleEdit = () => {
  if (!caseData.value) return
  isEdited.value = true
  const nextTitle = caseData.value.title.includes('（已编辑）') ? caseData.value.title : `${caseData.value.title}（已编辑）`
  const nextSummary = `${caseData.value.summary}\n\n[演示更新] 已补充客户价值、实施路径与关键交付成果。`
  const nextData = {
    ...caseData.value,
    title: nextTitle,
    summary: nextSummary,
    description: nextSummary
  }

  knowledgeApi.cases.update(caseData.value.id, {
    ...nextData,
    customerName: nextData.customer,
    locationName: nextData.location,
    projectPeriod: nextData.period
  }).then((result) => {
    if (!result?.success || !result?.data) {
      ElMessage.error(result?.message || '案例更新失败')
      return
    }

    caseData.value = applyCasePersistence({
      ...result.data,
      summary: result.data.summary || nextSummary
    })
    if (isMockMode()) {
      persistCasePatch(caseData.value.id, {
        title: caseData.value.title,
        summary: caseData.value.summary,
        description: caseData.value.description
      })
    }
    ElMessage.success('案例内容已更新')
  }).catch(() => {
    ElMessage.error('案例更新失败')
  })
}

const handleShare = () => {
  if (!caseData.value) return
  const currentUser = getCurrentUser()
  knowledgeApi.cases.createShareRecord(caseData.value.id, {
    createdBy: currentUser?.id ?? null,
    createdByName: currentUser?.name || currentUser?.username || '当前用户',
    baseUrl: window.location.origin
  }).then((result) => {
    if (!result?.success || !result?.data?.url) {
      ElMessage.error(result?.message || '分享失败')
      return
    }

    shareRecords.value = [result.data, ...shareRecords.value]
    navigator.clipboard.writeText(result.data.url).then(() => {
      ElMessage.success('分享链接已复制到剪贴板')
    }).catch(() => {
      ElMessage.success(result.data.url)
    })
  }).catch(() => {
    ElMessage.error('分享失败')
  })
}

const handleViewRelated = (relatedId) => {
  // 跳转到相关案例详情页
  router.push({ path: '/knowledge/case/detail', query: { id: relatedId } })
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<style scoped>
.case-detail-page {
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

.case-title {
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

.amount-text {
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
}

.use-count-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #409eff;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.section-card {
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

.card-title-with-icon .el-icon {
  color: #409eff;
  font-size: 18px;
}

.content-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-line;
}

.highlights-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.highlight-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  color: #606266;
}

.highlight-icon {
  color: #67c23a;
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 2px;
}

.tech-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.related-cases-card {
  margin-bottom: 20px;
}

.related-cases-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.related-case-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.related-case-item:hover {
  background: #ecf5ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.related-case-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.related-case-icon .el-icon {
  font-size: 20px;
  color: #fff;
}

.related-case-content {
  flex: 1;
  min-width: 0;
}

.related-case-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.related-case-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.related-case-arrow {
  color: #c0c4cc;
  transition: color 0.3s;
}

.related-case-item:hover .related-case-arrow {
  color: #409eff;
}

.loading-container,
.empty-container {
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

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .case-detail-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-right {
    width: 100%;
  }

  .header-right .el-button {
    width: 100%;
  }

  .case-title {
    font-size: 18px;
  }

  .action-buttons {
    flex-wrap: wrap;
  }

  .action-buttons .el-button {
    flex: 1;
    min-width: 120px;
  }

  :deep(.el-descriptions) {
    font-size: 12px;
  }

  :deep(.el-descriptions__label) {
    width: 100px !important;
  }
}
</style>
