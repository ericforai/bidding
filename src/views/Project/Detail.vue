<template>
  <div class="project-detail-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="!project" class="empty-container">
      <el-empty description="未找到项目信息">
        <el-button type="primary" @click="goBack">返回项目列表</el-button>
      </el-empty>
    </div>

    <!-- 正常内容 -->
    <el-container v-else class="project-detail-container">
    <el-page-header @back="goBack" class="page-header">
      <template #content>
        <div class="header-content">
          <span class="project-name">{{ project?.name }}</span>
          <el-tag :type="getStatusType(project?.status)" class="status-tag">
            {{ getStatusText(project?.status) }}
          </el-tag>
        </div>
      </template>
      <template #extra>
        <div class="header-actions">
          <el-button :icon="Edit" @click="handleEdit">编辑</el-button>
          <el-button type="primary" :icon="DocumentChecked" @click="handleSubmitApproval" v-if="canSubmit">
            提交审批
          </el-button>
          <el-button type="success" :icon="Coin" @click="handleRecordResult" v-if="canRecordResult">
            录入结果
          </el-button>
          <el-button type="warning" :icon="DataAnalysis" @click="goToResultPage">
            结果闭环
          </el-button>
          <el-button
            type="info"
            :icon="MagicStick"
            @click="toggleAssistantPanel"
            :class="{ 'is-active': assistantPanelVisible }"
          >
            智能助手
          </el-button>
        </div>
      </template>
    </el-page-header>

    <el-container class="detail-content">
      <!-- 左侧主要内容 -->
      <el-main class="main-content">
        <!-- 项目信息卡片 -->
        <el-card class="info-card">
          <template #header>
            <div class="card-title">
              <el-icon><InfoFilled /></el-icon>
              <span>项目信息</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目名称">{{ project?.name }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ project?.customer }}</el-descriptions-item>
            <el-descriptions-item label="预算">{{ project?.budget }} 万元</el-descriptions-item>
            <el-descriptions-item label="行业">{{ project?.industry }}</el-descriptions-item>
            <el-descriptions-item label="地区">{{ project?.region }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ project?.manager }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ project?.createTime }}</el-descriptions-item>
            <el-descriptions-item label="截止日期">{{ project?.deadline }}</el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="2">
              {{ project?.description }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- 进度条 -->
          <div class="progress-section">
            <div class="progress-header">
              <span class="progress-label">项目进度</span>
              <span class="progress-value">{{ formatScore(project?.progress || 0) }}%</span>
            </div>
            <el-progress
              :percentage="Number(project?.progress || 0)"
              :status="getProgressStatus(project?.progress)"
              :stroke-width="20"
            />
          </div>
        </el-card>

        <el-card class="approval-status-card" v-if="approvalHistory.length > 0 || project?.status === 'reviewing'">
          <template #header>
            <div class="card-title">
              <el-icon><Select /></el-icon>
              <span>审批状态</span>
              <el-tag v-if="currentApproval" :type="getApprovalStatusType(currentApproval.status)" size="small">
                {{ getApprovalStatusText(currentApproval.status) }}
              </el-tag>
            </div>
          </template>

          <div v-if="currentApproval" class="current-approval">
            <div class="approval-info-grid">
              <div class="approval-info-item">
                <span class="label">审批类型</span>
                <span>{{ currentApproval.typeName }}</span>
              </div>
              <div class="approval-info-item">
                <span class="label">申请人</span>
                <span>{{ currentApproval.applicantName }}（{{ currentApproval.applicantDept }}）</span>
              </div>
              <div class="approval-info-item">
                <span class="label">提交时间</span>
                <span>{{ currentApproval.submitTime }}</span>
              </div>
              <div class="approval-info-item" v-if="currentApproval.currentApproverName">
                <span class="label">当前审批人</span>
                <span class="approver">{{ currentApproval.currentApproverName }}</span>
              </div>
            </div>

            <div class="approval-actions" v-if="currentApproval.status === 'PENDING' && canApproveCurrent">
              <el-button type="success" :icon="CircleCheck" @click="handleQuickApprove">通过</el-button>
              <el-button type="danger" :icon="CircleClose" @click="handleQuickReject">驳回</el-button>
            </div>
          </div>

          <el-empty v-else description="暂无审批记录" :image-size="60" />
        </el-card>

        <!-- 费用汇总 -->
        <el-card class="expense-card">
          <template #header>
            <div class="card-title">
              <el-icon><Coin /></el-icon>
              <span>费用汇总</span>
              <el-button link type="primary" @click="goToExpensePage">费用管理</el-button>
            </div>
          </template>
          <el-table :data="projectExpenses" stripe size="small">
            <el-table-column prop="type" label="费用类型" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="getExpenseTypeColor(row.type)">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="130" align="right">
              <template #default="{ row }">
                ¥{{ row.amount }}万
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getExpenseStatusType(row.status)">
                  {{ getExpenseStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="date" label="日期" width="160" />
            <el-table-column prop="remark" label="说明" min-width="150" />
          </el-table>
          <div class="expense-total">
            <span>合计：¥{{ expenseTotal }}万元</span>
          </div>
        </el-card>

        <!-- 任务看板 -->
        <el-card class="task-card">
          <template #header>
            <div class="card-title">
              <el-icon><List /></el-icon>
              <span>任务看板</span>
              <el-button v-if="canManageProjectTasks" link type="primary" :icon="Plus" @click="handleAddTask">添加任务</el-button>
              <el-button v-if="isDemoMode" link type="warning" @click="handleResetTasks">重置任务</el-button>
            </div>
          </template>
          <TaskBoard
            :tasks="project?.tasks || []"
            :project-id="project?.id"
            :can-generate="!project?.tasks || project.tasks.length === 0"
            @task-click="handleTaskClick"
            @status-change="handleTaskStatusChange"
            @generate-tasks="handleGenerateTasks"
            @add-deliverable="handleAddDeliverable"
            @remove-deliverable="handleRemoveDeliverable"
            @submit-to-document="handleSubmitToDocument"
          />
        </el-card>

        <!-- 标书编制流程 -->
        <el-card class="process-card">
          <template #header>
            <div class="card-title">
              <el-icon><DocumentChecked /></el-icon>
              <span>标书编制流程</span>
              <el-button
                v-if="!bidProcess.initiated"
                type="primary"
                size="small"
                @click="handleInitiateProcess"
              >
                发起流程
              </el-button>
            </div>
          </template>

          <div v-if="!bidProcess.initiated" class="process-empty">
            <el-empty description="暂未发起标书编制流程">
              <el-button type="primary" @click="handleInitiateProcess">立即发起</el-button>
            </el-empty>
          </div>

          <div v-else class="process-content">
            <el-steps :active="bidProcess.currentStep" align-center finish-status="success">
              <el-step title="初稿编制" :description="getStepStatusText('draft')" />
              <el-step title="内部评审" :description="getStepStatusText('review')" />
              <el-step title="用印申请" :description="getStepStatusText('seal')" />
              <el-step title="封装提交" :description="getStepStatusText('submit')" />
            </el-steps>

            <div class="process-actions">
              <el-button
                :type="bidProcess.steps.draft.completed ? 'success' : 'primary'"
                :disabled="!canOperateStep('draft')"
                @click="handleDraftSubmit"
              >
                {{ bidProcess.steps.draft.completed ? '初稿已提交' : '提交初稿' }}
              </el-button>
              <el-button
                :type="bidProcess.steps.review.completed ? 'success' : 'warning'"
                :disabled="!canOperateStep('review')"
                @click="handleReview"
              >
                {{ bidProcess.steps.review.completed ? '评审已完成' : '发起评审' }}
              </el-button>
              <el-button
                :type="bidProcess.steps.seal.completed ? 'success' : 'primary'"
                :disabled="!canOperateStep('seal')"
                @click="handleSealApply"
              >
                {{ bidProcess.steps.seal.completed ? '用印已完成' : '用印申请' }}
              </el-button>
              <el-button
                :type="bidProcess.steps.submit.completed ? 'success' : 'primary'"
                :disabled="!canOperateStep('submit')"
                @click="handleSubmit"
              >
                {{ bidProcess.steps.submit.completed ? '已封装提交' : '封装提交' }}
              </el-button>
            </div>

            <!-- 流程状态详情 -->
            <div class="process-detail">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="流程发起人">{{ bidProcess.initiator }}</el-descriptions-item>
                <el-descriptions-item label="发起时间">{{ bidProcess.initiateTime }}</el-descriptions-item>
                <el-descriptions-item label="当前阶段">
                  <el-tag :type="getCurrentPhaseType()">{{ getCurrentPhaseText() }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="整体进度">
                  <el-progress
                    :percentage="getProcessProgress()"
                    :stroke-width="12"
                    :show-text="true"
                  />
                </el-descriptions-item>
              </el-descriptions>

              <!-- 交付物关联 -->
              <div class="deliverables-section" v-if="bidProcess.deliverables.length">
                <div class="section-title">交付物清单</div>
                <el-table :data="bidProcess.deliverables" size="small" max-height="200">
                  <el-table-column prop="name" label="交付物名称" min-width="150" />
                  <el-table-column prop="type" label="类型" width="100">
                    <template #default="{ row }">
                      <el-tag size="small">{{ row.type }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="uploader" label="上传者" width="100" />
                  <el-table-column prop="time" label="上传时间" width="140" />
                  <el-table-column label="操作" width="80">
                    <template #default="{ row }">
                      <el-button link type="primary" size="small" @click="handleDownloadDeliverable(row)">
                        下载
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 文档列表 -->
        <el-card class="document-card">
          <template #header>
            <div class="card-title">
              <el-icon><Folder /></el-icon>
              <span>项目文档</span>
              <el-button v-if="canManageProjectDocuments" link type="success" :icon="DocumentChecked" @click="handleArchiveDocuments">
                归档资料
              </el-button>
              <el-upload
                v-if="canManageProjectDocuments"
                :show-file-list="false"
                :before-upload="handleUpload"
                accept=".doc,.docx,.pdf,.xls,.xlsx"
              >
                <el-button link type="primary" :icon="Upload">上传文档</el-button>
              </el-upload>
            </div>
          </template>
          <el-table :data="project?.documents || []" style="width: 100%">
            <el-table-column prop="name" label="文档名称" min-width="200">
              <template #default="{ row }">
                <div class="file-name">
                  <el-icon><Document /></el-icon>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="uploader" label="上传者" width="120" />
            <el-table-column prop="time" label="上传时间" width="160" />
            <el-table-column prop="size" label="文件大小" width="100" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
                <el-button link type="danger" @click="handleDeleteDoc(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!project?.documents?.length" description="暂无文档" />
        </el-card>
      </el-main>

      <!-- 右侧信息栏 -->
      <el-aside width="320px" class="right-sidebar">
        <!-- AI智能检查面板 -->
        <el-card v-if="showAICheckCard" class="ai-check-card">
          <template #header>
            <div class="card-title">
              <el-icon><MagicStick /></el-icon>
              <span>AI智能检查</span>
              <el-button
                v-if="canRunAICheck"
                type="primary"
                size="small"
                @click="runAICheck"
                :loading="aiChecking"
              >
                <el-icon v-if="!aiChecking"><VideoPlay /></el-icon>
                {{ aiChecking ? '检查中...' : '开始检查' }}
              </el-button>
            </div>
          </template>

          <el-tabs v-model="activeAITab" class="ai-tabs">
            <!-- 合规性检查 -->
            <el-tab-pane name="compliance">
              <template #label>
                <div class="tab-label">
                  <span>合规性检查</span>
                  <el-badge
                    v-if="aiResult.compliance"
                    :value="aiResult.compliance.score"
                    :type="getBadgeType(aiResult.compliance.score)"
                    :max="100"
                  />
                </div>
              </template>

              <div class="check-result">
                <div class="score-section">
                  <el-progress
                    type="circle"
                    :percentage="Number(aiResult.compliance?.score || 0)"
                    :color="getProgressColor(aiResult.compliance?.score || 0)"
                    :width="100"
                  >
                    <template #default="{ percentage }">
                      <span class="score-text">{{ formatScore(percentage) }}分</span>
                    </template>
                  </el-progress>
                  <p class="score-level">{{ getScoreLevel(aiResult.compliance?.score || 0) }}</p>
                </div>

                <div class="issues-section" v-if="aiResult.compliance?.issues?.length">
                  <h4 class="section-title">检查结果</h4>
                  <el-table :data="aiResult.compliance.issues" size="small" max-height="300">
                    <el-table-column prop="category" label="类别" width="80" />
                    <el-table-column prop="item" label="检查项" min-width="120" show-overflow-tooltip />
                    <el-table-column prop="status" label="状态" width="70">
                      <template #default="{ row }">
                        <el-tag :type="row.status === 'pass' ? 'success' : 'danger'" size="small">
                          {{ row.status === 'pass' ? '通过' : '不通过' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="suggestion" label="建议" min-width="100" show-overflow-tooltip />
                  </el-table>
                </div>
                <el-empty v-else description="点击开始检查进行AI分析" :image-size="80" />
              </div>
            </el-tab-pane>

            <!-- 可投标能力检查 -->
            <el-tab-pane name="asset-check">
              <template #label>
                <div class="tab-label">
                  <span>可投标能力</span>
                  <el-badge
                    v-if="assetCheckResult"
                    :value="assetCheckResult.capability?.status === 'available' ? 1 : 0"
                    :type="assetCheckResult.capability?.status === 'available' ? 'success' : 'warning'"
                  />
                </div>
              </template>

              <div class="asset-check-tab">
                <div v-if="assetCheckResult && assetCheckResult.found" class="asset-check-content">
                  <div class="check-header">
                    <div class="site-info">
                      <h3>{{ assetCheckResult.site?.name }}</h3>
                      <el-tag
                        v-if="assetCheckResult.capability?.status === 'available'"
                        type="success"
                        size="large"
                      >
                        可投标
                      </el-tag>
                      <el-tag
                        v-else-if="assetCheckResult.capability?.status === 'risk'"
                        type="warning"
                        size="large"
                      >
                        有风险
                      </el-tag>
                      <el-tag v-else type="danger" size="large">
                        不可投标
                      </el-tag>
                    </div>
                    <a :href="assetCheckResult.site?.url" target="_blank" class="site-link">
                      {{ assetCheckResult.site?.url }}
                    </a>
                  </div>

                  <div class="check-items-grid">
                    <div class="check-item-card">
                      <div class="item-icon">
                        <el-icon v-if="assetCheckResult.capability?.hasAccount" class="icon-success">
                          <CircleCheck />
                        </el-icon>
                        <el-icon v-else class="icon-error"><CircleClose /></el-icon>
                      </div>
                      <div class="item-content">
                        <div class="item-label">账号状态</div>
                        <div class="item-value">
                          {{ assetCheckResult.capability?.hasAccount ? '已注册' : '未注册' }}
                          <span v-if="assetCheckResult.capability?.accountCount > 0">
                            ({{ assetCheckResult.capability?.accountCount }}个)
                          </span>
                        </div>
                      </div>
                    </div>

                    <div class="check-item-card">
                      <div class="item-icon">
                        <el-icon v-if="assetCheckResult.capability?.hasAvailableUK" class="icon-success">
                          <CircleCheck />
                        </el-icon>
                        <el-icon v-else-if="assetCheckResult.capability?.ukCount > 0" class="icon-warning">
                          <Warning />
                        </el-icon>
                        <el-icon v-else class="icon-success"><CircleCheck /></el-icon>
                      </div>
                      <div class="item-content">
                        <div class="item-label">UK状态</div>
                        <div class="item-value">
                          <span v-if="assetCheckResult.capability?.ukCount === 0">不需要</span>
                          <span v-else-if="assetCheckResult.capability?.hasAvailableUK">在库</span>
                          <span v-else>已借出</span>
                        </div>
                      </div>
                    </div>

                    <div class="check-item-card" v-if="assetCheckResult.capability?.primaryOwner">
                      <div class="item-icon">
                        <el-icon class="icon-user"><User /></el-icon>
                      </div>
                      <div class="item-content">
                        <div class="item-label">责任人</div>
                        <div class="item-value">
                          {{ assetCheckResult.capability?.primaryOwner }}
                          <span class="phone">({{ assetCheckResult.capability?.primaryPhone }})</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="check-actions">
                    <el-button type="primary" @click="goToSiteDetail">
                      查看详情
                    </el-button>
                    <el-button v-if="assetCheckResult.capability?.ukCount > 0" @click="borrowUK">
                      借用UK
                    </el-button>
                    <el-button @click="viewSOP">查看SOP</el-button>
                  </div>
                </div>

                <el-empty v-else description="未找到该站点的资产信息" :image-size="100">
                  <el-button type="primary" @click="goToAssetManagement">前往资产管理</el-button>
                </el-empty>
              </div>
            </el-tab-pane>

            <!-- 质量检查 -->
            <el-tab-pane name="quality">
              <template #label>
                <div class="tab-label">
                  <span>文书质量</span>
                  <el-badge
                    v-if="aiResult.quality"
                    :value="aiResult.quality.errors?.length || 0"
                    type="warning"
                  />
                </div>
              </template>

              <div class="quality-result">
                <div class="quality-summary" v-if="aiResult.quality">
                  <el-statistic
                    title="发现问题"
                    :value="aiResult.quality.errors?.length || 0"
                    suffix="个"
                  >
                    <template #prefix>
                      <el-icon color="#f56c6c"><WarningFilled /></el-icon>
                    </template>
                  </el-statistic>
                  <el-statistic
                    title="建议修改"
                    :value="aiResult.quality.suggestions?.length || 0"
                    suffix="条"
                  >
                    <template #prefix>
                      <el-icon color="#e6a23c"><QuestionFilled /></el-icon>
                    </template>
                  </el-statistic>
                </div>

                <h4 class="section-title" v-if="aiResult.quality?.errors?.length">问题列表</h4>
                <el-table :data="aiResult.quality?.errors || []" size="small" max-height="280">
                  <el-table-column prop="type" label="类型" width="70">
                    <template #default="{ row }">
                      <el-tag v-if="row.type === 'typo'" type="danger" size="small">错别字</el-tag>
                      <el-tag v-else-if="row.type === 'grammar'" type="warning" size="small">语法</el-tag>
                      <el-tag v-else type="info" size="small">格式</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="original" label="原文" min-width="80" show-overflow-tooltip />
                  <el-table-column prop="suggestion" label="建议修改" min-width="80" show-overflow-tooltip />
                  <el-table-column prop="location" label="位置" width="100" show-overflow-tooltip />
                  <el-table-column label="操作" width="100" fixed="right">
                    <template #default="{ row, $index }">
                      <el-button link type="primary" size="small" @click="handleAdoptSuggestion(row, $index)">
                        采纳
                      </el-button>
                      <el-button link type="info" size="small" @click="handleIgnoreSuggestion($index)">
                        忽略
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty
                  v-if="!aiResult.quality || !aiResult.quality.errors?.length"
                  description="点击开始检查进行文书质量分析"
                  :image-size="80"
                />
              </div>
            </el-tab-pane>

            <!-- 智能评分 -->
            <el-tab-pane name="score">
              <template #label>
                <div class="tab-label">
                  <span>智能评分</span>
                  <el-badge v-if="aiResult.score" :value="aiResult.score.total || 0" type="primary" />
                </div>
              </template>

              <div class="ai-score">
                <div class="score-header">
                  <h4>AI模拟专家评审</h4>
                  <el-tag :type="getOverallScoreType(aiResult.score?.total || 0)" size="large">
                    综合评分: {{ aiResult.score?.total || 0 }}分
                  </el-tag>
                </div>

                <el-descriptions :column="1" border size="small" class="score-descriptions">
                  <el-descriptions-item label="技术方案">
                    <div class="score-item">
                      <el-rate :model-value="aiResult.score?.tech || 0" disabled show-score />
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="商务响应">
                    <div class="score-item">
                      <el-rate :model-value="aiResult.score?.business || 0" disabled show-score />
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="价格竞争力">
                    <div class="score-item">
                      <el-rate :model-value="aiResult.score?.price || 0" disabled show-score />
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="企业资质">
                    <div class="score-item">
                      <el-rate :model-value="aiResult.score?.qualification || 0" disabled show-score />
                    </div>
                  </el-descriptions-item>
                </el-descriptions>

                <div class="ai-comment" v-if="aiResult.score?.comment">
                  <h4 class="section-title">AI评语</h4>
                  <div class="comment-content">
                    <p v-for="(paragraph, idx) in aiResult.score.comment.split('\n')" :key="idx">
                      {{ paragraph }}
                    </p>
                  </div>
                </div>

                <div class="ai-suggestions" v-if="aiResult.score?.suggestions?.length">
                  <h4 class="section-title">改进建议</h4>
                  <ul class="suggestions-list">
                    <li v-for="(suggestion, idx) in aiResult.score.suggestions" :key="idx">
                      <el-icon color="#409eff"><ArrowRight /></el-icon>
                      <span>{{ suggestion }}</span>
                    </li>
                  </ul>
                </div>

                <el-empty
                  v-if="!aiResult.score"
                  description="点击开始检查进行AI智能评分"
                  :image-size="80"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 快捷操作 -->
        <el-card class="action-card">
          <template #header>
            <div class="card-title">
              <el-icon><Operation /></el-icon>
              <span>快捷操作</span>
            </div>
          </template>
          <div class="action-list">
            <el-button v-if="canManageProjectDocuments" :icon="DocumentAdd" @click="handleAddDocument">添加文档</el-button>
            <el-button :icon="Share" @click="handleShare">分享项目</el-button>
            <el-button :icon="Download" @click="handleExport">导出资料</el-button>
            <el-button v-if="canSetProjectReminder" :icon="Bell" @click="handleSetReminder">设置提醒</el-button>
          </div>
        </el-card>

        <!-- 项目动态 -->
        <el-card class="timeline-card">
          <template #header>
            <div class="card-title">
              <el-icon><Clock /></el-icon>
              <span>项目动态</span>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="activity in activities"
              :key="activity.id"
              :timestamp="activity.time"
              placement="top"
            >
              <div class="activity-content">
                <span class="activity-user">{{ activity.user }}</span>
                <span class="activity-action">{{ activity.action }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-aside>

    <!-- 智能助手抽屉 -->
    <SmartAssistantPanel
      v-model:visible="assistantPanelVisible"
      :project-id="dialogProjectId"
      :show-demo-features="isDemoMode"
      @open-competition-intel="handleOpenCompetitionIntel"
      @open-roi-analysis="handleOpenRoiAnalysis"
      @open-score-coverage="handleOpenScoreCoverage"
      @open-compliance-check="handleOpenComplianceCheck"
      @open-version-control="handleOpenVersionControl"
      @open-collaboration="handleOpenCollaboration"
      @open-auto-tasks="handleOpenAutoTasks"
      @open-mobile-card="handleOpenMobileCard"
    />
    </el-container>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="taskDialogVisible" title="任务详情" width="500px">
      <el-descriptions :column="1" border v-if="currentTask">
        <el-descriptions-item label="任务名称">{{ currentTask.name }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ currentTask.owner }}</el-descriptions-item>
        <el-descriptions-item label="截止日期">{{ currentTask.deadline }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="getPriorityType(currentTask.priority)">
            {{ getPriorityText(currentTask.priority) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getTaskStatusType(currentTask.status)">
            {{ getTaskStatusText(currentTask.status) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 结果录入对话框 -->
    <el-dialog v-model="resultDialogVisible" title="投标结果录入" width="750px">
      <el-form :model="resultForm" label-width="120px">
        <!-- 投标结果选择 -->
        <el-form-item label="投标结果">
          <el-radio-group v-model="resultForm.result">
            <el-radio value="won">中标</el-radio>
            <el-radio value="lost">未中标</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 中标专属信息 -->
        <template v-if="resultForm.result === 'won'">
          <el-form-item label="中标金额">
            <el-input-number v-model="resultForm.amount" :min="0" :precision="2" :max="99999" />
            <span style="margin-left: 8px; color: #909399;">万元</span>
          </el-form-item>
          <el-form-item label="合同期限">
            <el-date-picker
              v-model="resultForm.contractPeriod"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 100%;"
            />
          </el-form-item>
          <el-form-item label="中标SKU数量">
            <el-input v-model="resultForm.skuCount" placeholder="如: 1500个SKU" style="width: 200px;" />
          </el-form-item>
          <el-form-item label="中标通知书">
            <el-upload
              :action="uploadAction"
              :headers="uploadHeaders"
              :before-upload="ensureDemoUpload"
              :on-success="handleUploadSuccess"
              :on-remove="handleUploadRemove"
              :file-list="noticeFileList"
              :limit="1"
              accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
            >
              <el-button type="primary" :icon="Upload">上传中标通知书</el-button>
              <template #tip>
                <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                  支持 PDF、Word、图片格式，最大 10MB
                </div>
              </template>
            </el-upload>
          </el-form-item>
        </template>

        <!-- 竞争对手信息 -->
        <el-form-item label="竞争对手信息">
          <el-table
            :data="resultForm.competitors"
            size="small"
            border
            style="width: 100%;"
            max-height="200"
          >
            <el-table-column prop="name" label="公司名称" width="140" />
            <el-table-column prop="skuCount" label="SKU数量" width="100" />
            <el-table-column prop="category" label="品类" width="120" />
            <el-table-column prop="discount" label="折扣" width="90" />
            <el-table-column prop="payment" label="账期" width="90" />
            <el-table-column label="操作" width="70" fixed="right">
              <template #default="{ $index }">
                <el-button
                  link
                  type="danger"
                  size="small"
                  :icon="Delete"
                  @click="removeCompetitor($index)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button
            type="primary"
            :icon="Plus"
            size="small"
            plain
            style="margin-top: 8px;"
            @click="addCompetitor"
          >
            添加竞争对手
          </el-button>
        </el-form-item>

        <!-- 复盘报告 -->
        <el-divider content-position="left">复盘报告</el-divider>
        <el-form-item label="技术亮点">
          <el-input
            v-model="resultForm.techHighlights"
            type="textarea"
            :rows="3"
            placeholder="记录本次投标中的技术亮点和创新点"
          />
        </el-form-item>
        <el-form-item label="报价策略">
          <el-input
            v-model="resultForm.priceStrategy"
            type="textarea"
            :rows="3"
            placeholder="记录报价策略及效果分析"
          />
        </el-form-item>
        <el-form-item label="客户反馈">
          <el-input
            v-model="resultForm.customerFeedback"
            type="textarea"
            :rows="3"
            placeholder="记录客户对本次投标的评价和反馈"
          />
        </el-form-item>
        <el-form-item label="改进建议">
          <el-input
            v-model="resultForm.improvements"
            type="textarea"
            :rows="2"
            placeholder="总结经验教训，提出改进建议"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveResult">提交结果</el-button>
      </template>
    </el-dialog>

    <!-- 竞争对手编辑对话框 -->
    <el-dialog v-model="competitorDialogVisible" title="添加竞争对手" width="500px">
      <el-form :model="competitorForm" label-width="100px">
        <el-form-item label="公司名称" required>
          <el-input v-model="competitorForm.name" placeholder="请输入竞争对手公司名称" />
        </el-form-item>
        <el-form-item label="SKU数量">
          <el-input v-model="competitorForm.skuCount" placeholder="如: 1200个" />
        </el-form-item>
        <el-form-item label="品类">
          <el-input v-model="competitorForm.category" placeholder="如: 办公用品" />
        </el-form-item>
        <el-form-item label="折扣">
          <el-input v-model="competitorForm.discount" placeholder="如: 85折" />
        </el-form-item>
        <el-form-item label="账期">
          <el-input v-model="competitorForm.payment" placeholder="如: 月结30天" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="competitorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddCompetitor">确定</el-button>
      </template>
    </el-dialog>

    <!-- AI功能弹窗组件 -->
    <CompetitionIntel
      v-model="showCompetitionIntel"
      :project-id="route.params.id"
    />

    <ComplianceCheck
      v-model="showComplianceCheck"
      :project-id="route.params.id"
    />

    <VersionControl
      v-model="showVersionControl"
      :project-id="dialogProjectId"
    />

    <CollaborationCenter
      v-model="showCollaboration"
      :project-id="dialogProjectId"
    />

    <ROIAnalysis
      v-model="showROIAnalysis"
      :project-id="route.params.id"
    />

    <AutoTasks
      v-if="isDemoMode"
      v-model="showAutoTasks"
      :project-id="route.params.id"
      :data="demoAutoTasks"
    />

    <MobileCard
      v-if="isDemoMode"
      v-model="showMobileCard"
      :project-id="route.params.id"
      :data="demoMobileCard"
    />

    <!-- 标书编制流程对话框 -->
    <el-dialog v-model="processDialogVisible" title="标书编制流程" width="900px" :close-on-click-modal="false">
      <el-tabs v-model="activeProcessTab" type="border-card">
        <!-- 初稿编制 -->
        <el-tab-pane label="初稿编制" name="draft">
          <el-form :model="draftForm" label-width="120px">
            <el-form-item label="编制人">
              <span>{{ draftForm.preparer || userStore.userName }}</span>
            </el-form-item>
            <el-form-item label="使用模板">
              <el-select v-model="draftForm.templateId" placeholder="请选择模板" style="width: 300px;">
                <el-option
                  v-for="tpl in templates"
                  :key="tpl.id"
                  :label="tpl.name"
                  :value="tpl.id"
                >
                  <div style="display: flex; justify-content: space-between;">
                    <span>{{ tpl.name }}</span>
                    <span style="color: #909399; font-size: 12px;">{{ tpl.category }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="初稿文件">
              <el-upload
                :action="uploadAction"
                :headers="uploadHeaders"
                :before-upload="ensureDemoUpload"
                :on-success="handleDraftFileSuccess"
                :file-list="draftFileList"
                :limit="3"
                accept=".doc,.docx,.pdf"
                drag
              >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">
                  将文件拖到此处，或<em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持 doc、docx、pdf 格式，最多上传3个文件
                  </div>
                </template>
              </el-upload>
            </el-form-item>
            <el-form-item label="备注说明">
              <el-input
                v-model="draftForm.remark"
                type="textarea"
                :rows="3"
                placeholder="请输入初稿编制的备注说明"
              />
            </el-form-item>
          </el-form>
          <div class="dialog-footer">
            <el-button @click="processDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSaveDraft">保存初稿</el-button>
          </div>
        </el-tab-pane>

        <!-- 内部评审 -->
        <el-tab-pane label="内部评审" name="review">
          <div class="review-section">
            <div class="section-header">
              <span>评审人员</span>
              <el-button type="primary" size="small" :icon="Plus" @click="handleAddReviewer">
                添加评审人
              </el-button>
            </div>
            <el-table :data="reviewers" border style="width: 100%; margin-bottom: 20px;">
              <el-table-column prop="name" label="评审人" width="120" />
              <el-table-column prop="role" label="角色" width="120">
                <template #default="{ row }">
                  <el-tag size="small" :type="getReviewerRoleType(row.role)">
                    {{ getReviewerRoleText(row.role) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="评审状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getReviewStatusType(row.status)" size="small">
                    {{ getReviewStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="comment" label="评审意见" min-width="200" show-overflow-tooltip />
              <el-table-column prop="reviewTime" label="评审时间" width="160" />
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row, $index }">
                  <el-button
                    link
                    type="primary"
                    size="small"
                    v-if="row.status === 'pending'"
                    @click="handleRemindReviewer(row)"
                  >
                    提醒
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    size="small"
                    @click="handleRemoveReviewer($index)"
                  >
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-divider>评审总览</el-divider>
            <el-progress
              :percentage="getReviewProgress()"
              :stroke-width="20"
              :show-text="true"
            >
              <span class="progress-text">评审进度: {{ getReviewedCount() }}/{{ reviewers.length }}</span>
            </el-progress>

            <div class="review-summary" v-if="reviewers.some(r => r.status === 'rejected')">
              <el-alert
                title="存在评审未通过，请处理相关意见"
                type="error"
                :closable="false"
                show-icon
              />
            </div>
          </div>
          <div class="dialog-footer">
            <el-button @click="processDialogVisible = false">关闭</el-button>
            <el-button type="primary" @click="handleCompleteReview" :disabled="!canCompleteReview()">
              完成评审
            </el-button>
          </div>
        </el-tab-pane>

        <!-- 用印申请 -->
        <el-tab-pane label="用印申请" name="seal">
          <el-form :model="sealForm" label-width="120px">
            <el-form-item label="用印类型" required>
              <el-checkbox-group v-model="sealForm.sealTypes">
                <el-checkbox label="official">公章</el-checkbox>
                <el-checkbox label="contract">合同章</el-checkbox>
                <el-checkbox label="legal">法人章</el-checkbox>
                <el-checkbox label="finance">财务章</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="用印事由">
              <el-input
                v-model="sealForm.reason"
                type="textarea"
                :rows="2"
                placeholder="请说明用印事由"
              />
            </el-form-item>
            <el-form-item label="用印文件">
              <el-upload
                :action="uploadAction"
                :headers="uploadHeaders"
                :before-upload="ensureDemoUpload"
                :on-success="handleSealFileSuccess"
                :file-list="sealFileList"
                :limit="5"
                accept=".pdf,.doc,.docx"
              >
                <el-button type="primary" :icon="Upload">上传待盖章文件</el-button>
                <template #tip>
                  <div class="el-upload__tip">
                    请上传需要盖章的文件，最多5个
                  </div>
                </template>
              </el-upload>
            </el-form-item>
            <el-form-item label="用印数量">
              <el-input-number v-model="sealForm.count" :min="1" :max="100" />
              <span style="margin-left: 8px;">份</span>
            </el-form-item>
            <el-form-item label="期望完成时间">
              <el-date-picker
                v-model="sealForm.expectedTime"
                type="datetime"
                placeholder="选择期望完成时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm"
              />
            </el-form-item>
          </el-form>
          <div class="dialog-footer">
            <el-button @click="processDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmitSeal">提交用印申请</el-button>
          </div>
        </el-tab-pane>

        <!-- 封装提交 -->
        <el-tab-pane label="封装提交" name="submit">
          <el-form :model="submitForm" label-width="120px">
            <el-form-item label="标书封装检查">
              <el-checkbox-group v-model="submitForm.checkList">
                <el-checkbox label="tech">技术方案已完成</el-checkbox>
                <el-checkbox label="business">商务文件已完成</el-checkbox>
                <el-checkbox label="qualification">资质文件已准备</el-checkbox>
                <el-checkbox label="price">报价文件已确认</el-checkbox>
                <el-checkbox label="seal">用印已完成</el-checkbox>
                <el-checkbox label="package">标书已装订</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="封装方式">
              <el-radio-group v-model="submitForm.packageType">
                <el-radio value="paper">纸质封装</el-radio>
                <el-radio value="electronic">电子标书</el-radio>
                <el-radio value="both">纸质+电子</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="密封要求">
              <el-input
                v-model="submitForm.sealRequirement"
                type="textarea"
                :rows="2"
                placeholder="请输入密封要求，如：密封条加盖公章"
              />
            </el-form-item>
            <el-form-item label="递交方式">
              <el-radio-group v-model="submitForm.deliveryMethod">
                <el-radio value="online">线上递交</el-radio>
                <el-radio value="offline">现场递交</el-radio>
                <el-radio value="courier">快递递交</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="递交时间" v-if="submitForm.deliveryMethod !== 'online'">
              <el-date-picker
                v-model="submitForm.deliveryTime"
                type="datetime"
                placeholder="选择递交时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm"
              />
            </el-form-item>
            <el-form-item label="递交地址" v-if="submitForm.deliveryMethod === 'offline' || submitForm.deliveryMethod === 'courier'">
              <el-input
                v-model="submitForm.deliveryAddress"
                placeholder="请输入递交地址"
              />
            </el-form-item>
            <el-form-item label="备注">
              <el-input
                v-model="submitForm.remark"
                type="textarea"
                :rows="2"
                placeholder="其他需要说明的事项"
              />
            </el-form-item>
          </el-form>
          <div class="dialog-footer">
            <el-button @click="processDialogVisible = false">取消</el-button>
            <el-button
              type="primary"
              @click="handleSubmitPackage"
              :disabled="submitForm.checkList.length < 6"
            >
              确认封装提交
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 评审人添加对话框 -->
    <el-dialog v-model="reviewerDialogVisible" title="添加评审人" width="500px">
      <el-form :model="reviewerForm" label-width="100px">
        <el-form-item label="评审人" required>
          <el-select v-model="reviewerForm.userId" placeholder="请选择评审人" style="width: 100%;">
            <el-option
              v-for="user in availableReviewers"
              :key="user.id"
              :label="user.name"
              :value="user.id"
            >
              <span>{{ user.name }}</span>
              <span style="color: #909399; font-size: 12px; margin-left: 10px;">{{ user.dept }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="评审角色" required>
          <el-select v-model="reviewerForm.role" placeholder="请选择评审角色" style="width: 100%;">
            <el-option label="技术评审" value="tech" />
            <el-option label="商务评审" value="business" />
            <el-option label="法务评审" value="legal" />
            <el-option label="财务评审" value="finance" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAddReviewer">确定</el-button>
      </template>
    </el-dialog>

    <ScoreDraftDialog
      v-model:visible="scoreDraftDialogVisible"
      :project-id="route.params.id"
      @generated="handleScoreDraftGenerated"
    />

    <ApprovalDialog
      v-model:visible="approvalDialogVisible"
      :mode="approvalMode"
      :project-id="project?.id"
      :project-name="project?.name"
      :approval-type="approvalType"
      :approval-info="currentApprovalItem"
      @success="handleApprovalSuccess"
    />
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { useBarStore } from '@/stores/bar'
import { knowledgeApi, projectsApi, collaborationApi, approvalApi } from '@/api'
import { complianceApi, scoreAnalysisApi } from '@/api/modules/ai.js'
import { feesApi } from '@/api/modules/fees.js'
import { auditApi } from '@/api/modules/audit.js'
import { getDemoAutoTasks, getDemoMobileCard, getDemoProjectById } from '@/api/mock-adapters/frontendDemo.js'
import { normalizeFeeForDisplay, normalizeAuditLogForTimeline } from './project-utils.js'
import {
  Edit, DocumentChecked, Coin, InfoFilled, List, Folder, Upload, Plus,
  MagicStick, Operation, DocumentAdd, Share, Download, Bell, Clock,
  Document, CircleCheckFilled, MoreFilled, Delete, UploadFilled, VideoPlay,
  WarningFilled, QuestionFilled, ArrowRight, CircleCheck, CircleClose, Warning, User,
  DataAnalysis, Select
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TaskBoard from '@/components/common/TaskBoard.vue'
import SmartAssistantPanel from '@/components/ai/SmartAssistantPanel.vue'
import CompetitionIntel from '@/components/ai/CompetitionIntel.vue'
import ComplianceCheck from '@/components/ai/ComplianceCheck.vue'
import VersionControl from '@/components/ai/VersionControl.vue'
import CollaborationCenter from '@/components/ai/CollaborationCenter.vue'
import ROIAnalysis from '@/components/ai/ROIAnalysis.vue'
import AutoTasks from '@/components/ai/AutoTasks.vue'
import MobileCard from '@/components/ai/MobileCard.vue'
import ApprovalDialog from '@/components/common/ApprovalDialog.vue'
import ScoreDraftDialog from '@/components/project/ScoreDraftDialog.vue'
import { getAccessToken } from '@/api/session.js'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const userStore = useUserStore()
const barStore = useBarStore()
const isDemoMode = false
const demoAutoTasks = ref([])
const demoMobileCard = ref(null)
const isApiProject = computed(() => !isDemoMode && /^\d+$/.test(String(route.params.id || '')))

const downloadTextFile = (filename, content, mimeType = 'text/plain;charset=utf-8') => {
  const blob = new Blob([content], { type: mimeType })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

// 加载状态
const loading = ref(true)
const approvalHistory = ref([])

// 资产检查结果
const assetCheckResult = ref(null)

// 对话框状态
const taskDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const competitorDialogVisible = ref(false)
const processDialogVisible = ref(false)
const reviewerDialogVisible = ref(false)
const scoreDraftDialogVisible = ref(false)
const approvalDialogVisible = ref(false)
const currentTask = ref(null)
const currentApprovalItem = ref({})
const approvalMode = ref('submit')
const approvalType = ref({ type: 'project_review', typeName: '立项审批' })

// AI功能弹窗状态
const showCompetitionIntel = ref(false)
const showComplianceCheck = ref(false)
const showVersionControl = ref(false)
const showCollaboration = ref(false)
const showROIAnalysis = ref(false)
const showAutoTasks = ref(false)
const showMobileCard = ref(false)

// 结果录入表单
const noticeFileList = ref([])
const uploadAction = ref(isDemoMode ? '/api/upload' : '')
const uploadHeaders = computed(() => {
  const token = getAccessToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const resultForm = ref({
  result: '',
  amount: null,
  contractPeriod: null,
  skuCount: '',
  noticeFile: '',
  competitors: [],
  techHighlights: '',
  priceStrategy: '',
  customerFeedback: '',
  improvements: ''
})

const competitorForm = ref({
  name: '',
  skuCount: '',
  category: '',
  discount: '',
  payment: ''
})

// 标书编制流程相关
const activeProcessTab = ref('draft')
const bidProcess = ref({
  initiated: false,
  initiator: '',
  initiateTime: '',
  currentStep: 0,
  steps: {
    draft: { completed: false, time: '' },
    review: { completed: false, time: '' },
    seal: { completed: false, time: '' },
    submit: { completed: false, time: '' }
  },
  deliverables: []
})

// 初稿表单
const draftFileList = ref([])
const draftForm = ref({
  preparer: userStore.userName,
  templateId: '',
  files: [],
  remark: ''
})

// 评审相关
const reviewers = ref([])

const reviewerForm = ref({
  userId: '',
  role: ''
})

const availableReviewers = computed(() => {
  const existingIds = reviewers.value.map(r => r.id)
  return (userStore.users || []).filter(u => !existingIds.includes(u.id))
})

const currentApproval = computed(() => approvalHistory.value[0] || null)
const canApproveCurrent = computed(() => {
  const currentName = userStore.userName || userStore.currentUser?.name || ''
  return currentApproval.value?.currentApproverName === currentName || currentUserRole.value === 'admin'
})
const currentUserRole = computed(() => userStore.currentUser?.role || '')

// 用印表单
const sealFileList = ref([])
const sealForm = ref({
  sealTypes: [],
  reason: '',
  files: [],
  count: 1,
  expectedTime: ''
})

// 封装提交表单
const submitForm = ref({
  checkList: [],
  packageType: 'paper',
  sealRequirement: '',
  deliveryMethod: 'online',
  deliveryTime: '',
  deliveryAddress: '',
  remark: ''
})

// 模板数据
const templates = ref([])

// 任务模板 - 根据不同项目类型自动生成任务
const taskTemplates = {
  government: [],
  energy: [],
  traffic: [],
  default: []
}

// 交付物类型映射
const deliverableTypeMap = {
  qualification: '资质文件',
  technical: '技术方案',
  document: '文档',
  quotation: '报价单',
  other: '其他'
}

// 项目动态
const activities = ref([])

// AI检查相关
const aiChecking = ref(false)
const activeAITab = ref('compliance')
const aiResult = ref({
  compliance: null,
  quality: null,
  score: null
})
const hasAiCheckResult = computed(() => Boolean(
  aiResult.value.compliance ||
  aiResult.value.score ||
  project.value?.aiCheck?.compliance ||
  project.value?.aiCheck?.quality
))
const canRunAICheck = computed(() => true)
const showAICheckCard = computed(() => true)

// 智能助手面板相关
const assistantPanelVisible = ref(false)

const toggleAssistantPanel = () => {
  assistantPanelVisible.value = !assistantPanelVisible.value
}

// 智能助手功能处理函数
const handleOpenCompetitionIntel = () => {
  showCompetitionIntel.value = true
}

const handleOpenRoiAnalysis = () => {
  showROIAnalysis.value = true
}

const handleOpenScoreCoverage = () => {
  ElMessage.info('评分点覆盖请查看项目创建页Step 4')
}

const handleOpenComplianceCheck = () => {
  showComplianceCheck.value = true
}

const handleOpenVersionControl = () => {
  showVersionControl.value = true
}

const handleOpenCollaboration = () => {
  showCollaboration.value = true
}

const handleOpenAutoTasks = () => {
  showAutoTasks.value = true
}

const handleOpenMobileCard = () => {
  showMobileCard.value = true
}

// 项目费用 Mock 数据
const projectExpenses = ref([])

const expenseTotal = computed(() => {
  return projectExpenses.value.reduce((sum, item) => sum + item.amount, 0).toFixed(2)
})

const getExpenseTypeColor = (type) => {
  const map = {
    '保证金': 'warning',
    '标书费': 'success',
    '差旅费': 'info',
    '制作费': 'primary',
    '公证费': 'danger',
    '其他': ''
  }
  return map[type] || ''
}

const getExpenseStatusType = (status) => {
  const map = {
    'paid': 'success',
    'pending': 'warning'
  }
  return map[status] || ''
}

const getExpenseStatusLabel = (status) => {
  const map = {
    'paid': '已支付',
    'pending': '待支付'
  }
  return map[status] || status
}

const goToExpensePage = () => {
  router.push('/resource/expense')
}

const project = computed(() => {
  if (projectStore.currentProject) {
    return projectStore.currentProject
  }

  if (!isDemoMode) {
    return null
  }

  const routeProjectId = String(route.params.id || '')
  return getDemoProjectById(routeProjectId)
})

const dialogProjectId = computed(() => project.value?.id ?? route.params.id)
const canManageProjectTasks = computed(() => isDemoMode || isApiProject.value)
const canManageProjectDocuments = computed(() => isDemoMode || isApiProject.value)
const canSetProjectReminder = computed(() => isDemoMode || isApiProject.value)

const canSubmit = computed(() => {
  return project.value?.status === 'drafting' || project.value?.status === 'reviewing'
})

const canRecordResult = computed(() => {
  return project.value?.status === 'bidding'
})

// 状态映射函数
const getStatusType = (status) => {
  const typeMap = {
    drafting: 'info',
    reviewing: 'warning',
    bidding: 'primary',
    won: 'success',
    lost: 'danger',
    pending: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    drafting: '草稿中',
    reviewing: '评审中',
    bidding: '投标中',
    won: '已中标',
    lost: '未中标',
    pending: '待立项'
  }
  return textMap[status] || status
}

const getProgressStatus = (progress) => {
  if (progress === 100) return 'success'
  return undefined
}

const getPriorityType = (priority) => {
  const typeMap = { high: 'danger', medium: 'warning', low: 'info' }
  return typeMap[priority] || 'info'
}

const getPriorityText = (priority) => {
  const textMap = { high: '高', medium: '中', low: '低' }
  return textMap[priority] || priority
}

const getTaskStatusType = (status) => {
  const typeMap = { todo: 'info', doing: 'warning', done: 'success' }
  return typeMap[status] || 'info'
}

const getTaskStatusText = (status) => {
  const textMap = { todo: '待办', doing: '进行中', done: '已完成' }
  return textMap[status] || status
}

const getCheckTitle = (key) => {
  const titleMap = {
    compliance: '合规性检查',
    quality: '质量检查'
  }
  return titleMap[key] || key
}

// AI检查相关函数
const getBadgeType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 75) return 'warning'
  return 'danger'
}

const getProgressColor = (score) => {
  if (score >= 90) return '#67c23a'
  if (score >= 75) return '#e6a23c'
  if (score >= 60) return '#f56c6c'
  return '#909399'
}

const getScoreLevel = (score) => {
  if (score >= 90) return '优秀'
  if (score >= 80) return '良好'
  if (score >= 70) return '合格'
  return '不合格'
}

// 格式化分数，保留2位小数
const formatScore = (score) => {
  return Number(score).toFixed(2)
}

const getOverallScoreType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 70) return 'warning'
  return 'danger'
}

const mapComplianceIssues = (issues = []) => issues.map((issue) => ({
  category: issue?.ruleType || issue?.severity || '合规',
  item: issue?.ruleName || issue?.description || '检查项',
  status: issue?.passed === false ? 'fail' : 'pass',
  suggestion: issue?.recommendation || issue?.description || ''
}))

const buildScorePanel = (analysis = {}) => {
  const dimensions = Array.isArray(analysis?.dimensions) ? analysis.dimensions : []
  const findScore = (candidates) => {
    const matched = dimensions.find((dimension) =>
      candidates.some((candidate) => String(dimension?.dimensionName || '').includes(candidate))
    )
    return Number(matched?.score || 0)
  }

  return {
    total: Number(analysis?.overallScore || 0),
    tech: findScore(['技术能力', '技术方案', '技术']),
    business: findScore(['商务响应', '商务', '团队经验']),
    price: findScore(['价格竞争力', '价格', '报价']),
    qualification: findScore(['企业资质', '资质', '合规性']),
    comment: analysis?.summary || '',
    suggestions: dimensions.map((dimension) => dimension?.comments).filter(Boolean) }
}

// AI检查相关函数
const runAICheck = async () => {
  aiChecking.value = true

  if (isDemoMode) {
    await new Promise((resolve) => setTimeout(resolve, 1500))

    const mockComplianceIssues = [
      { category: '资质', item: '营业执照年检', status: 'pass', suggestion: '资质有效' },
      { category: '资质', item: 'ISO认证有效期', status: 'pass', suggestion: '认证在有效期内' },
      { category: '响应', item: '技术参数偏离表', status: 'fail', suggestion: '第3项技术参数未响应，建议补充说明' },
      { category: '响应', item: '商务条款应答', status: 'pass', suggestion: '响应完整' },
      { category: '格式', item: '目录页码', status: 'fail', suggestion: '第15页页码与目录不符' },
      { category: '格式', item: '签署盖章', status: 'pass', suggestion: '签署完整' }
    ]

    const passCount = mockComplianceIssues.filter((item) => item.status === 'pass').length
    const complianceScore = Math.round((passCount / mockComplianceIssues.length) * 100)

    const mockScore = {
      total: 87,
      tech: 90,
      business: 85,
      price: 82,
      qualification: 95,
      comment: '技术方案整体完整，商务应答较为充分，但部分技术参数与格式细节需进一步修订。\n建议优先修复技术偏离表和目录页码问题，确保投标文件质量。',
      suggestions: [
        '补充第3项技术参数的响应说明',
        '修正目录页码与正文页码一致性',
        '完善格式规范，统一字体和间距',
        '加强项目案例的针对性描述'
      ]
    }

    aiResult.value = {
      compliance: {
        score: complianceScore,
        issues: mockComplianceIssues
      },
      quality: null,
      score: mockScore
    }

    if (project.value) {
      project.value.aiCheck = {
        compliance: { score: complianceScore, issues: [], passed: complianceScore >= 80 },
        quality: null
      }
    }

    aiChecking.value = false
    ElMessage.success('AI检查完成')
    return
  }

  if (!isApiProject.value) {
    aiResult.value = { compliance: null, quality: null, score: null }
    ElMessage.warning('当前项目ID不是后端真实ID，无法执行AI检查')
    aiChecking.value = false
    return
  }

  try {
    const [complianceResponse, scoreResponse] = await Promise.all([
      complianceApi.getCheckResult(route.params.id),
      scoreAnalysisApi.getAnalysis(route.params.id)
    ])

    if (complianceResponse?.success === false || scoreResponse?.success === false) {
      const message = complianceResponse?.message || scoreResponse?.message || '当前未获取到 AI 检查结果'
      aiResult.value = { compliance: null, quality: null, score: null }
      ElMessage.warning(message)
      return
    }

    const complianceRecord = Array.isArray(complianceResponse?.data)
      ? complianceResponse.data[0]
      : complianceResponse?.data

    aiResult.value = {
      compliance: complianceRecord
        ? {
            score: Number(complianceRecord.overallScore || complianceRecord.riskScore || 0),
            issues: mapComplianceIssues(complianceRecord.issues || [])
          }
        : null,
      quality: null,
      score: scoreResponse?.data ? buildScorePanel(scoreResponse.data) : null
    }

    if (project.value) {
      project.value.aiCheck = {
        compliance: aiResult.value.compliance,
        quality: null
      }
    }

    ElMessage.success('AI检查完成')
  } catch (error) {
    console.error('AI检查失败:', error)
    aiResult.value = { compliance: null, quality: null, score: null }
    ElMessage.error(error?.response?.data?.message || error?.message || 'AI检查失败')
  } finally {
    aiChecking.value = false
  }
}

// 采纳建议
const handleAdoptSuggestion = (row, index) => {
  ElMessage.success(`已采纳建议：${row.original} → ${row.suggestion}`)
  // 从列表中移除已采纳的建议
  if (aiResult.value.quality?.errors) {
    aiResult.value.quality.errors.splice(index, 1)
  }
}

// 忽略建议
const handleIgnoreSuggestion = (index) => {
  if (aiResult.value.quality?.errors) {
    aiResult.value.quality.errors.splice(index, 1)
  }
  ElMessage.info('已忽略该建议')
}

// 流程相关函数
const getStepStatusText = (step) => {
  const stepData = bidProcess.value.steps[step]
  if (stepData.completed) return '已完成'
  if (bidProcess.value.currentStep > getStepOrder(step)) return '进行中'
  return '待开始'
}

const getStepOrder = (step) => {
  const order = { draft: 0, review: 1, seal: 2, submit: 3 }
  return order[step] ?? 0
}

const canOperateStep = (step) => {
  const stepOrder = getStepOrder(step)
  const currentStep = bidProcess.value.currentStep

  // 如果当前步骤已经完成，不能再操作
  if (bidProcess.value.steps[step].completed) return false

  // 如果是当前步骤或之前的步骤，可以操作
  return stepOrder <= currentStep
}

const getCurrentPhaseType = () => {
  const typeMap = ['info', 'primary', 'warning', 'success']
  return typeMap[bidProcess.value.currentStep] || 'info'
}

const getCurrentPhaseText = () => {
  const textMap = ['初稿编制', '内部评审', '用印申请', '封装提交', '已完成']
  return textMap[bidProcess.value.currentStep] || '初稿编制'
}

const getProcessProgress = () => {
  const completedSteps = Object.values(bidProcess.value.steps).filter(s => s.completed).length
  return Math.round((completedSteps / 4) * 100)
}

// 评审相关函数
const getReviewerRoleType = (role) => {
  const typeMap = { tech: 'primary', business: 'success', legal: 'warning', finance: 'danger' }
  return typeMap[role] || 'info'
}

const getReviewerRoleText = (role) => {
  const textMap = { tech: '技术评审', business: '商务评审', legal: '法务评审', finance: '财务评审' }
  return textMap[role] || role
}

const getReviewStatusType = (status) => {
  const typeMap = { pending: 'info', approved: 'success', rejected: 'danger' }
  return typeMap[status] || 'info'
}

const getReviewStatusText = (status) => {
  const textMap = { pending: '待评审', approved: '已通过', rejected: '未通过' }
  return textMap[status] || status
}

const getReviewProgress = () => {
  const reviewedCount = reviewers.value.filter(r => r.status !== 'pending').length
  return reviewers.value.length ? Math.round((reviewedCount / reviewers.value.length) * 100) : 0
}

const getReviewedCount = () => {
  return reviewers.value.filter(r => r.status !== 'pending').length
}

const canCompleteReview = () => {
  return reviewers.value.length > 0 &&
         reviewers.value.every(r => r.status === 'approved') &&
         !bidProcess.value.steps.review.completed
}

// 导航函数
const goBack = () => {
  router.push('/project')
}

// 资产检查相关方法
const goToSiteDetail = () => {
  if (assetCheckResult.value?.site?.id) {
    router.push(`/resource/bar/site/${assetCheckResult.value.site.id}`)
  }
}

const borrowUK = () => {
  if (assetCheckResult.value?.site?.id) {
    router.push({
      path: `/resource/bar/site/${assetCheckResult.value.site.id}`,
      query: {
        fromProjectId: String(route.params.id || ''),
        fromProjectName: project.value?.name || ''
      }
    })
  }
}

const viewSOP = () => {
  if (assetCheckResult.value?.site?.id) {
    router.push(`/resource/bar/sop/${assetCheckResult.value.site.id}`)
  }
}

const goToAssetManagement = () => {
  router.push('/resource/bar')
}

// 处理函数
const handleEdit = () => {
  // 跳转到标书编辑器
  router.push(`/document/editor/${route.params.id}`)
}

const handleSubmitApproval = () => {
  approvalMode.value = 'submit'
  currentApprovalItem.value = {
    projectId: project.value?.id,
    projectName: project.value?.name,
    title: `${project.value?.name || '当前项目'} - ${approvalType.value.typeName}`,
    description: `发起 ${project.value?.name || '当前项目'} 的审批流程。` }
  approvalDialogVisible.value = true
}

const handleApprovalSuccess = async () => {
  if (approvalMode.value === 'submit') {
    await projectStore.updateProject(route.params.id, { status: 'reviewing' })
  }
  await loadApprovalHistory(route.params.id)
}

const handleQuickApprove = () => {
  approvalMode.value = 'approve'
  currentApprovalItem.value = currentApproval.value || {}
  approvalDialogVisible.value = true
}

const handleQuickReject = () => {
  approvalMode.value = 'reject'
  currentApprovalItem.value = currentApproval.value || {}
  approvalDialogVisible.value = true
}

const handleRecordResult = () => {
  resultForm.value = {
    result: '',
    amount: null,
    contractPeriod: null,
    skuCount: '',
    noticeFile: '',
    competitors: [],
    techHighlights: '',
    priceStrategy: '',
    customerFeedback: '',
    improvements: ''
  }
  noticeFileList.value = []
  resultDialogVisible.value = true
}

const goToResultPage = () => {
  router.push('/resource/bid-result')
}

const handleUploadSuccess = (response, file) => {
  if (response.code === 200) {
    resultForm.value.noticeFile = response.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleUploadRemove = () => {
  resultForm.value.noticeFile = ''
}

const addCompetitor = () => {
  competitorForm.value = {
    name: '',
    skuCount: '',
    category: '',
    discount: '',
    payment: ''
  }
  competitorDialogVisible.value = true
}

const confirmAddCompetitor = () => {
  if (!competitorForm.value.name) {
    ElMessage.warning('请输入竞争对手公司名称')
    return
  }
  resultForm.value.competitors.push({ ...competitorForm.value })
  competitorDialogVisible.value = false
  ElMessage.success('添加成功')
}

const removeCompetitor = (index) => {
  resultForm.value.competitors.splice(index, 1)
}

const handleSaveResult = async () => {
  if (!resultForm.value.result) {
    ElMessage.warning('请选择投标结果')
    return
  }

  if (resultForm.value.result === 'won' && !resultForm.value.amount) {
    ElMessage.warning('请填写中标金额')
    return
  }

  try {
    await projectStore.updateProject(route.params.id, {
      status: resultForm.value.result,
      resultAmount: resultForm.value.amount,
      resultData: {
        contractPeriod: resultForm.value.contractPeriod,
        skuCount: resultForm.value.skuCount,
        noticeFile: resultForm.value.noticeFile,
        competitors: resultForm.value.competitors,
        techHighlights: resultForm.value.techHighlights,
        priceStrategy: resultForm.value.priceStrategy,
        customerFeedback: resultForm.value.customerFeedback,
        improvements: resultForm.value.improvements
      }
    })
    ElMessage.success('结果录入成功')
    resultDialogVisible.value = false
  } catch {
    ElMessage.error('结果录入失败')
  }
}

// 根据项目类型获取任务模板
const getTaskTemplateByProject = (project) => {
  const industry = project?.industry?.toLowerCase() || ''
  if (industry.includes('政府') || industry.includes('gov')) {
    return taskTemplates.government
  } else if (industry.includes('能源') || industry.includes('电力') || industry.includes('energy')) {
    return taskTemplates.energy
  } else if (industry.includes('交通') || industry.includes('地铁') || industry.includes('traffic')) {
    return taskTemplates.traffic
  }
  return taskTemplates.default
}

// 自动生成任务
const handleGenerateTasks = () => {
  if (!project.value) {
    ElMessage.warning('项目信息未加载')
    return
  }

  if (isApiProject.value) {
    scoreDraftDialogVisible.value = true
    return
  }

  const template = getTaskTemplateByProject(project.value)
  const deadline = new Date(project.value.deadline)

  const newTasks = template.map((taskTemplate, index) => {
    const taskDeadline = new Date(deadline)
    taskDeadline.setDate(taskDeadline.getDate() - taskTemplate.deadlineOffset)

    return {
      id: `${project.value.id}_T${String(index + 1).padStart(3, '0')}`,
      name: taskTemplate.name,
      description: taskTemplate.description,
      owner: taskTemplate.owner,
      status: 'todo',
      priority: taskTemplate.priority,
      deadline: taskDeadline.toISOString().split('T')[0],
      hasDeliverable: taskTemplate.needsDeliverable,
      deliverableType: taskTemplate.deliverableType || 'other',
      deliverables: []
    }
  })

  // 更新项目任务
  project.value.tasks = newTasks

  // 添加动态记录
  activities.value.unshift({
    id: Date.now(),
    user: userStore.userName,
    action: `根据项目模板自动生成了 ${newTasks.length} 个任务`,
    time: new Date().toLocaleString('zh-CN', { hour12: false })
  })

  ElMessage.success(`已自动生成 ${newTasks.length} 个任务`)
}

const handleScoreDraftGenerated = (tasks) => {
  if (!project.value) return

  project.value.tasks = tasks.map((task) => ({
    ...task,
    deliverables: Array.isArray(task.deliverables) ? task.deliverables : [],
    hasDeliverable: Boolean(task.hasDeliverable) }))

  activities.value.unshift({
    id: Date.now(),
    user: userStore.userName,
    action: `根据评分标准生成了 ${tasks.length} 个正式任务`,
    time: new Date().toLocaleString('zh-CN', { hour12: false })
  })
}

const handleAddTask = () => {
  if (!project.value) return

  const nextIndex = (project.value.tasks?.length || 0) + 1
  const dueDate = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000)
  const newTask = {
    name: `新增任务 ${nextIndex}`,
    owner: userStore.userName,
    assignee: userStore.userName,
    department: userStore.currentUser?.dept || '未配置部门',
    dueDate: dueDate.toISOString().split('T')[0],
    priority: 'medium',
    status: 'todo',
    deliverables: [],
    hasDeliverable: false }

  if (!isApiProject.value) {
    if (!Array.isArray(project.value.tasks)) {
      project.value.tasks = []
    }
    project.value.tasks.unshift({ id: `TASK_${Date.now()}`, ...newTask })
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: `新增了任务「${newTask.name}」`,
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success('已新增演示任务')
    return
  }

  projectsApi.createTask(route.params.id, {
    title: newTask.name,
    description: '',
    assigneeId: userStore.currentUser?.id || null,
    assigneeName: userStore.userName,
    assigneeDeptCode: userStore.currentUser?.deptCode || '',
    assigneeDeptName: userStore.currentUser?.dept || '',
    assigneeRoleCode: userStore.currentUser?.roleCode || '',
    assigneeRoleName: userStore.currentUser?.roleName || '',
    priority: 'MEDIUM',
    dueDate: dueDate.toISOString() }).then((result) => {
    if (!result?.success || !result?.data) {
      throw new Error(result?.message || '新增任务失败')
    }
    if (!Array.isArray(project.value.tasks)) {
      project.value.tasks = []
    }
    project.value.tasks.unshift({
      ...result.data,
      deliverables: [],
      hasDeliverable: false })
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: `新增了任务「${result.data.name}」`,
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success('任务已新增')
  }).catch((error) => {
    ElMessage.error(error.message || '新增任务失败')
  })
}

// 重置任务（用于演示）
const handleResetTasks = () => {
  ElMessageBox.confirm(
    '确认重置所有任务？这将清空当前项目的所有任务数据。',
    '重置确认',
    {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    if (project.value) {
      project.value.tasks = []

      // 添加动态记录
      activities.value.unshift({
        id: Date.now(),
        user: userStore.userName,
        action: '重置了项目任务',
        time: new Date().toLocaleString('zh-CN', { hour12: false })
      })

      ElMessage.success('任务已重置，可以重新拆解任务')
    }
  }).catch(() => {
    // 用户取消
  })
}

const handleTaskClick = (task) => {
  currentTask.value = task
  taskDialogVisible.value = true
}

const handleTaskStatusChange = async (task, newStatus) => {
  if (!task) return
  try {
    await projectStore.updateTaskStatus(route.params.id, task.id, newStatus)
    ElMessage.success('任务状态已更新')
    if (isApiProject.value) {
      loadProjectActivities(route.params.id)
    }
  } catch (error) {
    ElMessage.error(error?.message || '任务状态更新失败')
  }
}

// 添加交付物（委托给 projectStore，由 TaskBoard.vue 已调用过 API，此处仅同步 UI 状态）
const handleAddDeliverable = async (taskId, deliverable) => {
  if (!project.value?.tasks) return

  const task = project.value.tasks.find(t => t.id === taskId)
  if (task) {
    // 添加动态记录
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: `为任务"${task.name}"上传了交付物: ${deliverable.name}`,
      time: new Date().toLocaleString('zh-CN', { hour12: false })
    })

    ElMessage.success('交付物已添加')
  }
}

// 删除交付物（委托给 projectStore，由 TaskBoard.vue 已调用过 API）
const handleRemoveDeliverable = async (taskId, deliverableId) => {
  if (!project.value?.tasks) return

  const task = project.value.tasks.find(t => t.id === taskId)
  if (task) {
    // 添加动态记录
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: `删除了任务"${task.name}"的交付物`,
      time: new Date().toLocaleString('zh-CN', { hour12: false })
    })

    ElMessage.success('交付物已删除')
  }
}

// 提交至标书编写流程（调用后端校验 API）
const handleSubmitToDocument = async (projectId) => {
  if (!project.value?.tasks) return

  try {
    const result = await projectStore.submitToBidDocument(projectId)
    const data = result?.data

    if (!data?.accepted) {
      ElMessage.warning(data?.message || '提交校验未通过，请检查任务完成状态和交付物')
      return
    }

    ElMessageBox.confirm(
      `${data.message || '所有校验通过'}\n\n总任务数: ${data.totalTasks}\n已完成: ${data.completedTasks}\n有交付物: ${data.tasksWithDeliverables}`,
      '提交确认',
      {
        confirmButtonText: '确认提交',
        cancelButtonText: '取消',
        type: 'success'
      }
    ).then(() => {
      handleInitiateProcess()

      activities.value.unshift({
        id: Date.now(),
        user: userStore.userName,
        action: '所有任务已完成，提交至标书编写流程',
        time: new Date().toLocaleString('zh-CN', { hour12: false })
      })

      ElMessage.success('已提交至标书编写流程，可开始编制标书')
    }).catch(() => {
      // 用户取消
    })
  } catch (error) {
    ElMessage.error(error?.message || '提交失败')
  }
}

const handleUpload = async (file) => {
  if (!project.value) return false

  const docPayload = {
    name: file.name,
    uploader: userStore.userName,
    time: new Date().toLocaleString('zh-CN', { hour12: false }),
    size: `${Math.max(1, Math.round((file.size || 1024 * 1024) / 1024 / 1024))}MB` }

  if (!isApiProject.value) {
    if (!Array.isArray(project.value.documents)) {
      project.value.documents = []
    }
    project.value.documents.unshift({
      id: `DOC_${Date.now()}`,
      ...docPayload })
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: `上传了文档「${file.name}」`,
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success(`已上传演示文档：${file.name}`)
    return false
  }

  try {
    const formData = new FormData()
    formData.set('file', file)
    formData.set('name', file.name)
    formData.set('size', docPayload.size)
    formData.set('fileType', file.type || 'application/octet-stream')
    formData.set('uploaderId', String(userStore.currentUser?.id || ''))
    formData.set('uploaderName', userStore.userName)
    const result = await projectsApi.uploadDocument(route.params.id, formData)
    if (!result?.success || !result?.data) {
      throw new Error(result?.message || '上传文档失败')
    }
    if (!Array.isArray(project.value.documents)) {
      project.value.documents = []
    }
    project.value.documents.unshift(result.data)
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: `上传了文档「${result.data.name}」`,
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success(`已上传文档：${result.data.name}`)
  } catch (error) {
    ElMessage.error(error.message || '上传文档失败')
  }
  return false
}

const handleDownload = (doc) => {
  downloadTextFile(doc.name, `演示文档：${doc.name}\n项目：${project.value?.name || ''}\n上传者：${doc.uploader || ''}`)
  ElMessage.success(`已下载 ${doc.name}`)
}

const handleDeleteDoc = async (doc) => {
  try {
    await ElMessageBox.confirm('确认删除该文档？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (isApiProject.value && /^\d+$/.test(String(doc?.id))) {
      const result = await projectsApi.deleteDocument(route.params.id, doc.id)
      if (!result?.success) {
        throw new Error(result?.message || '删除文档失败')
      }
    }

    if (Array.isArray(project.value?.documents)) {
      project.value.documents = project.value.documents.filter(item => String(item.id) !== String(doc.id))
    }
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

const handleCreateDocument = async (docName, size = '1.2MB') => {
  const formData = new FormData()
  formData.set('name', docName)
  formData.set('size', size)
  formData.set('fileType', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')
  formData.set('uploaderId', String(userStore.currentUser?.id || ''))
  formData.set('uploaderName', userStore.userName)
  const result = await projectsApi.uploadDocument(route.params.id, formData)
  if (!result?.success || !result?.data) {
    throw new Error(result?.message || '新增项目文档失败')
  }
  if (!Array.isArray(project.value.documents)) {
    project.value.documents = []
  }
  project.value.documents.unshift(result.data)
  activities.value.unshift({
    id: Date.now(),
    user: userStore.userName,
    action: `新增了项目文档「${result.data.name}」`,
    time: new Date().toLocaleString('zh-CN', { hour12: false }) })
  ElMessage.success('项目文档已新增')
}

const handleAddDocument = async () => {
  if (!project.value) return
  const docName = `项目文档_${new Date().toLocaleDateString('zh-CN').replaceAll('/', '')}.docx`

  if (!isApiProject.value) {
    const mockDoc = {
      id: `DOC_${Date.now()}`,
      name: docName,
      uploader: userStore.userName,
      time: new Date().toLocaleString('zh-CN', { hour12: false }),
      size: '1.2MB' }
    if (!Array.isArray(project.value.documents)) {
      project.value.documents = []
    }
    project.value.documents.unshift(mockDoc)
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: `新增了项目文档「${mockDoc.name}」`,
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success('已新增演示文档')
    return
  }

  try {
    await handleCreateDocument(docName)
  } catch (error) {
    ElMessage.error(error.message || '新增项目文档失败')
  }
}

const handleShare = async () => {
  const fallbackLink = `${window.location.origin}/project/${route.params.id}`

  if (!isApiProject.value) {
    navigator.clipboard.writeText(fallbackLink).then(() => {
      ElMessage.success('项目链接已复制到剪贴板')
    }).catch(() => {
      ElMessage.success(`分享链接：${fallbackLink}`)
    })
    return
  }

  try {
    const result = await projectsApi.createShareLink(route.params.id, {
      createdBy: userStore.currentUser?.id || null,
      createdByName: userStore.userName,
      baseUrl: window.location.origin })
    const shareLink = result?.data?.url || fallbackLink
    await navigator.clipboard.writeText(shareLink)
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: '生成了项目分享链接',
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success('项目分享链接已复制到剪贴板')
  } catch (error) {
    ElMessage.error(error.message || '生成分享链接失败')
  }
}

const handleExport = () => {
  if (isApiProject.value) {
    collaborationApi.exports.createExport(route.params.id, {
      format: 'json',
      exportedBy: userStore.currentUser?.id || null,
      exportedByName: userStore.userName }).then((result) => {
      if (!result?.success || !result?.data) {
        ElMessage.error(result?.message || '导出资料失败')
        return
      }
      downloadTextFile(
        result.data.fileName,
        result.data.content || '',
        result.data.contentType || 'application/json;charset=utf-8'
      )
      activities.value.unshift({
        id: Date.now(),
        user: userStore.userName,
        action: `导出了项目资料「${result.data.fileName}」`,
        time: new Date().toLocaleString('zh-CN', { hour12: false }) })
      ElMessage.success(`已导出 ${result.data.fileName}`)
    }).catch(() => {
      ElMessage.error('导出资料失败')
    })
    return
  }

  const payload = {
    project: project.value,
    expenses: projectExpenses.value,
    activities: activities.value,
    exportedAt: new Date().toISOString() }
  downloadTextFile(
    `${project.value?.name || '项目资料'}_导出.json`,
    JSON.stringify(payload, null, 2),
    'application/json;charset=utf-8'
  )
  ElMessage.success(`已生成演示导出包：${project.value?.name || '项目资料'}_导出.json`)
}

const handleArchiveDocuments = async () => {
  if (!project.value) return

  if (!isApiProject.value) {
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: '归档了项目资料',
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success('已完成本地归档记录')
    return
  }

  try {
    const result = await collaborationApi.exports.archive(route.params.id, {
      archivedBy: userStore.currentUser?.id || null,
      archivedByName: userStore.userName,
      archiveReason: '项目资料整理完成，归档留存' })
    if (!result?.success || !result?.data) {
      throw new Error(result?.message || '归档资料失败')
    }
    if (project.value) {
      project.value.status = 'archived'
    }
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: `归档了项目资料（${result.data.archiveReason}）`,
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success('项目资料归档成功')
  } catch (error) {
    ElMessage.error(error.message || '归档资料失败')
  }
}

const handleSetReminder = async () => {
  const remindAt = new Date()
  remindAt.setDate(remindAt.getDate() + 1)
  remindAt.setHours(9, 0, 0, 0)

  if (!isApiProject.value) {
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: '设置了项目跟进提醒',
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success('已设置本地提醒，默认明天 09:00 提醒')
    return
  }

  try {
    const result = await projectsApi.createReminder(route.params.id, {
      title: '项目跟进提醒',
      message: `请跟进项目「${project.value?.name || ''}」`,
      remindAt: remindAt.toISOString(),
      createdBy: userStore.currentUser?.id || null,
      createdByName: userStore.userName,
      recipient: '项目负责人' })
    if (!result?.success || !result?.data) {
      throw new Error(result?.message || '设置提醒失败')
    }
    activities.value.unshift({
      id: Date.now(),
      user: userStore.userName,
      action: `设置了项目提醒：${result.data.title}`,
      time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    ElMessage.success('项目提醒已创建')
  } catch (error) {
    ElMessage.error(error.message || '设置提醒失败')
  }
}

const loadProjectWorkflowData = async (projectId) => {
  if (!project.value || !isApiProject.value) {
    return
  }

  const [taskResult, documentResult] = await Promise.all([
    projectsApi.getTasks(projectId),
    projectsApi.getDocuments(projectId),
  ])

  project.value.tasks = taskResult?.success && Array.isArray(taskResult.data)
    ? taskResult.data.map((task) => ({
      ...task,
      deliverables: task.deliverables || [],
      hasDeliverable: Boolean(task.hasDeliverable) }))
    : []

  project.value.documents = documentResult?.success && Array.isArray(documentResult.data)
    ? documentResult.data
    : []
}

// 标书编制流程相关处理函数
const handleInitiateProcess = () => {
  bidProcess.value = {
    initiated: true,
    initiator: userStore.userName,
    initiateTime: new Date().toLocaleString('zh-CN'),
    currentStep: 0,
    steps: {
      draft: { completed: false, time: '' },
      review: { completed: false, time: '' },
      seal: { completed: false, time: '' },
      submit: { completed: false, time: '' }
    },
    deliverables: []
  }
  ElMessage.success('标书编制流程已发起')
  processDialogVisible.value = true
  activeProcessTab.value = 'draft'
}

const handleDraftSubmit = () => {
  activeProcessTab.value = 'draft'
  processDialogVisible.value = true
}

const handleDraftFileSuccess = (response, file) => {
  draftForm.value.files.push({
    name: file.name,
    url: response.data?.url || file.url
  })
}

const ensureDemoUpload = () => {
  return true
}

const handleSaveDraft = () => {
  if (!draftForm.value.templateId) {
    ElMessage.warning('请选择使用模板')
    return
  }

  bidProcess.value.steps.draft.completed = true
  bidProcess.value.steps.draft.time = new Date().toLocaleString('zh-CN')
  bidProcess.value.currentStep = 1

  // 添加交付物
  bidProcess.value.deliverables.push({
    name: '标书初稿',
    type: '初稿',
    uploader: userStore.userName,
    time: new Date().toLocaleString('zh-CN')
  })

  ElMessage.success('初稿已保存')
  processDialogVisible.value = false
}

const handleReview = () => {
  if (!bidProcess.value.steps.draft.completed) {
    ElMessage.warning('请先完成初稿编制')
    return
  }
  activeProcessTab.value = 'review'
  processDialogVisible.value = true
}

const handleAddReviewer = () => {
  reviewerForm.value = {
    userId: '',
    role: ''
  }
  reviewerDialogVisible.value = true
}

const handleConfirmAddReviewer = () => {
  if (!reviewerForm.value.userId || !reviewerForm.value.role) {
    ElMessage.warning('请填写完整信息')
    return
  }

  const user = (userStore.users || []).find(u => u.id === reviewerForm.value.userId)
  if (!user) {
    ElMessage.warning('未找到评审人信息')
    return
  }
  reviewers.value.push({
    id: user.id,
    name: user.name,
    role: reviewerForm.value.role,
    status: 'pending',
    comment: '',
    reviewTime: ''
  })

  reviewerDialogVisible.value = false
  ElMessage.success('评审人已添加')
}

const handleRemindReviewer = (reviewer) => {
  ElMessage.success(`已提醒 ${reviewer.name} 进行评审`)
}

const handleRemoveReviewer = (index) => {
  reviewers.value.splice(index, 1)
}

const handleCompleteReview = () => {
  bidProcess.value.steps.review.completed = true
  bidProcess.value.steps.review.time = new Date().toLocaleString('zh-CN')
  bidProcess.value.currentStep = 2

  bidProcess.value.deliverables.push({
    name: '评审报告',
    type: '评审',
    uploader: userStore.userName,
    time: new Date().toLocaleString('zh-CN')
  })

  ElMessage.success('评审已完成')
  processDialogVisible.value = false
}

const handleSealApply = () => {
  if (!bidProcess.value.steps.review.completed) {
    ElMessage.warning('请先完成内部评审')
    return
  }
  activeProcessTab.value = 'seal'
  processDialogVisible.value = true
}

const handleSealFileSuccess = (response, file) => {
  sealForm.value.files.push({
    name: file.name,
    url: response.data?.url || file.url
  })
}

const handleSubmitSeal = () => {
  if (sealForm.value.sealTypes.length === 0) {
    ElMessage.warning('请选择用印类型')
    return
  }

  bidProcess.value.steps.seal.completed = true
  bidProcess.value.steps.seal.time = new Date().toLocaleString('zh-CN')
  bidProcess.value.currentStep = 3

  bidProcess.value.deliverables.push({
    name: '用印文件',
    type: '用印',
    uploader: userStore.userName,
    time: new Date().toLocaleString('zh-CN')
  })

  ElMessage.success('用印申请已提交')
  processDialogVisible.value = false
}

const handleSubmit = () => {
  if (!bidProcess.value.steps.seal.completed) {
    ElMessage.warning('请先完成用印申请')
    return
  }
  activeProcessTab.value = 'submit'
  processDialogVisible.value = true
}

const handleSubmitPackage = () => {
  if (submitForm.value.checkList.length < 6) {
    ElMessage.warning('请完成所有封装检查项')
    return
  }

  bidProcess.value.steps.submit.completed = true
  bidProcess.value.steps.submit.time = new Date().toLocaleString('zh-CN')
  bidProcess.value.currentStep = 4

  bidProcess.value.deliverables.push({
    name: '封装标书',
    type: '封装',
    uploader: userStore.userName,
    time: new Date().toLocaleString('zh-CN')
  })

  ElMessage.success('标书已封装提交')
  processDialogVisible.value = false

  // 更新项目状态
  projectStore.updateProject(route.params.id, { status: 'bidding' })
}

const handleDownloadDeliverable = (item) => {
  if (isApiProject.value && item.url && item.url !== '#') {
    const link = document.createElement('a')
    link.href = item.url
    link.download = item.name || ''
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    ElMessage.success(`已下载 ${item.name}`)
    return
  }

  downloadTextFile(item.name, `演示交付物：${item.name}\n类型：${item.type || ''}\n上传者：${item.uploader || ''}`)
  ElMessage.success(`已下载 ${item.name}`)
}

onMounted(async () => {
  loading.value = true
  const projectId = route.params.id

  // 从store获取项目
  await projectStore.getProjectById(projectId)

  const templateResult = await knowledgeApi.templates.getList()
  templates.value = templateResult?.success && Array.isArray(templateResult.data)
    ? templateResult.data
    : []

  if (!projectStore.currentProject) {
    projectStore.currentProject = isDemoMode
      ? getDemoProjectById(projectId)
      : null
  }
  await loadProjectWorkflowData(projectId)
  if (isDemoMode) {
    demoAutoTasks.value = getDemoAutoTasks()
    demoMobileCard.value = getDemoMobileCard(projectId)
  }

  // 加载资产台账数据
  await barStore.getSites()

  // 尝试根据项目信息匹配站点
  const currentProject = projectStore.currentProject
  if (currentProject) {
    // 根据客户名称或地区匹配站点
    const matchedSite = barStore.sites.find(site => {
      return site.region === currentProject.region ||
             currentProject.customer?.includes(site.name?.substring(0, 4))
    })

    if (matchedSite) {
      const result = await barStore.checkSiteCapability(matchedSite.name)
      if (result.found) {
        assetCheckResult.value = result
      }
    }
  }

  await loadApprovalHistory(projectId)

  loadProjectExpenses(projectId)
  loadProjectActivities(projectId)

  loading.value = false
})

async function loadApprovalHistory(projectId) {
  try {
    const result = await approvalApi.getProjectApprovals(projectId)
    approvalHistory.value = Array.isArray(result?.data) ? result.data : []
  } catch (error) {
    console.error('加载审批历史失败:', error)
    approvalHistory.value = []
  }
}

async function loadProjectExpenses(projectId) {
  if (!projectId) return
  try {
    const result = await feesApi.getByProject(projectId)
    const data = Array.isArray(result?.data) ? result.data : []
    projectExpenses.value = data.map(normalizeFeeForDisplay)
  } catch (error) {
    console.warn('Failed to load project expenses:', error)
    projectExpenses.value = []
  }
}

async function loadProjectActivities(projectId) {
  if (!projectId) return
  try {
    const result = await auditApi.getLogs({ module: 'project' })
    const items = Array.isArray(result?.data?.items) ? result.data.items : []
    // Backend has no per-target filter yet; filter client-side by audit log target field
    const projectIdStr = String(projectId)
    const filtered = items.filter(item => String(item?.target || '') === projectIdStr)
    activities.value = filtered.map(normalizeAuditLogForTimeline)
  } catch (error) {
    console.warn('Failed to load project activities:', error)
    activities.value = []
  }
}

function getApprovalStatusType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
  return map[String(status || '').toUpperCase()] || 'info'
}

function getApprovalStatusText(status) {
  const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消' }
  return map[String(status || '').toUpperCase()] || (status || '未知状态')
}
</script>

<style scoped>
.project-detail-page {
  padding: 20px;
}

.project-detail-container {
  flex-direction: column;
}

.page-header {
  margin-bottom: 20px;
  background: white;
  padding: 16px 20px;
  border-radius: 8px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.project-name {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.status-tag {
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions .el-button.is-active {
  background-color: #409eff;
  color: white;
  border-color: #409eff;
}

.detail-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.main-content {
  flex: 1;
  padding: 0;
  overflow: visible;
}

.right-sidebar {
  padding: 0;
  overflow: visible;
}

.info-card,
.approval-status-card,
.task-card,
.expense-card,
.process-card,
.document-card,
.ai-card,
.action-card,
.timeline-card {
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.card-title .el-button {
  margin-left: auto;
}

.progress-section {
  margin-top: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-label {
  font-size: 14px;
  color: #606266;
}

.progress-value {
  font-size: 16px;
  font-weight: 500;
  color: #409eff;
}

.current-approval {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.approval-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.approval-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.approval-info-item .label {
  font-size: 12px;
  color: #6b7280;
}

.approval-info-item .approver {
  color: #1d4ed8;
  font-weight: 600;
}

.approval-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 标书编制流程样式 */
.process-empty {
  padding: 20px 0;
}

.process-content {
  padding: 20px 0;
}

.process-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin: 30px 0;
  flex-wrap: wrap;
}

.process-detail {
  margin-top: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.deliverables-section {
  margin-top: 16px;
}

/* 费用汇总样式 */
.expense-total {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

/* 评审相关样式 */
.review-section {
  padding: 10px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
}

.progress-text {
  font-size: 14px;
  color: #606266;
}

.review-summary {
  margin-top: 20px;
}

/* AI智能检查面板样式 */
.ai-check-card {
  margin-bottom: 20px;
}

.ai-check-card :deep(.el-card__header) {
  padding: 12px 16px;
}

.ai-tabs {
  background: transparent;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-label .el-badge {
  margin-left: 4px;
}

.check-result,
.quality-result,
.ai-score {
  padding: 8px 0;
}

.score-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.score-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.score-level {
  margin: 12px 0 0;
  font-size: 16px;
  font-weight: 500;
  color: #409eff;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin: 16px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.issues-section :deep(.el-table) {
  margin-top: 8px;
}

.quality-summary {
  display: flex;
  justify-content: space-around;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
}

.quality-summary :deep(.el-statistic) {
  text-align: center;
}

.quality-summary :deep(.el-statistic__head) {
  font-size: 13px;
  color: #909399;
}

.quality-summary :deep(.el-statistic__content) {
  font-size: 24px;
  font-weight: 600;
}

.ai-score .score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.ai-score .score-header h4 {
  margin: 0;
  color: white;
  font-size: 15px;
}

.ai-score .score-header .el-tag {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

.score-descriptions {
  margin-bottom: 16px;
}

.score-item {
  display: flex;
  align-items: center;
}

.score-item :deep(.el-rate) {
  margin-right: 8px;
}

.comment-content {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  line-height: 1.8;
  color: #606266;
}

.comment-content p {
  margin: 0 0 8px 0;
}

.comment-content p:last-child {
  margin-bottom: 0;
}

.ai-suggestions {
  margin-top: 16px;
}

.suggestions-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.suggestions-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  background: #ecf5ff;
  border-radius: 6px;
  margin-bottom: 8px;
  color: #606266;
  line-height: 1.6;
}

.suggestions-list li:last-child {
  margin-bottom: 0;
}

.suggestions-list .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

/* 保留原有AI检查样式（兼容旧数据） */
.ai-check-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.check-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.check-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.check-title {
  font-weight: 500;
  color: #303133;
}

.check-score {
  margin-bottom: 8px;
  color: #606266;
}

.score-value {
  font-size: 20px;
  font-weight: 500;
  color: #67c23a;
}

.score-value.score-low {
  color: #f56c6c;
}

.check-issues {
  margin-top: 8px;
}

.issues-title {
  font-size: 13px;
  color: #f56c6c;
  margin-bottom: 4px;
}

.issues-list {
  margin: 0;
  padding-left: 20px;
}

.issues-list li {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

.check-no-issues {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #67c23a;
  font-size: 13px;
}

.action-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.activity-content {
  font-size: 14px;
}

.activity-user {
  font-weight: 500;
  color: #409eff;
}

.activity-action {
  color: #606266;
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .project-detail-container {
    padding: 12px;
  }

  /* 头部移动端优化 */
  .detail-header {
    flex-direction: column;
    gap: 12px;
    padding: 16px;
  }

  .header-main {
    width: 100%;
  }

  .detail-title {
    font-size: 18px;
  }

  .header-actions {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .header-actions .el-button {
    flex: 1;
    min-width: 80px;
  }

  /* 标签页移动端优化 */
  :deep(.el-tabs__nav-wrap) {
    padding: 0 8px;
  }

  :deep(.el-tabs__item) {
    font-size: 13px;
    padding: 0 10px;
  }

  /* 卡片移动端优化 */
  .info-card {
    margin-bottom: 12px;
  }

  .info-card :deep(.el-card__header) {
    padding: 12px 16px;
  }

  .info-card :deep(.el-card__body) {
    padding: 16px;
  }

  /* 描述列表移动端优化 */
  :deep(.el-descriptions) {
    font-size: 12px;
  }

  :deep(.el-descriptions__label) {
    width: 90px !important;
  }

  /* 表格移动端优化 */
  :deep(.el-table) {
    font-size: 12px;
  }

  :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }

  :deep(.el-table__cell) {
    padding: 8px 4px;
  }

  /* 时间线移动端优化 */
  :deep(.el-timeline-item__wrapper) {
    padding-left: 0;
  }

  /* 对话框移动端优化 */
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }

  :deep(.el-dialog__body) {
    padding: 16px;
  }

  /* 操作列表移动端优化 */
  .action-list {
    grid-template-columns: 1fr;
  }

  /* 标签移动端优化 */
  .detail-tags {
    flex-wrap: wrap;
  }

  .detail-tags .el-tag {
    margin-bottom: 6px;
  }

  /* 进度条移动端优化 */
  :deep(.el-progress__text) {
    font-size: 12px !important;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .header-actions .el-button,
  .action-list .el-button {
    min-height: 44px;
  }

  .info-card:active {
    background: #f5f7fa;
  }
}

/* 资产检查 Tab 样式 */
.asset-check-tab {
  padding: 16px 0;
}

.asset-check-content {
  background: #f9fafb;
  border-radius: 8px;
  padding: 20px;
}

.asset-check-content .check-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.asset-check-content .site-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.asset-check-content .site-link {
  color: #909399;
  font-size: 13px;
  text-decoration: none;
}

.asset-check-content .site-link:hover {
  color: #409eff;
}

.asset-check-content .check-items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.asset-check-content .check-item-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.asset-check-content .check-item-card .item-icon {
  flex-shrink: 0;
}

.asset-check-content .check-item-card .icon-success {
  color: #67c23a;
  font-size: 28px;
}

.asset-check-content .check-item-card .icon-error {
  color: #f56c6c;
  font-size: 28px;
}

.asset-check-content .check-item-card .icon-warning {
  color: #e6a23c;
  font-size: 28px;
}

.asset-check-content .check-item-card .icon-user {
  color: #909399;
  font-size: 24px;
}

.asset-check-content .check-item-card .item-content {
  flex: 1;
}

.asset-check-content .check-item-card .item-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.asset-check-content .check-item-card .item-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.asset-check-content .check-item-card .item-value .phone {
  color: #909399;
  font-size: 12px;
}

.asset-check-content .check-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.asset-check-content .check-actions .el-button {
  flex: 1;
  min-width: 100px;
}
</style>
