<template>
  <div class="bidding-header">
    <div>
      <h2 class="page-title">标讯中心</h2>
      <p class="page-subtitle">AI智能匹配，发现优质商机</p>
    </div>
    <div class="header-actions">
      <el-button v-if="customerOpportunityEnabled" @click="$emit('open-customer-opportunities')">
        <el-icon><UserFilled /></el-icon>
        客户商机中心
      </el-button>
      <el-button v-if="canSyncExternalSource" type="primary" @click="$emit('open-source-config')">
        <el-icon><Setting /></el-icon>
        标讯源配置
      </el-button>
      <el-button
        v-if="canSyncExternalSource"
        type="success"
        :loading="fetchingTenders"
        @click="$emit('sync-external')"
      >
        <el-icon><Download /></el-icon>
        一键获取标讯
      </el-button>
      <el-button v-if="canCreateTender" type="warning" @click="$emit('open-manual-add')">
        <el-icon><Plus /></el-icon>
        人工录入
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { Download, Plus, Setting, UserFilled } from '@element-plus/icons-vue'

defineProps({
  customerOpportunityEnabled: { type: Boolean, default: true },
  canCreateTender: { type: Boolean, default: false },
  canSyncExternalSource: { type: Boolean, default: false },
  fetchingTenders: { type: Boolean, default: false },
})

defineEmits(['open-customer-opportunities', 'open-source-config', 'sync-external', 'open-manual-add'])
</script>
