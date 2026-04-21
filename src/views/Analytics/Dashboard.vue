<template>
  <div class="analytics-dashboard">
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>加载数据中...</p>
    </div>

    <!-- Page Header -->
    <div v-else class="page-header">
      <h2 class="page-title">数据分析</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="default"
          @change="handleDateChange"
        />
        <el-button type="primary" :icon="Refresh" @click="refreshData">刷新</el-button>
        <el-button :icon="Download" @click="exportData">导出</el-button>
      </div>
    </div>

    <!-- Metric Cards -->
    <div class="metric-cards">
      <div
        v-for="metric in metrics"
        :key="metric.key"
        class="b2b-metric-card"
        :class="'metric-' + getMetricColorClass(metric.key)"
        @click="handleMetricOverviewClick(metric.key)"
      >
        <div class="b2b-metric-content">
          <div class="b2b-metric-label">{{ metric.label }}</div>
          <div class="b2b-metric-value">{{ metric.value }}</div>
          <div class="b2b-metric-trend" :class="getTrendClass(metric.trendDirection)">
            <span class="trend-value">{{ metric.change }}</span>
            <span class="trend-label">较上月</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Row 1 -->
    <div class="charts-row">
      <!-- Win Rate Trend -->
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">中标率趋势</h3>
          <el-radio-group v-model="trendPeriod" size="small" @change="updateTrendChart">
            <el-radio-button value="month">月度</el-radio-button>
            <el-radio-button value="quarter">季度</el-radio-button>
            <el-radio-button value="year">年度</el-radio-button>
          </el-radio-group>
        </div>
        <LineChart :option="trendChartOption" height="300px" @chart-click="handleTrendClick" />
      </div>

      <!-- Competitor Analysis -->
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">竞争对手分析</h3>
          <el-tag size="small" type="info">市场份额</el-tag>
        </div>
        <PieChart :option="competitorChartOption" height="300px" @chart-click="handleCompetitorClick" />
      </div>
    </div>

    <!-- Charts Row 2 -->
    <div class="charts-row">
      <!-- Product Line ROI -->
      <div v-if="showProductLinesCard" class="chart-card chart-card-large">
        <div class="chart-header">
          <h3 class="chart-title">投入产出分析（按产品线）</h3>
          <el-radio-group v-model="productMetric" size="small" @change="updateProductChart">
            <el-radio-button value="revenue">收入</el-radio-button>
            <el-radio-button value="rate">中标率</el-radio-button>
            <el-radio-button value="roi">ROI</el-radio-button>
          </el-radio-group>
        </div>
        <BarChart :option="productChartOption" height="300px" @chart-click="handleProductClick" />
      </div>
    </div>

    <CustomerTypePanel
      :date-range="dateRange"
      :refresh-key="customerTypeRefreshKey"
      class="dashboard-section"
    />

    <!-- Charts Row 3 -->
    <div class="charts-row">
      <!-- Region Distribution -->
      <div class="chart-card chart-card-large">
        <div class="chart-header">
          <h3 class="chart-title">区域分布</h3>
          <el-radio-group v-model="regionView" size="small" @change="updateRegionChart">
            <el-radio-button value="amount">金额</el-radio-button>
            <el-radio-button value="bids">投标数</el-radio-button>
            <el-radio-button value="rate">中标率</el-radio-button>
          </el-radio-group>
        </div>
        <BarChart :option="regionChartOption" height="280px" @chart-click="handleRegionClick" />
      </div>
    </div>

    <!-- Detail Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="detailTitle"
      width="800px"
      @close="handleDialogClose"
    >
      <div class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item v-for="item in detailItems" :key="item.key" :label="item.label">
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="viewMore">查看更多</el-button>
      </template>
    </el-dialog>

    <!-- Drill Down Dialog -->
    <el-dialog
      v-model="showDrillDownDialog"
      :title="drillDownTitle"
      width="1000px"
      class="drill-down-dialog"
      destroy-on-close
    >
      <el-tabs v-model="drillDownTab" type="border-card">
        <!-- 相关项目 -->
        <el-tab-pane name="projects">
          <template #label>
            <span class="tab-label">
              <el-icon><Document /></el-icon>
              相关项目
              <el-badge v-if="drillDownData.projects.length > 0" :value="drillDownData.projects.length" class="tab-badge" />
            </span>
          </template>
          <el-table
            :data="drillDownData.projects"
            size="small"
            stripe
            :empty-text="drillDownData.projects.length === 0 ? '暂无相关项目' : ''"
          >
            <el-table-column prop="name" label="项目名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="customer" label="客户" width="120" />
            <el-table-column prop="budget" label="预算(万元)" width="100" align="right" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="manager" label="负责人" width="80" />
            <el-table-column prop="result" label="结果" width="80">
              <template #default="{ row }">
                <span v-if="row.result" :style="{ color: row.result === 'won' ? '#67c23a' : '#f56c6c' }">
                  {{ row.result === 'won' ? '中标' : '未中标' }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="goToProject(row.id)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 团队信息 -->
        <el-tab-pane name="team">
          <template #label>
            <span class="tab-label">
              <el-icon><User /></el-icon>
              团队信息
              <el-badge v-if="drillDownData.team.length > 0" :value="drillDownData.team.length" class="tab-badge" />
            </span>
          </template>
          <el-row :gutter="20">
            <el-col :span="drillDownData.stats ? 14 : 24">
              <div class="team-section">
                <h4 class="section-title">参与人员</h4>
                <el-table
                  :data="drillDownData.team"
                  size="small"
                  border
                  :empty-text="drillDownData.team.length === 0 ? '暂无团队数据' : ''"
                >
                  <el-table-column prop="name" label="姓名" width="100" />
                  <el-table-column prop="role" label="角色" width="120" />
                  <el-table-column prop="dept" label="部门" width="120" />
                  <el-table-column prop="participation" label="参与次数" width="100" align="center" />
                  <el-table-column label="中标率" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag
                        :type="row.winRate >= 40 ? 'success' : row.winRate >= 30 ? 'warning' : 'danger'"
                        size="small"
                      >
                        {{ row.winRate }}%
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-col>
            <el-col v-if="drillDownData.stats" :span="10">
              <div class="team-section">
                <h4 class="section-title">绩效统计</h4>
                <div class="team-stats">
                  <div class="stat-item">
                    <div class="stat-label">总参与次数</div>
                    <div class="stat-value">{{ drillDownData.stats.totalParticipation }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">中标次数</div>
                    <div class="stat-value stat-success">{{ drillDownData.stats.wonCount }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">团队中标率</div>
                    <div class="stat-value" :class="drillDownData.stats.teamWinRate >= 35 ? 'stat-success' : 'stat-warning'">
                      {{ drillDownData.stats.teamWinRate }}%
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">累计金额(万)</div>
                    <div class="stat-value stat-primary">{{ drillDownData.stats.totalAmount }}</div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 文件列表 -->
        <el-tab-pane name="files">
          <template #label>
            <span class="tab-label">
              <el-icon><FolderOpened /></el-icon>
              相关文件
              <el-badge v-if="drillDownData.files.length > 0" :value="drillDownData.files.length" class="tab-badge" />
            </span>
          </template>
          <el-table
            :data="drillDownData.files"
            size="small"
            stripe
            :empty-text="drillDownData.files.length === 0 ? '暂无文件' : ''"
          >
            <el-table-column prop="name" label="文件名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="project" label="所属项目" width="150" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getFileTypeColor(row.name)">
                  {{ getFileType(row.name) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="uploader" label="上传者" width="80" />
            <el-table-column prop="uploadTime" label="上传时间" width="140" />
            <el-table-column prop="size" label="大小" width="80" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="previewFile(row)">
                  预览
                </el-button>
                <el-button link type="success" size="small" @click="downloadFile(row)">
                  下载
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="showDrillDownDialog = false">关闭</el-button>
        <el-button type="primary" :icon="Download" @click="exportDrillDownData">导出数据</el-button>
      </template>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog
      v-model="previewFileDialogVisible"
      :title="`预览: ${previewFileName}`"
      width="80%"
      top="5vh"
      @close="previewFileUrl = ''"
    >
      <div class="file-preview-container">
        <iframe
          v-if="previewFileUrl"
          :src="previewFileUrl"
          class="file-preview-frame"
          frameborder="0"
        ></iframe>
        <div v-else class="preview-placeholder">
          <el-icon :size="60"><Document /></el-icon>
          <p>无法预览此文件</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewFileDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadFile({ name: previewFileName, url: previewFileUrl })">
          下载文件
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="metricDrawerVisible"
      :title="metricDrawerTitle"
      size="70%"
      destroy-on-close
      @close="handleMetricDrawerClose"
    >
      <div class="metric-drawer">
        <FeaturePlaceholder
          v-if="metricDrillDownPlaceholder"
          compact
          :title="metricDrillDownPlaceholder.title"
          :message="metricDrillDownPlaceholder.message"
          :hint="metricDrillDownPlaceholder.hint"
        />
        <template v-else>
        <div class="metric-drawer-toolbar">
          <div class="metric-drawer-filters">
            <el-select
              v-for="dimension in metricDrillDownDimensions"
              :key="dimension.key"
              :model-value="metricFilterValues[dimension.key] || 'ALL'"
              size="small"
              style="width: 180px"
              @change="(value) => handleMetricFilterChange(dimension.key, value)"
            >
              <el-option
                v-for="option in dimension.options"
                :key="`${dimension.key}-${option.value}`"
                :label="`${option.label}${option.count != null ? ` (${option.count})` : ''}`"
                :value="option.value"
              />
            </el-select>
          </div>
          <el-button size="small" @click="reloadMetricDrillDown">刷新明细</el-button>
        </div>

        <div class="metric-summary-grid">
          <div class="metric-summary-card">
            <span class="summary-label">记录数</span>
            <span class="summary-value">{{ metricDrillDownSummary.totalCount ?? 0 }}</span>
          </div>
          <div class="metric-summary-card">
            <span class="summary-label">金额</span>
            <span class="summary-value">{{ formatMetricAmount(metricDrillDownSummary.totalAmount) }}</span>
          </div>
          <div class="metric-summary-card" v-if="metricDrillDownSummary.wonCount != null">
            <span class="summary-label">中标数</span>
            <span class="summary-value">{{ metricDrillDownSummary.wonCount }}</span>
          </div>
          <div class="metric-summary-card" v-if="metricDrillDownSummary.winRate != null">
            <span class="summary-label">中标率</span>
            <span class="summary-value">{{ formatMetricRate(metricDrillDownSummary.winRate) }}</span>
          </div>
          <div class="metric-summary-card" v-if="metricDrillDownSummary.activeCount != null">
            <span class="summary-label">进行中</span>
            <span class="summary-value">{{ metricDrillDownSummary.activeCount }}</span>
          </div>
          <div class="metric-summary-card" v-if="metricDrillDownSummary.totalTeamMembers != null">
            <span class="summary-label">成员数</span>
            <span class="summary-value">{{ metricDrillDownSummary.totalTeamMembers }}</span>
          </div>
          <div class="metric-summary-card" v-if="metricDrillDownSummary.totalCompletedTasks != null">
            <span class="summary-label">已完成任务</span>
            <span class="summary-value">{{ metricDrillDownSummary.totalCompletedTasks }}</span>
          </div>
          <div class="metric-summary-card" v-if="metricDrillDownSummary.totalOverdueTasks != null">
            <span class="summary-label">逾期任务</span>
            <span class="summary-value">{{ metricDrillDownSummary.totalOverdueTasks }}</span>
          </div>
          <div class="metric-summary-card" v-if="metricDrillDownSummary.averageTaskCompletionRate != null">
            <span class="summary-label">平均完成率</span>
            <span class="summary-value">{{ formatMetricRate(metricDrillDownSummary.averageTaskCompletionRate) }}</span>
          </div>
        </div>

        <el-table v-loading="metricDrillDownLoading" :data="metricDrillDownItems" stripe>
          <el-table-column
            v-for="column in metricDrillDownColumns"
            :key="column.key"
            :prop="column.key"
            :label="column.label"
            :min-width="column.minWidth || 120"
            :width="column.width"
            :show-overflow-tooltip="column.overflow !== false"
          >
            <template #default="{ row }">
              <el-tag v-if="column.type === 'status'" size="small" :type="getMetricStatusTagType(row[column.key], metricDrawerType)">
                {{ formatMetricCell(column, row[column.key]) }}
              </el-tag>
              <span v-else>
                {{ formatMetricCell(column, row[column.key], row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column v-if="hasMetricDrillDownAction" label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleMetricRowAction(row)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="metric-drawer-pagination">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :current-page="metricPagination.page || 1"
            :page-size="metricPagination.size || 10"
            :total="metricPagination.total || 0"
            @current-change="handleMetricPageChange"
          />
        </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Download, Document, FolderOpened, User, Loading } from '@element-plus/icons-vue'
import { dashboardApi, projectsApi, getFeaturePlaceholder, isFeatureUnavailableResponse } from '@/api'
import { getDemoDashboardProjects } from '@/api/mock-adapters/frontendDemo.js'
import LineChart from '@/components/charts/LineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import BarChart from '@/components/charts/BarChart.vue'
import FeaturePlaceholder from '@/components/common/FeaturePlaceholder.vue'
import CustomerTypePanel from './components/CustomerTypePanel.vue'
import { notifyFeatureUnavailable } from '@/utils/featureFeedback'
import { useExport } from '@/composables/useExport'
import { ExportType } from '@/api'

const router = useRouter()
const route = useRoute()

// Data
const loading = ref(true)
const dateRange = ref([])
const dashboardData = ref(null)
const trendPeriod = ref('month')
const productMetric = ref('revenue')
const regionView = ref('amount')
const customerTypeRefreshKey = ref(0)

// Detail Dialog (原有的简单详情对话框)
const detailDialogVisible = ref(false)
const detailTitle = ref('')
const detailItems = ref([])

// Drill Down Dialog (新增的下钻对话框)
const showDrillDownDialog = ref(false)
const drillDownTitle = ref('')
const drillDownTab = ref('projects')
const drillDownData = ref({
  projects: [],
  team: [],
  files: [],
  stats: null
})
const currentDrillDownContext = ref(null)
const isDemoMode = false
const metricDrawerVisible = ref(false)
const metricDrawerType = ref('')
const metricDrawerTitle = ref('')
const metricDrillDownLoading = ref(false)
const metricDrillDownResponse = ref(null)
const metricDrillDownPlaceholder = ref(null)
const metricFilterValues = ref({})
const metricPaginationState = ref({ page: 1, size: 10 })
const pageFeaturePlaceholders = ref({})

// Metrics configuration
const metrics = computed(() => {
  if (!dashboardData.value) return []

  const getTrendDirection = (val) => {
    if (val === '--') return 'trend-neutral'
    return String(val).startsWith('+') ? 'trend-up' : 'trend-down'
  }

  return [
    {
      key: 'bids',
      label: '年度投标数',
      value: dashboardData.value.totalBids,
      change: dashboardData.value.totalBidsChange,
      trendDirection: getTrendDirection(dashboardData.value.totalBidsChange)
    },
    {
      key: 'winRate',
      label: '中标率',
      value: dashboardData.value.winRate + '%',
      change: dashboardData.value.winRateChange,
      trendDirection: getTrendDirection(dashboardData.value.winRateChange)
    },
    {
      key: 'amount',
      label: '中标金额',
      value: formatAmount(dashboardData.value.totalAmount),
      change: dashboardData.value.totalAmountChange,
      trendDirection: getTrendDirection(dashboardData.value.totalAmountChange)
    },
    {
      key: 'cost',
      label: '投入费用',
      value: formatAmount(dashboardData.value.totalCost),
      change: dashboardData.value.totalCostChange,
      trendDirection: getTrendDirection(dashboardData.value.totalCostChange)
    }
  ]
})

const metricTypeByCardKey = {
  bids: 'projects',
  winRate: 'win-rate',
  amount: 'revenue',
  cost: 'projects' }

const metricTitleMap = {
  revenue: '中标金额明细',
  'win-rate': '中标率明细',
  team: '人员绩效明细',
  projects: '进行中项目明细' }

const metricDrillDownItems = computed(() => metricDrillDownResponse.value?.items || [])
const metricDrillDownSummary = computed(() => metricDrillDownResponse.value?.summary || {})
const metricDrillDownDimensions = computed(() => metricDrillDownResponse.value?.filters?.dimensions || [])
const metricPagination = computed(() => metricDrillDownResponse.value?.pagination || metricPaginationState.value)
const hasMetricDrillDownAction = computed(() => ['revenue', 'win-rate', 'projects'].includes(metricDrawerType.value))
const productLinesPlaceholder = computed(() => pageFeaturePlaceholders.value.productLines || null)
const showProductLinesCard = computed(() => true)

const buildEmptyDashboardData = () => ({
  totalBids: 0,
  totalBidsChange: '--',
  inProgress: 0,
  wonThisYear: 0,
  winRate: 0,
  winRateChange: '--',
  totalAmount: 0,
  totalAmountChange: '--',
  totalCost: 0,
  totalCostChange: '--',
  trendData: [],
  competitors: [],
  productLines: [],
  regionData: [],
  statusDistribution: {},
  backendSummary: {
    activeProjects: 0,
    pendingTasks: 0 } })

const buildEmptyMetricDrillDownResponse = () => ({
  items: [],
  summary: { totalCount: 0, totalAmount: 0 },
  filters: { dimensions: [] },
  pagination: { page: 1, size: metricPaginationState.value.size || 10, total: 0, totalPages: 0, hasNext: false } })

const metricDrillDownColumns = computed(() => {
  const columnMap = {
    revenue: [
      { key: 'title', label: '标讯名称', minWidth: 240 },
      { key: 'subtitle', label: '来源/区域', minWidth: 120 },
      { key: 'status', label: '状态', width: 120, type: 'status' },
      { key: 'ownerName', label: '关联项目', minWidth: 200 },
      { key: 'score', label: 'AI评分', width: 100 },
      { key: 'amount', label: '金额(万)', width: 120, type: 'amount' },
      { key: 'createdAt', label: '创建时间', minWidth: 140, type: 'datetime' },
      { key: 'deadline', label: '截止时间', minWidth: 140, type: 'datetime' },
    ],
    'win-rate': [
      { key: 'title', label: '标讯名称', minWidth: 220 },
      { key: 'subtitle', label: '关联项目', minWidth: 200 },
      { key: 'outcome', label: '结果', width: 120, type: 'status' },
      { key: 'ownerName', label: '负责人', width: 120 },
      { key: 'amount', label: '金额(万)', width: 120, type: 'amount' },
      { key: 'rate', label: '命中率', width: 100, type: 'rate' },
      { key: 'createdAt', label: '创建时间', minWidth: 140, type: 'datetime' },
    ],
    team: [
      { key: 'title', label: '成员', width: 120 },
      { key: 'subtitle', label: '邮箱/部门', minWidth: 180 },
      { key: 'role', label: '角色', width: 120, type: 'status' },
      { key: 'count', label: '参与项目', width: 100 },
      { key: 'wonCount', label: '中标项目', width: 100 },
      { key: 'activeProjectCount', label: '进行中项目', width: 110 },
      { key: 'managedProjectCount', label: '负责项目', width: 100 },
      { key: 'completedTaskCount', label: '已完成任务', width: 110 },
      { key: 'overdueTaskCount', label: '逾期任务', width: 100 },
      { key: 'taskCompletionRate', label: '任务完成率', width: 110, type: 'rate' },
      { key: 'rate', label: '中标率', width: 100, type: 'rate' },
      { key: 'score', label: '绩效分', width: 90 },
      { key: 'amount', label: '累计金额(万)', width: 140, type: 'amount' },
    ],
    projects: [
      { key: 'title', label: '项目名称', minWidth: 220 },
      { key: 'subtitle', label: '标讯/客户', minWidth: 220 },
      { key: 'status', label: '状态', width: 120, type: 'status' },
      { key: 'ownerName', label: '负责人', width: 120 },
      { key: 'teamSize', label: '团队规模', width: 100 },
      { key: 'amount', label: '预算(万)', width: 120, type: 'amount' },
      { key: 'createdAt', label: '开始时间', minWidth: 140, type: 'datetime' },
      { key: 'deadline', label: '截止时间', minWidth: 140, type: 'datetime' },
    ] }

  return columnMap[metricDrawerType.value] || []
})

// Format amount to display
const formatAmount = (amount) => {
  if (amount >= 10000) {
    return (amount / 10000).toFixed(1) + '亿'
  }
  return amount + '万'
}

const formatMetricAmount = (amount) => {
  const numeric = Number(amount || 0)
  return `${numeric.toLocaleString('zh-CN')}万`
}

const formatMetricRate = (rate) => `${Number(rate || 0).toFixed(1)}%`

const formatMetricDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

// Helper function for metric color class
const getMetricColorClass = (key) => {
  const classMap = {
    'bids': 'blue',
    'winRate': 'green',
    'amount': 'orange',
    'cost': 'red'
  }
  return classMap[key] || 'blue'
}

// Helper function for trend class
const getTrendClass = (direction) => {
  if (direction === 'trend-neutral') return 'neutral'
  return direction === 'trend-up' ? 'positive' : 'negative'
}

// Trend Chart Option
const trendChartOption = computed(() => {
  if (!dashboardData.value) return {}

  const data = dashboardData.value.trendData || []
  const months = data.map(d => d.month)
  const rates = data.map(d => d.rate)
  const amounts = data.map(d => d.amount / 100) // Convert to smaller scale

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['中标率', '中标金额'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months,
      boundaryGap: false
    },
    yAxis: [
      {
        type: 'value',
        name: '中标率(%)',
        position: 'left',
        axisLabel: { formatter: '{value}%' }
      },
      {
        type: 'value',
        name: '金额(万)',
        position: 'right'
      }
    ],
    series: [
      {
        name: '中标率',
        type: 'line',
        smooth: true,
        data: rates,
        itemStyle: { color: '#67C23A' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
            ]
          }
        }
      },
      {
        name: '中标金额',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: amounts,
        itemStyle: { color: '#409EFF' }
      }
    ]
  }
})

// Competitor Chart Option
const competitorChartOption = computed(() => {
  if (!dashboardData.value) return {}

  const data = dashboardData.value.competitors || []
  const highlightIndex = data.findIndex(c => c.name === '我司')

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}万元 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data.map((item, index) => ({
          value: item.amount,
          name: item.name,
          itemStyle: index === highlightIndex ? { color: '#409EFF' } : undefined
        }))
      }
    ]
  }
})

// Product Line Chart Option
const productChartOption = computed(() => {
  if (!dashboardData.value) return {}

  const data = dashboardData.value.productLines || []
  const names = data.map(d => d.name)

  let seriesData = []
  let yAxisName = ''
  let seriesName = ''

  switch (productMetric.value) {
    case 'revenue':
      seriesData = data.map(d => d.revenue / 100)
      yAxisName = '收入(万)'
      seriesName = '收入'
      break
    case 'rate':
      seriesData = data.map(d => d.rate)
      yAxisName = '中标率(%)'
      seriesName = '中标率'
      break
    case 'roi':
      seriesData = data.map(d => ((d.revenue - d.cost) / d.cost * 100).toFixed(1))
      yAxisName = 'ROI(%)'
      seriesName = 'ROI'
      break
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = data[params[0].dataIndex]
        const metric = productMetric.value
        let valueText = ''
        if (metric === 'revenue') {
          valueText = `收入: ${item.revenue}万<br/>成本: ${item.cost}万<br/>中标率: ${item.rate}%`
        } else if (metric === 'rate') {
          valueText = `中标率: ${item.rate}%<br/>收入: ${item.revenue}万<br/>成本: ${item.cost}万`
        } else {
          const roi = ((item.revenue - item.cost) / item.cost * 100).toFixed(1)
          valueText = `ROI: ${roi}%<br/>收入: ${item.revenue}万<br/>成本: ${item.cost}万`
        }
        return `${params[0].name}<br/>${valueText}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: {
        interval: 0,
        rotate: 0
      }
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLabel: productMetric.value === 'rate' || productMetric.value === 'roi'
        ? { formatter: '{value}%' }
        : undefined
    },
    series: [
      {
        name: seriesName,
        type: 'bar',
        data: seriesData,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#9C27B0' },
              { offset: 1, color: '#E1BEE7' }
            ]
          },
          borderRadius: [8, 8, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: '#7B1FA2' },
                { offset: 1, color: '#CE93D8' }
              ]
            }
          }
        }
      }
    ]
  }
})

// Region Chart Option
const regionChartOption = computed(() => {
  if (!dashboardData.value) return {}

  const data = dashboardData.value.regionData || []
  const names = data.map(d => d.name)

  let seriesData = []
  let yAxisName = ''
  let color = ''

  switch (regionView.value) {
    case 'amount':
      seriesData = data.map(d => d.amount / 100)
      yAxisName = '金额(万)'
      color = '#409EFF'
      break
    case 'bids':
      seriesData = data.map(d => d.bids)
      yAxisName = '投标数'
      color = '#67C23A'
      break
    case 'rate':
      seriesData = data.map(d => d.rate)
      yAxisName = '中标率(%)'
      color = '#E6A23C'
      break
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = data[params[0].dataIndex]
        return `${params[0].name}<br/>${yAxisName}: ${params[0].value}<br/>投标数: ${item.bids}<br/>中标率: ${item.rate}%`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: names
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLabel: regionView.value === 'rate' ? { formatter: '{value}%' } : undefined
    },
    series: [
      {
        name: yAxisName,
        type: 'bar',
        data: seriesData,
        itemStyle: {
          color: color,
          borderRadius: [6, 6, 0, 0]
        },
        barWidth: '50%'
      }
    ]
  }
})

// Load data
const loadData = async () => {
  loading.value = true
  pageFeaturePlaceholders.value = {}
  try {
    const response = await dashboardApi.getOverview()
    if (!response?.success) {
      throw new Error(response?.message || '加载数据失败')
    }
    const nextData = {
      ...buildEmptyDashboardData(),
      ...(response.data || {}) }

    const productLinesResponse = await dashboardApi.getProductLines()
    if (productLinesResponse?.success) {
      nextData.productLines = Array.isArray(productLinesResponse.data) ? productLinesResponse.data : []
    } else if (isFeatureUnavailableResponse(productLinesResponse)) {
      if (!isDemoMode) {
        nextData.productLines = []
        dashboardData.value = nextData
        return
      }
      const placeholder = notifyFeatureUnavailable(productLinesResponse, {
        fallback: {
          title: '产品线分析当前不可用',
          hint: '其余指标仍基于真实后端数据加载，可稍后重试或联系管理员检查分析服务。' },
        level: 'warning' })
      pageFeaturePlaceholders.value = {
        ...pageFeaturePlaceholders.value,
        productLines: placeholder || getFeaturePlaceholder(productLinesResponse) }
      nextData.productLines = []
    } else if (productLinesResponse?.message) {
      ElMessage.warning(productLinesResponse.message)
      nextData.productLines = []
    }

    dashboardData.value = nextData
  } catch (error) {
    dashboardData.value = buildEmptyDashboardData()
    ElMessage.error(error?.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

const buildMetricDrillDownParams = () => {
  const [startDate, endDate] = Array.isArray(dateRange.value) ? dateRange.value : []
  const params = {
    page: metricPaginationState.value.page,
    size: metricPaginationState.value.size }

  if (startDate) {
    params.startDate = new Date(startDate).toISOString().slice(0, 10)
  }
  if (endDate) {
    params.endDate = new Date(endDate).toISOString().slice(0, 10)
  }

  Object.entries(metricFilterValues.value).forEach(([key, value]) => {
    if (value && value !== 'ALL') {
      if (key === 'status' && metricDrawerType.value === 'projects') {
        params.status = value
        return
      }
      params[key] = value
    }
  })

  return params
}

const openMetricDrillDown = async (type, options = {}) => {
  metricDrawerType.value = type
  metricDrawerTitle.value = metricTitleMap[type] || '明细'
  metricDrawerVisible.value = true
  metricDrillDownLoading.value = true
  metricDrillDownPlaceholder.value = null

  if (options.resetPaging !== false) {
    metricPaginationState.value = { page: 1, size: metricPaginationState.value.size || 10 }
  }
  if (options.filters) {
    metricFilterValues.value = { ...options.filters }
  }

  try {
    const response = await dashboardApi.getDrillDown(type, buildMetricDrillDownParams())
    if (isFeatureUnavailableResponse(response)) {
      if (!isDemoMode) {
        metricDrawerVisible.value = false
        metricDrawerType.value = ''
        metricDrawerTitle.value = ''
        metricDrillDownResponse.value = buildEmptyMetricDrillDownResponse()
        ElMessage.info('当前版本暂不开放该分析明细')
        return
      }
      metricDrillDownResponse.value = buildEmptyMetricDrillDownResponse()
      metricDrillDownPlaceholder.value = notifyFeatureUnavailable(response, {
        fallback: {
          title: '下钻明细当前不可用',
          hint: '当前无法返回该指标的明细数据，请稍后重试或联系管理员检查分析服务。' } }) || getFeaturePlaceholder(response)
      return
    }
    if (!response?.success) {
      throw new Error(response?.message || '加载下钻明细失败')
    }
    metricDrillDownResponse.value = response.data

    const nextFilters = {}
    ;(response.data?.filters?.dimensions || []).forEach((dimension) => {
      nextFilters[dimension.key] = metricFilterValues.value[dimension.key] || dimension.selectedValue || 'ALL'
    })
    metricFilterValues.value = nextFilters
  } catch (error) {
    ElMessage.error(error?.message || '加载下钻明细失败')
  } finally {
    metricDrillDownLoading.value = false
  }
}

const syncMetricQuery = async (type, extraQuery = {}) => {
  if (!type) return
  await router.replace({
    path: '/analytics/dashboard',
    query: {
      ...route.query,
      drilldown: type,
      ...extraQuery } })
}

const clearMetricQuery = async () => {
  const nextQuery = { ...route.query }
  delete nextQuery.drilldown
  delete nextQuery.status
  delete nextQuery.role
  delete nextQuery.outcome
  await router.replace({ path: '/analytics/dashboard', query: nextQuery })
}

const handleMetricOverviewClick = async (metricKey) => {
  const type = metricTypeByCardKey[metricKey]
  if (!type) return
  await syncMetricQuery(type)
}

const handleMetricFilterChange = async (key, value) => {
  metricFilterValues.value = {
    ...metricFilterValues.value,
    [key]: value }
  metricPaginationState.value = {
    ...metricPaginationState.value,
    page: 1 }

  const extraQuery = {}
  if (key === 'status') extraQuery.status = value === 'ALL' ? undefined : value
  if (key === 'role') extraQuery.role = value === 'ALL' ? undefined : value
  if (key === 'outcome') extraQuery.outcome = value === 'ALL' ? undefined : value
  await syncMetricQuery(metricDrawerType.value, extraQuery)
}

const reloadMetricDrillDown = async () => {
  if (!metricDrawerType.value) return
  await openMetricDrillDown(metricDrawerType.value, { resetPaging: false })
}

const handleMetricPageChange = async (page) => {
  metricPaginationState.value = {
    ...metricPaginationState.value,
    page }
  await openMetricDrillDown(metricDrawerType.value, { resetPaging: false })
}

const handleMetricDrawerClose = async () => {
  metricDrillDownResponse.value = null
  metricDrillDownPlaceholder.value = null
  metricDrawerType.value = ''
  metricDrawerTitle.value = ''
  metricFilterValues.value = {}
  metricPaginationState.value = { page: 1, size: 10 }
  await clearMetricQuery()
}

const formatMetricCell = (column, value, row = {}) => {
  if (value == null || value === '') return '-'

  if (column.type === 'amount') {
    return formatMetricAmount(value)
  }
  if (column.type === 'rate') {
    return formatMetricRate(value)
  }
  if (column.type === 'datetime') {
    return formatMetricDateTime(value)
  }
  if (column.type === 'status') {
    if (metricDrawerType.value === 'revenue' && column.key === 'status') {
      return getStatusText(String(value).toLowerCase())
    }
    if (metricDrawerType.value === 'projects' && column.key === 'status') {
      return {
        INITIATED: '已启动',
        PREPARING: '准备中',
        REVIEWING: '审核中',
        SEALING: '封装中',
        BIDDING: '投标中',
        ARCHIVED: '已归档' }[value] || value
    }
    if (metricDrawerType.value === 'win-rate' && column.key === 'outcome') {
      return {
        WON: '已中标',
        LOST: '未中标',
        IN_PROGRESS: '进行中' }[value] || value
    }
    if (metricDrawerType.value === 'team' && column.key === 'role') {
      return {
        ADMIN: '管理员',
        MANAGER: '经理',
        STAFF: '员工' }[value] || value
    }
  }

  if (column.key === 'subtitle' && metricDrawerType.value === 'projects' && row.relatedId) {
    return `${value}`
  }

  return value
}

const getMetricStatusTagType = (value, type) => {
  if (type === 'revenue') {
    return {
      PENDING: 'info',
      TRACKING: 'warning',
      BIDDED: 'success',
      ABANDONED: 'danger' }[value] || 'info'
  }
  if (type === 'projects') {
    return {
      INITIATED: 'info',
      PREPARING: 'warning',
      REVIEWING: 'primary',
      SEALING: 'warning',
      BIDDING: 'success',
      ARCHIVED: 'info' }[value] || 'info'
  }
  if (type === 'win-rate') {
    return {
      WON: 'success',
      LOST: 'danger',
      IN_PROGRESS: 'warning' }[value] || 'info'
  }
  if (type === 'team') {
    return {
      ADMIN: 'danger',
      MANAGER: 'primary',
      STAFF: 'success' }[value] || 'info'
  }
  return 'info'
}

const handleMetricRowAction = (row) => {
  if (metricDrawerType.value === 'projects') {
    router.push({ name: 'ProjectDetail', params: { id: row.id } })
    return
  }

  const projectId = row.relatedId || row.id
  if (projectId) {
    router.push({ name: 'ProjectDetail', params: { id: projectId } })
  }
}

// Event handlers
const handleDateChange = () => {
  if (metricDrawerVisible.value && metricDrawerType.value) {
    metricPaginationState.value = {
      ...metricPaginationState.value,
      page: 1 }
    openMetricDrillDown(metricDrawerType.value, { resetPaging: false })
  }
  ElMessage.info('日期范围已更新')
}

const refreshData = async () => {
  await loadData()
  customerTypeRefreshKey.value += 1
  ElMessage.success('数据已刷新')
}

const exportData = () => {
  const { exportExcel } = useExport()

  // 构建导出参数
  const params = {
    startDate: dateRange.value?.[0] || null,
    endDate: dateRange.value?.[1] || null
  }

  exportExcel(ExportType.DASHBOARD_OVERVIEW, params, '数据看板导出成功')
}

const updateTrendChart = () => {
  // Update chart based on period
}

const updateProductChart = () => {
  // Update chart based on metric
}

const updateRegionChart = () => {
  // Update chart based on view
}

const handleTrendClick = (params) => {
  const data = dashboardData.value.trendData[params.dataIndex]
  // 使用下钻对话框
  openDrillDownDialog('trend', data)
}

const handleCompetitorClick = (params) => {
  const competitor = dashboardData.value.competitors[params.dataIndex]
  // 使用下钻对话框
  openDrillDownDialog('competitor', competitor)
}

const handleProductClick = (params) => {
  if (productLinesPlaceholder.value) {
    ElMessage.info(isDemoMode ? productLinesPlaceholder.value.message : '当前版本暂不开放产品线分析')
    return
  }
  const product = dashboardData.value.productLines[params.dataIndex]
  if (!product) return
  // 使用下钻对话框
  openDrillDownDialog('product', product)
}

const handleRegionClick = (params) => {
  const region = dashboardData.value.regionData[params.dataIndex]
  // 使用下钻对话框
  openDrillDownDialog('region', region)
}

const handleDialogClose = () => {
  detailItems.value = []
}

// ========== 下钻功能相关函数 ==========

// 打开下钻对话框
const openDrillDownDialog = async (type, data) => {
  currentDrillDownContext.value = { type, data }
  drillDownTab.value = 'projects'

  switch (type) {
    case 'trend':
      drillDownTitle.value = `${data.month} 投标数据详情`
      await loadTrendDrillDownData(data)
      break
    case 'competitor':
      drillDownTitle.value = `${data.name} 竞争分析详情`
      loadCompetitorDrillDownData(data)
      break
    case 'product':
      drillDownTitle.value = `${data.name} 产品线详情`
      loadProductDrillDownData(data)
      break
    case 'region':
      drillDownTitle.value = `${data.name} 区域详情`
      loadRegionDrillDownData(data)
      break
  }

  showDrillDownDialog.value = true
}

// 加载趋势下钻数据
const buildAggregateOnlyDrillDown = (stats) => ({
  projects: [],
  team: [],
  files: [],
  stats
})

const loadTrendDrillDownData = async (monthData) => {
  if (!isDemoMode) {
    try {
      const projectResult = await projectsApi.getList()
      const projects = projectResult?.success && Array.isArray(projectResult.data)
        ? projectResult.data.map((project) => ({
          id: project.id,
          name: project.name,
          customer: project.customer || '-',
          budget: project.budget || 0,
          status: project.status || '-',
          manager: project.manager || '-',
          result: project.status === 'won' ? 'won' : project.status === 'lost' ? 'lost' : null
        }))
        : []

      drillDownData.value = {
        projects,
        team: [],
        files: [],
        stats: {
          totalParticipation: monthData.bids,
          wonCount: monthData.wins,
          teamWinRate: monthData.rate,
          totalAmount: monthData.amount
        }
      }
      return
    } catch (error) {
      ElMessage.warning('真实项目明细加载失败，当前仅展示聚合统计')
      drillDownData.value = buildAggregateOnlyDrillDown({
        totalParticipation: monthData.bids,
        wonCount: monthData.wins,
        teamWinRate: monthData.rate,
        totalAmount: monthData.amount
      })
      return
    }
  }

  const mockProjects = getDemoDashboardProjects()

  drillDownData.value = {
    projects: mockProjects.map(p => ({
      id: p.id,
      name: p.name,
      customer: p.customer,
      budget: p.budget,
      status: p.status,
      manager: p.manager,
      result: p.result
    })),
    team: generateMockTeamData(),
    files: generateMockFiles(),
    stats: {
      totalParticipation: monthData.bids,
      wonCount: monthData.wins,
      teamWinRate: monthData.rate,
      totalAmount: monthData.amount
    }
  }
}

// 加载竞争对手下钻数据
const loadCompetitorDrillDownData = (competitorData) => {
  if (!isDemoMode) {
    drillDownData.value = buildAggregateOnlyDrillDown({
      totalParticipation: competitorData.bids,
      wonCount: Math.floor(competitorData.bids * (competitorData.share / 100)),
      teamWinRate: Math.floor(competitorData.share),
      totalAmount: competitorData.amount
    })
    return
  }

  drillDownData.value = {
    projects: generateCompetitorProjects(competitorData),
    team: generateCompetitorTeam(competitorData),
    files: generateMockFiles(),
    stats: {
      totalParticipation: competitorData.bids,
      wonCount: Math.floor(competitorData.bids * (competitorData.share / 100)),
      teamWinRate: Math.floor(competitorData.share),
      totalAmount: competitorData.amount
    }
  }
}

// 加载产品线下钻数据
const loadProductDrillDownData = (productData) => {
  if (!isDemoMode) {
    drillDownData.value = buildAggregateOnlyDrillDown({
      totalParticipation: productData.bids,
      wonCount: Math.floor(productData.bids * (productData.rate / 100)),
      teamWinRate: productData.rate,
      totalAmount: productData.revenue
    })
    return
  }

  drillDownData.value = {
    projects: generateProductLineProjects(productData),
    team: generateProductTeam(productData),
    files: generateProductFiles(productData),
    stats: {
      totalParticipation: productData.bids,
      wonCount: Math.floor(productData.bids * (productData.rate / 100)),
      teamWinRate: productData.rate,
      totalAmount: productData.revenue
    }
  }
}

// 加载区域下钻数据
const loadRegionDrillDownData = (regionData) => {
  if (!isDemoMode) {
    drillDownData.value = buildAggregateOnlyDrillDown({
      totalParticipation: regionData.bids,
      wonCount: Math.floor(regionData.bids * (regionData.rate / 100)),
      teamWinRate: regionData.rate,
      totalAmount: regionData.amount
    })
    return
  }

  drillDownData.value = {
    projects: generateRegionProjects(regionData),
    team: generateRegionTeam(regionData),
    files: generateRegionFiles(regionData),
    stats: {
      totalParticipation: regionData.bids,
      wonCount: Math.floor(regionData.bids * (regionData.rate / 100)),
      teamWinRate: regionData.rate,
      totalAmount: regionData.amount
    }
  }
}

// 生成模拟团队数据
const generateMockTeamData = () => [
  { name: '小王', role: '销售经理', dept: '华南销售部', participation: 15, winRate: 42 },
  { name: '张经理', role: '投标经理', dept: '投标管理部', participation: 20, winRate: 38 },
  { name: '李工', role: '技术总监', dept: '技术部', participation: 12, winRate: 45 },
  { name: '王经理', role: '商务经理', dept: '商务部', participation: 18, winRate: 35 }
]

// 生成竞争对手相关项目
const generateCompetitorProjects = (competitor) => [
  { id: 'CP001', name: `${competitor.name}投标项目A`, customer: '某国企', budget: 800, status: 'won', manager: '竞对负责人', result: 'won' },
  { id: 'CP002', name: `${competitor.name}投标项目B`, customer: '某央企', budget: 600, status: 'lost', manager: '竞对负责人', result: 'won' },
  { id: 'CP003', name: `${competitor.name}投标项目C`, customer: '某政府', budget: 450, status: 'bidding', manager: '竞对负责人', result: null }
]

// 生成竞争对手团队
const generateCompetitorTeam = (competitor) => [
  { name: '对手A', role: '销售总监', dept: '销售部', participation: competitor.bids - 5, winRate: 40 },
  { name: '对手B', role: '技术经理', dept: '技术部', participation: competitor.bids - 8, winRate: 35 }
]

// 生成产品线相关项目
const generateProductLineProjects = (product) => {
  const productProjects = {
    '智慧办公': [
      { id: 'P001', name: '某央企智慧办公平台采购', customer: '某央企集团', budget: 500, status: 'bidding', manager: '小王', result: null },
      { id: 'P004', name: '省政府OA系统升级', customer: '某省政府', budget: 300, status: 'won', manager: '张经理', result: 'won' },
      { id: 'P005', name: '集团协同办公平台', customer: '某集团', budget: 450, status: 'lost', manager: '小王', result: 'lost' }
    ],
    '工业软件': [
      { id: 'P006', name: '制造执行系统(MES)', customer: '某制造企业', budget: 600, status: 'won', manager: '李工', result: 'won' }
    ],
    '云服务': [
      { id: 'P007', name: '私有云平台建设', customer: '某数据中心', budget: 1200, status: 'won', manager: '张经理', result: 'won' },
      { id: 'P008', name: '混合云解决方案', customer: '某金融', budget: 800, status: 'bidding', manager: '小王', result: null }
    ],
    '数据中心': [
      { id: 'P009', name: '核心机房改造', customer: '某运营商', budget: 500, status: 'won', manager: '李工', result: 'won' }
    ]
  }
  return productProjects[product.name] || []
}

// 生成产品线团队
const generateProductTeam = (product) => {
  const teams = {
    '智慧办公': [
      { name: '张架构师', role: '解决方案架构师', dept: '解决方案部', participation: 10, winRate: 45 },
      { name: '王产品', role: '产品经理', dept: '产品部', participation: 8, winRate: 40 }
    ],
    '工业软件': [
      { name: '赵工', role: '工业软件专家', dept: '技术部', participation: 6, winRate: 38 }
    ],
    '云服务': [
      { name: '刘云架构', role: '云架构师', dept: '云计算部', participation: 12, winRate: 48 },
      { name: '陈运维', role: '运维专家', dept: '运维部', participation: 10, winRate: 42 }
    ],
    '数据中心': [
      { name: '周基建', role: '基础设施专家', dept: '工程部', participation: 8, winRate: 35 }
    ]
  }
  return teams[product.name] || generateMockTeamData()
}

// 生成产品线文件
const generateProductFiles = (product) => {
  const files = [
    { id: 'F001', name: `${product.name}技术方案模板.docx`, project: '模板库', uploader: '技术部', uploadTime: '2025-01-15 10:30', size: '2.5MB' },
    { id: 'F002', name: `${product.name}报价清单.xlsx`, project: '某央企项目', uploader: '商务部', uploadTime: '2025-02-10 14:20', size: '156KB' }
  ]
  if (product.name === '智慧办公') {
    files.push(
      { id: 'F003', name: '信创适配说明文档.pdf', project: '某央企项目', uploader: '李工', uploadTime: '2025-02-18 09:15', size: '1.8MB' },
      { id: 'F004', name: '办公平台演示.pptx', project: '省政府OA', uploader: '张经理', uploadTime: '2025-02-20 16:45', size: '8.2MB' }
    )
  }
  return files
}

// 生成区域相关项目
const generateRegionProjects = (region) => {
  const regionMap = {
    '华南': [
      { id: 'P002', name: '华南电力集团集采项目', customer: '华南电力集团', budget: 1200, status: 'reviewing', manager: '张经理', result: null },
      { id: 'P003', name: '深圳地铁自动化系统', customer: '深圳地铁集团', budget: 800, status: 'won', manager: '小王', result: 'won' }
    ],
    '华东': [
      { id: 'P010', name: '华东电网信息化项目', customer: '华东电网', budget: 900, status: 'won', manager: '张经理', result: 'won' }
    ],
    '华北': [
      { id: 'P001', name: '某央企智慧办公平台采购', customer: '某央企集团', budget: 500, status: 'bidding', manager: '小王', result: null }
    ],
    '西南': [
      { id: 'P011', name: '西部云数据中心建设', customer: '某数据中心', budget: 2000, status: 'bidding', manager: '李工', result: null }
    ],
    '西北': [
      { id: 'P012', name: '西安智慧园区项目', customer: '某园区', budget: 400, status: 'won', manager: '小王', result: 'won' }
    ]
  }
  return regionMap[region.name] || []
}

// 生成区域团队
const generateRegionTeam = (region) => {
  const regionTeams = {
    '华南': [
      { name: '小王', role: '华南区域销售', dept: '华南销售部', participation: 18, winRate: 40 },
      { name: '华南技术支持', role: '技术工程师', dept: '技术部-华南', participation: 12, winRate: 38 }
    ],
    '华东': [
      { name: '华东经理', role: '华东区域经理', dept: '华东销售部', participation: 15, winRate: 42 }
    ],
    '华北': [
      { name: '华北经理', role: '华北区域经理', dept: '华北销售部', participation: 14, winRate: 36 }
    ],
    '西南': [
      { name: '西南经理', role: '西南区域经理', dept: '西南销售部', participation: 10, winRate: 32 }
    ],
    '西北': [
      { name: '西北经理', role: '西北区域经理', dept: '西北销售部', participation: 8, winRate: 30 }
    ]
  }
  return regionTeams[region.name] || generateMockTeamData()
}

// 生成区域文件
const generateRegionFiles = (region) => [
  { id: 'RF001', name: `${region.name}区域投标策略.docx`, project: '策略文档', uploader: '市场部', uploadTime: '2025-01-20 11:00', size: '1.2MB' },
  { id: 'RF002', name: `${region.name}客户分布分析.xlsx`, project: '市场分析', uploader: '数据部', uploadTime: '2025-02-01 15:30', size: '890KB' }
]

// 生成通用模拟文件
const generateMockFiles = () => [
  { id: 'DF001', name: '技术方案v2.0.docx', project: '某央企项目', uploader: '李工', uploadTime: '2025-02-25 14:30', size: '2.3MB' },
  { id: 'DF002', name: '商务应答v1.5.docx', project: '某央企项目', uploader: '王经理', uploadTime: '2025-02-25 16:00', size: '1.8MB' },
  { id: 'DF003', name: '中标通知书.pdf', project: '深圳地铁', uploader: '小王', uploadTime: '2025-02-20 10:00', size: '0.5MB' },
  { id: 'DF004', name: '报价单.xlsx', project: '华南电力', uploader: '财务部', uploadTime: '2025-02-18 09:30', size: '156KB' }
]

// 辅助函数：获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    'bidding': 'warning',
    'reviewing': 'info',
    'won': 'success',
    'lost': 'danger',
    'pending': 'info'
  }
  return statusMap[status] || 'info'
}

// 辅助函数：获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'bidding': '投标中',
    'reviewing': '评审中',
    'won': '已中标',
    'lost': '未中标',
    'pending': '待处理',
    'tracking': '跟踪中',
    'bidded': '已投标',
    'abandoned': '已放弃'
  }
  return statusMap[status] || status
}

// 辅助函数：获取文件类型
const getFileType = (fileName) => {
  const ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  const typeMap = {
    '.docx': 'Word',
    '.doc': 'Word',
    '.xlsx': 'Excel',
    '.xls': 'Excel',
    '.pdf': 'PDF',
    '.pptx': 'PPT',
    '.ppt': 'PPT',
    '.zip': '压缩包',
    '.rar': '压缩包'
  }
  return typeMap[ext] || '其他'
}

// 辅助函数：获取文件类型颜色
const getFileTypeColor = (fileName) => {
  const ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  const colorMap = {
    '.docx': 'primary',
    '.doc': 'primary',
    '.xlsx': 'success',
    '.xls': 'success',
    '.pdf': 'danger',
    '.pptx': 'warning',
    '.ppt': 'warning',
    '.zip': 'info',
    '.rar': 'info'
  }
  return colorMap[ext] || 'info'
}

// 跳转到项目详情
const goToProject = (projectId) => {
  showDrillDownDialog.value = false
  router.push({ name: 'ProjectDetail', params: { id: projectId } })
}

// 预览文件
const previewFileDialogVisible = ref(false)
const previewFileUrl = ref('')
const previewFileName = ref('')

const previewFile = (file) => {
  previewFileName.value = file.name

  // 支持预览的文件类型
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  const previewableExts = ['.pdf', '.jpg', '.jpeg', '.png', '.gif', '.txt', '.md']

  if (previewableExts.includes(ext)) {
    // 模拟文件 URL - 实际项目中应从后端 API 获取
    previewFileUrl.value = file.url || `/api/files/preview/${file.id}`
    previewFileDialogVisible.value = true
  } else if (['.docx', '.doc', '.xlsx', '.xls', '.pptx', '.ppt'].includes(ext)) {
    // Office 文档提示下载或使用在线预览服务
    ElMessageBox.confirm(
        `${file.name} 暂不支持直接预览，是否下载查看？`,
        '文件预览',
        {
          confirmButtonText: '下载',
          cancelButtonText: '取消',
          type: 'info'
        }
    ).then(() => {
      downloadFile(file)
    }).catch(() => {})
  } else {
    ElMessage.warning(`文件类型 ${ext} 暂不支持预览，请下载后查看`)
  }
}

// 下载文件
const downloadFile = (file) => {
  try {
    const downloadUrl = file.url || `/api/files/download/${file.id}`
    const token = getAccessToken()

    fetch(downloadUrl, {
      method: 'GET',
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
      .then(response => {
        if (!response.ok) throw new Error('下载失败')
        return response.blob()
      })
      .then(blob => {
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = file.name
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
        ElMessage.success(`开始下载: ${file.name}`)
      })
      .catch(error => {
        ElMessage.error(`文件下载失败: ${error.message}`)
      })
  } catch (error) {
    ElMessage.error(`下载失败: ${error.message}`)
  }
}

// 导出下钻数据
const exportDrillDownData = () => {
  const { exportExcel } = useExport()
  const { type } = currentDrillDownContext.value

  // 构建导出参数
  const params = {
    metricType: type,
    startDate: dateRange.value?.[0] || null,
    endDate: dateRange.value?.[1] || null,
    status: metricFilters.value?.status || null,
    role: metricFilters.value?.role || null
  }

  exportExcel(ExportType.DASHBOARD_DRILLDOWN, params, '数据明细导出成功')
}

const viewMore = () => {
  ElMessage.info('跳转到详情页面...')
  detailDialogVisible.value = false
}

watch(
  () => [route.query.drilldown, route.query.status, route.query.role, route.query.outcome],
  async ([drilldown, status, role, outcome]) => {
    if (!drilldown || loading.value) return

    const nextFilters = {}
    if (status) nextFilters.status = String(status).toUpperCase()
    if (role) nextFilters.role = String(role).toUpperCase()
    if (outcome) nextFilters.outcome = String(outcome).toUpperCase()

    await openMetricDrillDown(String(drilldown), { filters: nextFilters })
  }
)

onMounted(async () => {
  await loadData()
  if (route.query.drilldown) {
    const nextFilters = {}
    if (route.query.status) nextFilters.status = String(route.query.status).toUpperCase()
    if (route.query.role) nextFilters.role = String(route.query.role).toUpperCase()
    if (route.query.outcome) nextFilters.outcome = String(route.query.outcome).toUpperCase()
    await openMetricDrillDown(String(route.query.drilldown), { filters: nextFilters })
  }
})
</script>

<style scoped>
.analytics-dashboard {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100%;
}

/* Loading State */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #909399;
}

.loading-container .el-icon {
  font-size: 32px;
  color: #409eff;
  margin-bottom: 16px;
}

.loading-container p {
  font-size: 14px;
  margin: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

/* Metric Cards - using B2B classes */
.metric-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.b2b-metric-card {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.b2b-metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}

.metric-drawer {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-drawer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-drawer-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.metric-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.metric-summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
  background: linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%);
}

.summary-label {
  font-size: 12px;
  color: #64748B;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
  color: #0F172A;
}

.metric-drawer-pagination {
  display: flex;
  justify-content: flex-end;
}

/* Chart Cards */
.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.dashboard-section {
  margin-bottom: 20px;
}

.chart-card-large {
  grid-column: 1 / -1;
}

.chart-card {
  background: #fff;
  border-radius: var(--card-border-radius, 8px);
  padding: var(--card-padding, 20px);
  box-shadow: var(--card-shadow, 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04));
  border: var(--card-border, 1px solid #E8E8E8);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

/* Detail Dialog */
.detail-content {
  padding: 10px 0;
}

/* Responsive */
@media (max-width: 1400px) {
  .metric-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .dashboard-page {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .page-title {
    font-size: 20px;
  }

  .metric-cards {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .metric-drawer-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .charts-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .chart-card {
    min-height: 250px;
  }

  .chart-card :deep(.el-card__header) {
    padding: 12px 16px;
  }

  .chart-card :deep(.el-card__body) {
    padding: 16px;
  }

  /* 表格移动端优化 */
  .table-card :deep(.el-table) {
    font-size: 12px;
  }

  .table-card :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }

  /* 选择器移动端优化 */
  .header-controls {
    width: 100%;
  }

  .header-controls .el-select,
  .header-controls .el-date-picker {
    width: 100%;
  }

  /* 对话框移动端优化 */
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }

  :deep(.el-dialog__body) {
    padding: 16px;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .el-button {
    min-height: 44px;
  }
}

/* ========== 下钻对话框样式 ========== */
.drill-down-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.drill-down-dialog :deep(.el-tabs--border-card) {
  border: none;
  box-shadow: none;
}

.drill-down-dialog :deep(.el-tabs__content) {
  padding: 20px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-badge {
  margin-left: 4px;
}

.team-section {
  padding: 10px 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.team-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-value.stat-success {
  color: #67c23a;
}

.stat-value.stat-warning {
  color: #e6a23c;
}

.stat-value.stat-primary {
  color: #409eff;
}

/* 下钻对话框响应式 */
@media (max-width: 768px) {
  .drill-down-dialog :deep(.el-dialog) {
    width: 95% !important;
  }

  .drill-down-dialog :deep(.el-tabs__content) {
    padding: 12px;
  }

  .team-stats {
    grid-template-columns: 1fr;
    padding: 12px;
  }

  .stat-value {
    font-size: 20px;
  }
}

/* 文件预览对话框样式 */
.file-preview-container {
  width: 100%;
  height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-preview-frame {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-placeholder {
  text-align: center;
  color: #909399;
}

.preview-placeholder .el-icon {
  margin-bottom: 16px;
  color: #c0c4cc;
}

.preview-placeholder p {
  font-size: 14px;
  margin: 0;
}
</style>
