<template>
  <div class="table-container">
    <el-table
      ref="innerTableRef"
      class="tender-table"
      :data="rows"
      stripe
      scrollbar-always-on
      @selection-change="$emit('selection-change', $event)"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="50" align="center" fixed="left" />
      <el-table-column prop="title" label="项目名称" min-width="200" fixed="left" class-name="tender-main-column">
        <template #default="{ row = {} } = {}">
          <el-link
            v-if="safeTenderUrl(row.originalUrl)"
            class="tender-title-link"
            :href="safeTenderUrl(row.originalUrl)"
            target="_blank"
            rel="noopener noreferrer"
            type="primary"
            underline="never"
          >
            <span class="tender-title-text">{{ row.title }}</span>
            <el-icon><LinkIcon /></el-icon>
          </el-link>
          <span v-else class="tender-title-text tender-title-clickable" @click="$emit('view-detail', row.id)">
            {{ row.title }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源平台" width="110" align="center">
        <template #default="{ row = {} } = {}">
          <el-tag v-if="row.source" size="small" :type="getSourceTagType(row.source)">{{ row.source }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="region" label="总部所在地" width="90" align="center">
        <template #default="{ row = {} } = {}">{{ row.region || '-' }}</template>
      </el-table-column>
      <el-table-column prop="purchaserName" label="业主单位" width="140" show-overflow-tooltip>
        <template #default="{ row = {} } = {}">{{ row.purchaserName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="projectType" label="项目类型" width="100" align="center">
        <template #default="{ row = {} } = {}">{{ row.projectType || '-' }}</template>
      </el-table-column>
      <el-table-column prop="customerType" label="客户类型" width="90" align="center">
        <template #default="{ row = {} } = {}">{{ row.customerType || '-' }}</template>
      </el-table-column>
      <el-table-column prop="budget" label="营业收入(亿)" width="90" align="center">
        <template #default="{ row = {} } = {}">
          <span v-if="row.budget != null">{{ formatBudgetYi(row.budget) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="registrationDeadline" label="报名截止" width="140" align="center">
        <template #default="{ row = {} } = {}">
          <span v-if="row.registrationDeadline">{{ formatDate(row.registrationDeadline) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="bidOpeningTime" label="开标时间" width="140" align="center">
        <template #default="{ row = {} } = {}">
          <span v-if="row.bidOpeningTime">{{ formatDate(row.bidOpeningTime) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="标讯状态" width="92" align="center">
        <template #default="{ row = {} } = {}">
          <el-tag :type="getTenderStatusTagType(row.status)" size="small">
            {{ getTenderStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="projectManagerName" label="项目负责人" width="90" align="center">
        <template #default="{ row = {} } = {}">{{ row.projectManagerName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="department" label="项目部门" width="90" align="center">
        <template #default="{ row = {} } = {}">{{ row.department || '-' }}</template>
      </el-table-column>
      <el-table-column prop="biddingPersonName" label="投标负责人" width="90" align="center">
        <template #default="{ row = {} } = {}">{{ row.biddingPersonName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="70" align="center">
        <template #default="{ row = {} } = {}">
          <el-tag v-if="row.priority" :type="getPriorityTagType(row.priority)" size="small">
            {{ row.priority }}级
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="creatorName" label="创建人" width="80" align="center">
        <template #default="{ row = {} } = {}">{{ row.creatorName || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="320" align="center" fixed="right">
        <template #default="{ row = {} } = {}">
          <TenderActionMenu
            :row="row"
            :can-manage-tenders="canManageTenders"
            :can-delete-tenders="canDeleteTenders"
            :show-ai-entry="showAiEntry"
            :is-admin="isAdmin"
            @view-detail="$emit('view-detail', $event)"
            @ai-analysis="$emit('ai-analysis', $event)"
            @participate="$emit('participate', $event)"
            @distribute="$emit('distribute', $event)"
            @edit="$emit('edit', $event)"
            @review="$emit('review', $event)"
            @status-change="(target, status) => $emit('status-change', target, status)"
            @delete="$emit('delete', $event)"
            @set-reminder="$emit('set-reminder', $event)"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Link as LinkIcon } from '@element-plus/icons-vue'
import { formatBudgetWan, getSourceTagType, getSourceText, getSourceTypeTagType, getSourceTypeText, safeTenderUrl } from '../helpers.js'
import { getTenderStatusTagType, getTenderStatusText } from '../../bidding-utils-status.js'
import TenderActionMenu from './TenderActionMenu.vue'

defineProps({
  rows: { type: Array, default: () => [] },
  canManageTenders: { type: Boolean, default: false },
  canDeleteTenders: { type: Boolean, default: false },
  showAiEntry: { type: Boolean, default: true },
  isAdmin: { type: Boolean, default: false },
})

defineEmits([
  'selection-change', 'view-detail', 'ai-analysis', 'participate',
  'distribute', 'edit', 'review', 'status-change', 'delete', 'set-reminder',
])

const innerTableRef = ref(null)

defineExpose({
  clearSelection: () => innerTableRef.value?.clearSelection(),
  toggleRowSelection: (row, selected) => innerTableRef.value?.toggleRowSelection(row, selected),
})

const formatDate = (val) => {
  if (!val) return '-'
  const d = new Date(val)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const formatBudgetYi = (val) => {
  if (val == null) return '-'
  return (val / 100_000_000).toFixed(2)
}

const getPriorityTagType = (priority) => {
  const map = { S: 'danger', A: 'warning', B: 'primary', C: 'info' }
  return map[priority] || 'info'
}
</script>
