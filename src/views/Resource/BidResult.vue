<template>
  <div class="bid-result-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">投标结果闭环</h2>
        <p class="page-subtitle">结果登记、通知书上传、竞争对手分析</p>
      </div>
    </div>

    <el-row :gutter="20" v-loading="pageLoading">
      <el-col :span="6">
        <el-card class="feature-card" shadow="hover">
          <div class="card-icon system">
            <el-icon><Connection /></el-icon>
          </div>
          <h3 class="card-title">内部系统同步</h3>
          <p class="card-desc">从 ERP/CRM 获取中标结果</p>
          <el-button type="primary" plain :loading="syncing" @click="handleSyncInternal">
            <el-icon><Refresh /></el-icon>
            立即同步
          </el-button>
          <div class="card-stats">
            <span>最近同步: {{ formatDateTime(overview.lastSyncTime) || '暂无' }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="feature-card" shadow="hover">
          <div class="card-icon spider">
            <el-icon><Monitor /></el-icon>
          </div>
          <h3 class="card-title">公开信息抓取</h3>
          <p class="card-desc">生成待确认结果并进入人工核验</p>
          <el-button type="success" plain :loading="fetching" @click="handleAutoFetch">
            <el-icon><Search /></el-icon>
            开始抓取
          </el-button>
          <div class="card-stats">
            <el-tag size="small" type="warning">{{ overview.pendingCount }} 条待确认</el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="feature-card" shadow="hover">
          <div class="card-icon upload">
            <el-icon><Bell /></el-icon>
          </div>
          <h3 class="card-title">上传提醒</h3>
          <p class="card-desc">提醒负责人上传通知书/分析报告</p>
          <el-button type="warning" plain :loading="sendingAllReminders" @click="handleSendRemind">
            <el-icon><Message /></el-icon>
            发送提醒
          </el-button>
          <div class="card-stats">
            <span>{{ overview.uploadPending }} 人待上传</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="feature-card" shadow="hover">
          <div class="card-icon analysis">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <h3 class="card-title">竞争对手分析</h3>
          <p class="card-desc">基于真实竞争分析记录生成报表</p>
          <el-button type="danger" plain :loading="competitorReportLoading" @click="handleShowReport">
            <el-icon><Document /></el-icon>
            查看报表
          </el-button>
          <div class="card-stats">
            <span>{{ overview.competitorCount }} 条记录</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="result-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>待确认的抓取结果</span>
          <el-button type="primary" size="small" :disabled="selectedFetchIds.length === 0" @click="handleConfirmAll">
            批量确认
          </el-button>
        </div>
      </template>
      <el-table :data="fetchResults" stripe @selection-change="handleFetchSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="source" label="来源" width="150" />
        <el-table-column prop="projectName" label="项目名称" min-width="220" />
        <el-table-column prop="result" label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === 'won' ? 'success' : 'info'" size="small">
              {{ row.result === 'won' ? '中标' : '未中标' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="抓取时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.fetchTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleConfirm(row)">确认</el-button>
            <el-button size="small" type="danger" link @click="handleIgnore(row)">忽略</el-button>
            <el-button size="small" link @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="fetchResults.length === 0" description="当前没有待确认结果" :image-size="72" />
    </el-card>

    <el-card class="reminder-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>上传提醒记录</span>
          <el-button type="primary" size="small" :loading="sendingAllReminders" @click="handleSendRemindAll">
            全部提醒
          </el-button>
        </div>
      </template>
      <el-table :data="reminderRecords" stripe>
        <el-table-column prop="projectName" label="项目名称" min-width="220" />
        <el-table-column prop="owner" label="负责人" width="120" />
        <el-table-column prop="type" label="提醒类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.type === 'notice'" type="warning" size="small">中标通知书</el-tag>
            <el-tag v-else type="info" size="small">分析报告</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'pending'" type="danger" size="small">未上传</el-tag>
            <el-tag v-else-if="row.status === 'uploaded'" type="success" size="small">已上传</el-tag>
            <el-tag v-else type="info" size="small">已提醒</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提醒时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.remindTime) }}</template>
        </el-table-column>
        <el-table-column prop="lastReminderComment" label="最近说明" min-width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'uploaded' && row.lastResultId"
              size="small"
              type="primary"
              link
              @click="handleRemindAgain(row)"
            >
              再次提醒
            </el-button>
            <el-button v-else size="small" link disabled>已完成</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="reminderRecords.length === 0" description="当前没有提醒记录" :image-size="72" />
    </el-card>

    <el-dialog v-model="detailVisible" title="结果详情" width="640px">
      <template v-if="detailRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名称">{{ detailRecord.projectName }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ detailRecord.source }}</el-descriptions-item>
          <el-descriptions-item label="结果">{{ detailRecord.result === 'won' ? '中标' : '未中标' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ translateFetchStatus(detailRecord.status) }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detailRecord.ownerName || '待分配' }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ formatAmount(detailRecord.amount) }}</el-descriptions-item>
          <el-descriptions-item label="抓取时间">{{ formatDateTime(detailRecord.fetchTime) }}</el-descriptions-item>
          <el-descriptions-item label="项目 ID">{{ detailRecord.projectId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="忽略原因" :span="2">{{ detailRecord.ignoredReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提醒类型" :span="2">
            {{ (detailRecord.reminderTypes || []).join(' / ') || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <el-dialog v-model="reportVisible" title="竞争对手分析报表" width="900px">
      <el-table :data="competitorData" border>
        <el-table-column prop="company" label="竞争对手" width="180" fixed="left" />
        <el-table-column prop="skuCount" label="分析记录数" width="110" align="center" />
        <el-table-column prop="category" label="品类" width="120" />
        <el-table-column prop="discount" label="折扣" width="100" align="center" />
        <el-table-column prop="payment" label="账期" width="120" />
        <el-table-column prop="winRate" label="赢面均值" width="100" align="center" />
        <el-table-column prop="projectCount" label="项目数" width="100" align="center" />
        <el-table-column prop="trend" label="趋势" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.trend === 'up'" style="color: #67c23a"><CaretTop /></el-icon>
            <el-icon v-else-if="row.trend === 'down'" style="color: #f56c6c"><CaretBottom /></el-icon>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="competitorData.length === 0" description="当前没有竞争分析数据" :image-size="72" />
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Connection, Monitor, Bell, DataAnalysis, Refresh, Search, Message,
  Document, CaretTop, CaretBottom
} from '@element-plus/icons-vue'
import { bidResultsApi } from '@/api'

const pageLoading = ref(false)
const syncing = ref(false)
const fetching = ref(false)
const sendingAllReminders = ref(false)
const competitorReportLoading = ref(false)

const overview = ref({
  lastSyncTime: '',
  pendingCount: 0,
  uploadPending: 0,
  competitorCount: 0,
})
const fetchResults = ref([])
const reminderRecords = ref([])
const competitorData = ref([])
const selectedFetchIds = ref([])
const reportVisible = ref(false)
const detailVisible = ref(false)
const detailRecord = ref(null)

const formatDateTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  const amount = Number(value)
  if (Number.isNaN(amount)) return String(value)
  return `${amount.toFixed(2)} 元`
}

const translateFetchStatus = (status) => {
  const map = { pending: '待确认', confirmed: '已确认', ignored: '已忽略' }
  return map[status] || status || '-'
}

const loadOverview = async () => {
  const result = await bidResultsApi.getOverview()
  if (!result?.success) {
    throw new Error(result?.message || '加载概览失败')
  }
  overview.value = result.data
}

const loadFetchResults = async () => {
  const result = await bidResultsApi.getFetchResults()
  if (!result?.success) {
    throw new Error(result?.message || '加载待确认结果失败')
  }
  fetchResults.value = result.data
}

const loadReminders = async () => {
  const result = await bidResultsApi.getReminders()
  if (!result?.success) {
    throw new Error(result?.message || '加载提醒记录失败')
  }
  reminderRecords.value = result.data
}

const loadPage = async () => {
  pageLoading.value = true
  try {
    await Promise.all([loadOverview(), loadFetchResults(), loadReminders()])
  } catch (error) {
    ElMessage.error(error?.message || '加载投标结果闭环失败')
  } finally {
    pageLoading.value = false
  }
}

const handleFetchSelectionChange = (rows) => {
  selectedFetchIds.value = rows.map((item) => item.id)
}

const handleSyncInternal = async () => {
  syncing.value = true
  try {
    const result = await bidResultsApi.sync()
    if (!result?.success) {
      throw new Error(result?.message || '同步失败')
    }
    ElMessage.success(result?.data?.message || '同步完成')
    await loadPage()
  } catch (error) {
    ElMessage.error(error?.message || '同步失败')
  } finally {
    syncing.value = false
  }
}

const handleAutoFetch = async () => {
  fetching.value = true
  try {
    const result = await bidResultsApi.fetch()
    if (!result?.success) {
      throw new Error(result?.message || '抓取失败')
    }
    ElMessage.success(result?.data?.message || '抓取完成')
    await loadPage()
  } catch (error) {
    ElMessage.error(error?.message || '抓取失败')
  } finally {
    fetching.value = false
  }
}

const handleConfirm = async (row) => {
  try {
    const result = await bidResultsApi.confirm(row.id)
    if (!result?.success) {
      throw new Error(result?.message || '确认失败')
    }
    ElMessage.success('已确认该结果')
    await loadPage()
  } catch (error) {
    ElMessage.error(error?.message || '确认失败')
  }
}

const handleConfirmAll = async () => {
  if (selectedFetchIds.value.length === 0) return
  try {
    const result = await bidResultsApi.confirmBatch(selectedFetchIds.value)
    if (!result?.success) {
      throw new Error(result?.message || '批量确认失败')
    }
    ElMessage.success(result?.data?.message || '批量确认完成')
    selectedFetchIds.value = []
    await loadPage()
  } catch (error) {
    ElMessage.error(error?.message || '批量确认失败')
  }
}

const handleIgnore = async (row) => {
  try {
    await ElMessageBox.confirm(`确认忽略“${row.projectName}”吗？`, '忽略结果', {
      type: 'warning',
      confirmButtonText: '确认忽略',
      cancelButtonText: '取消',
    })
    const result = await bidResultsApi.ignore(row.id, '人工忽略')
    if (!result?.success) {
      throw new Error(result?.message || '忽略失败')
    }
    ElMessage.success('已忽略该结果')
    await loadPage()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '忽略失败')
    }
  }
}

const handleViewDetail = async (row) => {
  try {
    const result = await bidResultsApi.getDetail(row.id)
    if (!result?.success) {
      throw new Error(result?.message || '加载详情失败')
    }
    detailRecord.value = result.data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '加载详情失败')
  }
}

const handleSendRemind = async () => {
  const target = fetchResults.value.find((item) => item.result === 'won') || fetchResults.value[0]
  if (!target) {
    ElMessage.info('当前没有可提醒的待确认结果')
    return
  }
  sendingAllReminders.value = true
  try {
    const result = await bidResultsApi.sendReminder(target.id, '请及时上传结果资料')
    if (!result?.success) {
      throw new Error(result?.message || '发送提醒失败')
    }
    ElMessage.success('提醒已发送')
    await loadPage()
  } catch (error) {
    ElMessage.error(error?.message || '发送提醒失败')
  } finally {
    sendingAllReminders.value = false
  }
}

const handleSendRemindAll = async () => {
  if (fetchResults.value.length === 0) {
    ElMessage.info('当前没有可提醒的待确认结果')
    return
  }
  sendingAllReminders.value = true
  try {
    const result = await bidResultsApi.sendReminderBatch(fetchResults.value.map((item) => item.id), '请尽快上传结果资料')
    if (!result?.success) {
      throw new Error(result?.message || '批量提醒失败')
    }
    ElMessage.success(result?.data?.message || '批量提醒完成')
    await loadPage()
  } catch (error) {
    ElMessage.error(error?.message || '批量提醒失败')
  } finally {
    sendingAllReminders.value = false
  }
}

const handleRemindAgain = async (row) => {
  if (!row.lastResultId) {
    ElMessage.info('当前提醒记录没有关联结果，暂不可再次提醒')
    return
  }
  try {
    const result = await bidResultsApi.sendReminder(row.lastResultId, '再次提醒上传结果资料')
    if (!result?.success) {
      throw new Error(result?.message || '再次提醒失败')
    }
    ElMessage.success(`已再次提醒 ${row.owner}`)
    await loadPage()
  } catch (error) {
    ElMessage.error(error?.message || '再次提醒失败')
  }
}

const handleShowReport = async () => {
  competitorReportLoading.value = true
  try {
    const result = await bidResultsApi.getCompetitorReport()
    if (!result?.success) {
      throw new Error(result?.message || '加载竞争报表失败')
    }
    competitorData.value = result.data
    reportVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '加载竞争报表失败')
  } finally {
    competitorReportLoading.value = false
  }
}

onMounted(() => {
  loadPage()
})
</script>

<style scoped>
.bid-result-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #1a1a1a;
}

.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.feature-card {
  text-align: center;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.feature-card:hover {
  transform: translateY(-4px);
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 28px;
  color: #fff;
}

.card-icon.system {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.card-icon.spider {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.card-icon.upload {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.card-icon.analysis {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #303133;
}

.card-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 16px 0;
}

.card-stats {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

.result-card,
.reminder-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
