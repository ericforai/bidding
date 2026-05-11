<template>
  <div class="bidding-detail-page">
    <div class="breadcrumb-nav">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/bidding' }">标讯中心</el-breadcrumb-item>
        <el-breadcrumb-item>标讯详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div v-if="tender" class="detail-content">
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="detail-card-header">
            <div class="detail-header-left">
              <h2 class="tender-title">{{ tender.title }}</h2>
              <div class="tags-row">
                <el-tag v-for="tag in tender.tags" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
            </div>
            <div v-if="matchScoreState === 'ready' && matchScore" class="detail-header-right">
              <div class="ai-score-large" :class="getScoreClass(matchScore.totalScore)">
                <div class="score-value">{{ matchScore.totalScore }}分</div>
                <div class="score-label">匹配评分</div>
              </div>
            </div>
          </div>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="标题" :span="3">
            {{ tender.title }}
          </el-descriptions-item>
          <el-descriptions-item label="预算金额">
            <span class="amount-text">{{ formatBudgetWan(tender.budget) }}万元</span>
          </el-descriptions-item>
          <el-descriptions-item label="总部所在地">
            <el-tooltip v-if="regionMeta.isMissing" :content="regionMeta.tooltip" placement="top">
              <span class="field-missing">{{ regionMeta.text }}</span>
            </el-tooltip>
            <span v-else>{{ regionMeta.text }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="招标机构">
            {{ tender.tenderAgency || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="业主单位">
            {{ tender.purchaserName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="报名截止时间">
            <span v-if="tender.registrationDeadline">{{ formatTenderDate(tender.registrationDeadline) }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="开标时间">
            <span v-if="tender.bidOpeningTime">{{ formatTenderDate(tender.bidOpeningTime) }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="所属行业">
            <el-tooltip v-if="industryMeta.isMissing" :content="industryMeta.tooltip" placement="top">
              <span class="field-missing">{{ industryMeta.text }}</span>
            </el-tooltip>
            <span v-else>{{ industryMeta.text }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">
            {{ tender.contactName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系方式">
            {{ tender.contactPhone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发布日期">{{ formatTenderDate(tender.publishDate || tender.date) }}</el-descriptions-item>
          <el-descriptions-item label="截止日期">
            <span class="deadline-display" :class="getDeadlineClass(tender.deadline)">
              <span>{{ deadlineParts.date }}</span>
              <template v-if="deadlineParts.hasTime">
                <span class="deadline-separator">|</span>
                <span>{{ deadlineParts.time }}</span>
              </template>
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="客户类型">
            {{ tender.customerType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            {{ tender.priority || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusType(tender.status)" size="small">{{ getStatusText(tender.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="项目经理">
            {{ tender.projectManagerName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="分配人">
            {{ tender.assigneeName || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons">
          <el-button
            type="primary"
            size="large"
            :disabled="tender.status === 'BIDDING' || tender.status === 'WON' || tender.status === 'LOST' || tender.status === 'ABANDONED'"
            @click="handleParticipate"
          >
            <el-icon><DocumentAdd /></el-icon>
            {{ tender.status === 'BIDDING' ? '投标中' : tender.status === 'WON' ? '已中标' : tender.status === 'LOST' ? '未中标' : tender.status === 'ABANDONED' ? '已弃标' : '投标' }}
          </el-button>
          <el-button
            type="danger"
            size="large"
            :disabled="tender.status === 'ABANDONED' || tender.status === 'WON' || tender.status === 'LOST'"
            @click="handleAbandon"
          >
            <el-icon><CircleClose /></el-icon>
            {{ tender.status === 'ABANDONED' ? '已弃标' : '弃标' }}
          </el-button>
          <el-button
            v-if="tender && safeTenderUrl(tender.originalUrl)"
            type="success"
            size="large"
            @click="handleViewOriginal"
          >
            <el-icon><Link /></el-icon>
            查看官网公告
          </el-button>
        </div>
      </el-card>

    </div>

    <div v-else class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>
  </div>
</template>

<script setup>
import { CircleClose, DocumentAdd, Link } from '@element-plus/icons-vue'
import { formatBudgetWan, formatTenderDate, safeTenderUrl } from '../bidding-utils.js'
import { useBiddingDetailPage } from './useBiddingDetailPage.js'
import './styles/detail-layout.css'
import './styles/detail-overrides.css'

const {
  tender,
  matchScore,
  matchScoreState,
  regionMeta,
  industryMeta,
  deadlineParts,
  getScoreClass,
  getStatusType,
  getStatusText,
  getDeadlineClass,
  handleParticipate,
  handleViewOriginal,
  handleAbandon,
} = useBiddingDetailPage()
</script>
