<template>
  <div class="bid-result-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">投标结果闭环</h2>
        <p class="page-subtitle">结果登记、通知书上传、竞争对手分析</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="awardDialogVisible = true">
          <el-icon><Plus /></el-icon>手工登记投标结果
        </el-button>
      </div>
    </div>

    <div v-loading="pageLoading">
      <OverviewCards
        :overview="overview"
        :syncing="syncing"
        :fetching="fetching"
        :remind-loading="sendingAllReminders"
        :report-loading="competitorReportLoading"
        @sync="handleSyncInternal"
        @fetch="handleAutoFetch"
        @remind="handleSendRemindAll"
        @report="handleShowReport"
      />
      <FetchResultsTable
        :rows="fetchResults"
        v-model:selected-ids="selectedFetchIds"
        @confirm="handleConfirm"
        @ignore="handleIgnore"
        @detail="handleViewDetail"
        @confirm-batch="handleConfirmAll"
      />
      <ReminderTable
        :rows="reminderRecords"
        :loading="sendingAllReminders"
        @remind-all="handleSendRemindAll"
        @remind-again="handleRemindAgain"
      />
    </div>

    <RegisterAwardDialog
      v-model:visible="awardDialogVisible"
      :project-options="projectOptions"
      @saved="loadPage"
    />
    <RegisterCompetitorWinDialog
      v-model:visible="competitorWinDialogVisible"
      :competitor-options="competitorOptions"
      :project-options="projectOptions"
      @saved="handleShowReport"
    />
    <BidResultDetailDialog v-model:visible="detailVisible" :detail="detailRecord" />
    <CompetitorReportDialog
      v-model:visible="reportVisible"
      :rows="competitorData"
      @register-win="competitorWinDialogVisible = true"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { bidResultsApi, projectsApi, competitionIntelApi } from '@/api'
import OverviewCards from './bid-result/OverviewCards.vue'
import FetchResultsTable from './bid-result/FetchResultsTable.vue'
import ReminderTable from './bid-result/ReminderTable.vue'
import RegisterAwardDialog from './bid-result/RegisterAwardDialog.vue'
import RegisterCompetitorWinDialog from './bid-result/RegisterCompetitorWinDialog.vue'
import BidResultDetailDialog from './bid-result/BidResultDetailDialog.vue'
import CompetitorReportDialog from './bid-result/CompetitorReportDialog.vue'

const pageLoading = ref(false)
const syncing = ref(false)
const fetching = ref(false)
const sendingAllReminders = ref(false)
const competitorReportLoading = ref(false)

const overview = ref({ lastSyncTime: '', pendingCount: 0, uploadPending: 0, competitorCount: 0 })
const fetchResults = ref([])
const reminderRecords = ref([])
const competitorData = ref([])
const selectedFetchIds = ref([])
const projectOptions = ref([])
const competitorOptions = ref([])

const reportVisible = ref(false)
const detailVisible = ref(false)
const detailRecord = ref(null)
const awardDialogVisible = ref(false)
const competitorWinDialogVisible = ref(false)

const unwrap = async (promise, fallback) => {
  const result = await promise
  if (!result?.success) throw new Error(result?.message || fallback)
  return result.data
}

const runAction = async (promise, fallback, successMsg) => {
  try {
    const data = await unwrap(promise, fallback)
    ElMessage.success(data?.message || successMsg)
    await loadPage()
  } catch (error) {
    ElMessage.error(error?.message || fallback)
  }
}

const loadPage = async () => {
  pageLoading.value = true
  try {
    const [ov, fr, rm] = await Promise.all([
      unwrap(bidResultsApi.getOverview(), '加载概览失败'),
      unwrap(bidResultsApi.getFetchResults(), '加载待确认结果失败'),
      unwrap(bidResultsApi.getReminders(), '加载提醒记录失败')
    ])
    overview.value = ov
    fetchResults.value = fr
    reminderRecords.value = rm
  } catch (error) {
    ElMessage.error(error?.message || '加载投标结果闭环失败')
  } finally {
    pageLoading.value = false
  }
}

const loadDropdowns = async () => {
  try {
    const [projects, competitors] = await Promise.all([
      projectsApi.getList(),
      competitionIntelApi.getCompetitors()
    ])
    projectOptions.value = (projects?.data || []).map((p) => ({ id: p.id, name: p.name }))
    competitorOptions.value = (competitors?.data || []).map((c) => ({ id: c.id, name: c.companyName || c.name }))
  } catch (error) {
    ElMessage.warning(error?.message || '项目 / 竞争对手下拉加载失败，请刷新页面')
  }
}

const handleSyncInternal = async () => {
  syncing.value = true
  await runAction(bidResultsApi.sync(), '同步失败', '同步完成')
  syncing.value = false
}

const handleAutoFetch = async () => {
  fetching.value = true
  await runAction(bidResultsApi.fetch(), '抓取失败', '抓取完成')
  fetching.value = false
}

const handleConfirm = (row) => runAction(bidResultsApi.confirm(row.id), '确认失败', '已确认该结果')

const handleConfirmAll = async () => {
  if (selectedFetchIds.value.length === 0) return
  await runAction(bidResultsApi.confirmBatch(selectedFetchIds.value), '批量确认失败', '批量确认完成')
  selectedFetchIds.value = []
}

const handleIgnore = async (row) => {
  try {
    await ElMessageBox.confirm(`确认忽略"${row.projectName}"吗？`, '忽略结果', {
      type: 'warning', confirmButtonText: '确认忽略', cancelButtonText: '取消'
    })
    await runAction(bidResultsApi.ignore(row.id, '人工忽略'), '忽略失败', '已忽略该结果')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.message || '忽略失败')
  }
}

const handleViewDetail = async (row) => {
  try {
    detailRecord.value = await unwrap(bidResultsApi.getDetail(row.id), '加载详情失败')
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '加载详情失败')
  }
}

const handleSendRemindAll = async () => {
  if (fetchResults.value.length === 0) {
    ElMessage.info('当前没有可提醒的待确认结果')
    return
  }
  sendingAllReminders.value = true
  const ids = fetchResults.value.map((item) => item.id)
  await runAction(bidResultsApi.sendReminderBatch(ids, '请尽快上传结果资料'), '批量提醒失败', '批量提醒完成')
  sendingAllReminders.value = false
}

const handleRemindAgain = async (row) => {
  if (!row.lastResultId) {
    ElMessage.info('当前提醒记录没有关联结果，暂不可再次提醒')
    return
  }
  await runAction(
    bidResultsApi.sendReminder(row.lastResultId, '再次提醒上传结果资料'),
    '再次提醒失败',
    `已再次提醒 ${row.owner}`
  )
}

const handleShowReport = async () => {
  competitorReportLoading.value = true
  try {
    competitorData.value = await unwrap(bidResultsApi.getCompetitorReport(), '加载竞争报表失败')
    reportVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '加载竞争报表失败')
  } finally {
    competitorReportLoading.value = false
  }
}

onMounted(() => {
  loadPage()
  loadDropdowns()
})
</script>

<style scoped>
.bid-result-page { padding: 20px; }
.page-header {
  margin-bottom: 24px; display: flex;
  justify-content: space-between; align-items: flex-start;
}
.page-title { font-size: 24px; font-weight: 600; margin: 0 0 8px 0; color: #1a1a1a; }
.page-subtitle { font-size: 14px; color: #909399; margin: 0; }
</style>
