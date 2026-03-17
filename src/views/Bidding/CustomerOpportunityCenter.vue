<template>
  <div class="customer-opportunity-page" :class="{ 'is-loading': loading }">
    <div class="page-header">
      <div class="header-text">
        <h2 class="animate-fade-in">客户商机中心</h2>
        <p class="animate-fade-in-delay">基于销售情报（Sales Intelligence）的客户经营视图，智能研判历史规律与潜在商机。</p>
      </div>
      <div class="header-actions">
        <el-button @click="refreshPage" :loading="loading" class="btn-refresh">
          <el-icon><Refresh /></el-icon>
          刷新洞察
        </el-button>
        <el-button type="primary" class="btn-primary" @click="createProject" v-if="selectedCustomer">
          {{ selectedCustomer.prediction.convertedProjectId ? '查看项目' : '转为正式项目' }}
        </el-button>
      </div>
    </div>

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
        <div class="card-value">{{ item.value }}</div>
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
                class="search-input"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-select v-model="filters.sales" placeholder="销售负责人" size="default" clearable class="filter-item">
                <el-option label="全部销售" value="" />
                <el-option v-for="user in salesUsers" :key="user.id" :label="user.name" :value="user.name" />
              </el-select>
            </div>
            <div class="filter-row">
              <el-select v-model="filters.region" placeholder="全部地区" size="default" clearable class="filter-item">
                <el-option v-for="region in regions" :key="region" :label="region" :value="region" />
              </el-select>
              <el-select v-model="filters.industry" placeholder="全部行业" size="default" clearable class="filter-item">
                <el-option v-for="ind in industries" :key="ind" :label="ind" :value="ind" />
              </el-select>
              <el-select v-model="filters.status" placeholder="全部分类" size="default" clearable class="filter-item">
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
            <el-table-column prop="customerName" label="客户名称" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="customer-name-cell">
                  <strong>{{ row.customerName }}</strong>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="region" label="地区" width="100" show-overflow-tooltip />
            <el-table-column prop="industry" label="行业" width="100" show-overflow-tooltip />
            <el-table-column prop="salesRep" label="销售负责人" width="110" show-overflow-tooltip />
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
            <el-table-column prop="predictedNextWindow" label="预测窗口" width="120" align="center">
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
          <div v-else class="empty-state">
            <el-empty description="选择一个客户以查看深度洞察" />
          </div>
        </el-skeleton>
      </section>
    </div>

    <el-drawer v-model="historyDrawer" title="历史采购全景图" size="500px" class="premium-drawer">
      <div class="history-drawer-content" v-if="selectedCustomer">
        <div class="history-card" v-for="record in selectedCustomer.purchaseHistory" :key="record.recordId">
          <div class="h-card-header">
            <strong>{{ record.title }}</strong>
            <span class="h-date">{{ record.publishDate }}</span>
          </div>
          <div class="h-card-body">
            <div class="h-info">
              <el-tag size="small" effect="plain">{{ record.category }}</el-tag>
              <span class="h-budget">¥{{ record.budget }}万</span>
            </div>
            <div class="h-tags">
              <span v-for="tag in record.extractedTags" :key="tag" class="small-tag">#{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, User, InfoFilled, MagicStick } from '@element-plus/icons-vue'
import { mockData } from '@/api/mock'
import { loadDemoState } from '@/utils/demoPersistence'

const router = useRouter()
const loading = ref(true)

const customerInsights = ref(loadDemoState('customer-insights', (mockData.customerInsights || []).map(item => ({
  ...item,
  salesRep: item.salesRep || (['小王', '张经理', '李工'][Math.floor(Math.random() * 3)])
}))))
const customerPurchases = ref(mockData.customerPurchases || [])
const customerPredictions = ref(loadDemoState('customer-predictions', mockData.customerPredictions || []))

const filters = ref({ status: '', keyword: '', sales: '', region: '', industry: '' })
const salesUsers = computed(() => mockData.users.filter(u => u.role !== 'admin'))

const regions = computed(() => [...new Set(customerInsights.value.map(c => c.region))].filter(Boolean))
const industries = computed(() => [...new Set(customerInsights.value.map(c => c.industry))].filter(Boolean))
const statusOptions = [
  { label: '待判断机会', value: 'watch' },
  { label: '建议转项目', value: 'recommend' },
  { label: '已转化项目', value: 'converted' }
]
const activeCustomerId = ref(customerInsights.value[0]?.customerId || '')
const historyDrawer = ref(false)

onMounted(() => {
  // Simulate initial loading
  setTimeout(() => {
    loading.value = false
  }, 800)
})

const boardSummaries = computed(() => {
  const customers = customerInsights.value
  const predictions = customerPredictions.value
  const highValueCount = customers.filter(item => item.opportunityScore >= 85).length
  const shortTermCount = predictions.filter(item => /^2025-0[3-4]/.test(item.predictedWindow)).length
  const midTermCount = predictions.filter(item => /^2025-0[5-6]/.test(item.predictedWindow)).length
  const convertedCount = predictions.filter(item => item.convertedProjectId).length

  return [
    { label: '高价值客户', value: String(highValueCount), note: '核心经营资产', tag: '重点', tagType: 'success' },
    { label: '30D 预测机会', value: String(shortTermCount), note: '需近期重点研判', tag: '紧迫', tagType: 'danger' },
    { label: '远期潜客', value: String(midTermCount), note: '适合关系铺垫', tag: '观察', tagType: 'warning' },
    { label: '已转化', value: String(convertedCount), note: '已转正式项目池', tag: '完成', tagType: 'info' }
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

const refreshPage = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('洞察情报已同步至最新')
  }, 1000)
}

const createProject = () => {
  if (!selectedCustomer.value) return

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
  box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.08);
  border-color: #e0f2fe;
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
</style>
