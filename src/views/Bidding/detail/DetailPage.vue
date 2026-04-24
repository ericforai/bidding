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
          <el-descriptions-item label="预算金额">
            <span class="amount-text">{{ formatBudgetWan(tender.budget) }}万元</span>
          </el-descriptions-item>
          <el-descriptions-item label="所属地区">{{ tender.region }}</el-descriptions-item>
          <el-descriptions-item label="所属行业">{{ tender.industry }}</el-descriptions-item>
          <el-descriptions-item label="发布日期">{{ formatTenderDate(tender.publishDate || tender.date) }}</el-descriptions-item>
          <el-descriptions-item label="截止日期">
            <span :class="getDeadlineClass(tender.deadline)">{{ formatTenderDateTime(tender.deadline) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusType(tender.status)" size="small">{{ getStatusText(tender.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="中标概率" :span="3">
            <el-rate
              :model-value="probabilityRate"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons">
          <el-button type="primary" size="large" @click="handleParticipate">
            <el-icon><DocumentAdd /></el-icon>
            立即投标
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
          <el-button size="large" @click="handleFollow">
            <el-icon><StarFilled v-if="isFollowed" /><Star v-else /></el-icon>
            {{ isFollowed ? '已关注' : '加入关注' }}
          </el-button>
          <el-button size="large" @click="handleShare">
            <el-icon><Share /></el-icon>
            分享
          </el-button>
        </div>
      </el-card>

      <el-card v-if="showTenderAiSection" class="ai-analysis-card" shadow="never">
        <template #header>
          <div class="card-title-with-icon">
            <el-icon class="ai-icon"><MagicStick /></el-icon>
            AI智能分析
          </div>
        </template>

        <div class="analysis-content">
          <div class="analysis-section">
            <MatchScorePanel
              :score="matchScore"
              :loading="scoreLoading"
              :generating="scoreGenerating"
              :error="scoreError"
              @generate="handleGenerateMatchScore"
              @reload="loadMatchScore(tender.id)"
              @configure="handleConfigureMatchScore"
            />
          </div>

          <el-divider />

          <div class="analysis-section">
            <h4 class="detail-section-title">优势分析</h4>
            <div class="advantages-list">
              <div v-for="(advantage, index) in advantages" :key="index" class="advantage-item">
                <el-icon class="advantage-icon"><CircleCheckFilled /></el-icon>
                <span>{{ advantage }}</span>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="analysis-section">
            <h4 class="detail-section-title">AI建议</h4>
            <div class="suggestions">
              <el-alert
                v-for="(suggestion, index) in suggestions"
                :key="index"
                :title="suggestion.title"
                :type="suggestion.type"
                :closable="false"
                show-icon
              >
                <template #default>
                  <p>{{ suggestion.content }}</p>
                </template>
              </el-alert>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="related-cases-card" shadow="never">
        <template #header>
          <div class="card-title-with-icon">
            <el-icon><Briefcase /></el-icon>
            相关案例推荐
          </div>
        </template>

        <div class="cases-list">
          <div v-for="caseItem in relatedCases" :key="caseItem.id" class="case-item" @click="handleViewCase(caseItem.id)">
            <div class="case-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="case-content">
              <h5 class="case-title">{{ caseItem.title }}</h5>
              <div class="case-meta">
                <span>{{ caseItem.customer }}</span>
                <span>{{ caseItem.amount }}万元</span>
                <span>{{ caseItem.year }}年</span>
              </div>
              <p class="case-summary">{{ caseItem.summary }}</p>
              <div class="case-highlights">
                <el-tag v-for="highlight in caseItem.highlights" :key="highlight" size="small" type="info">
                  {{ highlight }}
                </el-tag>
              </div>
            </div>
            <div class="case-arrow">
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div v-else class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>
  </div>
</template>

<script setup>
import { ArrowRight, Briefcase, CircleCheckFilled, Document, DocumentAdd, Link, MagicStick, Share, Star, StarFilled } from '@element-plus/icons-vue'
import { formatBudgetWan, formatTenderDate, formatTenderDateTime, safeTenderUrl } from '../bidding-utils.js'
import MatchScorePanel from '../match-scoring/MatchScorePanel.vue'
import { useBiddingDetailPage } from './useBiddingDetailPage.js'
import './styles/detail-layout.css'
import './styles/detail-overrides.css'

const {
  showTenderAiSection,
  tender,
  isFollowed,
  matchScore,
  scoreLoading,
  scoreGenerating,
  scoreError,
  matchScoreState,
  probabilityRate,
  advantages,
  suggestions,
  relatedCases,
  getScoreClass,
  getStatusType,
  getStatusText,
  getDeadlineClass,
  handleParticipate,
  handleFollow,
  handleShare,
  handleViewOriginal,
  handleViewCase,
  loadMatchScore,
  handleGenerateMatchScore,
  handleConfigureMatchScore,
} = useBiddingDetailPage()
</script>
