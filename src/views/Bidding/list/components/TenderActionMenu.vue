<template>
  <div class="table-actions">
    <ElTooltip content="查看详情" placement="top">
      <ElButton size="small" :icon="View" aria-label="查看详情" @click="$emit('view-detail', row.id)" />
    </ElTooltip>
    <ElTooltip v-if="showAiEntry && shouldShowAiButton" content="AI分析" placement="top">
      <ElButton size="small" :icon="MagicStick" aria-label="AI分析" @click="$emit('ai-analysis', row.id)" />
    </ElTooltip>
    <ElTooltip v-if="shouldShowParticipateButton" content="参与投标" placement="top">
      <ElButton size="small" :icon="Document" aria-label="参与投标" @click="$emit('participate', row.id)" />
    </ElTooltip>
    <ElDropdown v-if="hasMoreActions" trigger="click">
      <ElButton size="small" :icon="MoreFilled" aria-label="更多操作" />
      <template #dropdown>
        <ElDropdownMenu>
          <template v-if="shouldShowAiButton">
            <ElDropdownItem @click="$emit('ai-analysis', row.id)">AI分析</ElDropdownItem>
          </template>
          <template v-if="!shouldShowParticipateButton">
            <ElDropdownItem @click="$emit('participate', row)">参与投标</ElDropdownItem>
          </template>
          <!-- 项目经理：状态=TRACKING时显示编辑按钮 -->
          <ElDropdownItem v-if="canEdit && row.status === 'TRACKING'" @click="$emit('edit', row)">
            编辑
          </ElDropdownItem>
          <!-- 管理员：状态=EVALUATED时显示审核按钮 -->
          <ElDropdownItem v-if="isAdmin && row.status === 'EVALUATED'" @click="$emit('review', row)">
            审核
          </ElDropdownItem>
          <ElDropdownItem v-if="canManageTenders" divided @click="$emit('distribute', row)">分发</ElDropdownItem>
          <ElDropdownItem v-if="canManageTenders" @click="$emit('status-change', row, 'TRACKING')">设为跟踪中</ElDropdownItem>
          <ElDropdownItem v-if="canManageTenders" @click="$emit('status-change', row, 'PENDING')">恢复待处理</ElDropdownItem>
          <ElDropdownItem v-if="canManageTenders" @click="$emit('status-change', row, 'BIDDED')">标记为已投标</ElDropdownItem>
          <ElDropdownItem v-if="canManageTenders" @click="$emit('status-change', row, 'ABANDONED')">标记为已放弃</ElDropdownItem>
          <ElDropdownItem v-if="canDeleteTenders" divided class="danger-item" @click="$emit('delete', row)">
            删除
          </ElDropdownItem>
        </ElDropdownMenu>
      </template>
    </ElDropdown>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { ElButton, ElDropdown, ElDropdownItem, ElDropdownMenu, ElTooltip } from 'element-plus'
import { Document, MagicStick, MoreFilled, View } from '@element-plus/icons-vue'

const props = defineProps({
  row: { type: Object, required: true },
  canManageTenders: { type: Boolean, default: false },
  canDeleteTenders: { type: Boolean, default: false },
  showAiEntry: { type: Boolean, default: true },
  isAdmin: { type: Boolean, default: false },
})

defineEmits([
  'view-detail',
  'ai-analysis',
  'participate',
  'distribute',
  'edit',
  'review',
  'status-change',
  'delete',
])

const containerWidth = ref(320)
const resizeObserver = ref(null)

const hasMoreActions = computed(() => props.canManageTenders || props.canDeleteTenders || props.canEdit || props.isAdmin)

const canEdit = computed(() => {
  // 项目经理可以编辑自己跟踪的标讯
  return props.row.status === 'TRACKING'
})

const shouldShowAiButton = computed(() => {
  const width = containerWidth.value
  if (props.showAiEntry) {
    if (hasMoreActions.value) {
      return width >= 310
    }
    return width >= 260
  }
  return false
})

const shouldShowParticipateButton = computed(() => {
  const width = containerWidth.value
  if (props.showAiEntry && shouldShowAiButton.value) {
    return width >= 390
  }
  if (props.showAiEntry) {
    return width >= 310
  }
  return width >= 200
})

onMounted(() => {
  const actionCell = document.querySelector('.table-actions')?.parentElement
  if (actionCell) {
    resizeObserver.value = new ResizeObserver((entries) => {
      for (const entry of entries) {
        containerWidth.value = entry.contentRect.width
      }
    })
    resizeObserver.value.observe(actionCell)
  }
})

onUnmounted(() => {
  resizeObserver.value?.disconnect()
})
</script>
