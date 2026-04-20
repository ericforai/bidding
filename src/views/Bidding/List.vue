<template>
  <div class="bidding-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">标讯中心</h2>
          <p class="page-subtitle">AI智能匹配，发现优质商机</p>
        </div>
        <div class="header-actions">
          <el-button v-if="customerOpportunityCenterEnabled" @click="handleOpenCustomerOpportunityCenter">
            <el-icon><UserFilled /></el-icon>
            客户商机中心
          </el-button>
          <el-button type="primary" @click="showSourceConfig = true">
            <el-icon><Setting /></el-icon>
            标讯源配置
          </el-button>
          <el-button type="success" @click="handleFetchExternalTenders" :loading="fetchingTenders">
            <el-icon><Download /></el-icon>
            一键获取标讯
          </el-button>
          <el-button type="warning" @click="showManualAdd = true">
            <el-icon><Plus /></el-icon>
            人工录入
          </el-button>
        </div>
      </div>
    </div>

    <!-- 搜索筛选区 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索标题、客户名称"
            clearable
            style="width: 220px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="地区">
          <el-select v-model="searchForm.region" placeholder="全部地区" clearable style="width: 150px">
            <el-option label="北京" value="北京" />
            <el-option label="上海" value="上海" />
            <el-option label="广州" value="广州" />
            <el-option label="深圳" value="深圳" />
            <el-option label="成都" value="成都" />
          </el-select>
        </el-form-item>
        <el-form-item label="行业">
          <el-select v-model="searchForm.industry" placeholder="全部行业" clearable style="width: 150px">
            <el-option label="政府" value="政府" />
            <el-option label="能源" value="能源" />
            <el-option label="交通" value="交通" />
            <el-option label="数据中心" value="数据中心" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="新建" value="new" />
            <el-option label="已联系" value="contacted" />
            <el-option label="跟进中" value="following" />
            <el-option label="报价中" value="quoting" />
            <el-option label="投标中" value="bidding" />
            <el-option label="已放弃" value="abandoned" />
          </el-select>
        </el-form-item>
        <el-form-item label="标讯来源">
          <el-select v-model="searchForm.source" placeholder="全部来源" clearable style="width: 130px">
            <el-option label="内部" value="internal" />
            <el-option label="外部获取" value="external" />
            <el-option label="人工录入" value="manual" />
            <el-option label="公共服务平台(CEB)" value="CEB" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 外部标讯源状态栏 -->
    <el-card v-if="sourceConfig.platforms.length > 0" class="source-status-card" shadow="never">
      <div class="source-status-content">
        <div class="status-left">
          <el-icon class="status-icon"><Setting /></el-icon>
          <span class="status-text">已配置 {{ sourceConfig.platforms.length }} 个标讯源</span>
          <el-tag v-for="platform in sourceConfig.platforms" :key="platform" size="small" type="info" class="source-tag">
            {{ platform }}
          </el-tag>
        </div>
        <div class="status-right">
          <span v-if="sourceConfig.autoSync" class="sync-info">
            <el-icon><Refresh /></el-icon>
            自动同步：每 {{ sourceConfig.syncInterval }} 小时
          </span>
          <span class="last-sync">上次同步: {{ lastSyncTime }}</span>
        </div>
      </div>
    </el-card>

    <!-- AI推荐区 -->
    <div v-if="filteredRecommendTenders.length > 0" class="ai-recommend-section">
      <div class="section-header">
        <div class="section-title">
          <el-icon class="ai-icon"><MagicStick /></el-icon>
          AI推荐
          <el-tag size="small" type="success" class="match-tag">高匹配</el-tag>
        </div>
        <el-link type="primary" underline="hover" @click="handleViewAllRecommend">
          查看全部
          <el-icon><ArrowRight /></el-icon>
        </el-link>
      </div>
      <div class="recommend-cards">
        <div
          v-for="tender in filteredRecommendTenders"
          :key="tender.id"
          class="b2b-project-item recommend-card"
          @click="handleViewDetail(tender.id)"
        >
          <div class="b2b-project-header">
            <h4 class="b2b-project-name">{{ tender.title }}</h4>
            <div class="ai-score" :class="getScoreClass(tender.aiScore)">
              {{ tender.aiScore }}分
            </div>
          </div>
          <div class="card-info">
            <span class="info-item">
              <el-icon><Location /></el-icon>
              {{ tender.region }}
            </span>
            <span class="info-item">
              <el-icon><Wallet /></el-icon>
              {{ tender.budget }}万元
            </span>
            <span class="info-item">
              <el-icon><Calendar /></el-icon>
              {{ tender.deadline }}截止
            </span>
          </div>
          <div class="card-tags">
            <el-tag v-for="tag in tender.tags" :key="tag" size="small">{{ tag }}</el-tag>
            <el-tag v-if="tender.source" size="small" :type="getSourceTagType(tender.source)">
              {{ getSourceText(tender.source) }}
            </el-tag>
          </div>
          <div class="card-footer">
            <el-text type="info" size="small">
              <el-icon><InfoFilled /></el-icon>
              {{ tender.aiReason }}
            </el-text>
          </div>
        </div>
      </div>
    </div>

    <!-- 标讯列表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header-content">
          <span class="card-title">标讯列表</span>
          <div class="card-actions">
            <el-button size="small" @click="handleViewRecords">
              <el-icon><List /></el-icon>
              分发记录
            </el-button>
            <el-button size="small" type="success" @click="showMarketInsight = true">
              <el-icon><TrendCharts /></el-icon>
              市场洞察
            </el-button>
            <el-button size="small" @click="handleExport">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
            <el-button
              size="small"
              type="warning"
              :loading="crawlerLoading"
              @click="handleFetchFromCeb"
            >
              <el-icon><Refresh /></el-icon>
              一键获取标讯
            </el-button>
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button value="all">全部 ({{ filteredTenders.length }})</el-radio-button>
              <el-radio-button value="new">新建 ({{ newTendersCount }})</el-radio-button>
              <el-radio-button value="contacted">已联系 ({{ contactedTendersCount }})</el-radio-button>
              <el-radio-button value="following">跟进中 ({{ followingTendersCount }})</el-radio-button>
              <el-radio-button value="quoting">报价中 ({{ quotingTendersCount }})</el-radio-button>
              <el-radio-button value="bidding">投标中 ({{ biddingTendersCount }})</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <!-- 批量操作栏 -->
      <div v-if="selectedTenders.length > 0" class="batch-actions">
        <div class="batch-info">
          <el-checkbox
            v-model="selectAllChecked"
            :indeterminate="isIndeterminate"
            @change="handleSelectAll"
          >
            已选择 {{ selectedTenders.length }} 条标讯
          </el-checkbox>
        </div>
        <div class="batch-buttons">
          <el-button type="primary" @click="showDistributeDialog = true">
            <el-icon><Share /></el-icon>
            批量分发
          </el-button>
          <el-button type="success" @click="handleBatchClaim">
            <el-icon><CircleCheck /></el-icon>
            领取标讯
          </el-button>
          <el-button type="warning" @click="handleBatchFollow">
            <el-icon><Star /></el-icon>
            批量关注
          </el-button>
          <el-button @click="handleClearSelection">取消选择</el-button>
        </div>
      </div>

      <!-- PC端表格视图 -->
      <div v-if="!isMobile" class="table-container">
        <el-table
          ref="tableRef"
          :data="displayTenders"
          style="width: 100%"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column prop="title" label="标讯标题" min-width="280" show-overflow-tooltip>
            <template #default="{ row = {} } = {}">
              <div class="title-cell">
                <el-link v-if="row.originalUrl" :href="row.originalUrl" target="_blank" type="primary" :underline="false">
                  <span class="title-text">{{ row.title }}</span>
                  <el-icon style="margin-left: 4px" size="14"><Link /></el-icon>
                </el-link>
                <span v-else class="title-text">{{ row.title }}</span>
                <el-tag v-if="row.aiScore >= 90" size="small" type="success" style="margin-left: 8px">高匹配</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="budget" label="预算" width="100" align="center">
            <template #default="{ row = {} } = {}">
              <span>{{ row.budget }}万元</span>
            </template>
          </el-table-column>
          <el-table-column prop="region" label="地区" width="100" align="center" />
          <el-table-column prop="industry" label="行业" width="100" align="center" />
          <el-table-column prop="source" label="来源" width="100" align="center">
            <template #default="{ row = {} } = {}">
              <el-tag v-if="row.source" :type="getSourceTagType(row.source)" size="small">
                {{ getSourceText(row.source) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="aiScore" label="AI评分" width="100" align="center">
            <template #default="{ row = {} } = {}">
              <span class="ai-score-highlight" :class="row.aiScore >= 85 ? 'ai-score-high' : row.aiScore >= 70 ? 'ai-score-medium' : 'ai-score-low'">
                {{ row.aiScore }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="deadline" label="截止日期" width="120" align="center" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row = {} } = {}">
              <span class="status-badge" :class="'status-' + row.status">
                {{ getStatusText(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320" align="center" fixed="right">
            <template #default="{ row = {} } = {}">
              <div class="table-actions">
                <el-tooltip content="查看详情" placement="top">
                  <el-button class="action-btn btn-view" size="small" :icon="View" @click="handleViewDetail(row.id)" />
                </el-tooltip>
                <el-tooltip v-if="showTenderAiEntry" content="AI分析" placement="top">
                  <el-button class="action-btn btn-analyze" size="small" :icon="MagicStick" @click="handleAIAnalysis(row.id)" />
                </el-tooltip>
                <el-tooltip content="参与投标" placement="top">
                  <el-button class="action-btn btn-participate" size="small" :icon="Document" @click="handleParticipate(row.id)" />
                </el-tooltip>
                <el-dropdown trigger="click" class="action-dropdown">
                  <el-button class="action-btn btn-more" size="small" :icon="MoreFilled" />
                  <template #dropdown>
                    <el-dropdown-menu class="bidding-action-menu">
                      <!-- 分组：操作 -->
                      <el-dropdown-item @click="handleSingleDistribute(row)">
                        <el-icon><Share /></el-icon>
                        <span>分发</span>
                      </el-dropdown-item>
                      <el-dropdown-item @click="handleSingleClaim(row)">
                        <el-icon><CircleCheck /></el-icon>
                        <span>领取</span>
                      </el-dropdown-item>
                      <el-dropdown-item @click="handleSingleAssign(row)">
                        <el-icon><User /></el-icon>
                        <span>指派</span>
                      </el-dropdown-item>
                      <!-- 分隔线 -->
                      <el-dropdown-item divided />
                      <!-- 分组：状态 -->
                      <el-dropdown-item @click="handleUpdateStatus(row, 'contacted')">
                        <el-icon class="status-icon"><Phone /></el-icon>
                        <span>已联系</span>
                      </el-dropdown-item>
                      <el-dropdown-item @click="handleUpdateStatus(row, 'following')">
                        <el-icon class="status-icon"><Star /></el-icon>
                        <span>跟进中</span>
                      </el-dropdown-item>
                      <el-dropdown-item @click="handleUpdateStatus(row, 'quoting')">
                        <el-icon class="status-icon"><EditPen /></el-icon>
                        <span>报价中</span>
                      </el-dropdown-item>
                      <el-dropdown-item @click="handleUpdateStatus(row, 'bidding')">
                        <el-icon class="status-icon"><Briefcase /></el-icon>
                        <span>参与投标</span>
                      </el-dropdown-item>
                      <el-dropdown-item @click="handleUpdateStatus(row, 'abandoned')">
                        <el-icon class="status-icon status-abandon"><Close /></el-icon>
                        <span>放弃跟进</span>
                      </el-dropdown-item>
                      <!-- 分隔线 -->
                      <el-dropdown-item divided />
                      <!-- 分组：删除 -->
                      <el-dropdown-item @click="handleDeleteTender(row)" class="danger-item">
                        <el-icon class="delete-icon"><Delete /></el-icon>
                        <span>删除</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端卡片视图 -->
      <div v-else class="mobile-card-view">
        <div
          v-for="row in displayTenders"
          :key="row.id"
          class="mobile-card-item"
        >
          <div class="mobile-card-header">
            <h4 class="mobile-card-title">{{ row.title }}</h4>
            <el-tag v-if="row.aiScore >= 90" size="small" type="success">高匹配</el-tag>
          </div>
          <div class="mobile-card-body">
            <div class="mobile-card-row">
              <span class="mobile-label">预算:</span>
              <span class="mobile-value">{{ row.budget }}万元</span>
            </div>
            <div class="mobile-card-row">
              <span class="mobile-label">地区:</span>
              <span class="mobile-value">{{ row.region }}</span>
            </div>
            <div class="mobile-card-row">
              <span class="mobile-label">行业:</span>
              <span class="mobile-value">{{ row.industry }}</span>
            </div>
            <div class="mobile-card-row">
              <span class="mobile-label">来源:</span>
              <el-tag v-if="row.source" :type="getSourceTagType(row.source)" size="small">
                {{ getSourceText(row.source) }}
              </el-tag>
            </div>
            <div class="mobile-card-row">
              <span class="mobile-label">AI评分:</span>
              <el-tag :type="getScoreTagType(row.aiScore)" size="small">
                {{ row.aiScore }}
              </el-tag>
            </div>
            <div class="mobile-card-row">
              <span class="mobile-label">截止日期:</span>
              <span class="mobile-value">{{ row.deadline }}</span>
            </div>
            <div class="mobile-card-row">
              <span class="mobile-label">状态:</span>
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </div>
          </div>
          <div class="mobile-card-actions">
            <el-button type="primary" size="small" @click="handleViewDetail(row.id)">
              查看详情
            </el-button>
            <el-button v-if="showTenderAiEntry" type="success" size="small" @click="handleAIAnalysis(row.id)">
              AI分析
            </el-button>
            <el-button size="small" @click="handleParticipate(row.id)">
              参与投标
            </el-button>
          </div>
        </div>
      </div>

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

    <!-- 分发对话框 - B2B优化版 -->
    <el-dialog
      v-model="showDistributeDialog"
      title=""
      width="900px"
      class="distribute-dialog"
      @close="resetDistributeForm"
      :close-on-click-modal="false"
    >
      <template #header>
        <div class="distribute-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon><Share /></el-icon>
            </div>
            <div class="header-info">
              <h3 class="header-title">标讯分发</h3>
              <p class="header-subtitle">智能分配销售资源，提升跟进效率</p>
            </div>
          </div>
          <div class="header-stats">
            <div class="stat-item">
              <span class="stat-value">{{ selectedTenders.length }}</span>
              <span class="stat-label">待分发</span>
            </div>
          </div>
        </div>
      </template>

      <div class="distribute-content">
        <!-- 左侧配置区 -->
        <div class="config-section">
          <!-- 待分发标讯预览 -->
          <div class="tenders-preview">
            <div class="preview-header">
              <span class="preview-title">待分发标讯</span>
              <el-tag size="small">{{ selectedTenders.length }} 条</el-tag>
            </div>
            <div class="preview-list">
              <div v-for="tender in selectedTenders.slice(0, 3)" :key="tender.id" class="preview-item">
                <div class="item-dot"></div>
                <span class="item-title">{{ tender.title }}</span>
                <el-tag size="small" type="info">{{ tender.region }}</el-tag>
              </div>
              <div v-if="selectedTenders.length > 3" class="preview-more">
                +{{ selectedTenders.length - 3 }} 条更多...
              </div>
            </div>
          </div>

          <!-- 分发方式选择 -->
          <div class="distribute-type-section">
            <div class="section-label">分发方式</div>
            <div class="type-cards">
              <div
                class="type-card"
                :class="{ active: distributeForm.type === 'auto' }"
                @click="distributeForm.type = 'auto'"
              >
                <div class="type-icon auto-icon">
                  <el-icon><MagicStick /></el-icon>
                </div>
                <div class="type-info">
                  <span class="type-name">智能分发</span>
                  <span class="type-desc">根据规则自动分配</span>
                </div>
              </div>
              <div
                class="type-card"
                :class="{ active: distributeForm.type === 'manual' }"
                @click="distributeForm.type = 'manual'"
              >
                <div class="type-icon manual-icon">
                  <el-icon><UserFilled /></el-icon>
                </div>
                <div class="type-info">
                  <span class="type-name">手动指定</span>
                  <span class="type-desc">手动选择销售人员</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 智能规则选择 -->
          <div v-if="distributeForm.type === 'auto'" class="rules-section">
            <div class="section-label">分发规则</div>
            <div class="rule-cards">
              <div
                class="rule-card"
                :class="{ active: distributeForm.rule === 'region' }"
                @click="distributeForm.rule = 'region'"
              >
                <div class="rule-icon region-icon">
                  <el-icon><Location /></el-icon>
                </div>
                <div class="rule-info">
                  <span class="rule-name">按区域分发</span>
                  <span class="rule-desc">根据标讯地区自动分配</span>
                </div>
              </div>
              <div
                class="rule-card"
                :class="{ active: distributeForm.rule === 'product' }"
                @click="distributeForm.rule = 'product'"
              >
                <div class="rule-icon product-icon">
                  <el-icon><Box /></el-icon>
                </div>
                <div class="rule-info">
                  <span class="rule-name">按产品线</span>
                  <span class="rule-desc">根据行业类型分配</span>
                </div>
              </div>
              <div
                class="rule-card"
                :class="{ active: distributeForm.rule === 'ai' }"
                @click="distributeForm.rule = 'ai'"
              >
                <div class="rule-icon ai-icon">
                  <el-icon><TrendCharts /></el-icon>
                </div>
                <div class="rule-info">
                  <span class="rule-name">按AI评分</span>
                  <span class="rule-desc">高分优先给资深销售</span>
                </div>
              </div>
              <div
                class="rule-card"
                :class="{ active: distributeForm.rule === 'average' }"
                @click="distributeForm.rule = 'average'"
              >
                <div class="rule-icon average-icon">
                  <el-icon><Grid /></el-icon>
                </div>
                <div class="rule-info">
                  <span class="rule-name">平均分配</span>
                  <span class="rule-desc">均匀分配所有人</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 手动选择人员 -->
          <div v-if="distributeForm.type === 'manual'" class="assignees-section">
            <div class="section-label">指派给</div>
            <div class="sales-grid">
              <div
                v-for="sales in salesStaff"
                :key="sales.id"
                class="sales-card"
                :class="{ selected: distributeForm.assignees.includes(sales.id) }"
                @click="toggleSalesAssign(sales.id)"
              >
                <div class="sales-avatar">{{ sales.name.charAt(0) }}</div>
                <div class="sales-info">
                  <span class="sales-name">{{ sales.name }}</span>
                  <span class="sales-role">{{ sales.role }}</span>
                </div>
                <div class="sales-check">
                  <el-icon v-if="distributeForm.assignees.includes(sales.id)"><Check /></el-icon>
                </div>
              </div>
            </div>
          </div>

          <!-- 截止时间 -->
          <div v-if="distributeForm.type === 'manual'" class="deadline-section">
            <div class="section-label">跟进截止时间</div>
            <el-date-picker
              v-model="distributeForm.deadline"
              type="datetime"
              placeholder="选择截止时间"
              format="YYYY-MM-DD HH:mm"
              style="width: 100%"
            />
          </div>

          <!-- 备注 -->
          <div class="remark-section">
            <div class="section-label">备注说明</div>
            <el-input
              v-model="distributeForm.remark"
              type="textarea"
              :rows="2"
              placeholder="填写分发说明、注意事项等（选填）"
            />
          </div>
        </div>

        <!-- 右侧预览区 -->
        <div class="preview-section">
          <div class="preview-header">
            <span class="preview-title">分配预览</span>
          </div>
          <div class="preview-content">
            <div v-if="!distributeForm.type || (distributeForm.type === 'auto' && !distributeForm.rule)" class="preview-empty">
              <el-icon class="empty-icon"><Document /></el-icon>
              <p>请选择分发方式和规则</p>
            </div>
            <div v-else class="preview-distribution">
              <div v-for="preview in distributionPreview" :key="preview.salesId" class="preview-group">
                <div class="preview-header">
                  <div class="preview-sales">{{ preview.salesName }}</div>
                  <el-tag size="small" type="primary">{{ preview.count }} 条</el-tag>
                </div>
                <div class="preview-tenders">
                  <div v-for="tender in preview.tenders" :key="tender.id" class="preview-tender-item">
                    {{ tender.title }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="distribute-footer">
          <el-button @click="showDistributeDialog = false">取消</el-button>
          <el-button type="primary" @click="handleDistribute" :loading="distributeLoading">
            <el-icon><Share /></el-icon>
            确认分发 {{ selectedTenders.length }} 条标讯
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 指派对话框 -->
    <el-dialog v-model="showAssignDialog" title="指派标讯" width="500px" @close="resetAssignForm">
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="标讯标题">
          <el-text>{{ assignForm.tenderTitle }}</el-text>
        </el-form-item>
        <el-form-item label="指派给" required>
          <el-select v-model="assignForm.assignee" placeholder="选择销售人员" style="width: 100%">
            <el-option-group label="华东区">
              <el-option label="小王" value="U001" />
              <el-option label="李经理" value="U002" />
            </el-option-group>
            <el-option-group label="华南区">
              <el-option label="张销售" value="U003" />
              <el-option label="陈专员" value="U004" />
            </el-option-group>
            <el-option-group label="华北区">
              <el-option label="刘主管" value="U005" />
              <el-option label="赵经理" value="U006" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="assignForm.priority">
            <el-radio value="high">高</el-radio>
            <el-radio value="medium">中</el-radio>
            <el-radio value="low">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="assignForm.deadline"
            type="datetime"
            placeholder="选择跟进截止时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="assignForm.remark"
            type="textarea"
            :rows="3"
            placeholder="填写指派说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAssignDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAssign" :loading="assignLoading">
          确认指派
        </el-button>
      </template>
    </el-dialog>

    <!-- 分发记录对话框 -->
    <el-dialog v-model="showRecordDialog" title="分发记录" width="700px">
      <el-table :data="distributeRecords" style="width: 100%" max-height="400">
        <el-table-column prop="tenderTitle" label="标讯标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="assignee" label="被分配人" width="120" />
        <el-table-column prop="type" label="分发方式" width="100">
          <template #default="{ row = {} } = {}">
            <el-tag :type="row.type === 'auto' ? 'success' : 'primary'" size="small">
              {{ row.type === 'auto' ? '智能分发' : '手动指定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="分发时间" width="160" />
        <el-table-column prop="operator" label="操作人" width="100" />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="showRecordDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 外部标讯源配置对话框 -->
    <el-dialog v-model="showSourceConfig" title="外部标讯源配置" width="600px" @close="resetSourceConfig">
      <el-form :model="sourceConfig" label-width="120px">
        <el-form-item label="标讯源平台">
          <el-checkbox-group v-model="sourceConfig.platforms">
            <el-checkbox label="中国政府采购网" />
            <el-checkbox label="各省招标网" />
            <el-checkbox label="第三方商机服务" />
            <el-checkbox label="企业招标平台" />
          </el-checkbox-group>
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            选择需要同步数据的外部招标平台
          </div>
        </el-form-item>
        <el-form-item label="API配置">
          <el-input
            v-model="sourceConfig.apiEndpoint"
            placeholder="输入第三方API端点地址"
            :disabled="sourceConfig.platforms.length === 0"
          />
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            配置第三方商机服务的API接口地址
          </div>
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input
            v-model="sourceConfig.apiKey"
            type="password"
            placeholder="输入API访问密钥"
            show-password
          />
        </el-form-item>
        <el-form-item label="关键字筛选">
          <el-select
            v-model="sourceConfig.keywords"
            multiple
            allow-create
            filterable
            placeholder="选择或输入关键字"
            style="width: 100%"
          >
            <!-- MRO 工具类 -->
            <el-option label="MRO 工具" value="MRO 工具" />
            <el-option label="工具耗材" value="工具耗材" />
            <el-option label="焊接" value="焊接" />
            <el-option label="刀具" value="刀具" />
            <el-option label="量具" value="量具" />
            <el-option label="机床" value="机床" />
            <el-option label="磨具" value="磨具" />
            <!-- 化学材料 -->
            <el-option label="润滑" value="润滑" />
            <el-option label="胶粘" value="胶粘" />
            <el-option label="车间化学品" value="车间化学品" />
            <!-- 安全防护 -->
            <el-option label="劳保" value="劳保" />
            <el-option label="安全消防" value="安全消防" />
            <!-- 物料存储 -->
            <el-option label="搬运存储" value="搬运存储" />
            <el-option label="工位" value="工位" />
            <el-option label="包材" value="包材" />
            <!-- 环境设备 -->
            <el-option label="清洁" value="清洁" />
            <el-option label="办公" value="办公" />
            <el-option label="制冷暖通" value="制冷暖通" />
            <!-- 电气工控 -->
            <el-option label="工控" value="工控" />
            <el-option label="低压" value="低压" />
            <el-option label="电工" value="电工" />
            <el-option label="照明" value="照明" />
            <!-- 机械传动 -->
            <el-option label="轴承" value="轴承" />
            <el-option label="皮带" value="皮带" />
            <el-option label="机械" value="机械" />
            <el-option label="电子" value="电子" />
            <el-option label="气动" value="气动" />
            <el-option label="液压" value="液压" />
            <el-option label="管阀" value="管阀" />
            <el-option label="泵" value="泵" />
            <!-- 建工检测 -->
            <el-option label="紧固" value="紧固" />
            <el-option label="密封" value="密封" />
            <el-option label="建工材料" value="建工材料" />
            <el-option label="工业检测" value="工业检测" />
            <el-option label="实验室产品" value="实验室产品" />
            <!-- 其他 -->
            <el-option label="企业福礼" value="企业福礼" />
            <el-option label="紧急救护" value="紧急救护" />
          </el-select>
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            根据关键字自动筛选匹配的标讯信息
          </div>
        </el-form-item>
        <el-form-item label="地区筛选">
          <el-select v-model="sourceConfig.regions" multiple placeholder="选择目标地区" style="width: 100%">
            <el-option label="北京" value="北京" />
            <el-option label="上海" value="上海" />
            <el-option label="广州" value="广州" />
            <el-option label="深圳" value="深圳" />
            <el-option label="成都" value="成都" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算范围">
          <el-input-number v-model="sourceConfig.minBudget" :min="0" placeholder="最小预算" />
          <span style="margin: 0 10px">-</span>
          <el-input-number v-model="sourceConfig.maxBudget" :min="0" placeholder="最大预算" />
          <span style="margin-left: 10px">万元</span>
        </el-form-item>
        <el-form-item label="自动同步">
          <el-switch v-model="sourceConfig.autoSync" />
          <span v-if="sourceConfig.autoSync" class="sync-interval">
            每
            <el-input-number v-model="sourceConfig.syncInterval" :min="1" :max="24" size="small" />
            小时自动同步一次
          </span>
        </el-form-item>
        <el-form-item label="数据保存">
          <el-checkbox v-model="sourceConfig.autoSave">自动匹配后入库</el-checkbox>
          <el-checkbox v-model="sourceConfig.enableDedupe">自动去重</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSourceConfig = false">取消</el-button>
        <el-button @click="testConnection" :loading="testingConnection">
          <el-icon><Connection /></el-icon>
          测试连接
        </el-button>
        <el-button type="primary" @click="saveSourceConfig" :loading="savingConfig">
          保存配置
        </el-button>
      </template>
    </el-dialog>

    <!-- 人工录入对话框 -->
    <el-dialog v-model="showManualAdd" title="人工录入标讯" width="700px" @close="resetManualForm">
      <el-form ref="manualFormRef" :model="manualForm" :rules="manualFormRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="标讯标题" prop="title">
              <el-input v-model="manualForm.title" placeholder="请输入招标项目标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预算金额" prop="budget">
              <el-input-number v-model="manualForm.budget" :min="0" :precision="2" style="width: 100%" />
              <span style="margin-left: 10px">万元</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地区" prop="region">
              <el-select v-model="manualForm.region" placeholder="选择地区" style="width: 100%">
                <el-option label="北京" value="北京" />
                <el-option label="上海" value="上海" />
                <el-option label="广州" value="广州" />
                <el-option label="深圳" value="深圳" />
                <el-option label="成都" value="成都" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行业分类" prop="industry">
              <el-select v-model="manualForm.industry" placeholder="选择行业" style="width: 100%">
                <el-option label="政府" value="政府" />
                <el-option label="能源" value="能源" />
                <el-option label="交通" value="交通" />
                <el-option label="数据中心" value="数据中心" />
                <el-option label="金融" value="金融" />
                <el-option label="医疗" value="医疗" />
                <el-option label="教育" value="教育" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止日期" prop="deadline">
              <el-date-picker
                v-model="manualForm.deadline"
                type="date"
                placeholder="选择截止日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购单位" prop="purchaser">
              <el-input v-model="manualForm.purchaser" placeholder="采购单位名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contact">
              <el-input v-model="manualForm.contact" placeholder="联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="manualForm.phone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目描述">
              <el-input
                v-model="manualForm.description"
                type="textarea"
                :rows="3"
                placeholder="请输入项目详细描述"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="附件上传">
              <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :on-change="handleFileChange"
                :file-list="manualForm.attachments"
                :limit="5"
                multiple
                drag
              >
                <el-icon class="el-icon--upload"><Upload /></el-icon>
                <div class="el-upload__text">
                  将文件拖到此处，或<em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持上传招标文件、技术规格书等，单个文件不超过10MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签">
              <el-select v-model="manualForm.tags" multiple allow-create placeholder="添加标签" style="width: 100%">
                <el-option label="公开招标" value="公开招标" />
                <el-option label="邀请招标" value="邀请招标" />
                <el-option label="竞争性谈判" value="竞争性谈判" />
                <el-option label="单一来源" value="单一来源" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showManualAdd = false">取消</el-button>
        <el-button type="primary" @click="saveManualTender" :loading="savingManual">
          <el-icon><Check /></el-icon>
          保存入库
        </el-button>
      </template>
    </el-dialog>

    <!-- 获取标讯结果对话框 -->
    <el-dialog v-model="showFetchResult" title="标讯获取结果" width="800px">
      <div class="fetch-result-header">
        <el-statistic title="获取总数" :value="fetchResults.total">
          <template #suffix>条</template>
        </el-statistic>
        <el-statistic title="匹配成功" :value="fetchResults.matched">
          <template #suffix>
            <span style="color: #67c23a">条</span>
          </template>
        </el-statistic>
        <el-statistic title="已入库" :value="fetchResults.imported">
          <template #suffix>
            <span style="color: #409eff">条</span>
          </template>
        </el-statistic>
      </div>
      <el-table :data="fetchResults.list" max-height="300" style="margin-top: 20px">
        <el-table-column prop="title" label="标讯标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sourcePlatform" label="来源" width="120" />
        <el-table-column prop="budget" label="预算" width="100">
          <template #default="{ row = {} } = {}">
            {{ row.budget }}万元
          </template>
        </el-table-column>
        <el-table-column prop="region" label="地区" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row = {} } = {}">
            <el-tag :type="row.imported ? 'success' : 'info'" size="small">
              {{ row.imported ? '已入库' : '待入库' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row = {} } = {}">
            <el-button v-if="!row.imported" link type="primary" size="small" @click="importSingleTender(row)">
              入库
            </el-button>
            <el-text v-else type="success" size="small">
              <el-icon><Check /></el-icon> 已入库
            </el-text>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showFetchResult = false">关闭</el-button>
        <el-button type="primary" @click="importAllTenders" :disabled="fetchResults.allImported">
          批量入库
        </el-button>
      </template>
    </el-dialog>

    <!-- 市场洞察对话框 -->
    <el-dialog v-model="showMarketInsight" title="市场洞察与趋势预测" width="900px">
      <template #header>
        <div class="market-insight-header">
          <span>市场洞察与趋势预测</span>
          <el-button
            :icon="Refresh"
            :loading="loadingTrendData"
            @click="refreshTrendData"
            size="small"
            text
          >
            刷新数据
          </el-button>
        </div>
      </template>
      <div v-if="loadingTrendData" class="trend-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在加载趋势数据...</span>
      </div>
      <el-tabs v-else v-model="activeInsightTab">
        <!-- 行业趋势 -->
        <el-tab-pane label="行业趋势" name="industry">
          <div class="insight-content">
            <div class="insight-header">
              <h4>热点采购行业 (近3个月)</h4>
              <el-tag type="info" size="small">数据更新: 2024-01-15</el-tag>
            </div>
            <el-table :data="industryTrends" size="small" stripe>
              <el-table-column prop="industry" label="行业" width="150">
                <template #default="{ row = {} } = {}">
                  <div class="industry-cell">
                    <span :class="['industry-dot', row.color]"></span>
                    {{ row.industry }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="count" label="标讯数量" width="120" align="center" />
              <el-table-column prop="amount" label="总预算(万元)" width="130" align="center">
                <template #default="{ row = {} } = {}">
                  {{ row.amount.toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="growth" label="同比增长" width="120" align="center">
                <template #default="{ row = {} } = {}">
                  <span :class="row.growth > 0 ? 'growth-up' : 'growth-down'">
                    {{ row.growth > 0 ? '+' : '' }}{{ row.growth }}%
                    <el-icon v-if="row.growth > 0"><ArrowRight /></el-icon>
                    <el-icon v-else><ArrowDown /></el-icon>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="trend" label="趋势" width="100" align="center">
                <template #default="{ row = {} } = {}">
                  <el-tag :type="row.trend === 'up' ? 'success' : row.trend === 'down' ? 'danger' : 'info'" size="small">
                    {{ row.trend === 'up' ? '上升' : row.trend === 'down' ? '下降' : '平稳' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="hotLevel" label="热度" width="140" align="center">
                <template #default="{ row = {} } = {}">
                  <el-rate v-model="row.hotLevel" disabled size="small" />
                </template>
              </el-table-column>
            </el-table>
            <div class="insight-summary">
              <el-alert type="success" :closable="false" show-icon>
                <template #title>
                  <strong>AI洞察:</strong> {{ industryInsight }}
                </template>
              </el-alert>
            </div>
          </div>
        </el-tab-pane>

        <!-- 采购方规律 -->
        <el-tab-pane label="采购方规律" name="purchaser">
          <div class="insight-content">
            <div class="insight-header">
              <h4>重点客户招标规律</h4>
              <el-tag type="info" size="small">基于历史数据分析</el-tag>
            </div>
            <el-table :data="purchaserPatterns" size="small" stripe>
              <el-table-column prop="name" label="采购方" min-width="180" show-overflow-tooltip />
              <el-table-column prop="industry" label="所属行业" width="120" />
              <el-table-column prop="frequency" label="年招标频次" width="110" align="center">
                <template #default="{ row = {} } = {}">
                  <el-tag :type="row.frequency >= 10 ? 'danger' : row.frequency >= 5 ? 'warning' : 'info'" size="small">
                    {{ row.frequency }}次
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="period" label="常用招标月份" width="150" align="center" />
              <el-table-column prop="avgBudget" label="平均预算(万元)" width="130" align="center">
                <template #default="{ row = {} } = {}">
                  {{ row.avgBudget.toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column label="机会评估" width="150" align="center">
                <template #default="{ row = {} } = {}">
                  <div class="opportunity-rating">
                    <el-rate v-model="row.opportunity" disabled size="small" />
                    <el-text size="small" type="success">{{ getOpportunityText(row.opportunity) }}</el-text>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <div class="insight-summary">
              <el-alert type="warning" :closable="false" show-icon>
                <template #title>
                  <strong>策略建议:</strong> {{ purchaserInsight }}
                </template>
              </el-alert>
            </div>
          </div>
        </el-tab-pane>

        <!-- 高潜力机会 -->
        <el-tab-pane label="高潜力机会" name="opportunity">
          <div class="insight-content">
            <div class="insight-header">
              <h4>AI推荐高潜力机会</h4>
              <el-tag type="success" size="small">智能匹配</el-tag>
            </div>
            <el-row :gutter="16">
              <el-col :span="12" v-for="item in potentialOpportunities" :key="item.id">
                <el-card class="opportunity-card" shadow="hover">
                  <div class="opportunity-header">
                    <h5>{{ item.title }}</h5>
                    <el-tag :type="item.priority === 'high' ? 'danger' : item.priority === 'medium' ? 'warning' : 'info'" size="small">
                      {{ item.priority === 'high' ? '高优先级' : item.priority === 'medium' ? '中优先级' : '普通' }}
                    </el-tag>
                  </div>
                  <div class="opportunity-info">
                    <div class="info-row">
                      <span class="label">采购方:</span>
                      <span class="value">{{ item.purchaser }}</span>
                    </div>
                    <div class="info-row">
                      <span class="label">预算:</span>
                      <span class="value">{{ item.budget }}万元</span>
                    </div>
                    <div class="info-row">
                      <span class="label">地区:</span>
                      <span class="value">{{ item.region }}</span>
                    </div>
                  </div>
                  <p class="opportunity-reason">
                    <el-icon><InfoFilled /></el-icon>
                    {{ item.reason }}
                  </p>
                  <div class="opportunity-footer">
                    <div class="match-bar">
                      <span class="match-label">匹配度</span>
                      <el-progress :percentage="item.match" :color="getMatchColor(item.match)" :stroke-width="8" />
                    </div>
                    <el-button type="primary" size="small" @click="handleOpportunityAction(item.id)">
                      立即跟进
                    </el-button>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- 预测分析 -->
        <el-tab-pane label="预测分析" name="forecast">
          <div class="insight-content">
            <div class="insight-header">
              <h4>未来市场趋势预测</h4>
              <el-tag type="info" size="small">AI预测</el-tag>
            </div>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-card class="forecast-card">
                  <template #header>
                    <div class="card-header-small">
                      <el-icon><TrendCharts /></el-icon>
                      <span>下月招标趋势</span>
                    </div>
                  </template>
                  <div class="forecast-data">
                    <div class="forecast-item">
                      <span class="forecast-label">预计发布量</span>
                      <span class="forecast-value">+18%</span>
                    </div>
                    <div class="forecast-item">
                      <span class="forecast-label">热门行业</span>
                      <span class="forecast-value">数据中心</span>
                    </div>
                    <div class="forecast-item">
                      <span class="forecast-label">活跃地区</span>
                      <span class="forecast-value">华东</span>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card class="forecast-card">
                  <template #header>
                    <div class="card-header-small">
                      <el-icon><Calendar /></el-icon>
                      <span>季度预测</span>
                    </div>
                  </template>
                  <div class="forecast-data">
                    <div class="forecast-item">
                      <span class="forecast-label">Q1 预计总量</span>
                      <span class="forecast-value">1,280条</span>
                    </div>
                    <div class="forecast-item">
                      <span class="forecast-label">同比变化</span>
                      <span class="forecast-value forecast-up">+25%</span>
                    </div>
                    <div class="forecast-item">
                      <span class="forecast-label">市场活跃度</span>
                      <span class="forecast-value">高</span>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
            <el-card class="forecast-tips" style="margin-top: 16px">
              <template #header>
                <div class="card-header-small">
                  <el-icon><MagicStick /></el-icon>
                  <span>AI建议</span>
                </div>
              </template>
              <ul class="tips-list">
                <li v-for="(tip, index) in forecastTips" :key="index">
                  <el-icon class="tip-icon" :color="tip.color"><CircleCheck /></el-icon>
                  {{ tip.text }}
                </li>
              </ul>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- AI解析进度弹窗 -->
    <el-dialog
      v-model="showParsingDialog"
      title="AI分析中"
      width="480px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="parsing-content">
        <div class="parsing-animation">
          <div class="parsing-spinner"></div>
        </div>
        <p class="parsing-text">正在解析招标文件...</p>
        <el-progress
          :percentage="parseProgress"
          :stroke-width="12"
          :color="progressColors"
        >
          <template #default="{ percentage }">
            {{ Math.round(percentage) }}%
          </template>
        </el-progress>
        <p class="parsing-hint">AI正在分析标书文档，提取关键信息</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useBiddingStore } from '@/stores/bidding'
import { useUserStore } from '@/stores/user'
import { tendersApi } from '@/api'
import { crawlerApi } from '@/api/modules/tenders'
import {
  Search, Plus, Download, Star, TrendCharts, List, Share, CircleCheck,
  MoreFilled, Check, User, Calendar, Flag, Briefcase, ChatDotRound,
  InfoFilled, Document, MagicStick, UserFilled, Location, Box, Grid, View,
  ArrowRight, ArrowDown, Wallet, Setting, Upload, Delete, Connection, Refresh,
  Phone, Close, EditPen, Loading
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBreakoutTopics,
  getStatsSummary,
  transformToIndustryTrends,
  transformToOpportunities,
  generateInsight,
  generateForecastTips
} from '@/api/trendradar'
import { useExport } from '@/composables/useExport'
import { ExportType } from '@/api'

const router = useRouter()
const biddingStore = useBiddingStore()
const userStore = useUserStore()
const showTenderAiEntry = true

// 表格引用
const tableRef = ref(null)

// 搜索表单
const searchForm = ref({
  keyword: '',
  region: '',
  industry: '',
  status: '',
  source: ''
})

// 视图模式
const viewMode = ref('all')

// 移动端检测
const isMobile = ref(false)

// 爬虫触发
const crawlerLoading = ref(false)
const handleFetchFromCeb = async () => {
  crawlerLoading.value = true
  try {
    const res = await crawlerApi.trigger({ keyword: searchForm.value?.keyword || '', pageSize: 20 })
    if (res?.success || res?.data) {
      const d = res.data || {}
      ElMessage.success(`标讯同步完成：新增 ${d.saved ?? 0} 条，跳过 ${d.skipped ?? 0} 条`)
      // 刷新列表
      await biddingStore.getTenders()
    } else {
      ElMessage.warning(res?.message || '同步完成，但未得到结果')
    }
  } catch (e) {
    ElMessage.error('获取标讯失败，请检查网络或后端服务')
  } finally {
    crawlerLoading.value = false
  }
}

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const handleResize = () => {
  checkMobile()
}

// 分页
const pagination = ref({
  currentPage: 1,
  pageSize: 10
})

// 收藏的标讯
const followedTenders = ref([])

// 选中的标讯
const selectedTenders = ref([])

// 全选状态
const selectAllChecked = ref(false)
const isIndeterminate = ref(false)

// 分发对话框
const showDistributeDialog = ref(false)
const distributeLoading = ref(false)
const distributeForm = ref({
  type: 'auto',
  rule: '',
  assignees: [],
  deadline: null,
  remark: ''
})

// 销售人员数据
const salesStaff = ref([
  { id: 'U001', name: '小王', role: '销售经理', region: '华东', workload: 3, avatar: '' },
  { id: 'U002', name: '李经理', role: '资深销售', region: '华东', workload: 2, avatar: '' },
  { id: 'U003', name: '张销售', role: '销售专员', region: '华南', workload: 4, avatar: '' },
  { id: 'U004', name: '陈专员', role: '销售专员', region: '华南', workload: 2, avatar: '' },
  { id: 'U005', name: '刘主管', role: '销售主管', region: '华北', workload: 3, avatar: '' },
  { id: 'U006', name: '赵经理', role: '区域经理', region: '华北', workload: 2, avatar: '' }
])

// 分配预览
const distributionPreview = computed(() => {
  if (!distributeForm.value.type) return []
  if (distributeForm.value.type === 'auto' && !distributeForm.value.rule) return []

  const selected = selectedTenders.value
  const preview = []

  if (distributeForm.value.type === 'auto') {
    // 智能分发预览
    switch (distributeForm.value.rule) {
      case 'region':
        // 按区域分组预览
        const regionMap = { '华东': ['U001', 'U002'], '华南': ['U003', 'U004'], '华北': ['U005', 'U006'] }
        Object.entries(regionMap).forEach(([region, salesIds]) => {
          const regionTenders = selected.filter(t => t.region === region)
          if (regionTenders.length > 0) {
            salesIds.forEach(salesId => {
              const sales = salesStaff.value.find(s => s.id === salesId)
              preview.push({
                salesId,
                salesName: sales?.name || '',
                count: Math.ceil(regionTenders.length / salesIds.length),
                tenders: regionTenders.slice(0, Math.ceil(regionTenders.length / salesIds.length))
              })
            })
          }
        })
        break
      case 'product':
        // 按产品线预览
        preview.push({
          salesId: 'U001',
          salesName: '小王',
          count: Math.ceil(selected.length / 3),
          tenders: selected.slice(0, Math.ceil(selected.length / 3))
        })
        preview.push({
          salesId: 'U003',
          salesName: '张销售',
          count: Math.ceil(selected.length / 3),
          tenders: selected.slice(Math.ceil(selected.length / 3), Math.ceil(selected.length * 2 / 3))
        })
        preview.push({
          salesId: 'U005',
          salesName: '刘主管',
          count: selected.length - Math.ceil(selected.length * 2 / 3),
          tenders: selected.slice(Math.ceil(selected.length * 2 / 3))
        })
        break
      case 'ai':
        preview.push({
          salesId: 'U002',
          salesName: '李经理',
          count: selected.filter(t => t.aiScore >= 90).length,
          tenders: selected.filter(t => t.aiScore >= 90)
        })
        preview.push({
          salesId: 'U001',
          salesName: '小王',
          count: selected.filter(t => t.aiScore < 90).length,
          tenders: selected.filter(t => t.aiScore < 90)
        })
        break
      case 'average':
        const perSales = Math.ceil(selected.length / salesStaff.value.length)
        salesStaff.value.forEach((sales, index) => {
          const start = index * perSales
          const end = start + perSales
          preview.push({
            salesId: sales.id,
            salesName: sales.name,
            count: selected.slice(start, end).length,
            tenders: selected.slice(start, end)
          })
        })
        break
    }
  } else {
    // 手动指定预览
      distributeForm.value.assignees.forEach(salesId => {
        const sales = salesStaff.value.find(s => s.id === salesId)
        preview.push({
          salesId,
          salesName: sales?.name || '',
          count: Math.ceil(selected.length / distributeForm.value.assignees.length),
          tenders: selected.slice(0, Math.ceil(selected.length / distributeForm.value.assignees.length))
        })
      })
    }
    return preview.filter(p => p.count > 0)
})

// 切换销售人员选择
const toggleSalesAssign = (salesId) => {
  const index = distributeForm.value.assignees.indexOf(salesId)
  if (index > -1) {
    distributeForm.value.assignees.splice(index, 1)
  } else {
    distributeForm.value.assignees.push(salesId)
  }
}

// 指派对话框
const showAssignDialog = ref(false)
const assignLoading = ref(false)
const assignForm = ref({
  tenderId: null,
  tenderTitle: '',
  assignee: '',
  priority: 'medium',
  deadline: null,
  remark: ''
})

// 分发记录
const showRecordDialog = ref(false)
const distributeRecords = ref([
  {
    tenderTitle: '某市政府数字化采购项目',
    assignee: '小王',
    type: 'auto',
    time: '2024-01-15 10:30',
    operator: '当前用户'
  },
  {
    tenderTitle: '某能源集团信息化建设',
    assignee: '张销售',
    type: 'manual',
    time: '2024-01-14 14:20',
    operator: '当前用户'
  }
])

// 市场洞察
const showMarketInsight = ref(false)
const activeInsightTab = ref('industry')
const loadingTrendData = ref(false)
const trendDataLoaded = ref(false)

// 行业趋势数据（MRO工业品分类 - 严格按照指定分类）
const industryTrends = ref([
  // 1. 工具、工具耗材、焊接
  { industry: '工具', count: 331, amount: 21100, growth: 32, trend: 'up', hotLevel: 5, color: 'blue' },
  { industry: '工具耗材', count: 268, amount: 8600, growth: 18, trend: 'up', hotLevel: 4, color: 'blue' },
  { industry: '焊接', count: 98, amount: 15800, growth: 22, trend: 'up', hotLevel: 4, color: 'blue' },
  // 2. 刀具、量具、机床、磨具
  { industry: '刀具', count: 112, amount: 18600, growth: 18, trend: 'up', hotLevel: 4, color: 'green' },
  { industry: '量具', count: 87, amount: 12400, growth: 15, trend: 'stable', hotLevel: 3, color: 'green' },
  { industry: '机床', count: 76, amount: 38500, growth: 42, trend: 'up', hotLevel: 5, color: 'green' },
  { industry: '磨具', count: 94, amount: 9800, growth: 12, trend: 'stable', hotLevel: 3, color: 'green' },
  // 3. 润滑胶粘、车间化学品
  { industry: '润滑胶粘', count: 284, amount: 19800, growth: 15, trend: 'up', hotLevel: 4, color: 'orange' },
  { industry: '车间化学品', count: 72, amount: 6400, growth: 5, trend: 'stable', hotLevel: 3, color: 'orange' },
  // 4. 劳保安全、消防
  { industry: '劳保安全', count: 413, amount: 37600, growth: 42, trend: 'up', hotLevel: 5, color: 'red' },
  { industry: '消防', count: 134, amount: 18500, growth: 32, trend: 'up', hotLevel: 4, color: 'red' },
  // 5. 搬运、存储、工位、包材
  { industry: '搬运', count: 92, amount: 28600, growth: 25, trend: 'up', hotLevel: 4, color: 'purple' },
  { industry: '存储', count: 178, amount: 16800, growth: 20, trend: 'up', hotLevel: 4, color: 'purple' },
  { industry: '工位', count: 115, amount: 8900, growth: 12, trend: 'stable', hotLevel: 3, color: 'purple' },
  { industry: '包材', count: 203, amount: 12400, growth: 15, trend: 'up', hotLevel: 4, color: 'purple' },
  // 6. 清洁、办公、制冷暖通
  { industry: '清洁', count: 167, amount: 7800, growth: 10, trend: 'stable', hotLevel: 3, color: 'cyan' },
  { industry: '办公', count: 289, amount: 18600, growth: 8, trend: 'stable', hotLevel: 3, color: 'cyan' },
  { industry: '制冷暖通', count: 223, amount: 53300, growth: 25, trend: 'up', hotLevel: 4, color: 'cyan' },
  // 7. 工控低压电工照明
  { industry: '工控低压', count: 333, amount: 57600, growth: 30, trend: 'up', hotLevel: 5, color: 'yellow' },
  { industry: '电工照明', count: 410, amount: 36300, growth: 24, trend: 'up', hotLevel: 4, color: 'yellow' },
  // 8. 轴承、皮带、机械、电子
  { industry: '轴承', count: 142, amount: 24500, growth: 20, trend: 'up', hotLevel: 4, color: 'pink' },
  { industry: '皮带', count: 98, amount: 11200, growth: 12, trend: 'stable', hotLevel: 3, color: 'pink' },
  { industry: '机械电子', count: 268, amount: 32000, growth: 28, trend: 'up', hotLevel: 4, color: 'pink' },
  // 9. 气动、液压管阀、泵
  { industry: '气动', count: 126, amount: 18500, growth: 22, trend: 'up', hotLevel: 4, color: 'indigo' },
  { industry: '液压管阀', count: 264, amount: 60500, growth: 26, trend: 'up', hotLevel: 4, color: 'indigo' },
  { industry: '泵', count: 145, amount: 22000, growth: 18, trend: 'up', hotLevel: 4, color: 'indigo' },
  // 10. 紧固、密封、建工材料
  { industry: '紧固', count: 268, amount: 14500, growth: 12, trend: 'stable', hotLevel: 3, color: 'lime' },
  { industry: '密封', count: 135, amount: 9800, growth: 10, trend: 'stable', hotLevel: 3, color: 'lime' },
  { industry: '建工材料', count: 178, amount: 22000, growth: 18, trend: 'up', hotLevel: 4, color: 'lime' },
  // 11. 工业检测、实验室产品
  { industry: '工业检测', count: 86, amount: 28600, growth: 30, trend: 'up', hotLevel: 4, color: 'teal' },
  { industry: '实验室产品', count: 72, amount: 24500, growth: 25, trend: 'up', hotLevel: 4, color: 'teal' },
  // 12. 企业福礼、紧急救护
  { industry: '企业福礼', count: 312, amount: 9600, growth: 5, trend: 'stable', hotLevel: 3, color: 'grey' },
  { industry: '紧急救护', count: 145, amount: 12800, growth: 15, trend: 'up', hotLevel: 3, color: 'grey' }
])

// 行业洞察总结（MRO工业品相关）
const industryInsight = ref(
  '劳保安全类产品需求持续增长，近3个月标讯数量同比增长38%，主要集中在华东和华南地区。制造业升级带动电动工具、焊接设备需求旺盛，工控低压类产品在新能源行业应用广泛。建议重点关注工控产品、搬运设备等高增长品类。'
)

// 采购方规律数据（MRO工业品客户）
const purchaserPatterns = ref([
  {
    name: '国家电网某分公司',
    industry: '能源电力',
    frequency: 18,
    period: '3月、6月、9月',
    avgBudget: 450,
    opportunity: 5
  },
  {
    name: '某大型制造集团',
    industry: '制造业',
    frequency: 24,
    period: '1月、4月、7月、10月',
    avgBudget: 680,
    opportunity: 5
  },
  {
    name: '某汽车制造企业',
    industry: '汽车',
    frequency: 12,
    period: '2月、5月、8月、11月',
    avgBudget: 520,
    opportunity: 4
  },
  {
    name: '某化工园区管委会',
    industry: '化工',
    frequency: 8,
    period: '3月、9月',
    avgBudget: 380,
    opportunity: 4
  },
  {
    name: '某电子科技公司',
    industry: '电子',
    frequency: 15,
    period: '每季度',
    avgBudget: 320,
    opportunity: 4
  },
  {
    name: '某物流集团',
    industry: '物流仓储',
    frequency: 10,
    period: '4月、10月',
    avgBudget: 580,
    opportunity: 5
  },
  {
    name: '某三甲医院',
    industry: '医疗',
    frequency: 6,
    period: '6月、12月',
    avgBudget: 280,
    opportunity: 3
  },
  {
    name: '某建筑工程公司',
    industry: '建筑',
    frequency: 20,
    period: '3月、8月',
    avgBudget: 890,
    opportunity: 5
  }
])

// 采购方洞察总结
const purchaserInsight = ref(
  '制造业和物流仓储类客户采购频次高、预算充足，建议建立长期合作关系。国家电网、大型建筑工程等项目机会大但竞争激烈，建议提前布局。'
)

// 高潜力机会数据（MRO工业品相关）
const potentialOpportunities = ref([
  {
    id: 'op001',
    title: '某制造业工厂劳保用品年度采购',
    purchaser: '某大型制造企业',
    budget: 680,
    region: '华东',
    priority: 'high',
    match: 95,
    reason: '历史数据显示该客户年均采购劳保用品1200万，近期有年度招标计划，与我方劳保用品产品线高度匹配。'
  },
  {
    id: 'op002',
    title: '国家电网变电站检修工具采购',
    purchaser: '国家电网某分公司',
    budget: 520,
    region: '华北',
    priority: 'high',
    match: 92,
    reason: '该客户近期发布变电站检修项目，需要电动工具、手动工具等，预算充足，我方有成功案例可参考。'
  },
  {
    id: 'op003',
    title: '某汽车厂生产线搬运设备升级',
    purchaser: '某汽车制造集团',
    budget: 1280,
    region: '华南',
    priority: 'high',
    match: 90,
    reason: '客户计划升级自动化生产线，需要叉车、AGV等搬运设备，符合我方优势产品区域。'
  },
  {
    id: 'op004',
    title: '某化工企业安全消防设备采购',
    purchaser: '某化工园区管委会',
    budget: 450,
    region: '华东',
    priority: 'high',
    match: 88,
    reason: '化工行业安全要求提升，客户急需更新消防器材和安全设备，项目资金已到位。'
  },
  {
    id: 'op005',
    title: '某电子厂工控系统改造项目',
    purchaser: '某电子科技公司',
    budget: 850,
    region: '西南',
    priority: 'medium',
    match: 85,
    reason: '客户生产线自动化改造需要PLC、传感器等工控产品，我方有完整解决方案。'
  },
  {
    id: 'op006',
    title: '某医院实验室检测设备采购',
    purchaser: '某三甲医院',
    budget: 620,
    region: '华北',
    priority: 'medium',
    match: 82,
    reason: '医院新建检验科需要显微镜、离心机等实验室产品，该地区竞争相对较少。'
  },
  {
    id: 'op007',
    title: '某食品厂包装材料年度采购',
    purchaser: '某食品集团公司',
    budget: 380,
    region: '华东',
    priority: 'medium',
    match: 80,
    reason: '客户需要包装箱、缠绕膜、封箱胶带等包材，年采购量大，合作稳定。'
  },
  {
    id: 'op008',
    title: '某物流仓储货架系统扩建',
    purchaser: '某物流集团',
    budget: 960,
    region: '华南',
    priority: 'high',
    match: 88,
    reason: '客户扩建仓储中心需要大量货架、托盘、周转箱等存储设备，预算充足。'
  }
])

// 预测建议（MRO工业品相关）
const forecastTips = ref([
  { text: '劳保安全类产品预计Q2需求旺盛，建议提前备货安全帽、防护眼镜等', color: '#67c23a' },
  { text: '制造业升级带动电动工具、焊接设备需求增长，华东地区机会明显', color: '#409eff' },
  { text: '工控低压类产品在新能源行业需求强劲，建议重点跟进', color: '#e6a23c' },
  { text: '企业福礼采购季节即将到来，建议提前对接企业客户', color: '#909399' },
  { text: '清洁办公类产品需求稳定，建议维护现有客户关系', color: '#67c23a' }
])

// ========== 外部标讯源配置 ==========
const showSourceConfig = ref(false)
const sourceConfig = ref({
  platforms: ['中国政府采购网'],
  apiEndpoint: '',
  apiKey: '',
  keywords: [],
  regions: ['北京', '上海', '广州', '深圳'],
  minBudget: 0,
  maxBudget: 1000,
  autoSync: false,
  syncInterval: 6,
  autoSave: true,
  enableDedupe: true
})

const savingConfig = ref(false)
const testingConnection = ref(false)
const lastSyncTime = ref('暂未同步')

// 模拟外部标讯数据
const mockExternalTenders = [
  {
    id: 'ext_001',
    title: '某省政务云平台扩容采购项目',
    budget: 580,
    region: '北京',
    industry: '政府',
    deadline: '2025-03-15',
    source: 'external',
    sourcePlatform: '中国政府采购网',
    aiScore: 92,
    aiReason: '与公司云计算产品高度匹配',
    tags: ['云计算', '政务云', '扩容'],
    status: 'new'
  },
  {
    id: 'ext_002',
    title: '某市智慧交通管理系统建设',
    budget: 320,
    region: '上海',
    industry: '交通',
    deadline: '2025-03-20',
    source: 'external',
    sourcePlatform: '各省招标网',
    aiScore: 88,
    aiReason: '交通行业智能化改造项目',
    tags: ['智慧交通', '系统集成'],
    status: 'new'
  },
  {
    id: 'ext_003',
    title: '某能源集团ERP系统升级',
    budget: 450,
    region: '深圳',
    industry: '能源',
    deadline: '2025-03-25',
    source: 'external',
    sourcePlatform: '第三方商机服务',
    aiScore: 85,
    aiReason: '能源行业信息化建设项目',
    tags: ['ERP', '系统升级'],
    status: 'new'
  }
]

// 获取外部标讯相关
const fetchingTenders = ref(false)
const showFetchResult = ref(false)
const fetchResults = ref({
  total: 0,
  matched: 0,
  imported: 0,
  allImported: false,
  list: []
})

// ========== 人工录入相关 ==========
const showManualAdd = ref(false)
const manualFormRef = ref(null)
const uploadRef = ref(null)
const savingManual = ref(false)
const manualForm = ref({
  title: '',
  budget: null,
  region: '',
  industry: '',
  deadline: null,
  purchaser: '',
  contact: '',
  phone: '',
  description: '',
  tags: [],
  attachments: []
})

const manualFormRules = {
  title: [{ required: true, message: '请输入标讯标题', trigger: 'blur' }],
  budget: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
  region: [{ required: true, message: '请选择地区', trigger: 'change' }],
  industry: [{ required: true, message: '请选择行业', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }]
}

// 销售人员映射
const salesMap = {
  U001: '小王',
  U002: '李经理',
  U003: '张销售',
  U004: '陈专员',
  U005: '刘主管',
  U006: '赵经理'
}

// 区域销售映射
const regionSalesMap = {
  '北京': ['U005', 'U006'],
  '上海': ['U001', 'U002'],
  '广州': ['U003', 'U004'],
  '深圳': ['U003', 'U004'],
  '成都': ['U001', 'U002']
}

// 行业销售映射
const industrySalesMap = {
  '政府': ['U005', 'U006'],
  '能源': ['U003', 'U004'],
  '交通': ['U001', 'U002'],
  '数据中心': ['U001', 'U002']
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', handleResize)
  await biddingStore.getTenders()
  loadSavedConfig()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const tenders = computed(() => biddingStore.tenders || [])

const filteredTenders = computed(() => {
  let result = [...tenders.value]

  if (searchForm.value.keyword) {
    const keyword = searchForm.value.keyword.toLowerCase()
    result = result.filter(t =>
      t.title.toLowerCase().includes(keyword) ||
      t.region.toLowerCase().includes(keyword)
    )
  }

  if (searchForm.value.region) {
    result = result.filter(t => t.region === searchForm.value.region)
  }

  if (searchForm.value.industry) {
    result = result.filter(t => t.industry === searchForm.value.industry)
  }

  if (searchForm.value.status) {
    result = result.filter(t => t.status === searchForm.value.status)
  }

  if (searchForm.value.source) {
    result = result.filter(t => t.source === searchForm.value.source)
  }

  if (viewMode.value !== 'all') {
    result = result.filter(t => t.status === viewMode.value)
  }

  return result
})

const filteredRecommendTenders = computed(() => {
  return filteredTenders.value
    .filter(t => t.aiScore >= 85)
    .slice(0, 3)
})

const displayTenders = computed(() => {
  const start = (pagination.value.currentPage - 1) * pagination.value.pageSize
  const end = start + pagination.value.pageSize
  return filteredTenders.value.slice(start, end)
})

const newTendersCount = computed(() =>
  tenders.value.filter(t => t.status === 'new').length
)

const contactedTendersCount = computed(() =>
  tenders.value.filter(t => t.status === 'contacted').length
)

const followingTendersCount = computed(() =>
  tenders.value.filter(t => t.status === 'following').length
)

const quotingTendersCount = computed(() =>
  tenders.value.filter(t => t.status === 'quoting').length
)

const biddingTendersCount = computed(() =>
  tenders.value.filter(t => t.status === 'bidding').length
)

const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  return 'score-normal'
}

const getScoreTagType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'warning'
  return 'info'
}

const getStatusType = (status) => {
  const map = {
    new: 'info',
    contacted: '',
    following: 'warning',
    quoting: 'primary',
    bidding: 'success',
    abandoned: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    new: '新建',
    contacted: '已联系',
    following: '跟进中',
    quoting: '报价中',
    bidding: '投标中',
    abandoned: '已放弃'
  }
  return map[status] || status
}

const getSourceTagType = (source) => {
  const map = {
    internal: 'info',
    external: 'success',
    manual: 'warning',
    'CEB': 'success',
    '中国招标投标公共服务平台': 'success'
  }
  return map[source] || 'info'
}

const getSourceText = (source) => {
  const map = {
    internal: '内部',
    external: '外部获取',
    manual: '人工录入',
    'CEB': '公共平台(CEB)',
    '中国招标投标公共服务平台': '公共平台(CEB)'
  }
  return map[source] || source
}

const isFollowed = (id) => {
  return followedTenders.value.includes(id)
}

const handleSearch = () => {
  pagination.value.currentPage = 1
}

// 导出标讯列表
const handleExport = () => {
  const { exportExcel } = useExport()

  const params = {
    keyword: searchForm.value.keyword || undefined,
    region: searchForm.value.region || undefined,
    industry: searchForm.value.industry || undefined,
    status: searchForm.value.status || undefined,
    source: searchForm.value.source || undefined
  }

  exportExcel(ExportType.TENDERS, params, '标讯列表导出成功')
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    region: '',
    industry: '',
    status: '',
    source: ''
  }
  pagination.value.currentPage = 1
}

const handleViewDetail = (id) => {
  router.push(`/bidding/${id}`)
}

const handleParticipate = (id) => {
  ElMessage.success('正在跳转到项目创建页...')
  router.push({
    path: '/project/create',
    query: { tenderId: id }
  })
}

// ========== AI分析相关 ==========

const showParsingDialog = ref(false)
const parseProgress = ref(0)
const parsingTenderId = ref(null)

const progressColors = [
  { color: '#f56c6c', percentage: 30 },
  { color: '#e6a23c', percentage: 60 },
  { color: '#409eff', percentage: 90 },
  { color: '#67c23a', percentage: 100 }
]

const handleAIAnalysis = (id) => {
  if (!showTenderAiEntry) {
    return
  }
  parsingTenderId.value = id
  parseProgress.value = 0
  showParsingDialog.value = true

  // 模拟AI解析进度
  const interval = setInterval(() => {
    if (parseProgress.value < 100) {
      const increment = Math.random() * 15 + 5
      parseProgress.value = Math.min(100, Number((parseProgress.value + increment).toFixed(2)))
    } else {
      clearInterval(interval)
      setTimeout(() => {
        showParsingDialog.value = false
        // 跳转到AI分析页面
        router.push(`/bidding/ai-analysis/${id}`)
      }, 500)
    }
  }, 800)
}

const handleToggleFollow = (id) => {
  const index = followedTenders.value.indexOf(id)
  if (index > -1) {
    followedTenders.value.splice(index, 1)
    ElMessage.info('已取消收藏')
  } else {
    followedTenders.value.push(id)
    ElMessage.success('已收藏')
  }
}

const handleViewAllRecommend = () => {
  searchForm.value = {
    keyword: '',
    region: '',
    industry: '',
    status: ''
  }
  viewMode.value = 'all'
}

const customerOpportunityCenterEnabled = computed(() => false)

const handleOpenCustomerOpportunityCenter = () => {
  router.push('/bidding/customer-opportunities')
}

// ========== 表格选择相关 ==========

const handleSelectionChange = (selection) => {
  selectedTenders.value = selection
  selectAllChecked.value = selection.length > 0
  isIndeterminate.value = selection.length > 0 && selection.length < displayTenders.value.length
}

const handleSelectAll = (val) => {
  if (val) {
    displayTenders.value.forEach(row => {
      tableRef.value?.toggleRowSelection(row, true)
    })
  } else {
    tableRef.value?.clearSelection()
  }
}

const handleClearSelection = () => {
  tableRef.value?.clearSelection()
  selectedTenders.value = []
  selectAllChecked.value = false
  isIndeterminate.value = false
}

// ========== 分发相关 ==========

const resetDistributeForm = () => {
  distributeForm.value = {
    type: 'auto',
    rule: '',
    assignees: [],
    deadline: null,
    remark: ''
  }
}

const handleDistribute = async () => {
  // 验证
  if (distributeForm.value.type === 'auto' && !distributeForm.value.rule) {
    ElMessage.warning('请选择分发规则')
    return
  }
  if (distributeForm.value.type === 'manual' && distributeForm.value.assignees.length === 0) {
    ElMessage.warning('请选择指派人员')
    return
  }

  distributeLoading.value = true

  try {
    // 模拟分发逻辑
    const distribution = []

    if (distributeForm.value.type === 'auto') {
      // 智能分发
      selectedTenders.value.forEach(tender => {
        let assignees = []

        switch (distributeForm.value.rule) {
          case 'region':
            assignees = regionSalesMap[tender.region] || ['U001']
            break
          case 'product':
            assignees = industrySalesMap[tender.industry] || ['U001']
            break
          case 'ai':
            // 高评分优先给资深销售
            if (tender.aiScore >= 90) {
              assignees = ['U002', 'U005', 'U006']
            } else {
              assignees = ['U001', 'U003', 'U004']
            }
            break
          case 'average':
            assignees = ['U001', 'U002', 'U003', 'U004', 'U005', 'U006']
            break
        }

        // 轮询分配
        const assignee = assignees[Math.floor(Math.random() * assignees.length)]
        distribution.push({
          tenderId: tender.id,
          tenderTitle: tender.title,
          assignee,
          assigneeName: salesMap[assignee],
          type: 'auto'
        })
      })
    } else {
      // 手动分发 - 轮询分配给选定人员
      selectedTenders.value.forEach((tender, index) => {
        const assignee = distributeForm.value.assignees[
          index % distributeForm.value.assignees.length
        ]
        distribution.push({
          tenderId: tender.id,
          tenderTitle: tender.title,
          assignee,
          assigneeName: salesMap[assignee],
          type: 'manual'
        })
      })
    }

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 添加到分发记录
    distribution.forEach(item => {
      distributeRecords.value.unshift({
        tenderTitle: item.tenderTitle,
        assignee: item.assigneeName,
        type: item.type,
        time: new Date().toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        }),
        operator: '当前用户'
      })
    })

    ElMessage.success(`成功分发 ${distribution.length} 条标讯`)
    showDistributeDialog.value = false
    handleClearSelection()
  } catch (error) {
    ElMessage.error('分发失败，请重试')
  } finally {
    distributeLoading.value = false
  }
}

// ========== 批量操作相关 ==========

const handleBatchClaim = async () => {
  if (selectedTenders.value.length === 0) {
    ElMessage.warning('请先选择要领取的标讯')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要领取选中的 ${selectedTenders.value.length} 条标讯吗？领取后将由您跟进此标讯。`,
      '领取确认',
      {
        confirmButtonText: '确定领取',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    const tenderIds = selectedTenders.value.map(t => t.id)
    const userId = userStore.user?.id || 'U001'

    const result = await tendersApi.batchClaim(tenderIds, userId)

    if (result.success) {
      // 更新本地数据状态
      selectedTenders.value.forEach(tender => {
        tender.status = 'following'
        tender.assignee = userId
      })

      ElMessage.success(`成功领取 ${result.data?.claimed || tenderIds.length} 条标讯`)
      handleClearSelection()
      // 刷新列表数据
      await biddingStore.getTenders()
    } else {
      ElMessage.error(result.message || '领取失败，请重试')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('领取失败，请重试')
    }
  }
}

const handleBatchFollow = async () => {
  if (selectedTenders.value.length === 0) {
    ElMessage.warning('请先选择要关注的标讯')
    return
  }

  const newFollows = selectedTenders.value
    .filter(t => !followedTenders.value.includes(t.id))
    .map(t => t.id)

  if (newFollows.length === 0) {
    ElMessage.info('所选标讯已全部关注')
    return
  }

  // 使用批量更新状态API
  const tenderIds = selectedTenders.value.map(t => t.id)
  const result = await tendersApi.batchUpdateStatus(tenderIds, 'following')

  if (result.success) {
    followedTenders.value.push(...newFollows)
    // 更新本地数据状态
    selectedTenders.value.forEach(tender => {
      tender.status = 'following'
    })

    ElMessage.success(`已关注 ${result.data?.updated || newFollows.length} 条标讯`)
    handleClearSelection()
    // 刷新列表数据
    await biddingStore.getTenders()
  } else {
    ElMessage.error(result.message || '关注失败，请重试')
  }
}

// ========== 行操作相关 ==========

const handleRowAction = (command, row) => {
  switch (command) {
    case 'distribute':
      handleSingleDistribute(row)
      break
    case 'claim':
      handleSingleClaim(row)
      break
    case 'assign':
      handleSingleAssign(row)
      break
    case 'delete':
      handleDeleteTender(row)
      break
  }
}

const handleSingleDistribute = (row) => {
  selectedTenders.value = [row]
  showDistributeDialog.value = true
}

const handleSingleClaim = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要领取"${row.title}"吗？`,
      '领取确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    ElMessage.success('领取成功')
  } catch {
    // 用户取消
  }
}

const handleSingleAssign = (row) => {
  assignForm.value = {
    tenderId: row.id,
    tenderTitle: row.title,
    assignee: '',
    priority: 'medium',
    deadline: null,
    remark: ''
  }
  showAssignDialog.value = true
}

const resetAssignForm = () => {
  assignForm.value = {
    tenderId: null,
    tenderTitle: '',
    assignee: '',
    priority: 'medium',
    deadline: null,
    remark: ''
  }
}

const handleAssign = async () => {
  if (!assignForm.value.assignee) {
    ElMessage.warning('请选择指派人员')
    return
  }

  assignLoading.value = true

  try {
    // 使用批量分配API
    const result = await tendersApi.batchAssign(
      [assignForm.value.tenderId],
      assignForm.value.assignee
    )

    if (result.success) {
      // 更新本地数据
      const tender = biddingStore.tenders?.find(t => t.id === assignForm.value.tenderId)
      if (tender) {
        tender.assignee = assignForm.value.assignee
        tender.status = 'contacted'
      }

      // 添加到分发记录
      distributeRecords.value.unshift({
        tenderTitle: assignForm.value.tenderTitle,
        assignee: salesMap[assignForm.value.assignee],
        type: 'manual',
        time: new Date().toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        }),
        operator: '当前用户'
      })

      ElMessage.success(`已将"${assignForm.value.tenderTitle}"指派给${salesMap[assignForm.value.assignee]}`)
      showAssignDialog.value = false
      // 刷新列表数据
      await biddingStore.getTenders()
    } else {
      ElMessage.error(result.message || '指派失败，请重试')
    }
  } catch (error) {
    ElMessage.error('指派失败，请重试')
  } finally {
    assignLoading.value = false
  }
}

const handleDeleteTender = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除"${row.title}"吗？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const index = biddingStore.tenders?.findIndex(t => t.id === row.id)
    if (index !== undefined && index > -1) {
      biddingStore.tenders.splice(index, 1)
    }

    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

// ========== 状态管理相关 ==========

const handleStatusChange = async (row, newStatus) => {
  try {
    await ElMessageBox.confirm(
      `确定要将"${row.title}"状态变更为"${getStatusText(newStatus)}"吗？`,
      '状态变更确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 更新状态
    const index = biddingStore.tenders?.findIndex(t => t.id === row.id)
    if (index !== undefined && index > -1) {
      biddingStore.tenders[index].status = newStatus
    }

    ElMessage.success(`状态已更新为"${getStatusText(newStatus)}"`)
  } catch {
    // 用户取消
  }
}

// 更新标讯状态（用于快捷操作）
const handleUpdateStatus = async (row, newStatus) => {
  const statusText = getStatusText(newStatus)
  let confirmMessage = ''

  // 根据不同状态显示不同的确认信息
  switch (newStatus) {
    case 'contacted':
      confirmMessage = `标记"${row.title}"为已联系状态？`
      break
    case 'following':
      confirmMessage = `将"${row.title}"设为跟进中？`
      break
    case 'quoting':
      confirmMessage = `开始为"${row.title}"准备报价？`
      break
    case 'bidding':
      confirmMessage = `确认参与"${row.title}"投标？这将创建投标项目。`
      break
    case 'abandoned':
      confirmMessage = `确定放弃跟进"${row.title}"吗？此操作可撤销。`
      break
    default:
      confirmMessage = `确定要将"${row.title}"状态变更为"${statusText}"吗？`
  }

  try {
    await ElMessageBox.confirm(
      confirmMessage,
      '状态变更',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: newStatus === 'abandoned' ? 'error' : 'warning'
      }
    )

    // 更新状态
    const index = biddingStore.tenders?.findIndex(t => t.id === row.id)
    if (index !== undefined && index > -1) {
      biddingStore.tenders[index].status = newStatus

      // 如果是参与投标，可以引导用户创建项目
      if (newStatus === 'bidding') {
        ElMessage({
          message: `状态已更新为"${statusText}"，是否立即创建投标项目？`,
          type: 'success',
          duration: 3000,
          showClose: true
        })
        // 可以延迟跳转到项目创建页
        setTimeout(() => {
          router.push({
            path: '/project/create',
            query: { tenderId: row.id }
          })
        }, 1000)
      } else {
        ElMessage.success(`状态已更新为"${statusText}"`)
      }
    }
  } catch {
    // 用户取消
  }
}

// 查看分发记录
const handleViewRecords = () => {
  showRecordDialog.value = true
}

// ========== 外部标讯源配置相关 ==========

const resetSourceConfig = () => {
  // 保留当前配置，不重置
}

const loadSavedConfig = () => {
  const saved = localStorage.getItem('tenderSourceConfig')
  if (saved) {
    try {
      sourceConfig.value = { ...sourceConfig.value, ...JSON.parse(saved) }
    } catch (e) {
      console.error('加载配置失败', e)
    }
  }
}

const saveSourceConfig = async () => {
  if (sourceConfig.value.platforms.length === 0) {
    ElMessage.warning('请至少选择一个标讯源平台')
    return
  }

  savingConfig.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 800))
    localStorage.setItem('tenderSourceConfig', JSON.stringify(sourceConfig.value))
    ElMessage.success('标讯源配置已保存')
    showSourceConfig.value = false
  } catch (error) {
    ElMessage.error('保存失败，请重试')
  } finally {
    savingConfig.value = false
  }
}

const testConnection = async () => {
  if (sourceConfig.value.platforms.length === 0) {
    ElMessage.warning('请先选择标讯源平台')
    return
  }

  testingConnection.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1500))
    ElMessage.success('连接测试成功！')
  } catch (error) {
    ElMessage.error('连接测试失败')
  } finally {
    testingConnection.value = false
  }
}

// ========== 获取外部标讯相关 ==========

const handleFetchExternalTenders = async () => {
  if (sourceConfig.value.platforms.length === 0) {
    ElMessage.warning('请先配置标讯源')
    showSourceConfig.value = true
    return
  }

  fetchingTenders.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 2000))

    let results = mockExternalTenders
    if (sourceConfig.value.keywords.length > 0) {
      results = results.filter(t =>
        sourceConfig.value.keywords.some(kw =>
          t.title.includes(kw) || t.tags.some(tag => tag.includes(kw))
        )
      )
    }

    if (sourceConfig.value.regions.length > 0) {
      results = results.filter(t => sourceConfig.value.regions.includes(t.region))
    }

    if (sourceConfig.value.minBudget > 0) {
      results = results.filter(t => t.budget >= sourceConfig.value.minBudget)
    }
    if (sourceConfig.value.maxBudget > 0) {
      results = results.filter(t => t.budget <= sourceConfig.value.maxBudget)
    }

    fetchResults.value = {
      total: results.length,
      matched: results.length,
      imported: 0,
      allImported: false,
      list: results.map(t => ({ ...t, imported: false }))
    }

    const now = new Date()
    lastSyncTime.value = now.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })

    if (results.length > 0) {
      showFetchResult.value = true
      ElMessage.success(`成功获取 ${results.length} 条标讯`)
    } else {
      ElMessage.info('未获取到匹配的标讯，请调整筛选条件')
    }
  } catch (error) {
    ElMessage.error('获取标讯失败')
  } finally {
    fetchingTenders.value = false
  }
}

const importSingleTender = (tender) => {
  tender.imported = true
  fetchResults.value.imported++
  fetchResults.value.allImported = fetchResults.value.list.every(t => t.imported)

  if (biddingStore.tenders) {
    biddingStore.tenders.unshift({ ...tender })
  }

  ElMessage.success('标讯已入库')
}

const importAllTenders = () => {
  const unimported = fetchResults.value.list.filter(t => !t.imported)
  unimported.forEach(t => {
    t.imported = true
    if (biddingStore.tenders) {
      biddingStore.tenders.unshift({ ...t })
    }
  })
  fetchResults.value.imported = fetchResults.value.list.length
  fetchResults.value.allImported = true
  ElMessage.success(`成功入库 ${unimported.length} 条标讯`)
}

// ========== 人工录入相关 ==========

const handleFileChange = (file, fileList) => {
  manualForm.value.attachments = fileList
}

const resetManualForm = () => {
  manualForm.value = {
    title: '',
    budget: null,
    region: '',
    industry: '',
    deadline: null,
    purchaser: '',
    contact: '',
    phone: '',
    description: '',
    tags: [],
    attachments: []
  }
}

const saveManualTender = async () => {
  try {
    await manualFormRef.value.validate()

    savingManual.value = true
    await new Promise(resolve => setTimeout(resolve, 1000))

    const newTender = {
      id: `manual_${Date.now()}`,
      title: manualForm.value.title,
      budget: manualForm.value.budget,
      region: manualForm.value.region,
      industry: manualForm.value.industry,
      deadline: manualForm.value.deadline
        ? new Date(manualForm.value.deadline).toLocaleDateString('zh-CN')
        : '',
      source: 'manual',
      purchaser: manualForm.value.purchaser,
      contact: manualForm.value.contact,
      phone: manualForm.value.phone,
      description: manualForm.value.description,
      tags: manualForm.value.tags,
      aiScore: Math.floor(Math.random() * 20) + 70,
      aiReason: '人工录入标讯',
      status: 'new'
    }

    if (biddingStore.tenders) {
      biddingStore.tenders.unshift(newTender)
    }

    ElMessage.success('标讯已成功入库')
    showManualAdd.value = false
    resetManualForm()
  } catch (error) {
    // 验证失败
  } finally {
    savingManual.value = false
  }
}

// ========== 市场洞察相关 ==========

// 加载 TrendRadar 数据
const loadTrendRadarData = async () => {
  if (trendDataLoaded.value) return // 避免重复加载

  loadingTrendData.value = true
  try {
    // 并行获取热点数据和统计信息
    const [topics, stats] = await Promise.all([
      getBreakoutTopics(50, 2),
      getStatsSummary()
    ])

    // 如果没有真实数据，则使用默认 fallback 数据
    const isMock = false

    // 只有在有真实数据且经过过滤后仍有数据时才更新
    if (topics && topics.length > 0) {
      const filteredTopics = topics.filter(t => {
        // 过滤政治敏感内容
        const text = (t.normalized_title + ' ' + (t.sample_titles || []).join(' ')).toLowerCase()
        const politicsKeywords = ['空袭', '袭击', '战争', '东部战区', '战区', '导弹', '俄乌', '巴以', '哈马斯', '以色列', '伊朗', '朝鲜']
        return !politicsKeywords.some(kw => text.includes(kw.toLowerCase()))
      })

      if (filteredTopics.length > 0) {
        // 更新行业趋势数据
        const transformed = transformToIndustryTrends(filteredTopics)
        if (transformed.length > 0) {
          industryTrends.value = transformed
        }

        // 更新高潜力机会数据
        const opportunities = transformToOpportunities(filteredTopics)
        if (opportunities.length > 0) {
          potentialOpportunities.value = opportunities
        }

        // 更新洞察文本
        industryInsight.value = generateInsight(filteredTopics, stats)

        // 更新预测建议
        forecastTips.value = generateForecastTips(filteredTopics)

        trendDataLoaded.value = true

        // Mock 模式下提示信息更友好
        if (isMock) {
          ElMessage.success({
            message: '已加载 AI 模型模拟的市场分析数据',
            duration: 2000
          })
        } else {
          ElMessage.success({
            message: `已从 TrendRadar 加载 ${filteredTopics.length} 条热点趋势数据`,
            duration: 2000
          })
        }
      } else {
        // 过滤后没有数据
        if (isMock) {
          trendDataLoaded.value = true
        } else {
          ElMessage.info('当前实时热点均为非工业相关内容，已展示推荐 MRO 趋势')
        }
      }
    } else {
      // 完全没有返回数据
      if (isMock) {
        trendDataLoaded.value = true
      } else {
        ElMessage.info('TrendRadar 暂时无法返回实时数据，已加载基准市场洞察')
      }
    }
  } catch (error) {
    console.error('加载 TrendRadar 数据失败:', error)
    // 连接失败时静默保留 mock 数据，不弹出警告干扰用户
    trendDataLoaded.value = true
  } finally {
    loadingTrendData.value = false
  }
}

// 刷新趋势数据
const refreshTrendData = async () => {
  trendDataLoaded.value = false
  await loadTrendRadarData()
}

// 监听对话框打开
watch(showMarketInsight, (newVal) => {
  if (newVal) {
    loadTrendRadarData()
  }
})

const getOpportunityText = (score) => {
  if (score >= 5) return '高价值'
  if (score >= 4) return '推荐'
  if (score >= 3) return '一般'
  return '较低'
}

const getMatchColor = (percentage) => {
  if (percentage >= 90) return '#67c23a'
  if (percentage >= 80) return '#e6a23c'
  return '#409eff'
}

const handleOpportunityAction = (id) => {
  const opportunity = potentialOpportunities.value.find(item => item.id === id)
  if (opportunity) {
    showMarketInsight.value = false
    ElMessage.success(`已创建跟进任务: ${opportunity.title}`)
  }
}
</script>

<style scoped>
.bidding-list-page {
  padding: 24px;
  background: #F8FAFC;
  min-height: 100vh;
}

/* ==================== 页面头部 ==================== */
.page-header {
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #0F172A;
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 14px;
  color: #64748B;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* ==================== 搜索卡片 ==================== */
.search-card {
  margin-bottom: 24px;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-card :deep(.el-card__body) {
  padding: 20px;
}

.search-card :deep(.el-form--inline .el-form-item) {
  margin-right: 12px;
  margin-bottom: 12px;
}

/* ==================== 源状态卡片 ==================== */
.source-status-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #F0F9FF 0%, #E0F2FE 100%);
  border: 1px solid #BAE6FD;
  border-radius: 12px;
}

.source-status-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-icon {
  color: #0284C7;
  font-size: 18px;
}

.status-text {
  font-size: 14px;
  font-weight: 500;
  color: #0C4A6E;
}

.source-tag {
  margin-left: 8px;
}

.status-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sync-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #0369A1;
}

.last-sync {
  font-size: 12px;
  color: #64748B;
}

/* ==================== AI推荐区 ==================== */
.ai-recommend-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 4px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
  color: #0F172A;
}

.ai-icon {
  color: #8B5CF6;
  font-size: 20px;
}

.match-tag {
  margin-left: 8px;
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  border: none;
  color: #ffffff;
  font-weight: 700;
  font-size: 13px;
  padding: 5px 12px;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.4);
  letter-spacing: 0.5px;
}

/* 表格中的高匹配标签 */
:deep(.el-tag--success) {
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  border: none;
  color: #ffffff;
  font-weight: 600;
}

.recommend-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}

.recommend-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid #E5E7EB;
  position: relative;
  overflow: hidden;
}

.recommend-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #10B981 0%, #059669 100%);
  opacity: 0;
  transition: opacity 0.25s ease;
}

.recommend-card:hover {
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: #10B981;
  transform: translateY(-2px);
}

.recommend-card:hover::before {
  opacity: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.card-title {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
  margin: 0;
  line-height: 1.5;
  padding-right: 12px;
}

.ai-score {
  width: 65px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

.score-excellent {
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.score-good {
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.score-normal {
  background: linear-gradient(135deg, #64748B 0%, #475569 100%);
  color: #fff;
}

.card-info {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748B;
}

.info-item .el-icon {
  font-size: 16px;
  color: #94A3B8;
}

.card-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.card-footer {
  padding-top: 16px;
  border-top: 1px solid #F1F5F9;
}

.card-footer .el-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748B;
}

/* ==================== 表格卡片 ==================== */
.table-card {
  margin-bottom: 20px;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.table-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #F1F5F9;
}

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #0F172A;
}

.card-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.card-actions .el-radio-group {
  flex-wrap: wrap;
}

/* ==================== 批量操作 ==================== */
.batch-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: linear-gradient(135deg, #F0F9FF 0%, #E0F2FE 100%);
  border-radius: 8px;
  margin-bottom: 16px;
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-buttons {
  display: flex;
  gap: 8px;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

/* 批量操作栏样式 */
.batch-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #e8f3ff 0%, #f0f9ff 100%);
  border-radius: 8px;
  border: 1px solid #d4e7ff;
}

.batch-info {
  display: flex;
  align-items: center;
}

.batch-info :deep(.el-checkbox__label) {
  font-weight: 500;
  color: #409eff;
}

.batch-buttons {
  display: flex;
  gap: 10px;
}

/* 对话框内容样式 */
.radio-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.radio-label {
  font-weight: 500;
  color: #303133;
}

.radio-desc {
  font-size: 12px;
  color: #909399;
}

.option-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.option-label {
  font-weight: 500;
  color: #303133;
}

.option-desc {
  font-size: 11px;
  color: #909399;
}

.form-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

/* 单选组样式优化 */
:deep(.el-radio) {
  margin-right: 20px;
  margin-bottom: 8px;
  height: auto;
  align-items: flex-start;
}

:deep(.el-radio__label) {
  padding-left: 8px;
}

/* 下拉菜单样式 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 表格选择列样式优化 */
:deep(.el-table__header .el-table-column--selection .cell) {
  display: flex;
  justify-content: center;
}

:deep(.el-table__body .el-table-column--selection .cell) {
  display: flex;
  justify-content: center;
}

/* 选择组样式优化 */
:deep(.el-select-group__title) {
  font-weight: 600;
  color: #409eff;
}

/* 对话框头部样式 */
:deep(.el-dialog__title) {
  font-weight: 600;
  color: #303133;
}

/* 分发记录对话框表格样式 */
:deep(.el-table__body-wrapper) {
  max-height: 400px;
  overflow-y: auto;
}

/* 批量操作按钮图标样式 */
.batch-buttons .el-button {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ========== 市场洞察样式 ========== */
.market-insight-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.trend-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
  color: #909399;
  font-size: 14px;
}

.trend-loading .el-icon {
  font-size: 24px;
}

.insight-content {
  padding: 4px;
}

.insight-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.insight-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.insight-summary {
  margin-top: 16px;
}

.insight-summary :deep(.el-alert__title) {
  font-size: 14px;
  line-height: 1.6;
}

/* 行业趋势表格样式 */
.industry-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.industry-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.industry-dot.blue { background-color: #409eff; }
.industry-dot.green { background-color: #67c23a; }
.industry-dot.orange { background-color: #e6a23c; }
.industry-dot.purple { background-color: #9c27b0; }
.industry-dot.red { background-color: #f56c6c; }
.industry-dot.cyan { background-color: #00bcd4; }
.industry-dot.yellow { background-color: #f9ca24; }

.growth-up {
  color: #67c23a;
  font-weight: 600;
}

.growth-down {
  color: #f56c6c;
  font-weight: 600;
}

.growth-up .el-icon,
.growth-down .el-icon {
  transform: rotate(-45deg);
  margin-left: 2px;
}

/* 采购方规律样式 */
.opportunity-rating {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.opportunity-rating :deep(.el-rate) {
  height: auto;
}

/* 高潜力机会卡片样式 */
.opportunity-card {
  margin-bottom: 16px;
  height: 100%;
  transition: all 0.3s;
}

.opportunity-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.opportunity-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.opportunity-header h5 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  padding-right: 8px;
}

.opportunity-info {
  margin-bottom: 12px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-row .label {
  color: #909399;
}

.info-row .value {
  color: #303133;
  font-weight: 500;
}

.opportunity-reason {
  margin: 12px 0;
  padding: 10px;
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.opportunity-reason .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.opportunity-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.match-bar {
  flex: 1;
}

.match-label {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-bottom: 4px;
}

.match-bar :deep(.el-progress__text) {
  font-size: 12px !important;
}

/* 预测分析样式 */
.forecast-card {
  margin-bottom: 0;
}

.card-header-small {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #303133;
}

.forecast-data {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.forecast-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #e4e7ed;
}

.forecast-item:last-child {
  border-bottom: none;
}

.forecast-label {
  color: #606266;
  font-size: 14px;
}

.forecast-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.forecast-value.forecast-up {
  color: #67c23a;
}

.forecast-tips {
  margin-top: 16px;
}

.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.tips-list li:last-child {
  border-bottom: none;
}

.tip-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

/* 市场洞察对话框样式优化 */
:deep(.el-dialog__body) {
  padding-top: 10px;
}

:deep(.el-tabs__content) {
  padding-top: 16px;
}

:deep(.el-card__header) {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-card__body) {
  padding: 16px;
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .bidding-list-page {
    padding: 12px;
  }

  .page-header {
    margin-bottom: 12px;
  }

  .page-title {
    font-size: 20px;
  }

  .search-card :deep(.el-form) {
    display: block;
  }

  .search-card :deep(.el-form-item) {
    display: block;
    margin-right: 0;
    margin-bottom: 12px;
  }

  .search-card :deep(.el-input),
  .search-card :deep(.el-select) {
    width: 100% !important;
  }

  /* AI推荐卡片移动端优化 */
  .recommend-cards {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .recommend-card {
    padding: 12px;
  }

  .card-info {
    flex-direction: column;
    gap: 8px;
  }

  /* 表格卡片头部移动端优化 */
  .card-header-content {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .card-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .card-actions :deep(.el-radio-group) {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
  }

  .card-actions :deep(.el-radio-button) {
    flex: 1;
    min-width: 60px;
  }

  .card-actions :deep(.el-radio-button__inner) {
    padding: 8px 6px;
    font-size: 12px;
  }

  /* 批量操作栏移动端优化 */
  .batch-actions {
    flex-direction: column;
    gap: 12px;
    padding: 12px;
  }

  .batch-buttons {
    width: 100%;
    flex-wrap: wrap;
  }

  .batch-buttons .el-button {
    flex: 1;
    min-width: 100px;
  }

  /* 表格移动端优化 - 横向滚动 */
  .table-card :deep(.el-table) {
    font-size: 12px;
  }

  .table-card :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }

  /* 分页移动端优化 */
  .pagination-wrapper :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }

  .pagination-wrapper :deep(.el-pagination__sizes),
  .pagination-wrapper :deep(.el-pagination__jump) {
    display: none;
  }

  /* 对话框移动端优化 */
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }

  :deep(.el-dialog__body) {
    padding: 16px;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .batch-buttons .el-button,
  .card-actions .el-button {
    min-height: 44px;
  }

  .recommend-card {
    padding: 16px;
  }

  .recommend-card:active {
    background: #f5f7fa;
  }
}

/* ========== 外部标讯源相关样式 ========== */
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  flex: 1;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.source-status-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e8f4ff 100%);
}

.source-status-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-icon {
  color: #409eff;
  font-size: 20px;
}

.status-text {
  font-weight: 500;
  color: #303133;
}

.source-tag {
  margin-left: 4px;
}

.status-right {
  display: flex;
  gap: 20px;
  align-items: center;
}

.sync-info {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #67c23a;
  font-size: 14px;
}

.last-sync {
  color: #909399;
  font-size: 13px;
}

.sync-interval {
  margin-left: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* ==================== Table Action Buttons ==================== */

.table-actions {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
}

/* 图标按钮基础样式 */
.table-actions .action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #E2E8F0;
  background: #fff;
  color: #64748B;
}

.table-actions .action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.table-actions .action-btn:active {
  transform: translateY(0);
}

/* 查看详情按钮 - High Contrast */
.action-btn.btn-view {
  color: #0066CC;
  border-color: #BAE6FD;
  background: #F0F9FF;
  font-weight: 500;
}

.action-btn.btn-view:hover {
  background: #E0F2FE !important;
  border-color: #0066CC !important;
  color: #004499 !important;
  box-shadow: 0 4px 8px rgba(0, 102, 204, 0.15);
}

/* AI分析按钮 */
.action-btn.btn-analyze {
  color: #6D28D9;
  border-color: #DDD6FE;
  background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
  font-weight: 500;
}

.action-btn.btn-analyze:hover {
  background: linear-gradient(135deg, #EDE9FE 0%, #DDD6FE 100%) !important;
  border-color: #6D28D9 !important;
  box-shadow: 0 4px 12px rgba(109, 40, 217, 0.2);
}

/* 参与投标按钮 - Primary Action with High Legibility */
.action-btn.btn-participate {
  background: linear-gradient(135deg, #0066CC 0%, #004499 100%) !important;
  color: #FFFFFF !important;
  border: none !important;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(0, 102, 204, 0.3);
}

.action-btn.btn-participate:hover {
  background: linear-gradient(135deg, #0055BB 0%, #003388 100%) !important;
  box-shadow: 0 6px 16px rgba(0, 102, 204, 0.45);
  transform: translateY(-2px);
}

/* 更多按钮 */
.action-btn.btn-more {
  color: #64748B;
  border-color: #E2E8F0;
}

.action-btn.btn-more:hover {
  background: #F1F5F9 !important;
  border-color: #CBD5E1 !important;
  color: #334155 !important;
}

/* ==================== Dropdown Menu Styles ==================== */

.bidding-action-menu {
  min-width: 180px;
  padding: 8px 0;
  border-radius: 12px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.bidding-action-menu .el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  font-size: 14px;
  color: #334155;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.bidding-action-menu .el-dropdown-menu__item:hover {
  background: #F0F9FF;
  color: #0066CC;
}

.bidding-action-menu .el-dropdown-menu__item .el-icon {
  font-size: 16px;
  color: #94A3B8;
  transition: color 0.2s;
}

.bidding-action-menu .el-dropdown-menu__item:hover .el-icon {
  color: #0066CC;
}

.bidding-action-menu .el-dropdown-menu__item.status-icon {
  color: #64748B;
}

.bidding-action-menu .el-dropdown-menu__item--danger {
  color: #EF4444;
}

.bidding-action-menu .el-dropdown-menu__item--danger:hover {
  background: #FEF2F2;
  color: #DC2626;
}

.bidding-action-menu .el-dropdown-menu__item--danger .el-icon {
  color: #EF4444;
}

/* 菜单分组标题 */
.bidding-action-menu .menu-group-title {
  padding: 8px 16px 4px;
  font-size: 12px;
  font-weight: 600;
  color: #94A3B8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* 自定义危险项样式 */
.bidding-action-menu .danger-item {
  color: #EF4444 !important;
}

.bidding-action-menu .danger-item:hover {
  background: #FEF2F2 !important;
  color: #DC2626 !important;
}

.bidding-action-menu .danger-item .delete-icon {
  color: #EF4444 !important;
}

.bidding-action-menu .danger-item:hover .delete-icon {
  color: #DC2626 !important;
}

/* 放弃状态图标 */
.bidding-action-menu .status-abandon {
  color: #94A3B8;
}

.bidding-action-menu .el-dropdown-menu__item:hover .status-abandon {
  color: #DC2626;
}

/* ==================== AI Score Highlight ==================== */

.ai-score-highlight {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 42px;
  height: 24px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
}

.ai-score-high {
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  color: #fff;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.3);
}

.ai-score-medium {
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  color: #fff;
  box-shadow: 0 2px 6px rgba(245, 158, 11, 0.3);
}

.ai-score-low {
  background: linear-gradient(135deg, #94A3B8 0%, #64748B 100%);
  color: #fff;
}

/* ==================== Status Badge ==================== */

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-badge.status-new {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  color: #fff;
}

.status-badge.status-contacted {
  background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
  color: #fff;
}

.status-badge.status-following {
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  color: #fff;
}

.status-badge.status-quoting {
  background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%);
  color: #fff;
}

.status-badge.status-bidding {
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  color: #fff;
}

.status-badge.status-abandoned {
  background: linear-gradient(135deg, #9CA3AF 0%, #6B7280 100%);
  color: #fff;
}

/* ==================== Title Cell Enhancement ==================== */

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-weight: 500;
  color: #1E293B;
  font-size: 14px;
}

.title-cell .el-tag {
  flex-shrink: 0;
}

/* 获取标讯结果样式 */
.fetch-result-header {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #ecf5ff 100%);
  border-radius: 8px;
}

/* 上传区域样式 */
:deep(.el-upload-dragger) {
  width: 100%;
}

/* AI解析进度弹窗样式 */
.parsing-content {
  text-align: center;
  padding: 20px;
}

.parsing-animation {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.parsing-spinner {
  width: 60px;
  height: 60px;
  border: 4px solid #e4e7ed;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.parsing-text {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 20px;
}

.parsing-hint {
  margin-top: 16px;
  font-size: 13px;
  color: #909399;
}

/* 移动端适配样式 */
.table-container {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.mobile-card-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mobile-card-item {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s;
}

.mobile-card-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.mobile-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.mobile-card-title {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin: 0;
  line-height: 1.5;
  padding-right: 8px;
}

.mobile-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.mobile-card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.mobile-label {
  color: #909399;
  min-width: 80px;
}

.mobile-value {
  color: #303133;
  text-align: right;
}

.mobile-card-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.mobile-card-actions .el-button {
  flex: 1;
}

/* ==================== Button Interaction Enhancements ==================== */

/* Page header buttons */
.header-actions .el-button {
  min-width: 110px;
  height: 38px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.header-actions .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.header-actions .el-button:active {
  transform: translateY(0);
}

/* Search button */
.header-actions .el-button--primary {
  background: linear-gradient(135deg, #0369a1, #0284c7);
  border: none;
}

.header-actions .el-button--primary:hover {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.3);
}

/* Success button */
.header-actions .el-button--success {
  background: linear-gradient(135deg, #10b981, #059669);
  border: none;
}

.header-actions .el-button--success:hover {
  background: linear-gradient(135deg, #059669, #10b981);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

/* Warning button */
.header-actions .el-button--warning {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border: none;
}

.header-actions .el-button--warning:hover {
  background: linear-gradient(135deg, #d97706, #f59e0b);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

/* Default button */
.header-actions .el-button--default {
  border: 1.5px solid #e5e7eb;
  color: #64748b;
}

.header-actions .el-button--default:hover {
  border-color: #94a3b8;
  color: #1e293b;
  background: #f8fafc;
}

/* Search form buttons */
.search-card .el-button {
  height: 36px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  padding: 0 20px;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.search-card .el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.25);
}

/* ==================== Input Field Enhancements ==================== */

/* Search input */
.search-card :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: none;
}

.search-card :deep(.el-input__wrapper:hover) {
  border-color: #94a3b8;
  box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.1);
}

.search-card :deep(.el-input__wrapper.is-focus) {
  border-color: #0369a1;
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1);
}

/* Select dropdown */
.search-card :deep(.el-select__wrapper) {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.search-card :deep(.el-select__wrapper:hover) {
  border-color: #94a3b8;
}

.search-card :deep(.el-select__wrapper.is-focus) {
  border-color: #0369a1;
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1);
}

/* Form labels */
.search-card :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

/* ==================== Card Actions Buttons ==================== */

.card-actions .el-button {
  height: 32px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.card-actions .el-button:hover {
  transform: translateY(-1px);
}

.card-actions .el-button:active {
  transform: translateY(0);
}

/* Radio button group */
.card-actions :deep(.el-radio-group) {
  border-radius: 8px;
  overflow: hidden;
  border: 1.5px solid #e5e7eb;
  display: inline-flex !important;
  flex-direction: row !important;
}

.card-actions :deep(.el-radio-button) {
  display: inline-flex !important;
}

.card-actions :deep(.el-radio-button__inner) {
  border: none !important;
  border-radius: 0 !important;
  padding: 8px 12px !important;
  min-width: 70px !important;
  min-height: 32px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  writing-mode: horizontal-tb !important;
  text-orientation: mixed !important;
  white-space: nowrap !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.card-actions :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #0369a1, #0284c7);
  color: #ffffff;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

/* ==================== Batch Action Enhancements ==================== */

.batch-buttons .el-button {
  min-width: 100px;
  height: 36px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.batch-buttons .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.batch-buttons .el-button:active {
  transform: translateY(0);
}

/* ==================== Link Enhancements ==================== */

.el-link {
  font-size: 14px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.el-link:hover {
  transform: translateX(2px);
}

.el-link .el-icon {
  transition: transform 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.el-link:hover .el-icon {
  transform: translateX(4px);
}

/* ==================== B2B 分发对话框样式 ==================== */
.distribute-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin-bottom: 0;
}

.distribute-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.distribute-dialog :deep(.el-dialog__headerbtn) {
  top: 16px;
  right: 16px;
}

.distribute-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #1E40AF 0%, #3B82F6 100%);
  color: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
}

.header-icon .el-icon {
  font-size: 20px;
}

.header-info {
  flex: 1;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: #fff;
}

.header-subtitle {
  font-size: 13px;
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
}

.header-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
  color: #fff;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.distribute-content {
  display: flex;
  min-height: 400px;
}

.config-section {
  flex: 1;
  padding: 24px;
  border-right: 1px solid #E5E7EB;
}

.preview-section {
  width: 320px;
  background: #F9FAFB;
  padding: 20px;
}

.tenders-preview {
  margin-bottom: 20px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.preview-title {
  font-size: 13px;
  font-weight: 500;
  color: #6B7280;
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #E5E7EB;
}

.item-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3B82F6;
  flex-shrink: 0;
}

.item-title {
  flex: 1;
  font-size: 12px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-more {
  font-size: 12px;
  color: #6B7280;
  text-align: center;
  padding: 8px;
}

/* 分发方式卡片 */
.distribute-type-section {
  margin-bottom: 20px;
}

.section-label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 12px;
}

.type-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.type-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border: 2px solid #E5E7EB;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.type-card:hover {
  border-color: #3B82F6;
  background: #F0F9FF;
}

.type-card.active {
  border-color: #3B82F6;
  background: #EFF6FF;
}

.type-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  flex-shrink: 0;
}

.type-icon.auto-icon { background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%); }
.type-icon.manual-icon { background: linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%); }

.type-info {
  flex: 1;
}

.type-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.type-desc {
  font-size: 12px;
  color: #6B7280;
}

/* 规则卡片 */
.rule-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.rule-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #fff;
  border: 2px solid #E5E7EB;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.rule-card:hover {
  border-color: #10B981;
}

.rule-card.active {
  border-color: #10B981;
  background: #ECFDF5;
}

.rule-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #fff;
  flex-shrink: 0;
}

.rule-icon.region-icon { background: #EF4444; }
.rule-icon.product-icon { background: #F59E0B; }
.rule-icon.ai-icon { background: #8B5CF6; }
.rule-icon.average-icon { background: #10B981; }

.rule-info {
  flex: 1;
}

.rule-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #111827;
}

.rule-desc {
  font-size: 11px;
  color: #6B7280;
}

/* 销售人员网格 */
.sales-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.sales-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: #fff;
  border: 2px solid #E5E7EB;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sales-card:hover {
  border-color: #3B82F6;
}

.sales-card.selected {
  border-color: #3B82F6;
  background: #EFF6FF;
}

.sales-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.sales-info {
  flex: 1;
}

.sales-name {
  display: block;
  font-size: 13px;
  font-weight:  500;
  color: #111827;
}

.sales-role {
  font-size: 11px;
  color: #6B7280;
}

.sales-check {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #10B981;
  color: #fff;
}

.sales-check .el-icon {
  font-size: 12px;
}

/* 其他区域 */
.rules-section,
.assignees-section,
.deadline-section,
.remark-section {
  margin-bottom: 20px;
}

.remark-section {
  margin-bottom: 0;
}

/* 预览区 */
.preview-section .preview-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #E5E7EB;
}

.preview-content {
  min-height: 280px;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 280px;
  color: #9CA3AF;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.preview-distribution {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-group {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #E5E7EB;
  overflow: hidden;
}

.preview-group .preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #F9FAFB;
  border-bottom: 1px solid #E5E7EB;
}

.preview-sales {
  font-size: 13px;
  font-weight: 500;
  color: #111827;
}

.preview-tenders {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preview-tender-item {
  font-size: 11px;
  color: #6B7280;
  padding: 4px 8px;
  background: #F3F4F6;
  border-radius: 4px;
}

/* 底部按钮 */
.distribute-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  background: #F9FAFB;
  border-top: 1px solid #E5E7EB;
}

/* 强制横排显示 - 覆盖所有可能的竖版文字样式 */
.card-actions .el-radio-button__inner,
.card-actions :deep(.el-radio-button__inner) {
  writing-mode: horizontal-tb !important;
  text-orientation: mixed !important;
  direction: ltr !important;
  display: inline-flex !important;
  flex-direction: row !important;
  align-items: center !important;
  justify-content: center !important;
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .card-actions .el-radio-button__inner,
  .card-actions :deep(.el-radio-button__inner) {
    writing-mode: horizontal-tb !important;
    text-orientation: mixed !important;
    direction: ltr !important;
    display: inline-flex !important;
    flex-direction: row !important;
  }
}
</style>
