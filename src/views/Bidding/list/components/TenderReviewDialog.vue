<template>
  <el-dialog
    v-model="dialogVisible"
    title="标讯审核"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="标讯标题">
        <el-input :model-value="tenderTitle" disabled />
      </el-form-item>

      <el-form-item label="评估信息">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="评估人">{{ evaluation?.evaluatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评估时间">{{ formatDate(evaluation?.evaluatedAt) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预估预算">{{ evaluation?.estimatedBudget ? `${evaluation.estimatedBudget}元` : '-' }}</el-descriptions-item>
          <el-descriptions-item label="风险评估">{{ evaluation?.riskAssessment || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-form-item>

      <el-form-item label="评估内容">
        <div class="evaluation-content">{{ evaluation?.evaluationContent || '-' }}</div>
      </el-form-item>

      <el-divider />

      <el-form-item label="审核意见" prop="reviewComment">
        <el-input
          v-model="form.reviewComment"
          type="textarea"
          :rows="3"
          placeholder="请填写审核意见"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="danger" :loading="abandoning" @click="handleAbandon">
        弃标
      </el-button>
      <el-button type="success" :loading="approving" @click="handleApprove">
        投标
      </el-button>
    </template>
  </el-dialog>

  <!-- 弃标原因对话框 -->
  <el-dialog
    v-model="abandonDialogVisible"
    title="填写弃标原因"
    width="500px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-form
      ref="abandonFormRef"
      :model="abandonForm"
      :rules="abandonRules"
      label-width="100px"
    >
      <el-form-item label="弃标原因" prop="abandonmentReason">
        <el-input
          v-model="abandonForm.abandonmentReason"
          type="textarea"
          :rows="4"
          placeholder="请填写弃标原因"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="abandonDialogVisible = false">取消</el-button>
      <el-button type="danger" :loading="confirmAbandoning" @click="confirmAbandon">
        确认弃标
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { tendersApi } from '@/api/modules/tenders'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  tenderId: {
    type: [Number, String],
    default: null
  },
  tenderTitle: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const dialogVisible = ref(false)
const abandonDialogVisible = ref(false)
const formRef = ref(null)
const abandonFormRef = ref(null)
const evaluation = ref(null)
const loading = ref(false)
const approving = ref(false)
const abandoning = ref(false)
const confirmAbandoning = ref(false)

const form = ref({
  reviewComment: ''
})

const abandonForm = ref({
  abandonmentReason: ''
})

const rules = {
  reviewComment: []
}

const abandonRules = {
  abandonmentReason: [
    { required: true, message: '请填写弃标原因', trigger: 'blur' }
  ]
}

watch(() => props.modelValue, async (val) => {
  dialogVisible.value = val
  if (val && props.tenderId) {
    await loadEvaluation()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
  if (!val) {
    resetForm()
  }
})

const loadEvaluation = async () => {
  loading.value = true
  try {
    const response = await tendersApi.getEvaluation(props.tenderId)
    if (response?.success !== false && response?.data) {
      evaluation.value = response.data
    }
  } catch (error) {
    console.error('获取评估信息失败:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const handleClose = () => {
  dialogVisible.value = false
}

const resetForm = () => {
  form.value.reviewComment = ''
  abandonForm.value.abandonmentReason = ''
  evaluation.value = null
}

const handleApprove = async () => {
  approving.value = true
  try {
    const response = await tendersApi.reviewTender(props.tenderId, {
      approved: true,
      reviewComment: form.value.reviewComment
    })
    if (response?.success !== false) {
      ElMessage.success('审核通过，标讯已标记为已投标')
      emit('success', response?.data)
      handleClose()
    } else {
      ElMessage.error(response?.message || '审核失败')
    }
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败，请重试')
  } finally {
    approving.value = false
  }
}

const handleAbandon = () => {
  abandonDialogVisible.value = true
}

const confirmAbandon = async () => {
  if (!abandonFormRef.value) return

  try {
    await abandonFormRef.value.validate()
  } catch {
    return
  }

  confirmAbandoning.value = true
  try {
    const response = await tendersApi.reviewTender(props.tenderId, {
      approved: false,
      abandonmentReason: abandonForm.value.abandonmentReason,
      reviewComment: form.value.reviewComment
    })
    if (response?.success !== false) {
      ElMessage.success('标讯已标记为已放弃')
      emit('success', response?.data)
      abandonDialogVisible.value = false
      handleClose()
    } else {
      ElMessage.error(response?.message || '弃标失败')
    }
  } catch (error) {
    console.error('弃标失败:', error)
    ElMessage.error('弃标失败，请重试')
  } finally {
    confirmAbandoning.value = false
  }
}
</script>

<style scoped>
.evaluation-content {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
