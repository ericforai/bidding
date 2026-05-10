<template>
  <div class="mobile-card-view">
    <article v-for="row in rows" :key="row.id" class="mobile-card-item">
      <div class="mobile-card-header">
        <h4>{{ row.title }}</h4>
        <el-tag v-if="row.aiScore >= 90" size="small" type="success">高匹配</el-tag>
      </div>
      <div class="mobile-card-body">
        <div><span>预算</span><strong>{{ formatBudgetWan(row.budget) }}万元</strong></div>
        <div><span>总部所在地</span><strong>{{ row.region }}</strong></div>
        <div>
          <span>来源</span>
          <el-tag v-if="row.sourceType" :type="getSourceTypeTagType(row.sourceType)" size="small">
            {{ getSourceTypeText(row.sourceType) }}
          </el-tag>
        </div>
        <div>
          <span>状态</span>
          <el-tag :type="getTenderStatusTagType(row.status)" size="small">
            {{ getTenderStatusText(row.status) }}
          </el-tag>
        </div>
      </div>
      <div class="mobile-card-actions">
        <el-button type="primary" size="small" @click="$emit('view-detail', row.id)">查看详情</el-button>
        <el-button v-if="showAiEntry" type="success" size="small" @click="$emit('ai-analysis', row.id)">AI分析</el-button>
        <el-button size="small" @click="$emit('participate', row.id)">参与投标</el-button>
        <el-dropdown v-if="canManageTenders || canDeleteTenders || isAdmin" trigger="click">
          <el-button size="small">更多</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <template v-if="row.status === 'TRACKING'">
                <el-dropdown-item @click="$emit('edit', row)">编辑</el-dropdown-item>
              </template>
              <template v-if="isAdmin && row.status === 'EVALUATED'">
                <el-dropdown-item @click="$emit('review', row)">审核</el-dropdown-item>
              </template>
              <template v-if="canManageTenders">
                <el-dropdown-item @click="$emit('distribute', row)">分发</el-dropdown-item>
                <el-dropdown-item @click="$emit('status-change', row, 'TRACKING')">设为跟踪中</el-dropdown-item>
                <el-dropdown-item @click="$emit('status-change', row, 'PENDING')">恢复待处理</el-dropdown-item>
                <el-dropdown-item @click="$emit('status-change', row, 'BIDDED')">标记为已投标</el-dropdown-item>
              </template>
              <el-dropdown-item v-if="canDeleteTenders" divided @click="$emit('delete', row)">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </article>
  </div>
</template>

<script setup>
import { formatBudgetWan, getSourceTypeTagType, getSourceTypeText } from '../helpers.js'
import { getTenderStatusTagType, getTenderStatusText } from '../../bidding-utils-status.js'

defineProps({
  rows: { type: Array, default: () => [] },
  canManageTenders: { type: Boolean, default: false },
  canDeleteTenders: { type: Boolean, default: false },
  showAiEntry: { type: Boolean, default: true },
  isAdmin: { type: Boolean, default: false },
})

defineEmits(['view-detail', 'ai-analysis', 'participate', 'distribute', 'edit', 'review', 'status-change', 'delete'])
</script>
