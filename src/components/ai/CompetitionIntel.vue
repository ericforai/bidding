<template>
  <el-dialog
    v-model="visible"
    title="🕵️ 竞争情报分析"
    :width="900"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>分析中...</span>
    </div>

    <div v-else-if="data" class="competition-intel">
      <!-- 本标可能对手 Top 3 -->
      <section class="competitors-section">
        <h3 class="section-title">
          <el-icon><TrendCharts /></el-icon>
          本标可能对手 Top 3
        </h3>
        <div class="competitors-grid">
          <el-card
            v-for="competitor in data.competitors"
            :key="competitor.id"
            :class="['competitor-card', `threat-${competitor.threatLevel}`]"
            shadow="hover"
          >
            <template #header>
              <div class="competitor-header">
                <el-avatar :size="48" class="competitor-avatar">
                  {{ competitor.name.slice(0, 2) }}
                </el-avatar>
                <div class="competitor-info">
                  <h4 class="competitor-name">{{ competitor.name }}</h4>
                  <el-tag
                    :type="competitor.threatLevel === 'high' ? 'danger' : 'warning'"
                    :icon="competitor.threatLevel === 'high' ? 'Warning' : 'InfoFilled'"
                    size="small"
                  >
                    {{ competitor.threatLevel === 'high' ? '高风险' : '中风险' }}
                  </el-tag>
                </div>
              </div>
            </template>

            <div class="competitor-stats">
              <div class="stat-item">
                <span class="stat-label">同类标出现</span>
                <span class="stat-value">{{ competitor.similarBids }} 次</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">价格区间</span>
                <span class="stat-value">{{ competitor.priceRange }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">中标率</span>
                <span class="stat-value">{{ competitor.winRate }}%</span>
              </div>
            </div>

            <div class="competitor-tactics">
              <el-icon><ChatDotRound /></el-icon>
              <span class="tactics-text">{{ competitor.tactics }}</span>
            </div>

            <el-progress
              v-if="competitor.threatLevel === 'high'"
              :percentage="competitor.winRate"
              :color=" ['#f56c6c', '#e6a23c']"
              :show-text="false"
              class="threat-bar"
            />
          </el-card>
        </div>
      </section>

      <!-- 应对策略建议 -->
      <section class="strategies-section">
        <h3 class="section-title">
          <el-icon><Guide /></el-icon>
          应对策略建议
        </h3>
        <el-timeline class="strategy-timeline">
          <el-timeline-item
            v-for="(strategy, index) in data.strategies"
            :key="index"
            :timestamp="strategy.priority === 'high' ? '优先处理' : '建议执行'"
            :type="strategy.priority === 'high' ? 'primary' : 'info'"
            placement="top"
            :icon="strategy.priority === 'high' ? 'StarFilled' : 'Operation'"
            :hollow="strategy.priority !== 'high'"
          >
            <div class="strategy-item">
              <el-tag
                :type="strategy.priority === 'high' ? 'danger' : 'info'"
                size="small"
                class="strategy-tag"
              >
                {{ strategy.priority === 'high' ? '必须' : '建议' }}
              </el-tag>
              <span class="strategy-text">{{ strategy.text }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </section>
    </div>

    <el-empty v-else description="暂无竞争情报数据" />

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
      <el-button type="primary" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出分析报告
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  Loading, TrendCharts, Guide, ChatDotRound, Download,
  Warning, InfoFilled, StarFilled, Operation
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  projectId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'export'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const data = ref(null)

// Mock 数据
const mockData = {
  P001: {
    competitors: [
      {
        id: 'C001',
        name: '华东科技',
        threatLevel: 'high',
        similarBids: 12,
        priceRange: '800-1200万',
        winRate: 65,
        tactics: '低价策略，常以低于市场价10%-15%投标，适合价格敏感型客户'
      },
      {
        id: 'C002',
        name: '智慧云通',
        threatLevel: 'medium',
        similarBids: 8,
        priceRange: '1000-1500万',
        winRate: 45,
        tactics: '技术领先策略，强调创新能力和AI能力，适合技术型客户'
      },
      {
        id: 'C003',
        name: '城投建设',
        threatLevel: 'medium',
        similarBids: 15,
        priceRange: '900-1300万',
        winRate: 55,
        tactics: '本地化优势，强调本地服务能力，适合政府类项目'
      }
    ],
    strategies: [
      { priority: 'high', text: '突出技术方案创新性，避免与华东科技拼价格' },
      { priority: 'high', text: '尽快补齐智慧城市类案例，可借用关联公司案例' },
      { priority: 'medium', text: '加强客户关系维护，安排高层拜访' },
      { priority: 'medium', text: '报价建议控制在1000-1200万区间' }
    ]
  }
}

const loadData = async () => {
  if (!props.projectId) return

  loading.value = true

  // 模拟 API 调用延迟
  await new Promise(resolve => setTimeout(resolve, 800))

  // 获取 mock 数据，实际应从 API 获取
  data.value = mockData[props.projectId] || mockData.P001

  loading.value = false
}

const handleClose = () => {
  visible.value = false
}

const handleExport = () => {
  emit('export', data.value)
  ElMessage.success('竞争情报报告导出成功')
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    loadData()
  }
})
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 12px;
  color: var(--el-text-color-secondary);
}

.loading-container .el-icon {
  font-size: 32px;
}

.competition-intel {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

/* 竞争对手卡片 */
.competitors-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.competitor-card {
  border-radius: var(--card-border-radius, 8px);
  transition: all 0.25s ease;
}

.competitor-card.threat-high {
  border-color: var(--color-danger, #DD2200);
}

.competitor-card.threat-high :deep(.el-card__header) {
  background: linear-gradient(135deg, #fef0f0 0%, #ffffff 100%);
}

.competitor-card.threat-medium {
  border-color: var(--color-warning, #FF8800);
}

.competitor-card.threat-medium :deep(.el-card__header) {
  background: linear-gradient(135deg, #fdf6ec 0%, #ffffff 100%);
}

.competitor-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.competitor-avatar {
  background: var(--color-primary, #0066CC);
  color: white;
  font-weight: 600;
}

.competitor-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.competitor-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.competitor-stats {
  display: flex;
  justify-content: space-around;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary, #0066CC);
}

.competitor-tactics {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 8px;
}

.tactics-text {
  flex: 1;
  line-height: 1.6;
}

.threat-bar {
  margin-top: 8px;
}

/* 策略时间线 */
.strategies-section {
  padding-top: 8px;
}

.strategy-timeline {
  padding-left: 8px;
}

.strategy-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  border-left: 3px solid var(--color-primary, #0066CC);
}

.strategy-tag {
  flex-shrink: 0;
}

.strategy-text {
  flex: 1;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}

@media (max-width: 768px) {
  .competitors-grid {
    grid-template-columns: 1fr;
  }
}
</style>
