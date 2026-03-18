<template>
  <div class="customer-opportunity-page" :class="{ 'is-loading': loading }">
    <div class="page-header">
      <div class="header-text">
        <h2 class="animate-fade-in">客户商机中心</h2>
        <p class="animate-fade-in-delay">基于销售情报（Sales Intelligence）的客户经营视图，智能研判历史规律与潜在商机。</p>
      </div>
      <div class="header-actions">
        <el-button
          @click="refreshInsights"
          :loading="isMockMode && isScanning"
          :disabled="!isMockMode"
          class="btn-refresh"
        >
          <el-icon><Refresh /></el-icon>
          {{ isMockMode ? '刷新洞察' : '洞察未接入' }}
        </el-button>
        <el-button type="primary" class="btn-primary" @click="createProject" v-if="selectedCustomer">
          {{ selectedCustomer.prediction.convertedProjectId ? '查看项目' : '转为正式项目' }}
        </el-button>
      </div>
    </div>

    <!-- AI Scanning Overlay -->
    <transition name="fade">
      <div v-if="isMockMode && isScanning" class="scanning-overlay">
        <div class="scan-grid"></div>
        <div class="scan-line"></div>
        <div class="scan-content">
          <div class="hologram-box">
            <el-icon class="rotating"><Refresh /></el-icon>
          </div>
          <h3>AI 引擎正在分析全域数据...</h3>
          <p>正在研判采购规律 · 识别机会评分 · 测算预算窗口</p>
        </div>
      </div>
    </transition>

    <!-- Skeleton Screen for Top Board -->
    <template v-if="loading">
      <div class="top-board">
        <el-skeleton v-for="i in 4" :key="i" animated>
          <template #template>
            <div class="board-card skeleton-card">
              <el-skeleton-item variant="text" style="width: 50%" />
              <el-skeleton-item variant="h3" style="width: 80%; margin-top: 12px" />
              <el-skeleton-item variant="text" style="width: 60%; margin-top: 8px" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </template>

    <div v-else class="top-board">
      <div class="board-card hover-lift" v-for="item in boardSummaries" :key="item.label">
        <div class="card-label">
          <span>{{ item.label }}</span>
          <el-tag size="small" :type="item.tagType" effect="light" class="tag-glow">{{ item.tag }}</el-tag>
        </div>
        <div class="card-main">
          <div class="card-value">{{ item.value }}</div>
          <div class="card-trend" :class="item.placeholder ? 'neutral' : (item.isUp ? 'up' : 'down')">
            <template v-if="item.placeholder">
              <el-tag size="small" type="info" effect="plain">{{ item.trendLabel || '未接入' }}</el-tag>
            </template>
            <template v-else>
              <el-icon><CaretTop v-if="item.isUp" /><CaretBottom v-else /></el-icon>
              {{ item.trend }}%
            </template>
          </div>
        </div>
        <div class="spark-box">
          <div class="spark-line" :class="item.tagType"></div>
        </div>
        <p class="card-note">{{ item.note }}</p>
      </div>
    </div>

    <div class="content-grid">
      <section class="customer-list-panel">
        <div class="panel-header search-integrated">
          <div class="panel-title">
            <el-icon class="title-icon"><User /></el-icon>
            <h3>客户池</h3>
          </div>
          <div class="header-filters multi-row">
            <div class="filter-row">
              <el-input
                v-model="filters.keyword"
                placeholder="搜索名称..."
                clearable
                size="default"
                :disabled="!isMockMode"
                class="search-input"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-select v-model="filters.sales" placeholder="销售负责人" size="default" clearable :disabled="!isMockMode" class="filter-item">
                <el-option label="全部销售" value="" />
                <el-option v-for="user in salesUsers" :key="user.id" :label="user.name" :value="user.name" />
              </el-select>
            </div>
            <div class="filter-row">
              <el-select v-model="filters.region" placeholder="全部地区" size="default" clearable :disabled="!isMockMode" class="filter-item">
                <el-option v-for="region in regions" :key="region" :label="region" :value="region" />
              </el-select>
              <el-select v-model="filters.industry" placeholder="全部行业" size="default" clearable :disabled="!isMockMode" class="filter-item">
                <el-option v-for="ind in industries" :key="ind" :label="ind" :value="ind" />
              </el-select>
              <el-select v-model="filters.status" placeholder="全部分类" size="default" clearable :disabled="!isMockMode" class="filter-item">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
          </div>
        </div>
        
        <el-skeleton :loading="loading" animated :rows="10">
          <el-table
            :data="filteredCustomers"
            size="default"
            row-key="customerId"
            @row-click="selectCustomer"
            :row-class-name="rowClass"
            class="premium-table"
          >
            <template #empty>
              <el-empty :description="isMockMode ? '暂无符合条件的客户' : '客户商机数据源未接入，当前仅保留演示模式'" />
            </template>
            <el-table-column prop="customerName" label="客户名称" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="customer-name-cell">
                  <strong>{{ row.customerName }}</strong>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="region" label="地区" width="100" show-overflow-tooltip />
            <el-table-column prop="industry" label="行业" width="100" show-overflow-tooltip />
            <el-table-column prop="salesRep" label="销售负责人" width="140" show-overflow-tooltip />
            <el-table-column prop="opportunityScore" label="机会评分" width="110" align="center">
              <template #default="{ row }">
                <div class="score-container">
                  <span class="score-num" :class="getScoreClass(row.opportunityScore)">{{ row.opportunityScore }}</span>
                  <el-progress 
                    :percentage="row.opportunityScore" 
                    :show-text="false" 
                    :stroke-width="4"
                    :color="getScoreColor(row.opportunityScore)"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="predictedNextWindow" label="预测窗口" width="140" align="center">
              <template #default="{ row }">
                <span class="window-tag">{{ row.predictedNextWindow }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-skeleton>
      </section>

      <section class="customer-detail-panel">
        <el-skeleton :loading="loading" animated :rows="15">
          <div v-if="selectedCustomer" class="detail-container scrollable">
            <!-- Customer Brief -->
            <div class="detail-header-card">
              <div class="header-main">
                <div class="avatar-box">
                  {{ selectedCustomer.customerName.charAt(0) }}
                </div>
                <div class="info-box">
                  <div class="info-top">
                    <h3>{{ selectedCustomer.customerName }}</h3>
                    <el-tag size="small" :type="getStatusType(selectedCustomer.status)" effect="dark">
                      {{ getStatusLabel(selectedCustomer.status) }}
                    </el-tag>
                  </div>
                  <p class="info-sub">{{ selectedCustomer.industry }} · {{ selectedCustomer.region }}</p>
                </div>
              </div>
              <div class="header-actions">
                 <el-button link type="primary" @click="historyDrawer = true">购买全记录</el-button>
              </div>
            </div>

            <!-- Profiling Grid -->
            <div class="glass-section">
              <h4 class="section-title">客户画像</h4>
              <div class="profiling-grid">
                <div class="profiling-item">
                  <span class="label">主要经营品类</span>
                  <div class="tags-row">
                    <el-tag v-for="cat in selectedCustomer.mainCategories" :key="cat" size="small" effect="plain" class="m-1">{{ cat }}</el-tag>
                  </div>
                </div>
                <div class="profiling-item">
                   <span class="label">平均预算规模</span>
                   <p class="value">¥ {{ selectedCustomer.avgBudget }} <small>万元</small></p>
                </div>
                <div class="profiling-item">
                   <span class="label">采购周期特征</span>
                   <p class="value">{{ selectedCustomer.cycleType }}</p>
                </div>
              </div>
            </div>

            <!-- Prediction Insights -->
            <div class="insight-section">
              <div class="section-header">
                <h4 class="section-title">智能商机研判</h4>
                <div class="confidence-badge" :style="{ color: confidenceColor(normalizeConfidence(selectedCustomer.prediction.confidence)) }">
                  可信度 {{ normalizeConfidence(selectedCustomer.prediction.confidence) }}%
                </div>
              </div>
              
              <div class="prediction-card">
                <div class="pred-grid">
                  <div class="pred-item highlight">
                    <span class="label">预测项目名称</span>
                    <p>{{ selectedCustomer.prediction.suggestedProjectName }}</p>
                  </div>
                  <div class="pred-item">
                    <span class="label">预测品类</span>
                    <p>{{ selectedCustomer.prediction.predictedCategory }}</p>
                  </div>
                  <div class="pred-item">
                    <span class="label">预测时间窗口</span>
                    <p>{{ selectedCustomer.prediction.predictedWindow }}</p>
                  </div>
                  <div class="pred-item">
                    <span class="label">预测预算</span>
                    <p>¥ {{ selectedCustomer.prediction.predictedBudgetMin }} - {{ selectedCustomer.prediction.predictedBudgetMax }} <small>万</small></p>
                  </div>
                </div>
                <div class="reason-box">
                  <el-icon><InfoFilled /></el-icon>
                  <span>{{ selectedCustomer.prediction.reasoningSummary }}</span>
                </div>
              </div>
            </div>

            <!-- Purchase Patterns -->
            <div class="glass-section purchase-patterns">
              <h4 class="section-title">近一年采购规律</h4>
              <div class="timeline-container">
                <el-timeline>
                  <el-timeline-item
                    v-for="record in selectedCustomer.purchaseHistory.slice(0, 3)"
                    :key="record.recordId"
                    :timestamp="record.publishDate"
                    :type="record.isKey ? 'primary' : ''"
                  >
                    <div class="timeline-content">
                      <p class="t-title">{{ record.title }}</p>
                      <div class="t-meta">
                        <span>{{ record.category }}</span>
                        <span class="divider"></span>
                        <span>¥{{ record.budget }}万</span>
                      </div>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </div>
              <p class="insight-summary">
                <el-icon><MagicStick /></el-icon>
                {{ selectedCustomer.predictionSummary }}
              </p>
            </div>
          </div>
          <div v-else-if="isMockMode" class="smart-onboarding">
            <div class="onboarding-content">
              <div class="ai-avatar-large shadow-glow">
                <el-icon><MagicStick /></el-icon>
              </div>
              <h2>欢迎访问商机中心</h2>
              <p>我是您的 AI 销售助理。我已经为您分析了最新的市场动向与采购规律。</p>
              
              <div class="onboarding-suggestions">
                <div class="suggest-title">您可以尝试：</div>
                <div class="suggest-cards">
                  <div class="s-card" @click="selectFirstHighValue">
                    <el-icon><Star /></el-icon>
                    <span>查看高价值潜力客户</span>
                  </div>
                  <div class="s-card" @click="filters.status = 'recommend'">
                    <el-icon><TrendCharts /></el-icon>
                    <span>筛选建议立项的机会</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="api-empty-state">
            <div class="api-empty-card">
              <el-tag size="small" type="info" effect="light" class="api-empty-tag">API 模式</el-tag>
              <h2>客户商机中心暂未接入真实数据源</h2>
              <p>当前页面仅保留 mock 演示链路。真实模式下不会读取 demo 数据，也不会模拟 AI 洞察或扫描成功态。</p>
              <div class="api-empty-actions">
                <el-button disabled>刷新洞察</el-button>
                <el-button link type="primary" @click="router.push('/bidding')">返回标讯中心</el-button>
              </div>
            </div>
          </div>
        </el-skeleton>
      </section>
    </div>

    <el-drawer v-model="historyDrawer" title="历史采购全景图" size="600px" class="premium-drawer">
      <div v-if="selectedCustomer && customerHistory.length" class="panoramic-view">
        <!-- Dashboard Stats -->
        <div class="panoramic-stats">
          <div class="stat-card blue">
            <span class="stat-label">累计采购项目</span>
            <p class="stat-value">{{ drawerStats.totalCount }}</p>
          </div>
          <div class="stat-card purple">
            <span class="stat-label">累计预算总额</span>
            <p class="stat-value">¥{{ drawerStats.totalBudget }}<small>万</small></p>
          </div>
          <div class="stat-card green">
            <span class="stat-label">首选品类</span>
            <p class="stat-value">{{ drawerStats.topCategory }}</p>
          </div>
        </div>

        <!-- Category Distribution Visualization -->
        <div class="panoramic-section">
          <h4 class="section-title">品类购买频率分布</h4>
          <div class="category-bars">
            <div v-for="cat in categoryStats" :key="cat.name" class="cat-bar-item">
              <div class="cat-info">
                <span>{{ cat.name }}</span>
                <span>{{ cat.count }}次 ({{ cat.percent }}%)</span>
              </div>
              <div class="cat-progress-bg">
                <div class="cat-progress-fill" :style="{ width: cat.percent + '%', backgroundColor: cat.color }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- History Timeline -->
        <div class="panoramic-section">
          <h4 class="section-title">采购历史全轨迹</h4>
          <div class="history-scroll-list">
            <div class="history-item-card" v-for="record in customerHistory" :key="record.recordId">
              <div class="h-item-top">
                <el-tag size="small" :type="record.budget > 500 ? 'danger' : 'info'" effect="light">¥{{ record.budget }}万</el-tag>
                <span class="h-item-date">{{ record.publishDate }}</span>
              </div>
              <p class="h-item-title">{{ record.title }}</p>
              <div class="h-item-footer">
                <span class="h-item-cat"><el-icon><MagicStick /></el-icon> {{ record.category }}</span>
                <div class="h-item-tags">
                  <span v-for="tag in record.extractedTags" :key="tag" class="micro-tag">{{ tag }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="panoramic-empty">
        <el-empty description="该客户暂无更多历史记录" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, User, InfoFilled, MagicStick, Search, CaretTop, CaretBottom, Star, TrendCharts } from '@element-plus/icons-vue'
import { isMockMode as getIsMockMode } from '@/api/config.js'
import { useCustomerOpportunityCenterData } from '@/api/modules/customerOpportunity.js'

const router = useRouter()
const loading = ref(true)
const isMockMode = getIsMockMode()
const { customerInsights, customerPurchases, customerPredictions, salesUsers } = useCustomerOpportunityCenterData()
const filters = ref({ status: '', keyword: '', sales: '', region: '', industry: '' })

const regions = computed(() => [...new Set(customerInsights.value.map(c => c.region))].filter(Boolean))
const industries = computed(() => [...new Set(customerInsights.value.map(c => c.industry))].filter(Boolean))
const statusOptions = [
  { label: '待判断机会', value: 'watch' },
  { label: '建议转项目', value: 'recommend' },
  { label: '已转化项目', value: 'converted' }
]
const activeCustomerId = ref('')
const historyDrawer = ref(false)
const isScanning = ref(false)

onMounted(() => {
  const delay = isMockMode ? 800 : 220
  setTimeout(() => {
    loading.value = false
  }, delay)
})

const customerHistory = computed(() => {
  if (!selectedCustomer.value) return []
  return customerPurchases.value.filter((p) => p.customerId === selectedCustomer.value.customerId)
    .sort((a, b) => new Date(b.publishDate) - new Date(a.publishDate))
})

const drawerStats = computed(() => {
  const history = customerHistory.value
  const totalCount = history.length
  const totalBudget = history.reduce((sum, item) => sum + (item.budget || 0), 0)
  
  const cats = {}
  history.forEach(item => {
    cats[item.category] = (cats[item.category] || 0) + 1
  })
  const topCategory = Object.entries(cats).sort((a, b) => b[1] - a[1])[0]?.[0] || '未知'
  
  return { totalCount, totalBudget, topCategory }
})

const categoryStats = computed(() => {
  const history = customerHistory.value
  const total = history.length
  if (!total) return []
  
  const cats = {}
  history.forEach(item => {
    cats[item.category] = (cats[item.category] || 0) + 1
  })
  
  const colors = ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#64748b']
  return Object.entries(cats)
    .sort((a, b) => b[1] - a[1])
    .map(([name, count], index) => ({
      name,
      count,
      percent: Math.round((count / total) * 100),
      color: colors[index % colors.length]
    }))
})

const boardSummaries = computed(() => {
  if (!isMockMode) {
    return [
      { label: '客户池', value: '--', note: '真实客户数据源未接入', tag: '未接入', tagType: 'info', placeholder: true, trendLabel: 'API' },
      { label: '采购记录', value: '--', note: '历史采购服务仅在演示模式可见', tag: '未接入', tagType: 'info', placeholder: true, trendLabel: 'API' },
      { label: '预测商机', value: '--', note: '预测结果不会在真实模式下伪造', tag: '未接入', tagType: 'info', placeholder: true, trendLabel: 'API' },
      { label: '项目转化', value: '--', note: '转项目链路需在 mock 模式体验', tag: '未接入', tagType: 'info', placeholder: true, trendLabel: 'API' }
    ]
  }
  const customers = customerInsights.value
  const predictions = customerPredictions.value
  const highValueCount = customers.filter(item => item.opportunityScore >= 85).length
  const shortTermCount = predictions.filter(item => /^2025-0[3-4]/.test(item.predictedWindow)).length
  const midTermCount = predictions.filter(item => /^2025-0[5-6]/.test(item.predictedWindow)).length
  const convertedCount = predictions.filter(item => item.convertedProjectId).length

  return [
    { label: '高价值客户', value: String(highValueCount), note: '核心经营资产', tag: '重点', tagType: 'success', trend: 12, isUp: true },
    { label: '30D 预测机会', value: String(shortTermCount), note: '需近期重点研判', tag: '紧迫', tagType: 'danger', trend: 8, isUp: true },
    { label: '远期潜客', value: String(midTermCount), note: '适合关系铺垫', tag: '观察', tagType: 'warning', trend: 3, isUp: false },
    { label: '已转化', value: String(convertedCount), note: '已转正式项目池', tag: '完成', tagType: 'info', trend: 20, isUp: true }
  ]
})

const filteredCustomers = computed(() =>
  customerInsights.value.filter((customer) => {
    if (filters.value.status && customer.status !== filters.value.status) {
      return false
    }
    if (filters.value.keyword && !customer.customerName.toLowerCase().includes(filters.value.keyword.toLowerCase())) {
      return false
    }
    if (filters.value.sales && customer.salesRep !== filters.value.sales) {
      return false
    }
    if (filters.value.region && customer.region !== filters.value.region) {
      return false
    }
    if (filters.value.industry && customer.industry !== filters.value.industry) {
      return false
    }
    return true
  })
)

const selectedCustomer = computed(() => {
  const baseCustomer = customerInsights.value.find((item) => item.customerId === activeCustomerId.value)
  if (!baseCustomer) return null

  const purchaseHistory = customerPurchases.value.filter((item) => item.customerId === baseCustomer.customerId)
  const prediction = customerPredictions.value.find((item) => item.customerId === baseCustomer.customerId)

  return {
    ...baseCustomer,
    purchaseHistory,
    prediction: prediction || {
      opportunityId: '',
      suggestedProjectName: '待智能研判',
      predictedCategory: '---',
      predictedBudgetMin: 0,
      predictedBudgetMax: 0,
      predictedWindow: '待判断',
      confidence: 0,
      reasoningSummary: '当前数据不足，暂无法生成高置信度预测。',
      evidenceRecords: [],
      convertedProjectId: ''
    }
  }
})

const selectCustomer = (row) => {
  activeCustomerId.value = row.customerId
}

const rowClass = ({ row }) => (row.customerId === activeCustomerId.value ? 'row-active' : '')

const confidenceColor = (value) => (value >= 80 ? '#10b981' : value >= 60 ? '#f59e0b' : '#3b82f6')
const getScoreColor = (score) => (score >= 80 ? '#10b981' : score >= 60 ? '#f59e0b' : '#64748b')
const getScoreClass = (score) => (score >= 80 ? 'high' : score >= 60 ? 'mid' : 'low')
const normalizeConfidence = (score) => Math.max(0, Math.min(100, Math.round(Number(score || 0) * 100)))

const getStatusLabel = (status) => {
  const statusMap = {
    watch: '待研判',
    recommend: '商机推荐',
    converted: '已立项'
  }
  return statusMap[status] || '待研判'
}

const getStatusType = (status) => {
  const statusTypeMap = {
    watch: 'info',
    recommend: 'success',
    converted: 'warning'
  }
  return statusTypeMap[status] || 'info'
}

const buildDeadlineFromWindow = (windowValue) => {
  if (!windowValue) return ''
  if (/^\d{4}-\d{2}$/.test(windowValue)) {
    return `${windowValue}-28`
  }
  return ''
}

const refreshInsights = () => {
  if (!isMockMode) {
    ElMessage.info('客户商机中心在真实模式下暂未接入数据源')
    return
  }
  isScanning.value = true
  setTimeout(() => {
    isScanning.value = false
    ElMessage.success('AI 智能洞察已同步至最新')
  }, 2500)
}

const selectFirstHighValue = () => {
  if (!isMockMode) return
  const first = customerInsights.value.find(c => c.opportunityScore >= 85)
  if (first) activeCustomerId.value = first.customerId
}

const createProject = () => {
  if (!selectedCustomer.value || !isMockMode) return

  if (selectedCustomer.value.prediction.convertedProjectId) {
    router.push(`/project/${selectedCustomer.value.prediction.convertedProjectId}`)
    return
  }

  const averageBudget = Math.round(
    (Number(selectedCustomer.value.prediction.predictedBudgetMin || 0) +
      Number(selectedCustomer.value.prediction.predictedBudgetMax || 0)) / 2
  )

  router.push({
    path: '/project/create',
    query: {
      projectName: selectedCustomer.value.prediction.suggestedProjectName,
      customerName: selectedCustomer.value.customerName,
      industry: selectedCustomer.value.industry,
      region: selectedCustomer.value.region,
      budget: String(averageBudget),
      deadline: buildDeadlineFromWindow(selectedCustomer.value.prediction.predictedWindow),
      tags: selectedCustomer.value.mainCategories.join(','),
      description: `基于历史采购规律预测，建议围绕“${selectedCustomer.value.prediction.predictedCategory}”提前立项跟进。`,
      remark: `预测时间窗口：${selectedCustomer.value.prediction.predictedWindow}；置信度：${normalizeConfidence(selectedCustomer.value.prediction.confidence)}%`,
      sourceModule: 'customer-opportunity-center',
      sourceCustomerId: selectedCustomer.value.customerId,
      sourceCustomerName: selectedCustomer.value.customerName,
      sourceOpportunityId: selectedCustomer.value.prediction.opportunityId,
      sourceReasoningSummary: selectedCustomer.value.prediction.reasoningSummary
    }
  })
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');

.customer-opportunity-page {
  padding: var(--space-6, 24px);
  min-height: 100vh;
  background: #f8fafc;
  font-family: 'Plus Jakarta Sans', -apple-system, system-ui, sans-serif;
  color: #0f172a;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-8, 32px);
}

.header-text h2 {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.02em;
}

.header-text p {
  color: #64748b;
  margin: 8px 0 0;
  font-size: 15px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-refresh {
  background: white;
  border-color: #e2e8f0;
  color: #475569;
  font-weight: 500;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-refresh:hover {
  background: #f1f5f9;
  transform: translateY(-1px);
}

/* Top Board */
.top-board {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.board-card {
  background: white;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.board-card.hover-lift:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 30px -8px rgba(3, 105, 161, 0.15);
  border-color: #bae6fd;
}

.card-main {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 16px 0 4px;
}

.card-trend {
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 2px;
}

.card-trend.up { color: #10b981; }
.card-trend.down { color: #f43f5e; }
.card-trend.neutral { color: #64748b; }

.spark-box {
  height: 32px;
  margin-bottom: 12px;
  display: flex;
  align-items: flex-end;
}

.spark-line {
  height: 4px;
  width: 100%;
  border-radius: 2px;
  background: #f1f5f9;
  position: relative;
  overflow: hidden;
}

.spark-line::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 60%;
  border-radius: 2px;
  animation: spark-slide 2s ease-in-out infinite alternate;
}

.spark-line.success::after { background: #10b981; }
.spark-line.danger::after { background: #f43f5e; }
.spark-line.warning::after { background: #f59e0b; }
.spark-line.info::after { background: #3b82f6; }

@keyframes spark-slide {
  from { transform: translateX(-20%); }
  to { transform: translateX(120%); }
}

.board-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: transparent;
  transition: background 0.3s;
}

.board-card:hover::after {
  background: #0369a1;
}

.card-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
}

.card-value {
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
  margin: 16px 0 8px;
  font-feature-settings: "tnum";
}

.card-note {
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
}

.tag-glow {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* Content Layout */
.content-grid {
  display: grid;
  grid-template-columns: 800px 1fr;
  gap: 24px;
  height: calc(100vh - 280px);
  min-height: 600px;
}

.customer-list-panel,
.customer-detail-panel {
  background: white;
  border-radius: 20px;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
}

.panel-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header.search-integrated {
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
  padding: 16px 20px;
}

.header-filters.multi-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-row {
  display: flex;
  gap: 8px;
}

.search-input {
  flex: 2;
}

.filter-item {
  flex: 1;
  min-width: 120px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 20px;
  color: #0369a1;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

/* Table Style */
.premium-table {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #f8fafc;
}

.customer-name-cell {
  padding: 4px 0;
}

.customer-name-cell strong {
  display: block;
  font-size: 14px;
  color: #1e293b;
}

.score-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.score-num {
  font-size: 13px;
  font-weight: 600;
  font-feature-settings: "tnum";
}

.score-num.high { color: #10b981; }
.score-num.mid { color: #f59e0b; }
.score-num.low { color: #64748b; }

.window-tag {
  font-size: 12px;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  color: #475569;
  white-space: nowrap;
}

.row-active {
  background-color: #f0f9ff !important;
}

.row-active td {
  border-left: 3px solid #0369a1;
}

/* Detail Panel */
.detail-container {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.detail-header-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-main {
  display: flex;
  gap: 16px;
  align-items: center;
}

.avatar-box {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.2);
}

.info-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-top h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.info-sub {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 14px;
}

.glass-section {
  background: #f8fafc;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid #f1f5f9;
}

.section-title {
  margin: 0 0 16px;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #94a3b8;
  font-weight: 600;
}

.profiling-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.label {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.profiling-item .value {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.profiling-item .value small {
  font-size: 12px;
  font-weight: 400;
}

/* Prediction Section */
.insight-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.confidence-badge {
  font-size: 13px;
  font-weight: 700;
  padding: 4px 12px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
}

.prediction-card {
  background: linear-gradient(to bottom right, #ffffff, #f0f9ff);
  border: 1px solid #bae6fd;
  border-radius: 16px;
  padding: 24px;
}

.pred-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 20px;
}

.pred-item.highlight p {
  color: #0369a1;
  font-size: 20px;
  font-weight: 700;
}

.pred-item p {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.reason-box {
  background: rgba(3, 105, 161, 0.05);
  padding: 12px 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #0369a1;
  font-size: 14px;
  line-height: 1.5;
}

/* Timeline */
.timeline-container {
  margin-top: 16px;
}

.timeline-content .t-title {
  font-weight: 600;
  margin: 0 0 4px;
  font-size: 14px;
}

.t-meta {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
}

.divider {
  width: 1px;
  height: 10px;
  background: #cbd5e1;
  margin: 0 8px;
}

.insight-summary {
  margin: 20px 0 0;
  padding-top: 20px;
  border-top: 1px dashed #e2e8f0;
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  font-style: italic;
  display: flex;
  gap: 8px;
}

.insight-summary .el-icon {
  color: #10b981;
  font-size: 18px;
  flex-shrink: 0;
}

/* Animations */
.animate-fade-in {
  animation: fadeIn 0.6s ease-out;
}

.animate-fade-in-delay {
  animation: fadeIn 0.6s ease-out 0.2s both;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Skeleton refinements */
.skeleton-card {
  border: 1px solid #f1f5f9;
}

/* Panoramic Drawer Styles */
.panoramic-view {
  padding: 0 4px;
}

.panoramic-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 16px 12px;
  border-radius: 12px;
  color: white;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card.blue { background: linear-gradient(135deg, #3b82f6, #2563eb); box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2); }
.stat-card.purple { background: linear-gradient(135deg, #8b5cf6, #7c3aed); box-shadow: 0 4px 12px rgba(124, 58, 237, 0.2); }
.stat-card.green { background: linear-gradient(135deg, #10b981, #059669); box-shadow: 0 4px 12px rgba(5, 150, 105, 0.2); }

.stat-label {
  font-size: 11px;
  opacity: 0.9;
  font-weight: 500;
}

.stat-value {
  font-size: 20px;
  font-weight: 800;
  margin: 0;
}

.stat-value small {
  font-size: 12px;
  font-weight: 400;
  margin-left: 2px;
}

.panoramic-section {
  margin-bottom: 28px;
}

.category-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.cat-bar-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cat-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.cat-progress-bg {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.cat-progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 1s ease-out;
}

.history-scroll-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
}

.history-item-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  transform: translateX(4px);
}

.h-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.h-item-date {
  font-size: 12px;
  color: #94a3b8;
}

.h-item-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
}

.h-item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.h-item-cat {
  font-size: 13px;
  color: #3b82f6;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.h-item-tags {
  display: flex;
  gap: 6px;
}

.micro-tag {
  font-size: 11px;
  padding: 2px 6px;
  background: #f1f5f9;
  border-radius: 4px;
  color: #64748b;
}

.panoramic-empty {
  padding-top: 60px;
}

.premium-drawer .el-drawer__header {
  margin-bottom: 20px;
  padding: 20px 24px 0;
  font-weight: 800;
  font-size: 18px;
  color: #1e293b;
}

.premium-drawer .el-drawer__body {
  padding: 0 24px 24px;
}

/* Scrollbar */
.premium-drawer .el-drawer__body::-webkit-scrollbar {
  width: 4px;
}

/* Scrollbar */
.scrollable::-webkit-scrollbar {
  width: 6px;
}

.scrollable::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}

.scrollable::-webkit-scrollbar-track {
  background: transparent;
}
/* Intelligence Overlay */
.scanning-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(8px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.scan-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(37, 99, 235, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.1) 1px, transparent 1px);
  background-size: 40px 40px;
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(to bottom, transparent, #2563eb, transparent);
  box-shadow: 0 0 20px #2563eb;
  animation: scan-move 2.5s linear infinite;
}

@keyframes scan-move {
  from { top: 0%; }
  to { top: 100%; }
}

.scan-content {
  text-align: center;
  z-index: 10;
}

.hologram-box {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2px solid #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  margin: 0 auto 24px;
  background: rgba(37, 99, 235, 0.2);
  box-shadow: 0 0 30px rgba(37, 99, 235, 0.4);
}

.rotating {
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Smart Onboarding */
.smart-onboarding {
  height: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 40px;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
}

.api-empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
}

.api-empty-card {
  max-width: 420px;
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 40px -20px rgba(15, 23, 42, 0.2);
  padding: 32px;
  text-align: center;
}

.api-empty-tag {
  margin-bottom: 16px;
}

.api-empty-card h2 {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.api-empty-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.api-empty-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.onboarding-content {
  text-align: center;
  max-width: 400px;
  animation: fade-in-up 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-avatar-large {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #0f172a 0%, #3b82f6 100%);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  color: white;
  font-size: 32px;
}

.shadow-glow {
  box-shadow: 0 8px 30px rgba(59, 130, 246, 0.3);
}

.onboarding-content h2 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 12px;
}

.onboarding-content p {
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 32px;
}

.onboarding-suggestions {
  text-align: left;
}

.suggest-title {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.suggest-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.s-card {
  padding: 16px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.s-card:hover {
  border-color: #3b82f6;
  background: #f0f9ff;
  transform: translateX(4px);
}

.s-card .el-icon {
  font-size: 18px;
  color: #3b82f6;
}

.s-card span {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
