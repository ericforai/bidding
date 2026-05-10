<template>
  <el-dialog
    v-model="dialogVisible"
    title="项目评估"
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

      <el-form-item label="评估内容" prop="evaluationContent">
        <el-input
          v-model="form.evaluationContent"
          type="textarea"
          :rows="6"
          placeholder="请填写项目评估内容，包括资质响应、交付能力、竞争分析等"
          maxlength="10000"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="预估预算" prop="estimatedBudget">
        <el-input-number
          v-model="form.estimatedBudget"
          :precision="2"
          :step="10000"
          :min="0"
          :controls="false"
          placeholder="请输入预估预算"
          style="width: 100%"
        >
          <template #suffix>元</template>
        </el-input-number>
      </el-form-item>

      <el-form-item label="风险评估" prop="riskAssessment">
        <el-input
          v-model="form.riskAssessment"
          type="textarea"
          :rows="3"
          placeholder="请评估项目风险，包括技术风险、商务风险、履约风险等"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="备注" prop="notes">
        <el-input
          v-model="form.notes"
          type="textarea"
          :rows="3"
          placeholder="其他补充说明"
          maxlength="2000"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        提交评估
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
const formRef = ref(null)
const submitting = ref(false)

const form = ref({
  evaluationContent: '',
  estimatedBudget: null,
  riskAssessment: '',
  notes: ''
})

const rules = {
  evaluationContent: [
    { required: true, message: '请填写评估内容', trigger: 'blur' }
  ]
}

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
  if (!val) {
    resetForm()
  }
})

const handleClose = () => {
  dialogVisible.value = false
}

const resetForm = () => {
  form.value = {
    evaluationContent: '',
    estimatedBudget: null,
    riskAssessment: '',
    notes: ''
  }
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true

  try {
    const response = await tendersApi.submitEvaluation(props.tenderId, form.value)
    if (response?.success !== false) {
      ElMessage.success('评估提交成功')
      emit('success', response?.data)
      handleClose()
    } else {
      ElMessage.error(response?.message || '评估提交失败')
    }
  } catch (error) {
    console.error('评估提交失败:', error)
    ElMessage.error('评估提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>
