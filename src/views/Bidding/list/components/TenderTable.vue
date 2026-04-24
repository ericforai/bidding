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
      <el-table-column type="selection" width="44" />
      <el-table-column prop="title" label="标讯" min-width="260" class-name="tender-main-column">
        <template #default="{ row = {} } = {}">
          <div class="tender-main-cell">
            <div class="tender-title-line">
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
              <span v-else class="tender-title-text">{{ row.title }}</span>
              <el-tag v-if="row.aiScore >= 90" size="small" type="success">高匹配</el-tag>
            </div>
            <div class="tender-meta-line">
              <el-tag v-if="row.source" :type="getSourceTagType(row.source)" size="small">
                {{ getSourceText(row.source) }}
              </el-tag>
              <span v-if="row.region" class="meta-item">{{ row.region }}</span>
              <span v-if="row.industry" class="meta-item">{{ row.industry }}</span>
              <span v-if="row.deadline" class="meta-item">截止 {{ row.deadline }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="budget" label="预算" width="82" align="center">
        <template #default="{ row = {} } = {}">
          <span class="budget-cell">{{ formatBudgetWan(row.budget) }}万</span>
        </template>
      </el-table-column>
      <el-table-column prop="aiScore" label="AI评分" width="78" align="center">
        <template #default="{ row = {} } = {}">
          <span class="ai-score-highlight">{{ row.aiScore || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="92" align="center">
        <template #default="{ row = {} } = {}">
          <el-tag :type="getTenderStatusTagType(row.status)" size="small">
            {{ getTenderStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="224" align="center" fixed="right">
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
