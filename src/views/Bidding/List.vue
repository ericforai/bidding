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
            <el-option label="跟进中" value="following" />
            <el-option label="投标中" value="bidding" />
          </el-select>
        </el-form-item>
        <el-form-item label="标讯来源">
          <el-select v-model="searchForm.source" placeholder="全部来源" clearable style="width: 130px">
            <el-option label="内部" value="internal" />
            <el-option label="外部获取" value="external" />
            <el-option label="人工录入" value="manual" />
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
        <el-link type="primary" :underline="false" @click="handleViewAllRecommend">
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
              {{ tender.aiScore }}
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
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button value="all">全部 ({{ filteredTenders.length }})</el-radio-button>
              <el-radio-button value="new">新建 ({{ newTendersCount }})</el-radio-button>
              <el-radio-button value="following">跟进中 ({{ followingTendersCount }})</el-radio-button>
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
            <template #default="{ row }">
              <div class="title-cell">
                <span class="title-text">{{ row.title }}</span>
                <el-tag v-if="row.aiScore >= 90" size="small" type="success">高匹配</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="budget" label="预算" width="100" align="center">
            <template #default="{ row }">
              <span>{{ row.budget }}万元</span>
            </template>
          </el-table-column>
          <el-table-column prop="region" label="地区" width="100" align="center" />
          <el-table-column prop="industry" label="行业" width="100" align="center" />
          <el-table-column prop="source" label="来源" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.source" :type="getSourceTagType(row.source)" size="small">
                {{ getSourceText(row.source) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="aiScore" label="AI评分" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getScoreTagType(row.aiScore)" size="small">
                {{ row.aiScore }}分
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deadline" label="截止日期" width="120" align="center" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleViewDetail(row.id)">
                查看详情
              </el-button>
              <el-button link type="success" size="small" @click="handleAIAnalysis(row.id)">
                AI分析
              </el-button>
              <el-button link type="primary" size="small" @click="handleParticipate(row.id)">
                参与投标
              </el-button>
              <el-button
                link
                :type="isFollowed(row.id) ? 'warning' : 'default'"
                size="small"
                @click="handleToggleFollow(row.id)"
              >
                <el-icon>
                  <Star v-if="isFollowed(row.id)" />
                  <StarFilled v-else />
                </el-icon>
              </el-button>
              <el-dropdown trigger="click" @command="(cmd) => handleRowAction(cmd, row)">
                <el-button link type="primary" size="small">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="distribute">
                      <el-icon><Share /></el-icon>分发
                    </el-dropdown-item>
                    <el-dropdown-item command="claim">
                      <el-icon><CircleCheck /></el-icon>领取
                    </el-dropdown-item>
                    <el-dropdown-item command="assign">
                      <el-icon><User /></el-icon>指派
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
                {{ row.aiScore }}分
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
            <el-button type="success" size="small" @click="handleAIAnalysis(row.id)">
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

    <!-- 分发对话框 -->
    <el-dialog v-model="showDistributeDialog" title="标讯分发" width="580px" @close="resetDistributeForm">
      <el-form ref="distributeFormRef" :model="distributeForm" label-width="120px">
        <el-form-item label="分发标讯数量">
          <el-text type="info">
            共 {{ selectedTenders.length }} 条标讯待分发
          </el-text>
        </el-form-item>
        <el-form-item label="分发方式" required>
          <el-radio-group v-model="distributeForm.type">
            <el-radio value="auto">
              <div class="radio-content">
                <span class="radio-label">智能分发</span>
                <span class="radio-desc">根据规则自动分配</span>
              </div>
            </el-radio>
            <el-radio value="manual">
              <div class="radio-content">
                <span class="radio-label">手动指定</span>
                <span class="radio-desc">手动选择销售人员</span>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分发规则" v-if="distributeForm.type === 'auto'" required>
          <el-select v-model="distributeForm.rule" placeholder="选择分发规则" style="width: 100%">
            <el-option label="按区域自动分发" value="region">
              <div class="option-content">
                <span class="option-label">按区域自动分发</span>
                <span class="option-desc">根据标讯地区自动分配给对应区域销售</span>
              </div>
            </el-option>
            <el-option label="按产品线自动分发" value="product">
              <div class="option-content">
                <span class="option-label">按产品线自动分发</span>
                <span class="option-desc">根据标讯行业类型自动分配给对应产品线销售</span>
              </div>
            </el-option>
            <el-option label="按AI评分分发" value="ai">
              <div class="option-content">
                <span class="option-label">按AI评分分发</span>
                <span class="option-desc">高评分标讯优先分配给资深销售</span>
              </div>
            </el-option>
            <el-option label="平均分配" value="average">
              <div class="option-content">
                <span class="option-label">平均分配</span>
                <span class="option-desc">平均分配给所有可用销售人员</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="指派给" v-if="distributeForm.type === 'manual'" required>
          <el-select
            v-model="distributeForm.assignees"
            multiple
            placeholder="选择销售人员"
            style="width: 100%"
          >
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
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            多选时将按顺序轮询分配
          </div>
        </el-form-item>
        <el-form-item label="截止时间" v-if="distributeForm.type === 'manual'">
          <el-date-picker
            v-model="distributeForm.deadline"
            type="datetime"
            placeholder="选择跟进截止时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input
            v-model="distributeForm.remark"
            type="textarea"
            :rows="3"
            placeholder="填写分发说明、注意事项等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDistributeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleDistribute" :loading="distributeLoading">
          <el-icon><Share /></el-icon>
          确认分发
        </el-button>
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
          <template #default="{ row }">
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
            <el-option label="智慧办公" value="智慧办公" />
            <el-option label="信息化建设" value="信息化建设" />
            <el-option label="系统集成" value="系统集成" />
            <el-option label="软件开发" value="软件开发" />
            <el-option label="云计算" value="云计算" />
            <el-option label="大数据" value="大数据" />
            <el-option label="人工智能" value="人工智能" />
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
          <template #default="{ row }">
            {{ row.budget }}万元
          </template>
        </el-table-column>
        <el-table-column prop="region" label="地区" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.imported ? 'success' : 'info'" size="small">
              {{ row.imported ? '已入库' : '待入库' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
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
      <el-tabs v-model="activeInsightTab">
        <!-- 行业趋势 -->
        <el-tab-pane label="行业趋势" name="industry">
          <div class="insight-content">
            <div class="insight-header">
              <h4>热点采购行业 (近3个月)</h4>
              <el-tag type="info" size="small">数据更新: 2024-01-15</el-tag>
            </div>
            <el-table :data="industryTrends" size="small" stripe>
              <el-table-column prop="industry" label="行业" width="150">
                <template #default="{ row }">
                  <div class="industry-cell">
                    <span :class="['industry-dot', row.color]"></span>
                    {{ row.industry }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="count" label="标讯数量" width="120" align="center" />
              <el-table-column prop="amount" label="总预算(万元)" width="130" align="center">
                <template #default="{ row }">
                  {{ row.amount.toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="growth" label="同比增长" width="120" align="center">
                <template #default="{ row }">
                  <span :class="row.growth > 0 ? 'growth-up' : 'growth-down'">
                    {{ row.growth > 0 ? '+' : '' }}{{ row.growth }}%
                    <el-icon v-if="row.growth > 0"><ArrowRight /></el-icon>
                    <el-icon v-else><ArrowDown /></el-icon>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="trend" label="趋势" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.trend === 'up' ? 'success' : row.trend === 'down' ? 'danger' : 'info'" size="small">
                    {{ row.trend === 'up' ? '上升' : row.trend === 'down' ? '下降' : '平稳' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="hotLevel" label="热度" width="140" align="center">
                <template #default="{ row }">
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
                <template #default="{ row }">
                  <el-tag :type="row.frequency >= 10 ? 'danger' : row.frequency >= 5 ? 'warning' : 'info'" size="small">
                    {{ row.frequency }}次
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="period" label="常用招标月份" width="150" align="center" />
              <el-table-column prop="avgBudget" label="平均预算(万元)" width="130" align="center">
                <template #default="{ row }">
                  {{ row.avgBudget.toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column label="机会评估" width="150" align="center">
                <template #default="{ row }">
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useBiddingStore } from '@/stores/bidding'
import {
  Search,
  MagicStick,
  ArrowRight,
  ArrowDown,
  Location,
  Wallet,
  Calendar,
  InfoFilled,
  Star,
  StarFilled,
  Share,
  CircleCheck,
  User,
  List,
  TrendCharts,
  Plus,
  Download,
  Setting,
  Upload,
  Check,
  Delete,
  Connection,
  Refresh
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const biddingStore = useBiddingStore()

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

// 行业趋势数据
const industryTrends = ref([
  { industry: '数据中心', count: 156, amount: 45800, growth: 45, trend: 'up', hotLevel: 5, color: 'blue' },
  { industry: '智慧城市', count: 132, amount: 38600, growth: 32, trend: 'up', hotLevel: 5, color: 'green' },
  { industry: '能源管理', count: 98, amount: 28500, growth: 18, trend: 'up', hotLevel: 4, color: 'orange' },
  { industry: '交通信息化', count: 87, amount: 23400, growth: 12, trend: 'up', hotLevel: 4, color: 'purple' },
  { industry: '医疗数字化', count: 76, amount: 19800, growth: 8, trend: 'stable', hotLevel: 3, color: 'red' },
  { industry: '教育信息化', count: 65, amount: 15600, growth: -5, trend: 'down', hotLevel: 3, color: 'cyan' },
  { industry: '金融科技', count: 54, amount: 32500, growth: 22, trend: 'up', hotLevel: 4, color: 'yellow' }
])

// 行业洞察总结
const industryInsight = ref(
  '数据中心行业持续火热，近3个月标讯数量同比增长45%，主要集中在华东和华南地区。智慧城市建设需求旺盛，建议重点关注云服务、大数据相关项目。'
)

// 采购方规律数据
const purchaserPatterns = ref([
  {
    name: '某省政府采购中心',
    industry: '政府',
    frequency: 24,
    period: '3月、6月、9月',
    avgBudget: 280,
    opportunity: 5
  },
  {
    name: '国家电网某分公司',
    industry: '能源',
    frequency: 18,
    period: '4月、8月、11月',
    avgBudget: 450,
    opportunity: 5
  },
  {
    name: '某市交通投资集团',
    industry: '交通',
    frequency: 12,
    period: '5月、10月',
    avgBudget: 320,
    opportunity: 4
  },
  {
    name: '某大型银行总行',
    industry: '金融',
    frequency: 15,
    period: '2月、7月、12月',
    avgBudget: 580,
    opportunity: 5
  },
  {
    name: '某市教育局',
    industry: '教育',
    frequency: 8,
    period: '1月、8月',
    avgBudget: 120,
    opportunity: 3
  },
  {
    name: '某市卫健委',
    industry: '医疗',
    frequency: 10,
    period: '4月、9月',
    avgBudget: 180,
    opportunity: 3
  }
])

// 采购方洞察总结
const purchaserInsight = ref(
  '国家电网及大型银行项目预算高、机会大，建议提前布局。政府客户采购周期性强，建议在招标月份前2个月开始跟进。'
)

// 高潜力机会数据
const potentialOpportunities = ref([
  {
    id: 'op001',
    title: '某省政务云平台升级项目',
    purchaser: '某省政府采购中心',
    budget: 1200,
    region: '华东',
    priority: 'high',
    match: 95,
    reason: '历史数据显示该客户年均采购2400万，近期有云服务相关需求释放，与我方产品线高度匹配。'
  },
  {
    id: 'op002',
    title: '国家电网数字化运维系统',
    purchaser: '国家电网某分公司',
    budget: 850,
    region: '华北',
    priority: 'high',
    match: 92,
    reason: '该客户近期频繁发布相关标讯，预算充足，我方有成功案例可参考。'
  },
  {
    id: 'op003',
    title: '某市智慧交通平台建设',
    purchaser: '某市交通投资集团',
    budget: 680,
    region: '华南',
    priority: 'medium',
    match: 88,
    reason: '符合我方交通行业解决方案优势区域，竞争压力较小。'
  },
  {
    id: 'op004',
    title: '某银行核心系统改造',
    purchaser: '某大型银行总行',
    budget: 1500,
    region: '华东',
    priority: 'high',
    match: 90,
    reason: '高预算项目，客户资金实力强，我方在金融行业有技术积累。'
  },
  {
    id: 'op005',
    title: '某市医院信息系统集成',
    purchaser: '某市卫健委',
    budget: 320,
    region: '西南',
    priority: 'medium',
    match: 82,
    reason: '医疗数字化趋势明显，该地区项目竞争相对较少。'
  },
  {
    id: 'op006',
    title: '某工业园区能源管理平台',
    purchaser: '某工业园区管委会',
    budget: 280,
    region: '华东',
    priority: 'medium',
    match: 85,
    reason: '绿色能源政策支持，项目成功率较高。'
  }
])

// 预测建议
const forecastTips = ref([
  { text: '数据中心行业预计未来3个月将持续增长，建议加大技术储备', color: '#67c23a' },
  { text: '华东地区政府客户Q1采购需求集中，建议提前安排销售资源', color: '#409eff' },
  { text: '能源行业客户预算充足，建议优先跟进国家电网相关项目', color: '#e6a23c' },
  { text: '智慧城市项目逐渐向二三线城市下沉，可拓展新的市场区域', color: '#909399' }
])

// ========== 外部标讯源配置 ==========
const showSourceConfig = ref(false)
const sourceConfig = ref({
  platforms: [],
  apiEndpoint: '',
  apiKey: '',
  keywords: ['智慧办公', '信息化建设'],
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

const followingTendersCount = computed(() =>
  tenders.value.filter(t => t.status === 'following').length
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
    new: '',
    following: 'warning',
    bidding: 'primary'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    new: '新建',
    following: '跟进中',
    bidding: '投标中'
  }
  return map[status] || status
}

const getSourceTagType = (source) => {
  const map = {
    internal: 'info',
    external: 'success',
    manual: 'warning'
  }
  return map[source] || ''
}

const getSourceText = (source) => {
  const map = {
    internal: '内部',
    external: '外部获取',
    manual: '人工录入'
  }
  return map[source] || source
}

const isFollowed = (id) => {
  return followedTenders.value.includes(id)
}

const handleSearch = () => {
  pagination.value.currentPage = 1
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

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))

    ElMessage.success(`成功领取 ${selectedTenders.value.length} 条标讯`)
    handleClearSelection()
  } catch {
    // 用户取消
  }
}

const handleBatchFollow = async () => {
  const newFollows = selectedTenders.value
    .filter(t => !followedTenders.value.includes(t.id))
    .map(t => t.id)

  if (newFollows.length === 0) {
    ElMessage.info('所选标讯已全部关注')
    return
  }

  followedTenders.value.push(...newFollows)
  ElMessage.success(`已关注 ${newFollows.length} 条标讯`)
  handleClearSelection()
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
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 800))

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

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))

    ElMessage.success('状态已更新')
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
  margin-left: 4px;
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  border: none;
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
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
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
</style>
