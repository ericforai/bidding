<template>
  <el-card class="stage-view" shadow="never">
    <template #header>复盘 (Retrospective)</template>
    <el-form :model="form" label-width="140px" :disabled="locked">
      <el-form-item label="复盘摘要" required>
        <el-input v-model="form.summary" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="经验/亮点" v-if="resultType !== 'WITHDRAWN'">
        <el-input v-model="form.highlights" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="教训/差距" v-if="['LOST', 'VOID'].includes(resultType)">
        <el-input v-model="form.lessons" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="后续计划" v-if="resultType === 'WON'">
        <el-input v-model="form.followUpPlan" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交复盘</el-button>
      </el-form-item>
    </el-form>

    <el-divider v-if="isAdmin" />
    <el-form v-if="isAdmin" :model="review" label-width="140px">
      <el-form-item label="审核决定">
        <el-radio-group v-model="review.decision">
          <el-radio label="APPROVE">通过</el-radio>
          <el-radio label="REJECT">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="审核意见" :required="review.decision === 'REJECT'">
        <el-input v-model="review.comment" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item>
        <el-button
          type="warning"
          :disabled="review.decision === 'REJECT' && !review.comment"
          :loading="reviewing"
          @click="doReview"
        >
          提交审核
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectLifecycleApi } from '@/api/modules/projectLifecycle.js'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  projectId: { type: [String, Number], required: true },
  resultType: { type: String, default: '' },
})

const userStore = useUserStore()
const isAdmin = computed(() => String(userStore.userRole || '').toLowerCase() === 'admin')

const form = reactive({
  summary: '',
  highlights: '',
  lessons: '',
  followUpPlan: '',
})
const review = reactive({ decision: 'APPROVE', comment: '' })
const view = ref(null)
const locked = ref(false)
const submitting = ref(false)
const reviewing = ref(false)

async function load() {
  try {
    const r = await projectLifecycleApi.getRetrospective(props.projectId)
    view.value = r?.data || r
    if (view.value) {
      Object.assign(form, view.value)
      locked.value = view.value.reviewStatus === 'APPROVED'
    }
  } catch (e) {
    if (e?.response?.status !== 404) console.warn(e)
  }
}

async function submit() {
  submitting.value = true
  try {
    await projectLifecycleApi.submitRetrospective(props.projectId, form)
    ElMessage.success('复盘已提交')
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function doReview() {
  if (review.decision === 'REJECT' && !review.comment.trim()) {
    return ElMessage.warning('驳回必须填写审核意见')
  }
  reviewing.value = true
  try {
    await projectLifecycleApi.reviewRetrospective(props.projectId, review)
    ElMessage.success('审核已提交')
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '审核失败')
  } finally {
    reviewing.value = false
  }
}

onMounted(load)
</script>
