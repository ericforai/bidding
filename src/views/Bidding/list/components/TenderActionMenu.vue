<template>
  <div class="table-actions">
    <ElTooltip content="查看详情" placement="top">
      <ElButton size="small" :icon="View" aria-label="查看详情" @click="$emit('view-detail', row.id)" />
    </ElTooltip>
    <ElTooltip v-if="showAiEntry" content="AI分析" placement="top">
      <ElButton size="small" :icon="MagicStick" aria-label="AI分析" @click="$emit('ai-analysis', row.id)" />
    </ElTooltip>
    <ElTooltip content="参与投标" placement="top">
      <ElButton size="small" :icon="Document" aria-label="参与投标" @click="$emit('participate', row.id)" />
    </ElTooltip>
    <ElDropdown v-if="canManageTenders || canDeleteTenders" trigger="click">
      <ElButton size="small" :icon="MoreFilled" aria-label="更多操作" />
      <template #dropdown>
        <ElDropdownMenu>
          <template v-if="canManageTenders">
            <ElDropdownItem @click="$emit('distribute', row)">分发</ElDropdownItem>
            <ElDropdownItem @click="$emit('claim', row)">领取</ElDropdownItem>
            <ElDropdownItem @click="$emit('assign', row)">指派</ElDropdownItem>
            <ElDropdownItem divided @click="$emit('status-change', row, 'TRACKING')">设为跟踪中</ElDropdownItem>
            <ElDropdownItem @click="$emit('status-change', row, 'PENDING')">恢复待处理</ElDropdownItem>
            <ElDropdownItem @click="$emit('status-change', row, 'BIDDED')">标记为已投标</ElDropdownItem>
            <ElDropdownItem @click="$emit('status-change', row, 'ABANDONED')">标记为已放弃</ElDropdownItem>
          </template>
          <ElDropdownItem v-if="canDeleteTenders" divided class="danger-item" @click="$emit('delete', row)">
            删除
          </ElDropdownItem>
        </ElDropdownMenu>
      </template>
    </ElDropdown>
  </div>
</template>

<script setup>
import { ElButton, ElDropdown, ElDropdownItem, ElDropdownMenu, ElTooltip } from 'element-plus'
import { Document, MagicStick, MoreFilled, View } from '@element-plus/icons-vue'

defineProps({
  row: { type: Object, required: true },
  canManageTenders: { type: Boolean, default: false },
  canDeleteTenders: { type: Boolean, default: false },
  showAiEntry: { type: Boolean, default: true },
})

defineEmits([
  'view-detail',
  'ai-analysis',
  'participate',
  'distribute',
  'claim',
  'assign',
  'status-change',
  'delete',
])
</script>
