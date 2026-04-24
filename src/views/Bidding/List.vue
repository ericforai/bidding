<template>
  <div class="bidding-list-page">
    <BiddingPageHeader
      :customer-opportunity-enabled="customerOpportunityCenterEnabled"
      :can-create-tender="canCreateTender"
      :can-sync-external-source="canSyncExternalSource"
      :fetching-tenders="sourceConfig.fetchingTenders.value"
      @open-customer-opportunities="handleOpenCustomerOpportunityCenter"
      @open-source-config="openSourceConfig"
      @sync-external="sourceConfig.syncExternalTenders"
      @open-manual-add="openManualAdd"
    />

    <TenderSearchCard
      :model-value="searchForm"
      @search="handleSearch"
      @reset="handleReset"
    />

    <SourceStatusCard
      :source-config="sourceConfig.sourceConfig.value"
      :last-sync-time="sourceConfig.lastSyncTime.value"
    />

    <AiRecommendSection
      :tenders="filteredRecommendTenders"
      @view-all="handleViewAllRecommend"
      @view-detail="handleViewDetail"
    />

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header-content">
          <span class="card-title">标讯列表</span>
          <div class="card-actions">
            <el-button size="small" @click="distribution.showRecordDialog.value = true">
              <el-icon><ListIcon /></el-icon>
              分发记录
            </el-button>
            <el-button size="small" type="success" @click="marketInsight.showMarketInsight.value = true">
              <el-icon><TrendCharts /></el-icon>
              市场洞察
            </el-button>
            <el-button size="small" @click="handleExport">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
            <el-button
              v-if="canSyncExternalSource"
              size="small"
              type="warning"
              :loading="sourceConfig.fetchingTenders.value"
              @click="sourceConfig.syncExternalTenders"
            >
              <el-icon><Refresh /></el-icon>
              一键获取标讯
            </el-button>
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button value="all">全部 ({{ statusCounts.all }})</el-radio-button>
              <el-radio-button value="PENDING">待处理 ({{ statusCounts.pending }})</el-radio-button>
              <el-radio-button value="TRACKING">跟踪中 ({{ statusCounts.tracking }})</el-radio-button>
              <el-radio-button value="BIDDED">已投标 ({{ statusCounts.bidded }})</el-radio-button>
              <el-radio-button value="ABANDONED">已放弃 ({{ statusCounts.abandoned }})</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <TenderBatchActionBar
        :selected-count="selection.selectedTenders.value.length"
        :select-all-checked="selection.selectAllChecked.value"
        :is-indeterminate="selection.isIndeterminate.value"
        :can-manage-tenders="canManageTenders"
        @select-all="selection.handleSelectAll"
        @distribute="distribution.openDistributeDialog"
        @claim="batchActions.handleBatchClaim"
        @follow="batchActions.handleBatchFollow"
        @clear="selection.handleClearSelection"
      />

      <TenderTable
        v-if="!isMobile"
        :ref="(instance) => { selection.tableRef.value = instance }"
        :rows="displayTenders"
        :can-manage-tenders="canManageTenders"
        :can-delete-tenders="canDeleteTenders"
        :show-ai-entry="showTenderAiEntry"
        @selection-change="selection.handleSelectionChange"
        @view-detail="handleViewDetail"
        @ai-analysis="handleAIAnalysis"
        @participate="handleParticipate"
        @distribute="distribution.openSingleDistribute"
        @claim="batchActions.handleSingleClaim"
        @assign="distribution.openAssignDialog"
        @status-change="batchActions.handleUpdateStatus"
        @delete="batchActions.handleDeleteTender"
      />
      <TenderMobileCards
        v-else
        :rows="displayTenders"
        :can-manage-tenders="canManageTenders"
        :can-delete-tenders="canDeleteTenders"
        :show-ai-entry="showTenderAiEntry"
        @view-detail="handleViewDetail"
        @ai-analysis="handleAIAnalysis"
        @participate="handleParticipate"
        @claim="batchActions.handleSingleClaim"
        @assign="distribution.openAssignDialog"
        @status-change="batchActions.handleUpdateStatus"
        @delete="batchActions.handleDeleteTender"
      />

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredTenders.length"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>

    <DistributeDialog
      v-model="distribution.showDistributeDialog.value"
      :selected-tenders="selection.selectedTenders.value"
      :candidates="distribution.candidates.value"
      :preview="distribution.distributionPreview.value"
      :form="distribution.distributeForm.value"
      :loading="distribution.distributeLoading.value"
      :loading-candidates="distribution.loadingCandidates.value"
      @reset="distribution.resetDistributeForm"
      @submit="distribution.handleDistribute"
    />
    <AssignDialog
      v-model="distribution.showAssignDialog.value"
      :form="distribution.assignForm.value"
      :candidates="distribution.candidates.value"
      :loading="distribution.assignLoading.value"
      :loading-candidates="distribution.loadingCandidates.value"
      @reset="distribution.resetAssignForm"
      @submit="distribution.handleAssign"
    />
    <RecordsDialog v-model="distribution.showRecordDialog.value" :records="distribution.distributeRecords.value" />
    <SourceConfigDialog
      v-model="sourceConfig.showSourceConfig.value"
      :source-config="sourceConfig.sourceConfig.value"
      :saving="sourceConfig.savingConfig.value"
      :testing="sourceConfig.testingConnection.value"
      @save="sourceConfig.saveSourceConfig"
      @test="sourceConfig.testConnection"
    />
    <ManualTenderDialog
      v-model="manualCreate.showManualAdd.value"
      :ref="(instance) => { manualCreate.manualFormRef.value = instance }"
      :form="manualCreate.manualForm.value"
      :saving="manualCreate.savingManual.value"
      @reset="manualCreate.resetManualForm"
      @file-change="manualCreate.handleFileChange"
      @submit="manualCreate.saveManualTender"
    />
    <FetchResultDialog v-model="sourceConfig.fetchResult.value.visible" :result="sourceConfig.fetchResult.value" />
    <MarketInsightDialog
      v-model="marketInsight.showMarketInsight.value"
      v-model:active-tab="marketInsight.activeInsightTab.value"
      :loading="marketInsight.loadingTrendData.value"
      :industry-trends="marketInsight.industryTrends.value"
      :opportunities="marketInsight.potentialOpportunities.value"
      :industry-insight="marketInsight.industryInsight.value"
      :forecast-tips="marketInsight.forecastTips.value"
      @refresh="marketInsight.refreshTrendData"
    />
    <AiParsingDialog v-model="showParsingDialog" :progress="parseProgress" />
  </div>
</template>

<script setup>
import { Download, List as ListIcon, Refresh, TrendCharts } from '@element-plus/icons-vue'
import AiParsingDialog from './list/components/AiParsingDialog.vue'
import AiRecommendSection from './list/components/AiRecommendSection.vue'
import AssignDialog from './list/components/AssignDialog.vue'
import BiddingPageHeader from './list/components/BiddingPageHeader.vue'
import DistributeDialog from './list/components/DistributeDialog.vue'
import FetchResultDialog from './list/components/FetchResultDialog.vue'
import ManualTenderDialog from './list/components/ManualTenderDialog.vue'
import MarketInsightDialog from './list/components/MarketInsightDialog.vue'
import RecordsDialog from './list/components/RecordsDialog.vue'
import SourceConfigDialog from './list/components/SourceConfigDialog.vue'
import SourceStatusCard from './list/components/SourceStatusCard.vue'
import TenderBatchActionBar from './list/components/TenderBatchActionBar.vue'
import TenderMobileCards from './list/components/TenderMobileCards.vue'
import TenderSearchCard from './list/components/TenderSearchCard.vue'
import TenderTable from './list/components/TenderTable.vue'
import { useTenderListPage } from './list/useTenderListPage.js'
import './list/styles/list-page.css'
import './list/styles/table.css'
import './list/styles/mobile-page.css'

const {
  searchForm,
  viewMode,
  isMobile,
  pagination,
  filteredTenders,
  filteredRecommendTenders,
  displayTenders,
  statusCounts,
  canManageTenders,
  canCreateTender,
  canDeleteTenders,
  canSyncExternalSource,
  customerOpportunityCenterEnabled,
  showTenderAiEntry,
  showParsingDialog,
  parseProgress,
  selection,
  sourceConfig,
  manualCreate,
  marketInsight,
  batchActions,
  distribution,
  handleSearch,
  handleReset,
  handleExport,
  handleViewDetail,
  handleParticipate,
  handleViewAllRecommend,
  handleOpenCustomerOpportunityCenter,
  openManualAdd,
  openSourceConfig,
  handleAIAnalysis,
} = useTenderListPage()
</script>
