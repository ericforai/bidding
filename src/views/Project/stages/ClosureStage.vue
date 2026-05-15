<template>
  <div class="closure-stage">
    <DepositReturnPanel :preview="preview" />
    <el-card class="stage-view" shadow="never">
      <template #header>结项 (Closure)</template>
      <el-form :model="form" label-width="160px">
        <el-form-item label="保证金是否已退回?" v-if="preview?.hasDeposit">
          <el-radio-group v-model="form.depositReturned">
            <el-radio :label="true">是</el-radio>
            <el-radio :label="false">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="preview?.hasDeposit && form.depositReturned === false"
          type="warning"
          :closable="false"
          show-icon
          title="保证金未退回时不允许结项 (PRD §3.6.3)。请先登记退回信息。"
        />
        <el-form-item label="退回日期" v-if="form.depositReturned === true">
          <el-date-picker
            v-model="form.depositReturnDate"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="退回凭证文档 ID" v-if="form.depositReturned === true">
          <el-input-number v-model="form.depositReturnEvidenceId" :min="1" />
        </el-form-item>
        <el-form-item label="归档位置">
          <el-input v-model="form.archiveLocation" />
        </el-form-item>
        <el-form-item label="结项备注">
          <el-input v-model="form.notes" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :disabled="!canSubmit"
            :loading="submitting"
            @click="submit"
          >
            确认结项
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectLifecycleApi } from '@/api/modules/projectLifecycle.js'
import DepositReturnPanel from '@/components/project/stage/DepositReturnPanel.vue'

const props = defineProps({ projectId: { type: [String, Number], required: true } })
const emit = defineEmits(['closed'])

const preview = ref(null)
const submitting = ref(false)
const form = reactive({
  depositReturned: null,
  depositReturnDate: '',
  depositReturnEvidenceId: null,
  archiveLocation: '',
  notes: '',
})

const canSubmit = computed(() => {
  if (preview.value?.alreadyClosed) return false
  if (preview.value?.stageLocked) return false
  if (!preview.value?.hasDeposit) return preview.value?.canClose !== false
  // PRD §3.6.3: 保证金未退回禁止结项
  if (form.depositReturned !== true) return false
  if (!form.depositReturnDate) return false
  if (!form.depositReturnEvidenceId) return false
  return preview.value?.canClose !== false
})

async function loadPreview() {
  try {
    const r = await projectLifecycleApi.getClosurePreview(props.projectId)
    preview.value = r?.data || r
    if (preview.value?.depositReturnStatus === 'RETURNED') {
      form.depositReturned = true
      form.depositReturnDate = preview.value.depositReturnDate || ''
      form.depositReturnEvidenceId = preview.value.depositReturnEvidenceId || null
    }
  } catch (e) {
    console.warn(e)
  }
}

async function submit() {
  submitting.value = true
  try {
    await projectLifecycleApi.submitClosure(props.projectId, form)
    ElMessage.success('项目已结项')
    emit('closed')
    await loadPreview()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '结项失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ canSubmit, form, preview })
onMounted(loadPreview)
</script>

<style scoped>
.closure-stage { display: flex; flex-direction: column; gap: 12px; }
</style>
