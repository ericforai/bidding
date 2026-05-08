<template>
  <el-card class="stage-view" shadow="never">
    <template #header>标书编制 (Drafting)</template>
    <el-form :model="leads" label-width="120px" inline>
      <el-form-item label="主负责人 ID">
        <el-input-number v-model="leads.primaryLeadUserId" :min="1" />
      </el-form-item>
      <el-form-item label="副负责人 ID">
        <el-input-number v-model="leads.secondaryLeadUserId" :min="1" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="savingLeads" @click="saveLeads">保存负责人</el-button>
      </el-form-item>
    </el-form>
    <el-divider />
    <el-descriptions :column="2" border size="small">
      <el-descriptions-item label="任务总数">{{ view?.totalTasks ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="已完成">{{ view?.completedTasks ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="主负责人">{{ view?.primaryLeadUserId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="副负责人">{{ view?.secondaryLeadUserId || '-' }}</el-descriptions-item>
    </el-descriptions>
    <div class="actions">
      <el-button type="success" :loading="advancing" @click="advance">推进至评标</el-button>
    </div>
    <el-alert
      v-if="advanceError"
      :title="advanceError"
      type="error"
      :closable="true"
      @close="advanceError = ''"
    />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectLifecycleApi } from '@/api/modules/projectLifecycle.js'

const props = defineProps({ projectId: { type: [String, Number], required: true } })
const emit = defineEmits(['advanced'])

const view = ref(null)
const leads = reactive({ primaryLeadUserId: null, secondaryLeadUserId: null })
const savingLeads = ref(false)
const advancing = ref(false)
const advanceError = ref('')

async function load() {
  try {
    const r = await projectLifecycleApi.getDrafting(props.projectId)
    view.value = r?.data || r
    leads.primaryLeadUserId = view.value?.primaryLeadUserId || null
    leads.secondaryLeadUserId = view.value?.secondaryLeadUserId || null
  } catch (e) {
    console.warn(e)
  }
}

async function saveLeads() {
  savingLeads.value = true
  try {
    await projectLifecycleApi.assignDraftingLeads(props.projectId, leads)
    ElMessage.success('负责人已保存')
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    savingLeads.value = false
  }
}

async function advance() {
  advanceError.value = ''
  advancing.value = true
  try {
    await projectLifecycleApi.advanceDrafting(props.projectId)
    ElMessage.success('已推进至评标阶段')
    emit('advanced')
  } catch (e) {
    if (e?.response?.status === 409) {
      advanceError.value = e?.response?.data?.message || '存在未完成任务，无法推进'
    } else {
      ElMessage.error(e?.response?.data?.message || '推进失败')
    }
  } finally {
    advancing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.actions { margin-top: 12px; }
</style>
