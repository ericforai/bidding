<template>
  <div class="bidding-detail-page">
    <div class="breadcrumb-nav">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/bidding' }">标讯中心</el-breadcrumb-item>
        <el-breadcrumb-item>标讯详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div v-if="tender" class="detail-content">
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="detail-card-header">
            <div class="detail-header-left">
              <h2 class="tender-title">{{ tender.title }}</h2>
              <div class="tags-row">
                <el-tag v-for="tag in tender.tags" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
            </div>
            <div v-if="matchScoreState === 'ready' && matchScore" class="detail-header-right">
              <div class="ai-score-large" :class="getScoreClass(matchScore.totalScore)">
                <div class="score-value">{{ matchScore.totalScore }}分</div>
                <div class="score-label">匹配评分</div>
              </div>
            </div>
          </div>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="标题" :span="3">
            {{ tender.title }}
          </el-descriptions-item>
          <el-descriptions-item label="预算金额">
            <span class="amount-text">{{ formatBudgetWan(tender.budget) }}万元</span>
          </el-descriptions-item>
          <el-descriptions-item label="总部所在地">
            <el-tooltip v-if="regionMeta.isMissing" :content="regionMeta.tooltip" placement="top">
              <span class="field-missing">{{ regionMeta.text }}</span>
            </el-tooltip>
            <span v-else>{{ regionMeta.text }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="招标机构">
            {{ tender.tenderAgency || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="业主单位">
            {{ tender.purchaserName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="报名截止时间">
            <span v-if="tender.registrationDeadline">{{ formatTenderDate(tender.registrationDeadline) }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="开标时间">
            <span v-if="tender.bidOpeningTime">{{ formatTenderDate(tender.bidOpeningTime) }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="所属行业">
            <el-tooltip v-if="industryMeta.isMissing" :content="industryMeta.tooltip" placement="top">
              <span class="field-missing">{{ industryMeta.text }}</span>
            </el-tooltip>
            <span v-else>{{ industryMeta.text }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">
            {{ tender.contactName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系方式">
            {{ tender.contactPhone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发布日期">{{ formatTenderDate(tender.publishDate || tender.date) }}</el-descriptions-item>
          <el-descriptions-item label="截止日期">
            <span class="deadline-display" :class="getDeadlineClass(tender.deadline)">
              <span>{{ deadlineParts.date }}</span>
              <template v-if="deadlineParts.hasTime">
                <span class="deadline-separator">|</span>
                <span>{{ deadlineParts.time }}</span>
              </template>
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="客户类型">
            {{ tender.customerType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            {{ tender.priority || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusType(tender.status)" size="small">{{ getStatusText(tender.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="项目经理">
            {{ tender.projectManagerName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="分配人">
            {{ tender.assigneeName || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons">
          <el-button
            v-if="tender && safeTenderUrl(tender.originalUrl)"
            type="success"
            size="large"
            @click="handleViewOriginal"
          >
            <el-icon><Link /></el-icon>
            查看官网公告
          </el-button>
        </div>
      </el-card>

      <TenderEvaluationForm
        v-if="tender"
        :evaluation="tenderEvaluation"
        :current-user-role="currentUserRole"
        :tender-id="Number(tender.id)"
        @submit="handleEvaluationSubmit"
        @save-draft="handleEvaluationSaveDraft"
        @bid="handleParticipate"
        @abandon="handleAbandonWithReason"
      />

    </div>

    <div v-else class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Link } from '@element-plus/icons-vue'
import { formatBudgetWan, formatTenderDate, safeTenderUrl } from '../bidding-utils.js'
import { useBiddingDetailPage } from './useBiddingDetailPage.js'
import { useUserStore } from '@/stores/user'
import { tendersApi } from '@/api'
import TenderEvaluationForm from './TenderEvaluationForm.vue'
import './styles/detail-layout.css'
import './styles/detail-overrides.css'

const {
  tender,
  matchScore,
  matchScoreState,
  regionMeta,
  industryMeta,
  deadlineParts,
  getScoreClass,
  getStatusType,
  getStatusText,
  getDeadlineClass,
  handleParticipate,
  handleViewOriginal,
  handleAbandon,
} = useBiddingDetailPage()

// Reference handleAbandon so existing logic remains accessible even though
// the button now lives inside <TenderEvaluationForm>.
void handleAbandon

const userStore = useUserStore()
const currentUserRole = computed(() => userStore?.userRole || 'STAFF')

// Evaluation payload owned by the parent — the form is a pure presentational
// child. Backend wiring (load / save / submit) lives here so the form stays
// focused on form-state + emits.
const tenderEvaluation = ref(null)
// M4: prevent double-click submit/save while a request is in flight.
const submitting = ref(false)
const savingDraft = ref(false)

// V119: load existing evaluation (or empty DRAFT) as soon as the tender is
// resolved by useBiddingDetailPage. Watcher keeps it in sync if the tender id
// changes (e.g. route param swap without remounting the page).
watch(
  () => tender.value?.id,
  async (id) => {
    if (!id) return
    try {
      const result = await tendersApi.loadEvaluation(id)
      if (result?.success !== false) {
        tenderEvaluation.value = result?.data || null
      }
    } catch (e) {
      // Non-fatal: a missing evaluation (or a permission gate) should not
      // block the detail view from rendering.
      console.warn('loadEvaluation failed:', e?.message || e)
    }
  },
  { immediate: true }
)

async function handleEvaluationSaveDraft(payload) {
  if (!tender.value || savingDraft.value) return
  savingDraft.value = true
  try {
    const result = await tendersApi.saveEvaluationDraft(tender.value.id, payload)
    if (result?.success !== false) {
      tenderEvaluation.value = result?.data || { ...payload, evaluationStatus: 'DRAFT' }
      ElMessage.success('草稿已保存')
    } else {
      ElMessage.error(result?.message || '草稿保存失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '草稿保存失败')
  } finally {
    savingDraft.value = false
  }
}

async function handleEvaluationSubmit(payload) {
  if (!tender.value || submitting.value) return
  submitting.value = true
  try {
    const result = await tendersApi.submitEvaluationFinal(tender.value.id, payload)
    if (result?.success !== false) {
      tenderEvaluation.value = result?.data || { ...payload, evaluationStatus: 'SUBMITTED' }
      ElMessage.success('评估已提交')
    } else {
      ElMessage.error(result?.message || '评估提交失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '评估提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleAbandonWithReason({ reason }) {
  if (!tender.value) return
  try {
    const result = await tendersApi.abandon(tender.value.id, { reason })
    if (result?.success && result?.data?.accepted) {
      ElMessage.success(result.data.message || '已放弃该标讯')
      tender.value = { ...tender.value, status: 'ABANDONED' }
    } else {
      ElMessage.warning(result?.data?.message || '弃标失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '弃标失败')
  }
}
</script>
