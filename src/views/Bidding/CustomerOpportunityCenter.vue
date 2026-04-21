<template>
  <div class="customer-opportunity-page" :class="{ 'is-loading': loading }">
    <div class="page-header">
      <div class="header-text">
        <h2 class="animate-fade-in">客户商机中心</h2>
        <p class="animate-fade-in-delay">基于销售情报（Sales Intelligence）的客户经营视图，智能研判历史规律与潜在商机。</p>
      </div>
      <div class="header-actions">
        <el-button
          @click="refreshInsights"
          :loading="customerOpportunityDemoEnabled && isScanning"
          :disabled="!customerOpportunityDemoEnabled"
          class="btn-refresh"
        >
          <el-icon><Refresh /></el-icon>
          {{ customerOpportunityDemoEnabled ? '刷新洞察' : '洞察未接入' }}
        </el-button>
        <el-button type="primary" class="btn-primary" @click="createProject" v-if="selectedCustomer">
          {{ selectedCustomer.prediction.convertedProjectId ? '查看项目' : '转为正式项目' }}
        </el-button>
      </div>
    </div>

    <!-- AI Scanning Overlay -->
    <transition name="fade">
      <div v-if="customerOpportunityDemoEnabled && isScanning" class="scanning-overlay">
        <div class="scan-grid"></div>
        <div class="scan-line"></div>
        <div class="scan-content">
          <div class="hologram-box">
            <el-icon class="rotating"><Refresh /></el-icon>
          </div>
          <h3>AI 引擎正在分析全域数据...</h3>
          <p>正在研判采购规律 · 识别机会评分 · 测算预算窗口</p>
        </div>
      </div>
    </transition>

    <CustomerOpportunityBoard :loading="loading" :board-summaries="boardSummaries" />

    <div class="content-grid">
      <CustomerOpportunityFiltersTable
        :loading="loading"
        :demo-enabled="customerOpportunityDemoEnabled"
        :filters="filters"
        :sales-users="salesUsers"
        :regions="regions"
        :industries="industries"
        :status-options="statusOptions"
        :filtered-customers="filteredCustomers"
        :active-customer-id="activeCustomerId"
        @update:filters="filters = $event"
        @select-customer="selectCustomer"
      />

      <CustomerOpportunityDetail
        :loading="loading"
        :selected-customer="selectedCustomer"
        :view-state="viewState"
        @open-history="historyDrawer = true"
        @select-first-high-value="selectFirstHighValue"
        @recommend-project="filters = { ...filters, status: 'recommend' }"
        @go-bidding="router.push('/bidding')"
      />
    </div>

    <CustomerOpportunityHistoryDrawer
      v-model="historyDrawer"
      :selected-customer="selectedCustomer"
      :customer-history="customerHistory"
      :drawer-stats="drawerStats"
      :category-stats="categoryStats"
    />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useCustomerOpportunityCenter } from '@/composables/useCustomerOpportunityCenter.js'
import CustomerOpportunityBoard from '@/views/Bidding/customer-opportunity/CustomerOpportunityBoard.vue'
import CustomerOpportunityDetail from '@/views/Bidding/customer-opportunity/CustomerOpportunityDetail.vue'
import CustomerOpportunityFiltersTable from '@/views/Bidding/customer-opportunity/CustomerOpportunityFiltersTable.vue'
import CustomerOpportunityHistoryDrawer from '@/views/Bidding/customer-opportunity/CustomerOpportunityHistoryDrawer.vue'
import { buildCreateProjectQuery } from '@/views/Bidding/customerOpportunityView.js'

const router = useRouter()
const {
  loading,
  customerOpportunityDemoEnabled,
  salesUsers,
  filters,
  regions,
  industries,
  statusOptions,
  activeCustomerId,
  historyDrawer,
  isScanning,
  filteredCustomers,
  selectedCustomer,
  customerHistory,
  drawerStats,
  categoryStats,
  boardSummaries,
  viewState,
  selectCustomer,
  selectFirstHighValue: selectFirstHighValueAction
} = useCustomerOpportunityCenter()

const refreshInsights = () => {
  if (!customerOpportunityDemoEnabled) {
    ElMessage.info('客户商机中心在真实模式下暂未接入数据源')
    return
  }
  isScanning.value = true
  setTimeout(() => {
    isScanning.value = false
    ElMessage.success('AI 智能洞察已同步至最新')
  }, 2500)
}

const selectFirstHighValue = () => {
  selectFirstHighValueAction()
}

const createProject = () => {
  if (!selectedCustomer.value || !customerOpportunityDemoEnabled) return

  if (selectedCustomer.value.prediction.convertedProjectId) {
    router.push(`/project/${selectedCustomer.value.prediction.convertedProjectId}`)
    return
  }

  router.push({
    path: '/project/create',
    query: buildCreateProjectQuery(selectedCustomer.value)
  })
}
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');

.customer-opportunity-page {
  padding: var(--space-6, 24px);
  min-height: 100vh;
  background: #f8fafc;
  font-family: 'Plus Jakarta Sans', -apple-system, system-ui, sans-serif;
  color: #0f172a;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-8, 32px);
}

.header-text h2 {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.02em;
}

.header-text p {
  color: #64748b;
  margin: 8px 0 0;
  font-size: 15px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-refresh {
  background: white;
  border-color: #e2e8f0;
  color: #475569;
  font-weight: 500;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-refresh:hover {
  background: #f1f5f9;
  transform: translateY(-1px);
}

/* Top Board */
.top-board {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.board-card {
  background: white;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.board-card.hover-lift:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 30px -8px rgba(3, 105, 161, 0.15);
  border-color: #bae6fd;
}

.card-main {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 16px 0 4px;
}

.card-trend {
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 2px;
}

.card-trend.up { color: #10b981; }
.card-trend.down { color: #f43f5e; }
.card-trend.neutral { color: #64748b; }

.spark-box {
  height: 32px;
  margin-bottom: 12px;
  display: flex;
  align-items: flex-end;
}

.spark-line {
  height: 4px;
  width: 100%;
  border-radius: 2px;
  background: #f1f5f9;
  position: relative;
  overflow: hidden;
}

.spark-line::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 60%;
  border-radius: 2px;
  animation: spark-slide 2s ease-in-out infinite alternate;
}

.spark-line.success::after { background: #10b981; }
.spark-line.danger::after { background: #f43f5e; }
.spark-line.warning::after { background: #f59e0b; }
.spark-line.info::after { background: #3b82f6; }

@keyframes spark-slide {
  from { transform: translateX(-20%); }
  to { transform: translateX(120%); }
}

.board-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: transparent;
  transition: background 0.3s;
}

.board-card:hover::after {
  background: #0369a1;
}

.card-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
}

.card-value {
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
  margin: 16px 0 8px;
  font-feature-settings: "tnum";
}

.card-note {
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
}

.tag-glow {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* Content Layout */
.content-grid {
  display: grid;
  grid-template-columns: 800px 1fr;
  gap: 24px;
  height: calc(100vh - 280px);
  min-height: 600px;
}

.customer-list-panel,
.customer-detail-panel {
  background: white;
  border-radius: 20px;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
}

.panel-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header.search-integrated {
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
  padding: 16px 20px;
}

.header-filters.multi-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-row {
  display: flex;
  gap: 8px;
}

.search-input {
  flex: 2;
}

.filter-item {
  flex: 1;
  min-width: 120px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 20px;
  color: #0369a1;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

/* Table Style */
.premium-table {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #f8fafc;
}

.customer-name-cell {
  padding: 4px 0;
}

.customer-name-cell strong {
  display: block;
  font-size: 14px;
  color: #1e293b;
}

.score-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.score-num {
  font-size: 13px;
  font-weight: 600;
  font-feature-settings: "tnum";
}

.score-num.high { color: #10b981; }
.score-num.mid { color: #f59e0b; }
.score-num.low { color: #64748b; }

.window-tag {
  font-size: 12px;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  color: #475569;
  white-space: nowrap;
}

.row-active {
  background-color: #f0f9ff !important;
}

.row-active td {
  border-left: 3px solid #0369a1;
}

/* Detail Panel */
.detail-container {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.detail-header-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-main {
  display: flex;
  gap: 16px;
  align-items: center;
}

.avatar-box {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.2);
}

.info-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-top h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.info-sub {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 14px;
}

.glass-section {
  background: #f8fafc;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid #f1f5f9;
}

.section-title {
  margin: 0 0 16px;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #94a3b8;
  font-weight: 600;
}

.profiling-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.label {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.profiling-item .value {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.profiling-item .value small {
  font-size: 12px;
  font-weight: 400;
}

/* Prediction Section */
.insight-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.confidence-badge {
  font-size: 13px;
  font-weight: 700;
  padding: 4px 12px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
}

.prediction-card {
  background: linear-gradient(to bottom right, #ffffff, #f0f9ff);
  border: 1px solid #bae6fd;
  border-radius: 16px;
  padding: 24px;
}

.pred-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 20px;
}

.pred-item.highlight p {
  color: #0369a1;
  font-size: 20px;
  font-weight: 700;
}

.pred-item p {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.reason-box {
  background: rgba(3, 105, 161, 0.05);
  padding: 12px 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #0369a1;
  font-size: 14px;
  line-height: 1.5;
}

/* Timeline */
.timeline-container {
  margin-top: 16px;
}

.timeline-content .t-title {
  font-weight: 600;
  margin: 0 0 4px;
  font-size: 14px;
}

.t-meta {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
}

.divider {
  width: 1px;
  height: 10px;
  background: #cbd5e1;
  margin: 0 8px;
}

.insight-summary {
  margin: 20px 0 0;
  padding-top: 20px;
  border-top: 1px dashed #e2e8f0;
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  font-style: italic;
  display: flex;
  gap: 8px;
}

.insight-summary .el-icon {
  color: #10b981;
  font-size: 18px;
  flex-shrink: 0;
}

/* Animations */
.animate-fade-in {
  animation: fadeIn 0.6s ease-out;
}

.animate-fade-in-delay {
  animation: fadeIn 0.6s ease-out 0.2s both;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Skeleton refinements */
.skeleton-card {
  border: 1px solid #f1f5f9;
}

/* Panoramic Drawer Styles */
.panoramic-view {
  padding: 0 4px;
}

.panoramic-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 16px 12px;
  border-radius: 12px;
  color: white;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card.blue { background: linear-gradient(135deg, #3b82f6, #2563eb); box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2); }
.stat-card.purple { background: linear-gradient(135deg, #8b5cf6, #7c3aed); box-shadow: 0 4px 12px rgba(124, 58, 237, 0.2); }
.stat-card.green { background: linear-gradient(135deg, #10b981, #059669); box-shadow: 0 4px 12px rgba(5, 150, 105, 0.2); }

.stat-label {
  font-size: 11px;
  opacity: 0.9;
  font-weight: 500;
}

.stat-value {
  font-size: 20px;
  font-weight: 800;
  margin: 0;
}

.stat-value small {
  font-size: 12px;
  font-weight: 400;
  margin-left: 2px;
}

.panoramic-section {
  margin-bottom: 28px;
}

.category-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.cat-bar-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cat-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.cat-progress-bg {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.cat-progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 1s ease-out;
}

.history-scroll-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
}

.history-item-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  transform: translateX(4px);
}

.h-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.h-item-date {
  font-size: 12px;
  color: #94a3b8;
}

.h-item-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
}

.h-item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.h-item-cat {
  font-size: 13px;
  color: #3b82f6;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.h-item-tags {
  display: flex;
  gap: 6px;
}

.micro-tag {
  font-size: 11px;
  padding: 2px 6px;
  background: #f1f5f9;
  border-radius: 4px;
  color: #64748b;
}

.panoramic-empty {
  padding-top: 60px;
}

.premium-drawer .el-drawer__header {
  margin-bottom: 20px;
  padding: 20px 24px 0;
  font-weight: 800;
  font-size: 18px;
  color: #1e293b;
}

.premium-drawer .el-drawer__body {
  padding: 0 24px 24px;
}

/* Scrollbar */
.premium-drawer .el-drawer__body::-webkit-scrollbar {
  width: 4px;
}

/* Scrollbar */
.scrollable::-webkit-scrollbar {
  width: 6px;
}

.scrollable::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}

.scrollable::-webkit-scrollbar-track {
  background: transparent;
}
/* Intelligence Overlay */
.scanning-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(8px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.scan-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(37, 99, 235, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.1) 1px, transparent 1px);
  background-size: 40px 40px;
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(to bottom, transparent, #2563eb, transparent);
  box-shadow: 0 0 20px #2563eb;
  animation: scan-move 2.5s linear infinite;
}

@keyframes scan-move {
  from { top: 0%; }
  to { top: 100%; }
}

.scan-content {
  text-align: center;
  z-index: 10;
}

.hologram-box {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2px solid #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  margin: 0 auto 24px;
  background: rgba(37, 99, 235, 0.2);
  box-shadow: 0 0 30px rgba(37, 99, 235, 0.4);
}

.rotating {
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Smart Onboarding */
.smart-onboarding {
  height: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 40px;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
}

.api-empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
}

.api-empty-card {
  max-width: 420px;
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 40px -20px rgba(15, 23, 42, 0.2);
  padding: 32px;
  text-align: center;
}

.api-empty-tag {
  margin-bottom: 16px;
}

.api-empty-card h2 {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.api-empty-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.api-empty-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.onboarding-content {
  text-align: center;
  max-width: 400px;
  animation: fade-in-up 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-avatar-large {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #0f172a 0%, #3b82f6 100%);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  color: white;
  font-size: 32px;
}

.shadow-glow {
  box-shadow: 0 8px 30px rgba(59, 130, 246, 0.3);
}

.onboarding-content h2 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 12px;
}

.onboarding-content p {
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 32px;
}

.onboarding-suggestions {
  text-align: left;
}

.suggest-title {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.suggest-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.s-card {
  padding: 16px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.s-card:hover {
  border-color: #3b82f6;
  background: #f0f9ff;
  transform: translateX(4px);
}

.s-card .el-icon {
  font-size: 18px;
  color: #3b82f6;
}

.s-card span {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
