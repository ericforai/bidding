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
            <div class="detail-header-right">
              <div class="ai-score-large" :class="getScoreClass(tender.aiScore)">
                <div class="score-value">{{ tender.aiScore }}分</div>
                <div class="score-label">AI评分</div>
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
          <el-descriptions-item label="发布日期">{{ tender.date }}</el-descriptions-item>
          <el-descriptions-item label="截止日期">
            <span :class="getDeadlineClass(tender.deadline)">{{ tender.deadline }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusType(tender.status)" size="small">{{ getStatusText(tender.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="中标概率" :span="3">
            <el-rate
              v-model="probabilityRate"
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
            <h4 class="detail-section-title">匹配度分析</h4>
            <div class="match-bars">
              <div class="match-bar-item">
                <div class="bar-label">
                  <span>行业匹配</span>
                  <span class="bar-value">95%</span>
                </div>
                <el-progress :percentage="95" :stroke-width="10" :show-text="false" />
              </div>
              <div class="match-bar-item">
                <div class="bar-label">
                  <span>地区匹配</span>
                  <span class="bar-value">88%</span>
                </div>
                <el-progress :percentage="88" :stroke-width="10" :show-text="false" color="#e6a23c" />
              </div>
              <div class="match-bar-item">
                <div class="bar-label">
                  <span>资质匹配</span>
                  <span class="bar-value">92%</span>
                </div>
                <el-progress :percentage="92" :stroke-width="10" :show-text="false" color="#67c23a" />
              </div>
              <div class="match-bar-item">
                <div class="bar-label">
                  <span>历史合作</span>
                  <span class="bar-value">80%</span>
                </div>
                <el-progress :percentage="80" :stroke-width="10" :show-text="false" color="#909399" />
              </div>
            </div>
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
import { formatBudgetWan, safeTenderUrl } from '../bidding-utils.js'
import { useBiddingDetailPage } from './useBiddingDetailPage.js'
import './styles/detail-layout.css'
import './styles/detail-overrides.css'

const {
  showTenderAiSection,
  tender,
  isFollowed,
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
} = useBiddingDetailPage()
</script>
