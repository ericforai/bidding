<template>
  <div class="evaluation-stage">
    <div class="action-bar">
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      <el-button type="success" @click="handleBid">投标</el-button>
      <el-button type="danger" @click="handleAbandon">弃标</el-button>
    </div>

    <el-divider />

    <div class="main-content">
      <div class="form-section">
        <EvaluationForm
          ref="formRef"
          v-model="formData"
          :saving="saving"
          @save="handleSave"
          @reset="handleReset"
        />
      </div>

      <EvaluationStatusPanel
        :view="view"
        :transitioning="transitioning"
        v-model:targetSubStage="targetSubStage"
        @transition="handleTransition"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectLifecycleApi } from '@/api/modules/projectLifecycle.js'
import EvaluationForm from './components/EvaluationForm.vue'
import EvaluationStatusPanel from './components/EvaluationStatusPanel.vue'

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

function buildPayload() {
  return {
    background: formData.background,
    competitors: formData.competitors,
    contractPeriod: formData.contractPeriod,
    shortlistedBidders: formData.shortlistedBidders,
    platformFee: formData.platformFee,
    previousBid: formData.previousBid || null,
    recommendation: formData.recommendation
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

function syncFormData() {
  if (!view.value) return
  formData.background = view.value.background || ''
  formData.competitors = view.value.competitors || ''
  formData.contractPeriod = view.value.contractPeriod || ''
  formData.shortlistedBidders = view.value.shortlistedBidders ?? null
  formData.platformFee = view.value.platformFee ?? null
  formData.previousBid = view.value.previousBid || ''
  formData.recommendation = view.value.recommendation ?? null
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    saving.value = true
    const r = await projectLifecycleApi.updateEvaluationForm(props.projectId, buildPayload())
    view.value = r?.data || r
    ElMessage.success('表单已保存')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
  syncFormData()
}

async function handleSubmit() {
  submitting.value = true
  try {
    await formRef.value?.validate()
    await projectLifecycleApi.updateEvaluationForm(props.projectId, buildPayload())
    await load()
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
    await projectLifecycleApi.transitionEvaluationSubStage(props.projectId, { target: targetSubStage.value })
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

@media (max-width: 900px) {
  .main-content {
    flex-direction: column;
  }

  .evaluation-status {
    width: 100%;
  }
}
</style>
