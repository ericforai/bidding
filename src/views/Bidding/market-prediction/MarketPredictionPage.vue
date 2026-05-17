<template>
  <div class="market-prediction-page">
    <el-card class="prediction-header-card">
      <template #header>
        <div class="card-header">
          <span class="title">商机时间预测</span>
          <div class="header-actions">
            <el-tag type="info" size="small">
              至少需要 {{ minHistoricalCount }} 条历史数据
            </el-tag>
            <el-button size="small" @click="refreshPredictions" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <div class="prediction-summary">
        <div class="summary-item">
          <span class="summary-value">{{ predictions.length }}</span>
          <span class="summary-label">监测业主</span>
        </div>
        <div class="summary-item">
          <span class="summary-value high-confidence">{{ highConfidenceCount }}</span>
          <span class="summary-label">高置信度</span>
        </div>
        <div class="summary-item">
          <span class="summary-value upcoming">{{ upcomingCount }}</span>
          <span class="summary-label">近期招标</span>
        </div>
      </div>
    </el-card>

    <el-card class="prediction-list-card">
      <template #header>
        <div class="card-header">
          <span class="title">预测详情</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索业主名称"
            prefix-icon="Search"
            style="width: 240px"
            clearable
          />
        </div>
      </template>

      <div v-loading="loading" class="prediction-content">
        <el-empty
          v-if="!loading && predictions.length === 0"
          description="暂无预测数据"
        />

        <div v-else class="prediction-grid">
          <div
            v-for="item in filteredPredictions"
            :key="item.purchaserHash"
            class="prediction-card"
            :class="{ 'no-data': !item.hasData }"
          >
            <div class="card-header">
              <span class="purchaser-name">{{ item.purchaserName || '未知业主' }}</span>
              <el-tag
                :type="getConfidenceType(item.confidence)"
                size="small"
              >
                {{ getConfidenceText(item.confidence) }}
              </el-tag>
            </div>

            <div v-if="item.hasData" class="card-body">
              <div class="next-tender">
                <span class="label">预计下次招标</span>
                <span class="date">{{ item.nextTenderDate }}</span>
              </div>

              <div class="stats-row">
                <div class="stat-item">
                  <span class="stat-label">历史条数</span>
                  <span class="stat-value">{{ item.historicalCount }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">置信度</span>
                  <span class="stat-value">{{ Math.round(item.confidence * 100) }}%</span>
                </div>
              </div>

              <div class="confidence-bar">
                <el-progress
                  :percentage="Math.round(item.confidence * 100)"
                  :color="getProgressColor(item.confidence)"
                  :show-text="false"
                />
              </div>

              <p class="note">{{ item.note }}</p>
            </div>

            <div v-else class="card-body no-data">
              <el-empty description="暂无足够历史数据" :image-size="60" />
            </div>

            <div class="card-footer">
              <el-button
                size="small"
                type="primary"
                link
                @click="viewTenders(item)"
              >
                查看历史标讯
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  purchaserHashes: {
    type: Array,
    default: () => []
  }
})

const loading = ref(false)
const predictions = ref([])
const searchKeyword = ref('')
const minHistoricalCount = ref(2)

const filteredPredictions = computed(() => {
  if (!searchKeyword.value) return predictions.value
  return predictions.value.filter(p =>
    (p.purchaserName || '').toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const highConfidenceCount = computed(() =>
  predictions.value.filter(p => p.hasData && p.confidence >= 0.7).length
)

const upcomingCount = computed(() => {
  const now = new Date()
  const thirtyDaysLater = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
  return predictions.value.filter(p => {
    if (!p.hasData || !p.nextTenderDate) return false
    const nextDate = new Date(p.nextTenderDate)
    return nextDate >= now && nextDate <= thirtyDaysLater
  }).length
})

const getConfidenceType = (confidence) => {
  if (confidence >= 0.8) return 'success'
  if (confidence >= 0.5) return 'warning'
  return 'danger'
}

const getConfidenceText = (confidence) => {
  if (confidence >= 0.8) return '高置信'
  if (confidence >= 0.5) return '中置信'
  return '低置信'
}

const getProgressColor = (confidence) => {
  if (confidence >= 0.8) return '#67C23A'
  if (confidence >= 0.5) return '#E6A23C'
  return '#F56C6C'
}

const fetchPredictions = async () => {
  if (props.purchaserHashes.length === 0) return

  loading.value = true
  try {
    const response = await fetch('/api/market-prediction/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ purchaserHashes: props.purchaserHashes })
    })
    const result = await response.json()
    if (result.code === 0 || result.success) {
      predictions.value = result.data || []
    }
  } catch (error) {
    console.error('获取预测失败', error)
    ElMessage.error('获取预测数据失败')
  } finally {
    loading.value = false
  }
}

const fetchMinCount = async () => {
  try {
    const response = await fetch('/api/market-prediction/config/min-count')
    const result = await response.json()
    if (result.code === 0 || result.success) {
      minHistoricalCount.value = result.data
    }
  } catch (error) {
    console.error('获取配置失败', error)
  }
}

const refreshPredictions = () => {
  fetchPredictions()
}

const viewTenders = (item) => {
  // 跳转到标讯列表并按业主筛选
  console.log('查看历史标讯', item.purchaserHash)
  // TODO: 跳转到标讯列表
}

onMounted(() => {
  fetchMinCount()
  fetchPredictions()
})
</script>

<style scoped>
.market-prediction-page {
  padding: 20px;
}

.prediction-header-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.prediction-summary {
  display: flex;
  gap: 40px;
  padding: 16px 0;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.summary-value.high-confidence {
  color: #67C23A;
}

.summary-value.upcoming {
  color: #E6A23C;
}

.summary-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.prediction-list-card {
  min-height: 400px;
}

.prediction-content {
  min-height: 300px;
}

.prediction-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.prediction-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  transition: box-shadow 0.3s;
}

.prediction-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.prediction-card.no-data {
  opacity: 0.7;
}

.prediction-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.purchaser-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.card-body {
  margin-bottom: 12px;
}

.next-tender {
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
}

.next-tender .label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.next-tender .date {
  font-size: 24px;
  font-weight: 700;
  color: #409EFF;
}

.stats-row {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.confidence-bar {
  margin-bottom: 12px;
}

.note {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.card-footer {
  text-align: center;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
</style>
