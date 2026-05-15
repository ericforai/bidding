<template>
  <el-card class="stage-view" shadow="never">
    <template #header>立项 (Initiation)</template>
    <el-form :model="form" label-width="120px" :disabled="locked">
      <el-form-item label="业主单位" required>
        <el-input v-model="form.ownerUnit" :disabled="fieldLocked" />
      </el-form-item>
      <el-form-item label="开标时间" required>
        <el-date-picker
          v-model="form.bidOpenTime"
          type="datetime"
          :disabled="fieldLocked"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="项目类型" required>
        <el-select v-model="form.projectType">
          <el-option label="集采" value="GROUP_PURCHASE" />
          <el-option label="单项目" value="SINGLE_PROJECT" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户类型" required>
        <el-select v-model="form.customerType">
          <el-option label="政府" value="GOVERNMENT" />
          <el-option label="企业" value="ENTERPRISE" />
          <el-option label="事业单位" value="PUBLIC_INSTITUTION" />
        </el-select>
      </el-form-item>
      <el-form-item label="预计投标家数" required>
        <el-input-number v-model="form.expectedBidders" :min="0" />
      </el-form-item>
      <el-form-item label="合同期限(月)" required>
        <el-input-number v-model="form.contractPeriodMonths" :min="0" />
      </el-form-item>
      <el-form-item label="年营收(万元)" required>
        <el-input-number v-model="form.annualRevenue" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="项目负责人 ID" required>
        <el-input-number v-model="form.ownerUserId" :min="1" />
      </el-form-item>
      <el-form-item label="保证金金额">
        <el-input-number v-model="form.depositAmount" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="保证金缴纳方式">
        <el-input v-model="form.depositPaymentMethod" />
      </el-form-item>
      <el-form-item label="竞争对手 (可选)">
        <el-input v-model="form.competitors" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ existing ? '更新立项信息' : '提交立项' }}
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectLifecycleApi } from '@/api/modules/projectLifecycle.js'

const props = defineProps({ projectId: { type: [String, Number], required: true } })
const emit = defineEmits(['updated'])

const form = reactive({
  ownerUnit: '',
  bidOpenTime: '',
  projectType: 'SINGLE_PROJECT',
  customerType: 'ENTERPRISE',
  expectedBidders: 0,
  contractPeriodMonths: 0,
  annualRevenue: 0,
  ownerUserId: null,
  depositAmount: 0,
  depositPaymentMethod: '',
  competitors: '',
})
const existing = ref(false)
const locked = ref(false)
const fieldLocked = ref(false)
const submitting = ref(false)

async function load() {
  try {
    const r = await projectLifecycleApi.getInitiation(props.projectId)
    const d = r?.data || r
    if (d) {
      Object.assign(form, d)
      existing.value = true
      fieldLocked.value = !!d.bidOpenTime && !!d.ownerUnit
    }
  } catch (e) {
    if (e?.response?.status !== 404) console.warn(e)
  }
}

async function submit() {
  submitting.value = true
  try {
    if (existing.value) {
      await projectLifecycleApi.updateInitiation(props.projectId, form)
    } else {
      await projectLifecycleApi.submitInitiation(props.projectId, form)
    }
    ElMessage.success('立项信息已保存')
    fieldLocked.value = true
    existing.value = true
    emit('updated')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
