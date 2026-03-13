<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="580px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 提交审批表单 -->
    <div v-if="mode === 'submit'" class="approval-form">
      <el-form :model="formData" label-width="100px" label-position="left">
        <el-form-item label="审批类型">
          <el-tag size="large" type="primary">{{ approvalType.typeName }}</el-tag>
        </el-form-item>
        <el-form-item label="项目名称">
          <span class="form-value">{{ projectName }}</span>
        </el-form-item>
        <el-form-item label="申请说明" v-if="showReason">
          <el-input
            v-model="formData.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入申请说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="审批流程" v-if="approvalFlow.length > 0">
          <div class="approval-flow">
            <div
              v-for="(node, index) in approvalFlow"
              :key="index"
              class="flow-node"
              :class="{ 'is-next': index === 0 }"
            >
              <div class="node-dot">
                <el-icon><User /></el-icon>
              </div>
              <div class="node-content">
                <div class="node-name">{{ node.nodeName }}</div>
                <div class="node-user">{{ node.approverName }}</div>
              </div>
              <div v-if="index < approvalFlow.length - 1" class="node-arrow">
                <el-icon><ArrowDown /></el-icon>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 审批操作表单 -->
    <div v-else-if="mode === 'approve' || mode === 'reject'" class="approval-form">
      <el-form :model="formData" label-width="100px" label-position="left">
        <el-form-item label="项目名称">
          <span class="form-value">{{ approvalInfo.projectName }}</span>
        </el-form-item>
        <el-form-item label="申请类型">
          <el-tag size="small">{{ approvalInfo.typeName }}</el-tag>
        </el-form-item>
        <el-form-item label="申请人">
          <span class="form-value">{{ approvalInfo.applicantName }}（{{ approvalInfo.applicantDept }}）</span>
        </el-form-item>
        <el-form-item label="申请时间">
          <span class="form-value">{{ approvalInfo.submitTime }}</span>
        </el-form-item>
        <el-form-item label="审批进度" v-if="approvalInfo.approvalNodes && approvalInfo.approvalNodes.length > 1">
          <el-steps :active="currentStepIndex" finish-status="success" simple>
            <el-step
              v-for="(node, index) in approvalInfo.approvalNodes"
              :key="index"
              :title="node.nodeName"
              :status="getNodeStatus(node.status)"
            />
          </el-steps>
        </el-form-item>
        <el-form-item :label="mode === 'approve' ? '审批意见' : '驳回原因'" required>
          <el-input
            v-model="formData.comment"
            type="textarea"
            :rows="4"
            :placeholder="mode === 'approve' ? '请输入审批意见（可选）' : '请输入驳回原因'"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <!-- 审批详情查看 -->
    <div v-else-if="mode === 'detail'" class="approval-detail">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="项目名称">{{ approvalInfo.projectName }}</el-descriptions-item>
        <el-descriptions-item label="申请类型">
          <el-tag size="small" :type="getTypeTagType(approvalInfo.type)">{{ approvalInfo.typeName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请人">{{ approvalInfo.applicantName }}（{{ approvalInfo.applicantDept }}）</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ approvalInfo.submitTime }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="getStatusTagType(approvalInfo.status)">{{ getStatusText(approvalInfo.status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="approvalInfo.approvalNodes && approvalInfo.approvalNodes.length > 0" class="approval-nodes">
        <h4 class="nodes-title">审批流程</h4>
        <el-timeline>
          <el-timeline-item
            v-for="(node, index) in approvalInfo.approvalNodes"
            :key="index"
            :timestamp="node.approvalTime || '待审批'"
            :type="getNodeTimelineType(node.status)"
            :icon="getNodeIcon(node.status)"
          >
            <div class="node-info">
              <div class="node-header">
                <span class="node-name">{{ node.nodeName }}</span>
                <el-tag size="small" :type="getNodeStatusTagType(node.status)">
                  {{ getNodeStatusText(node.status) }}
                </el-tag>
              </div>
              <div class="node-user">审批人：{{ node.approverName }}</div>
              <div v-if="node.opinion" class="node-opinion">审批意见：{{ node.opinion }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <div v-if="approvalInfo.comment && approvalInfo.status !== 'pending'" class="approval-result">
        <h4 class="result-title">审批结果</h4>
        <p class="result-comment">{{ approvalInfo.comment }}</p>
      </div>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          v-if="mode === 'submit'"
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          提交审批
        </el-button>
        <el-button
          v-if="mode === 'approve'"
          type="success"
          :loading="submitting"
          @click="handleApprove"
        >
          通过
        </el-button>
        <el-button
          v-if="mode === 'reject'"
          type="danger"
          :loading="submitting"
          @click="handleReject"
        >
          驳回
        </el-button>
        <el-button
          v-if="mode === 'detail' && approvalInfo.status === 'pending' && canOperate"
          type="success"
          :loading="submitting"
          @click="switchMode('approve')"
        >
          通过
        </el-button>
        <el-button
          v-if="mode === 'detail' && approvalInfo.status === 'pending' && canOperate"
          type="danger"
          :loading="submitting"
          @click="switchMode('reject')"
        >
          驳回
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, ArrowDown, CircleCheck, CircleClose, Clock } from '@element-plus/icons-vue'
import { approvalApi } from '@/api'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  visible: Boolean,
  mode: {
    type: String,
    default: 'detail', // submit, approve, reject, detail
    validator: (val) => ['submit', 'approve', 'reject', 'detail'].includes(val)
  },
  projectId: String,
  projectName: String,
  approvalType: {
    type: Object,
    default: () => ({ type: 'project_review', typeId: 'project_review', typeName: '立项审批' })
  },
  approvalInfo: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:visible', 'success', 'close'])

const userStore = useUserStore()

const dialogVisible = ref(false)
const submitting = ref(false)
const currentMode = ref(props.mode)

const formData = ref({
  reason: '',
  comment: ''
})

const showReason = computed(() => {
  return props.approvalType?.type === 'seal' || props.approvalType?.type === 'budget'
})

const dialogTitle = computed(() => {
  switch (currentMode.value) {
    case 'submit': return '提交审批申请'
    case 'approve': return '审批通过'
    case 'reject': return '审批驳回'
    case 'detail': return '审批详情'
    default: return '审批'
  }
})

const approvalFlow = computed(() => {
  return props.approvalInfo?.approvalNodes || []
})

const currentStepIndex = computed(() => {
  if (!props.approvalInfo?.approvalNodes) return 0
  const index = props.approvalInfo.approvalNodes.findIndex(n => n.status === 'pending')
  return index === -1 ? props.approvalInfo.approvalNodes.length : index
})

const canOperate = computed(() => {
  if (!props.approvalInfo?.currentApproverId) return false
  // 简化：当前用户是审批人就可以操作
  return userStore.userRole === 'admin' || userStore.userRole === 'manager'
})

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    currentMode.value = props.mode
    formData.value = { reason: '', comment: '' }
  }
})

watch(dialogVisible, (val) => {
  emit('update:visible', val)
})

const switchMode = (mode) => {
  currentMode.value = mode
}

const handleClose = () => {
  dialogVisible.value = false
  emit('close')
}

const handleSubmit = async () => {
  try {
    await ElMessageBox.confirm('确认提交审批申请？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    submitting.value = true
    const result = await approvalApi.submitApproval(props.projectId, props.approvalType, {
      applicantId: userStore.currentUser?.id,
      applicantName: userStore.currentUser?.name,
      applicantDept: userStore.currentUser?.dept || '投标管理部',
      formJson: { reason: formData.value.reason }
    })

    if (result.success) {
      ElMessage.success('审批申请已提交')
      dialogVisible.value = false
      emit('success', result.data)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleApprove = async () => {
  try {
    submitting.value = true
    const result = await approvalApi.approveApproval(props.approvalInfo.id, formData.value.comment)

    if (result.success) {
      ElMessage.success('已通过审批')
      dialogVisible.value = false
      emit('success', result.data)
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleReject = async () => {
  if (!formData.value.comment.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }

  try {
    submitting.value = true
    const result = await approvalApi.rejectApproval(props.approvalInfo.id, formData.value.comment)

    if (result.success) {
      ElMessage.success('已驳回申请')
      dialogVisible.value = false
      emit('success', result.data)
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 状态相关方法
const getStatusTagType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger', cancelled: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待审批', approved: '已通过', rejected: '已驳回', cancelled: '已取消' }
  return map[status] || status
}

const getTypeTagType = (type) => {
  const map = { project_review: 'primary', budget: 'warning', seal: 'success', document: 'info' }
  return map[type] || 'info'
}

const getNodeStatus = (status) => {
  const map = { pending: 'wait', approved: 'success', rejected: 'error', cancelled: 'finish' }
  return map[status] || 'wait'
}

const getNodeStatusText = (status) => {
  const map = { pending: '待审批', approved: '已通过', rejected: '已驳回', cancelled: '已取消' }
  return map[status] || status
}

const getNodeStatusTagType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger', cancelled: 'info' }
  return map[status] || 'info'
}

const getNodeTimelineType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'primary'
}

const getNodeIcon = (status) => {
  if (status === 'approved') return CircleCheck
  if (status === 'rejected') return CircleClose
  return Clock
}
</script>

<style scoped>
.approval-form {
  padding: 10px 0;
}

.form-value {
  color: #333;
  font-weight: 500;
}

.approval-flow {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.flow-node {
  display: flex;
  align-items: center;
  gap: 12px;
}

.node-dot {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f0f0f0;
  color: #909399;
  flex-shrink: 0;
}

.flow-node.is-next .node-dot {
  background: #409eff;
  color: #fff;
}

.flow-node.is-next .node-name {
  color: #409eff;
  font-weight: 600;
}

.node-content {
  flex: 1;
}

.node-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.node-user {
  font-size: 12px;
  color: #909399;
}

.node-arrow {
  color: #dcdfe6;
  margin-left: 8px;
}

.approval-detail {
  max-height: 60vh;
  overflow-y: auto;
}

.approval-nodes {
  margin-top: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.nodes-title {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.node-info {
  width: 100%;
}

.node-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.node-user {
  font-size: 13px;
  color: #666;
}

.node-opinion {
  font-size: 13px;
  color: #333;
  margin-top: 4px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 4px;
}

.approval-result {
  margin-top: 16px;
  padding: 16px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
}

.result-title {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #0284c7;
}

.result-comment {
  margin: 0;
  font-size: 14px;
  color: #333;
}
</style>
