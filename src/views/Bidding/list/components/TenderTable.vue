<template>
  <div class="table-container">
    <el-table ref="innerTableRef" :data="rows" stripe @selection-change="$emit('selection-change', $event)">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="title" label="标讯标题" min-width="280" show-overflow-tooltip>
        <template #default="{ row = {} } = {}">
          <div class="title-cell">
            <el-link
              v-if="safeTenderUrl(row.originalUrl)"
              :href="safeTenderUrl(row.originalUrl)"
              target="_blank"
              rel="noopener noreferrer"
              type="primary"
              underline="never"
            >
              <span>{{ row.title }}</span>
              <el-icon><LinkIcon /></el-icon>
            </el-link>
            <span v-else>{{ row.title }}</span>
            <el-tag v-if="row.aiScore >= 90" size="small" type="success">高匹配</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="budget" label="预算" width="100" align="center">
        <template #default="{ row = {} } = {}">{{ formatBudgetWan(row.budget) }}万元</template>
      </el-table-column>
      <el-table-column prop="region" label="地区" width="90" align="center" />
      <el-table-column prop="industry" label="行业" width="110" align="center" />
      <el-table-column prop="source" label="来源" width="120" align="center">
        <template #default="{ row = {} } = {}">
          <el-tag v-if="row.source" :type="getSourceTagType(row.source)" size="small">
            {{ getSourceText(row.source) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="aiScore" label="AI评分" width="90" align="center">
        <template #default="{ row = {} } = {}">
          <span class="ai-score-highlight">{{ row.aiScore || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="deadline" label="截止日期" width="120" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row = {} } = {}">
          <el-tag :type="getTenderStatusTagType(row.status)" size="small">
            {{ getTenderStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="270" align="center" fixed="right">
        <template #default="{ row = {} } = {}">
          <TenderActionMenu
            :row="row"
            :can-manage-tenders="canManageTenders"
            :can-delete-tenders="canDeleteTenders"
            :show-ai-entry="showAiEntry"
            @view-detail="$emit('view-detail', $event)"
            @ai-analysis="$emit('ai-analysis', $event)"
            @participate="$emit('participate', $event)"
            @distribute="$emit('distribute', $event)"
            @claim="$emit('claim', $event)"
            @assign="$emit('assign', $event)"
            @status-change="(target, status) => $emit('status-change', target, status)"
            @delete="$emit('delete', $event)"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Link as LinkIcon } from '@element-plus/icons-vue'
import { formatBudgetWan, getSourceTagType, getSourceText, safeTenderUrl } from '../helpers.js'
import { getTenderStatusTagType, getTenderStatusText } from '../../bidding-utils-status.js'
import TenderActionMenu from './TenderActionMenu.vue'

defineProps({
  rows: { type: Array, default: () => [] },
  canManageTenders: { type: Boolean, default: false },
  canDeleteTenders: { type: Boolean, default: false },
  showAiEntry: { type: Boolean, default: true },
})

defineEmits([
  'selection-change',
  'view-detail',
  'ai-analysis',
  'participate',
  'distribute',
  'claim',
  'assign',
  'status-change',
  'delete',
])

const innerTableRef = ref(null)

defineExpose({
  clearSelection: () => innerTableRef.value?.clearSelection(),
  toggleRowSelection: (row, selected) => innerTableRef.value?.toggleRowSelection(row, selected),
})
</script>
