<template>
  <div class="evaluation-stage">
    <!-- 顶部按钮栏 -->
    <div class="action-bar">
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      <el-button type="success" @click="handleBid">投标</el-button>
      <el-button type="danger" @click="handleAbandon">弃标</el-button>
    </div>

    <el-divider />

    <!-- 主体内容：左侧表单 + 右侧状态 -->
    <div class="main-content">
      <!-- 左侧：评估表单 -->
      <div class="form-section">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="140px"
          size="default"
        >
          <el-form-item label="项目背景" prop="background">
            <el-input
              v-model="formData.background"
              type="textarea"
              :rows="4"
              placeholder="请输入项目背景"
            />
          </el-form-item>

          <el-form-item label="竞争对手情况" prop="competitors">
            <el-input
              v-model="formData.competitors"
              type="textarea"
              :rows="4"
              placeholder="请输入竞争对手情况"
            />
          </el-form-item>

          <el-form-item label="项目合同周期" prop="contractPeriod">
            <el-input
              v-model="formData.contractPeriod"
              placeholder="如：12个月、2年"
              maxlength="64"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="入围家数" prop="shortlistedBidders">
            <el-input-number
              v-model="formData.shortlistedBidders"
              :min="1"
              :max="999"
              placeholder="请输入入围家数"
            />
          </el-form-item>

          <el-form-item label="平台服务费" prop="platformFee">
            <el-input-number
              v-model="formData.platformFee"
              :precision="2"
              :min="0"
              :max="9999999999.99"
              placeholder="请输入平台服务费"
            />
          </el-form-item>

          <el-form-item label="上一轮报价情况" prop="previousBid">
            <el-input
              v-model="formData.previousBid"
              type="textarea"
              :rows="3"
              placeholder="请输入上一轮报价情况（非必填）"
            />
          </el-form-item>

          <el-form-item label="建议是否投标" prop="recommendation">
            <el-radio-group v-model="formData.recommendation">
              <el-radio :value="true">建议投标</el-radio>
              <el-radio :value="false">不建议投标</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存表单</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 右侧：评估状态信息（只读） -->
      <div class="status-section">
        <el-card shadow="never" class="status-card">
          <template #header>
            <span>评估状态信息</span>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="当前子阶段">
              {{ view?.subStage || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="评标开始时间">
              {{ view?.evaluationStartedAt ? formatDate(view.evaluationStartedAt) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="收到定标材料时间">
              {{ view?.boardReceivedAt ? formatDate(view.boardReceivedAt) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="公示时间">
              {{ view?.announcedAt ? formatDate(view.announcedAt) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="建议是否投标">
              {{ formatRecommendation(view?.recommendation) }}
            </el-descriptions-item>
            <el-descriptions-item label="最后更新人">
              {{ view?.updatedBy || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="最后更新时间">
              {{ view?.updatedAt ? formatDate(view.updatedAt) : '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 子阶段切换 -->
        <el-card shadow="never" class="status-card" style="margin-top: 16px">
          <template #header>
            <span>子阶段切换</span>
          </template>
          <el-form label-width="100px" size="small">
            <el-form-item label="切换至">
              <el-select v-model="targetSubStage" placeholder="选择子阶段" style="width: 100%">
                <el-option label="评标进行中" value="IN_PROGRESS" />
                <el-option label="待定标" value="AWAITING_BOARD" />
                <el-option label="结果已公示" value="ANNOUNCED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" :loading="transitioning" @click="handleTransition">
                确认切换
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectLifecycleApi } from '@/api/modules/projectLifecycle.js'

const props = defineProps({
  projectId: { type: [String, Number], required: true }
})

const formRef = ref(null)
const view = ref(null)
const targetSubStage = ref('')
const saving = ref(false)
const submitting = ref(false)
const transitioning = ref(false)

const formData = reactive({
  background: '',
  competitors: '',
  contractPeriod: '',
  shortlistedBidders: null,
  platformFee: null,
  previousBid: '',
  recommendation: null
})

const formRules = {
  background: [{ required: true, message: '请输入项目背景', trigger: 'blur' }],
  competitors: [{ required: true, message: '请输入竞争对手情况', trigger: 'blur' }],
  contractPeriod: [{ required: true, message: '请输入项目合同周期', trigger: 'blur' }],
  shortlistedBidders: [{ required: true, message: '请输入入围家数', trigger: 'blur' }],
  platformFee: [{ required: true, message: '请输入平台服务费', trigger: 'blur' }]
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatRecommendation(val) {
  if (val === true) return '建议投标'
  if (val === false) return '不建议投标'
  return '-'
}

function syncFormData() {
  if (view.value) {
    formData.background = view.value.background || ''
    formData.competitors = view.value.competitors || ''
    formData.contractPeriod = view.value.contractPeriod || ''
    formData.shortlistedBidders = view.value.shortlistedBidders ?? null
    formData.platformFee = view.value.platformFee ?? null
    formData.previousBid = view.value.previousBid || ''
    formData.recommendation = view.value.recommendation ?? null
  }
}

async function load() {
  try {
    const r = await projectLifecycleApi.getEvaluation(props.projectId)
    view.value = r?.data || r
    syncFormData()
  } catch (e) {
    console.warn('加载评估数据失败', e)
  }
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        background: formData.background,
        competitors: formData.competitors,
        contractPeriod: formData.contractPeriod,
        shortlistedBidders: formData.shortlistedBidders,
        platformFee: formData.platformFee,
        previousBid: formData.previousBid || null,
        recommendation: formData.recommendation
      }
      const r = await projectLifecycleApi.updateEvaluationForm(props.projectId, payload)
      view.value = r?.data || r
      ElMessage.success('表单已保存')
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

function handleReset() {
  formRef.value?.resetFields()
  syncFormData()
}

async function handleSubmit() {
  submitting.value = true
  try {
    await formRef.value?.validate()
    const payload = {
      background: formData.background,
      competitors: formData.competitors,
      contractPeriod: formData.contractPeriod,
      shortlistedBidders: formData.shortlistedBidders,
      platformFee: formData.platformFee,
      previousBid: formData.previousBid || null,
      recommendation: formData.recommendation
    }
    const r = await projectLifecycleApi.updateEvaluationForm(props.projectId, payload)
    view.value = r?.data || r
    ElMessage.success('提交成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleBid() {
  ElMessage.info('投标功能开发中')
}

async function handleAbandon() {
  ElMessage.info('弃标功能开发中')
}

async function handleTransition() {
  if (!targetSubStage.value) return ElMessage.warning('请选择子阶段')
  transitioning.value = true
  try {
    await projectLifecycleApi.transitionEvaluationSubStage(props.projectId, {
      target: targetSubStage.value
    })
    ElMessage.success('子阶段已切换')
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '切换失败')
  } finally {
    transitioning.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.evaluation-stage {
  padding: 16px;
}

.action-bar {
  display: flex;
  gap: 12px;
}

.main-content {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.form-section {
  flex: 1;
  min-width: 0;
}

.status-section {
  width: 320px;
  flex-shrink: 0;
}

.status-card {
  border: 1px solid #ebeef5;
}

@media (max-width: 900px) {
  .main-content {
    flex-direction: column;
  }

  .status-section {
    width: 100%;
  }
}
</style>
