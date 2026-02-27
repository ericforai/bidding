<template>
  <div class="case-container">
    <div class="page-header">
      <h2 class="page-title">案例库</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="handleAdd">
          新增案例
        </el-button>
      </div>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索案例名称、客户、亮点"
            clearable
            :prefix-icon="Search"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item label="行业">
          <el-select v-model="searchForm.industry" placeholder="全部行业" clearable>
            <el-option
              v-for="item in industries"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年份">
          <el-select v-model="searchForm.year" placeholder="全部年份" clearable>
            <el-option
              v-for="item in years"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额范围">
          <el-select v-model="searchForm.amount" placeholder="全部金额" clearable>
            <el-option
              v-for="item in amountRanges"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 标签筛选 -->
      <div class="tags-filter">
        <span class="tags-label">常用标签：</span>
        <el-tag
          v-for="tag in commonTags"
          :key="tag"
          :type="selectedTags.includes(tag) ? '' : 'info'"
          :effect="selectedTags.includes(tag) ? 'dark' : 'plain'"
          class="tag-item"
          @click="toggleTag(tag)"
        >
          {{ tag }}
        </el-tag>
      </div>
    </el-card>

    <!-- 案例卡片列表 -->
    <div class="case-list" v-loading="loading">
      <div
        v-for="item in filteredCases"
        :key="item.id"
        class="case-card"
        @click="handleView(item)"
      >
        <div class="case-card-header">
          <h3 class="case-title">{{ item.title }}</h3>
          <el-tag :type="getYearTagType(item.year)" size="small">
            {{ item.year }}年
          </el-tag>
        </div>

        <div class="case-card-body">
          <div class="case-info-row">
            <span class="info-label">
              <el-icon><OfficeBuilding /></el-icon>
              客户
            </span>
            <span class="info-value">{{ item.customer }}</span>
          </div>

          <div class="case-info-row">
            <span class="info-label">
              <el-icon><Coin /></el-icon>
              金额
            </span>
            <span class="info-value amount">{{ formatAmount(item.amount) }}</span>
          </div>

          <div class="case-info-row">
            <span class="info-label">
              <el-icon><Location /></el-icon>
              地区
            </span>
            <span class="info-value">{{ item.location }}</span>
          </div>

          <div class="case-info-row">
            <span class="info-label">
              <el-icon><Calendar /></el-icon>
              时间
            </span>
            <span class="info-value">{{ item.period }}</span>
          </div>
        </div>

        <div class="case-tags">
          <el-tag
            v-for="tag in item.tags"
            :key="tag"
            size="small"
            :type="getTagType(tag)"
            effect="plain"
          >
            {{ tag }}
          </el-tag>
        </div>

        <div class="case-highlights">
          <div class="highlights-title">
            <el-icon><Star /></el-icon>
            项目亮点
          </div>
          <ul class="highlights-list">
            <li v-for="(highlight, index) in item.highlights.slice(0, 3)" :key="index">
              {{ highlight }}
            </li>
          </ul>
        </div>

        <div class="case-card-footer">
          <span class="view-count">
            <el-icon><View /></el-icon>
            {{ item.viewCount }}
          </span>
          <span class="use-count">
            <el-icon><DocumentCopy /></el-icon>
            引用 {{ item.useCount }} 次
          </span>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="filteredCases.length === 0"
        description="暂无案例数据"
        :image-size="120"
      />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="pagination.total > 0">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[12, 24, 48]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 添加案例对话框 -->
    <el-dialog
      v-model="addCaseDialogVisible"
      title="添加案例"
      width="800px"
      class="add-case-dialog"
    >
      <el-tabs v-model="addCaseTab">
        <!-- 从项目转案例 -->
        <el-tab-pane label="从项目转案例" name="fromProject">
          <el-form :model="caseForm" label-width="120px" :rules="caseFormRules" ref="caseFormRef">
            <el-form-item label="选择已中标项目" prop="sourceProjectId">
              <el-select
                v-model="caseForm.sourceProjectId"
                placeholder="选择已中标项目"
                filterable
                @change="loadProjectInfo"
                style="width: 100%"
              >
                <el-option
                  v-for="project in wonProjects"
                  :key="project.id"
                  :label="project.name"
                  :value="project.id"
                >
                  <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span>{{ project.name }}</span>
                    <span style="color: #909399; font-size: 12px;">{{ project.customer }} | {{ project.budget }}万</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <el-divider content-position="left">项目信息（自动填充）</el-divider>

            <el-form-item label="案例标题" prop="title">
              <el-input v-model="caseForm.title" placeholder="将自动从项目名称生成" />
            </el-form-item>

            <el-form-item label="客户行业" prop="industry">
              <el-select v-model="caseForm.industry" placeholder="请选择行业" style="width: 100%">
                <el-option label="政府" value="政府" />
                <el-option label="能源" value="能源" />
                <el-option label="交通" value="交通" />
                <el-option label="金融" value="金融" />
                <el-option label="制造业" value="制造业" />
                <el-option label="教育" value="教育" />
                <el-option label="医疗" value="医疗" />
                <el-option label="互联网" value="互联网" />
              </el-select>
            </el-form-item>

            <el-form-item label="项目金额" prop="amount">
              <el-input-number v-model="caseForm.amount" :min="0" :step="10" />
              <span style="margin-left: 8px">万元</span>
            </el-form-item>

            <el-form-item label="实施周期" prop="period">
              <el-date-picker
                v-model="caseForm.period"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%"
                value-format="YYYY.MM"
              />
            </el-form-item>

            <el-form-item label="所在地区" prop="location">
              <el-input v-model="caseForm.location" placeholder="如：浙江杭州" />
            </el-form-item>

            <el-form-item label="标签" prop="tags">
              <el-select v-model="caseForm.tags" multiple filterable allow-create placeholder="选择或输入标签" style="width: 100%">
                <el-option label="智慧办公" value="智慧办公" />
                <el-option label="OA" value="OA" />
                <el-option label="信创" value="信创" />
                <el-option label="系统集成" value="系统集成" />
                <el-option label="智慧城市" value="智慧城市" />
                <el-option label="大数据" value="大数据" />
                <el-option label="云计算" value="云计算" />
                <el-option label="物联网" value="物联网" />
                <el-option label="人工智能" value="人工智能" />
                <el-option label="信息安全" value="信息安全" />
              </el-select>
            </el-form-item>

            <!-- 结构化归档内容 -->
            <el-divider content-position="left">结构化归档</el-divider>

            <el-form-item label="项目概述" prop="description">
              <el-input
                v-model="caseForm.description"
                type="textarea"
                :rows="3"
                placeholder="简要描述项目背景、目标和主要内容..."
              />
            </el-form-item>

            <el-form-item label="技术亮点" prop="techHighlights">
              <el-input
                v-model="caseForm.techHighlights"
                type="textarea"
                :rows="4"
                placeholder="记录本次项目的技术亮点和创新点，每行一个..."
              />
            </el-form-item>

            <el-form-item label="报价策略" prop="priceStrategy">
              <el-input
                v-model="caseForm.priceStrategy"
                type="textarea"
                :rows="3"
                placeholder="记录报价策略和定价逻辑..."
              />
            </el-form-item>

            <el-form-item label="成功关键因素">
              <el-checkbox-group v-model="caseForm.successFactors">
                <el-checkbox label="技术优势">技术优势</el-checkbox>
                <el-checkbox label="价格合理">价格合理</el-checkbox>
                <el-checkbox label="客户关系">客户关系</el-checkbox>
                <el-checkbox label="交付能力">交付能力</el-checkbox>
                <el-checkbox label="品牌影响力">品牌影响力</el-checkbox>
                <el-checkbox label="响应速度">响应速度</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="经验教训" prop="lessons">
              <el-input
                v-model="caseForm.lessons"
                type="textarea"
                :rows="3"
                placeholder="记录经验教训和改进建议..."
              />
            </el-form-item>

            <el-form-item label="附件">
              <el-upload
                v-model:file-list="caseForm.attachments"
                action="#"
                :auto-upload="false"
                multiple
                :on-change="handleAttachmentChange"
                :on-remove="handleAttachmentRemove"
              >
                <el-button type="primary" plain>上传项目文档</el-button>
                <template #tip>
                  <div style="color: #909399; font-size: 12px; margin-top: 4px">
                    支持上传技术方案、报价单、合同等相关文档
                  </div>
                </template>
              </el-upload>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 手动录入 -->
        <el-tab-pane label="手动录入" name="manual">
          <el-form :model="manualCaseForm" label-width="120px" :rules="caseFormRules" ref="manualCaseFormRef">
            <el-form-item label="案例标题" prop="title">
              <el-input v-model="manualCaseForm.title" placeholder="请输入案例标题" />
            </el-form-item>

            <el-form-item label="客户名称" prop="customer">
              <el-input v-model="manualCaseForm.customer" placeholder="请输入客户名称" />
            </el-form-item>

            <el-form-item label="客户行业" prop="industry">
              <el-select v-model="manualCaseForm.industry" placeholder="请选择行业" style="width: 100%">
                <el-option label="政府" value="政府" />
                <el-option label="能源" value="能源" />
                <el-option label="交通" value="交通" />
                <el-option label="金融" value="金融" />
                <el-option label="制造业" value="制造业" />
                <el-option label="教育" value="教育" />
                <el-option label="医疗" value="医疗" />
                <el-option label="互联网" value="互联网" />
              </el-select>
            </el-form-item>

            <el-form-item label="项目金额" prop="amount">
              <el-input-number v-model="manualCaseForm.amount" :min="0" :step="10" />
              <span style="margin-left: 8px">万元</span>
            </el-form-item>

            <el-form-item label="实施周期" prop="period">
              <el-date-picker
                v-model="manualCaseForm.period"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%"
                value-format="YYYY.MM"
              />
            </el-form-item>

            <el-form-item label="所在地区" prop="location">
              <el-input v-model="manualCaseForm.location" placeholder="如：浙江杭州" />
            </el-form-item>

            <el-form-item label="标签" prop="tags">
              <el-select v-model="manualCaseForm.tags" multiple filterable allow-create placeholder="选择或输入标签" style="width: 100%">
                <el-option label="智慧办公" value="智慧办公" />
                <el-option label="OA" value="OA" />
                <el-option label="信创" value="信创" />
                <el-option label="系统集成" value="系统集成" />
                <el-option label="智慧城市" value="智慧城市" />
                <el-option label="大数据" value="大数据" />
                <el-option label="云计算" value="云计算" />
                <el-option label="物联网" value="物联网" />
                <el-option label="人工智能" value="人工智能" />
                <el-option label="信息安全" value="信息安全" />
              </el-select>
            </el-form-item>

            <el-divider content-position="left">案例详情</el-divider>

            <el-form-item label="项目概述" prop="description">
              <el-input
                v-model="manualCaseForm.description"
                type="textarea"
                :rows="3"
                placeholder="简要描述项目背景、目标和主要内容..."
              />
            </el-form-item>

            <el-form-item label="技术亮点" prop="techHighlights">
              <el-input
                v-model="manualCaseForm.techHighlights"
                type="textarea"
                :rows="4"
                placeholder="记录本次项目的技术亮点和创新点，每行一个..."
              />
            </el-form-item>

            <el-form-item label="报价策略" prop="priceStrategy">
              <el-input
                v-model="manualCaseForm.priceStrategy"
                type="textarea"
                :rows="3"
                placeholder="记录报价策略和定价逻辑..."
              />
            </el-form-item>

            <el-form-item label="成功关键因素">
              <el-checkbox-group v-model="manualCaseForm.successFactors">
                <el-checkbox label="技术优势">技术优势</el-checkbox>
                <el-checkbox label="价格合理">价格合理</el-checkbox>
                <el-checkbox label="客户关系">客户关系</el-checkbox>
                <el-checkbox label="交付能力">交付能力</el-checkbox>
                <el-checkbox label="品牌影响力">品牌影响力</el-checkbox>
                <el-checkbox label="响应速度">响应速度</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="经验教训" prop="lessons">
              <el-input
                v-model="manualCaseForm.lessons"
                type="textarea"
                :rows="3"
                placeholder="记录经验教训和改进建议..."
              />
            </el-form-item>

            <el-form-item label="附件">
              <el-upload
                v-model:file-list="manualCaseForm.attachments"
                action="#"
                :auto-upload="false"
                multiple
                :on-change="handleAttachmentChange"
                :on-remove="handleAttachmentRemove"
              >
                <el-button type="primary" plain>上传项目文档</el-button>
                <template #tip>
                  <div style="color: #909399; font-size: 12px; margin-top: 4px">
                    支持上传技术方案、报价单、合同等相关文档
                  </div>
                </template>
              </el-upload>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="addCaseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCase" :loading="saving">保存案例</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="currentCase?.title"
      width="900px"
      class="case-detail-dialog"
    >
      <div class="case-detail" v-if="currentCase">
        <div class="detail-header">
          <div class="detail-meta">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="客户名称">
                {{ currentCase.customer }}
              </el-descriptions-item>
              <el-descriptions-item label="项目金额">
                {{ formatAmount(currentCase.amount) }}
              </el-descriptions-item>
              <el-descriptions-item label="项目年份">
                {{ currentCase.year }}年
              </el-descriptions-item>
              <el-descriptions-item label="所在地区">
                {{ currentCase.location }}
              </el-descriptions-item>
              <el-descriptions-item label="实施周期">
                {{ currentCase.period }}
              </el-descriptions-item>
              <el-descriptions-item label="所属行业">
                <el-tag size="small">{{ currentCase.industry }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">项目概述</h4>
          <p class="section-content">{{ currentCase.description }}</p>
        </div>

        <div class="detail-section">
          <h4 class="section-title">项目亮点</h4>
          <ul class="highlight-list">
            <li v-for="(highlight, index) in currentCase.highlights" :key="index">
              <el-icon class="highlight-icon"><Check /></el-icon>
              {{ highlight }}
            </li>
          </ul>
        </div>

        <div class="detail-section">
          <h4 class="section-title">相关标签</h4>
          <div class="detail-tags">
            <el-tag
              v-for="tag in currentCase.tags"
              :key="tag"
              :type="getTagType(tag)"
              effect="plain"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">相关附件</h4>
          <div class="detail-files">
            <el-button type="primary" link :icon="Download">案例详情.pdf</el-button>
            <el-button type="primary" link :icon="Download">项目图片.zip</el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" :icon="DocumentCopy">引用此案例</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Plus,
  Search,
  OfficeBuilding,
  Coin,
  Location,
  Calendar,
  Star,
  View,
  DocumentCopy,
  Download,
  Check
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 搜索表单
const searchForm = reactive({
  keyword: '',
  industry: '',
  year: '',
  amount: ''
})

// 选中的标签
const selectedTags = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 12,
  total: 0
})

// 加载状态
const loading = ref(false)

// 对话框
const detailDialogVisible = ref(false)
const addCaseDialogVisible = ref(false)
const addCaseTab = ref('fromProject')
const currentCase = ref(null)
const caseFormRef = ref(null)
const manualCaseFormRef = ref(null)
const saving = ref(false)

// 行业映射
const industryMap = {
  '政府': 'government',
  '能源': 'energy',
  '交通': 'transport',
  '金融': 'finance',
  '制造业': 'manufacturing',
  '教育': 'education',
  '医疗': 'healthcare',
  '互联网': 'internet'
}

// 案例表单
const caseForm = reactive({
  sourceProjectId: null,
  title: '',
  customer: '',
  industry: '',
  amount: null,
  period: null,
  location: '',
  tags: [],
  description: '',
  techHighlights: '',
  priceStrategy: '',
  successFactors: [],
  lessons: '',
  attachments: []
})

// 手动录入表单
const manualCaseForm = reactive({
  title: '',
  customer: '',
  industry: '',
  amount: null,
  period: null,
  location: '',
  tags: [],
  description: '',
  techHighlights: '',
  priceStrategy: '',
  successFactors: [],
  lessons: '',
  attachments: []
})

// 表单验证规则
const caseFormRules = {
  sourceProjectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  title: [{ required: true, message: '请输入案例标题', trigger: 'blur' }],
  customer: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  industry: [{ required: true, message: '请选择行业', trigger: 'change' }],
  amount: [{ required: true, message: '请输入项目金额', trigger: 'blur' }],
  location: [{ required: true, message: '请输入所在地区', trigger: 'blur' }],
  description: [{ required: true, message: '请输入项目概述', trigger: 'blur' }],
  techHighlights: [{ required: true, message: '请输入技术亮点', trigger: 'blur' }]
}

// Mock 已中标项目数据
const wonProjects = [
  {
    id: 1,
    name: '某市智慧城市一体化管理平台',
    customer: '杭州市人民政府',
    budget: 3850,
    location: '浙江杭州',
    industry: '政府',
    status: '已中标',
    signDate: '2023.06',
    deliveryDate: '2024.12',
    techStack: ['Vue3', 'Java', 'MySQL', 'Redis'],
    description: '通过构建城市大数据中心，整合公安、交通、城管、环保等多部门数据资源，打造"城市大脑"',
    priceInfo: {
      total: 3850,
      cost: 2800,
      profit: 1050,
      strategy: '采用价值定价法，强调数字化转型带来的行政成本节约'
    },
    highlights: [
      '整合12个委办局数据，实现数据共享与业务协同',
      '构建城市运行监测中心，实时响应城市事件',
      '年均节省行政成本约2000万元'
    ],
    successFactors: ['技术优势', '品牌影响力', '客户关系'],
    lessons: '前期需求调研需更充分，部分接口对接时间超出预期',
    attachments: ['技术方案.pdf', '报价单.xlsx', '合同.pdf']
  },
  {
    id: 2,
    name: '省级银行核心业务系统升级改造',
    customer: '浙江省农村信用社联合社',
    budget: 5600,
    location: '浙江杭州',
    industry: '金融',
    status: '已中标',
    signDate: '2023.01',
    deliveryDate: '2024.06',
    techStack: ['Spring Cloud', 'Oracle', 'Redis', 'Kafka'],
    description: '基于分布式架构重构核心业务系统，采用微服务设计理念',
    priceInfo: {
      total: 5600,
      cost: 4200,
      profit: 1400,
      strategy: '竞标定价，参考同行报价下浮5%'
    },
    highlights: [
      '实现系统双活架构，可用性达到99.99%',
      '日均处理交易量从500万笔提升至2000万笔',
      '通过央行等保四级认证'
    ],
    successFactors: ['技术优势', '交付能力', '价格合理'],
    lessons: '测试环境与生产环境差异导致部分问题未提前发现',
    attachments: ['架构设计.pdf', '测试报告.docx']
  },
  {
    id: 3,
    name: '国家电网省级电力调度自动化系统',
    customer: '国网浙江省电力有限公司',
    budget: 4200,
    location: '浙江杭州',
    industry: '能源',
    status: '已中标',
    signDate: '2022.03',
    deliveryDate: '2023.12',
    techStack: ['C++', 'Qt', 'PostgreSQL', 'InfluxDB'],
    description: '建设新一代电网调度自动化系统，融合物联网、大数据、人工智能技术',
    priceInfo: {
      total: 4200,
      cost: 3100,
      profit: 1100,
      strategy: '成本加成定价，考虑行业特殊性增加15%风险溢价'
    },
    highlights: [
      '实现电网运行实时监控与智能预警',
      '故障定位准确率提升至95%',
      '支撑新能源消纳，提升电网调峰能力'
    ],
    successFactors: ['技术优势', '品牌影响力'],
    lessons: '与现有系统集成的复杂度被低估，需要预留更多缓冲时间',
    attachments: ['需求规格说明书.pdf', '接口文档.docx']
  },
  {
    id: 4,
    name: '省交通运输厅综合交通管控平台',
    customer: '浙江省交通运输厅',
    budget: 2800,
    location: '浙江杭州',
    industry: '交通',
    status: '已中标',
    signDate: '2023.05',
    deliveryDate: '2024.10',
    techStack: ['Vue3', 'Python', 'PostgreSQL', 'Flink'],
    description: '构建覆盖全省的综合交通管控平台，整合各类交通数据资源',
    priceInfo: {
      total: 2800,
      cost: 2100,
      profit: 700,
      strategy: '价值定价，强调拥堵改善带来的社会效益'
    },
    highlights: [
      '整合高速公路、普通国省道监控视频3万余路',
      '实现拥堵自动识别与预警，准确率达90%',
      '节假日拥堵指数下降15%'
    ],
    successFactors: ['技术优势', '价格合理'],
    lessons: '视频处理性能优化需提前规划，后期成本较高',
    attachments: ['实施方案.pdf', '演示视频.mp4']
  },
  {
    id: 5,
    name: '三甲医院智慧医疗信息系统',
    customer: '浙江大学医学院附属第一医院',
    budget: 1950,
    location: '浙江杭州',
    industry: '医疗',
    status: '已中标',
    signDate: '2022.08',
    deliveryDate: '2023.11',
    techStack: ['.NET', 'SQL Server', 'React', 'DICOM'],
    description: '按照国家智慧医院建设标准，建设以电子病历为核心的医院信息系统',
    priceInfo: {
      total: 1950,
      cost: 1500,
      profit: 450,
      strategy: '渗透定价，争取成为区域标杆案例'
    },
    highlights: [
      '实现电子病历全覆盖，达到五级水平',
      '建设互联网医院，月均线上问诊超10万人次',
      '通过国家医疗健康信息互联互通标准化成熟度五级乙等测评'
    ],
    successFactors: ['技术优势', '客户关系', '响应速度'],
    lessons: '医院内部系统对接复杂度大，需加强与IT部门沟通',
    attachments: ['技术方案.pdf', '互联互通测评报告.pdf']
  },
  {
    id: 6,
    name: '双一流大学智慧校园平台',
    customer: '浙江大学',
    budget: 2200,
    location: '浙江杭州',
    industry: '教育',
    status: '已中标',
    signDate: '2022.05',
    deliveryDate: '2023.09',
    techStack: ['Vue3', 'Java', 'MySQL', 'Elasticsearch'],
    description: '建设以"一个中心、三大平台、N个应用"为架构的智慧校园平台',
    priceInfo: {
      total: 2200,
      cost: 1650,
      profit: 550,
      strategy: '竞争定价，略低于主要竞争对手'
    },
    highlights: [
      '构建"一网通办"师生服务平台，服务事项300余项',
      '建设智慧教室200间，支持混合式教学',
      '入选教育部教育信息化优秀案例'
    ],
    successFactors: ['技术优势', '价格合理', '品牌影响力'],
    lessons: '学生用户量大，需重点关注系统性能和并发处理',
    attachments: ['建设方案.pdf', '用户手册.docx']
  },
  {
    id: 7,
    name: '大型制造集团MES系统实施',
    customer: '万向集团公司',
    budget: 1680,
    location: '浙江杭州',
    industry: '制造业',
    status: '已中标',
    signDate: '2023.03',
    deliveryDate: '2024.08',
    techStack: ['C#', 'WPF', 'SQL Server', 'MQTT'],
    description: '基于工业互联网架构，建设覆盖生产全过程的MES系统',
    priceInfo: {
      total: 1680,
      cost: 1250,
      profit: 430,
      strategy: '成本加成定价，加成率约35%'
    },
    highlights: [
      '实现生产全过程数字化管理',
      '生产效率提升20%，产品不良率下降35%',
      '实现生产设备预测性维护，设备利用率提升15%'
    ],
    successFactors: ['技术优势', '交付能力'],
    lessons: '生产现场环境复杂，设备接入难度大，需提前调研',
    attachments: ['MES方案.pdf', '实施计划.xlsx']
  },
  {
    id: 8,
    name: '头部电商平台数据中台建设',
    customer: '网易严选',
    budget: 3200,
    location: '浙江杭州',
    industry: '互联网',
    status: '已中标',
    signDate: '2022.06',
    deliveryDate: '2023.08',
    techStack: ['Hadoop', 'Spark', 'Hive', 'Doris'],
    description: '建设企业级数据中台，构建数据采集、存储、计算、服务的一体化能力',
    priceInfo: {
      total: 3200,
      cost: 2400,
      profit: 800,
      strategy: '价值定价，强调数据资产对业务的价值'
    },
    highlights: [
      '整合内部20余个业务系统数据',
      '构建统一数据资产管理体系',
      '支撑智能推荐，点击转化率提升25%'
    ],
    successFactors: ['技术优势', '品牌影响力'],
    lessons: '数据质量治理是长期工作，需建立持续运营机制',
    attachments: ['数据中台架构.pdf', '数据规范.docx']
  }
]

// 加载项目信息到表单
const loadProjectInfo = (projectId) => {
  const project = wonProjects.find(p => p.id === projectId)
  if (project) {
    caseForm.title = project.name + ' - 成功案例'
    caseForm.customer = project.customer
    caseForm.industry = project.industry
    caseForm.amount = project.budget
    caseForm.location = project.location
    caseForm.period = [project.signDate, project.deliveryDate]

    // 根据项目技术栈自动生成标签
    const autoTags = [...project.techStack]
    if (project.industry === '政府') autoTags.push('智慧城市', '政务服务')
    if (project.industry === '金融') autoTags.push('金融科技', '高可用')
    if (project.industry === '能源') autoTags.push('工业软件', '实时监控')
    if (project.industry === '交通') autoTags.push('智慧交通', '视频分析')
    caseForm.tags = [...new Set(autoTags)]

    caseForm.description = project.description
    caseForm.techHighlights = project.highlights.join('\n')
    caseForm.priceStrategy = `报价策略：${project.priceInfo.strategy}\n总报价：${project.priceInfo.total}万元\n成本：${project.priceInfo.cost}万元\n利润：${project.priceInfo.profit}万元`
    caseForm.successFactors = [...project.successFactors]
    caseForm.lessons = project.lessons

    // 模拟附件列表
    caseForm.attachments = project.attachments.map(name => ({
      name: name,
      url: '#',
      status: 'success'
    }))
  }
}

// 附件变化处理
const handleAttachmentChange = (file, fileList) => {
  // 文件变化时更新列表
}

// 附件移除处理
const handleAttachmentRemove = (file, fileList) => {
  // 文件移除时更新列表
}

// 保存案例
const saveCase = async () => {
  try {
    // 根据当前tab选择对应的表单进行验证
    const isFromProject = addCaseTab.value === 'fromProject'
    const formRef = isFromProject ? caseFormRef : manualCaseFormRef
    const formData = isFromProject ? caseForm : manualCaseForm

    if (!formRef.value) return

    await formRef.value.validate()

    saving.value = true

    // 构建案例数据
    const periodText = formData.period && formData.period.length === 2
      ? `${formData.period[0]} - ${formData.period[1]}`
      : '2023.01 - 2024.12'

    const highlights = formData.techHighlights.split('\n').filter(h => h.trim())

    const newCase = {
      id: Date.now(),
      title: formData.title,
      customer: formData.customer,
      amount: formData.amount,
      year: new Date().getFullYear(),
      location: formData.location,
      period: periodText,
      industry: industryMap[formData.industry] || 'government',
      tags: formData.tags,
      highlights: highlights,
      description: formData.description,
      // 存储结构化归档信息（实际项目中应存储到后端）
      archivedInfo: {
        techHighlights: formData.techHighlights,
        priceStrategy: formData.priceStrategy,
        successFactors: formData.successFactors,
        lessons: formData.lessons,
        attachments: formData.attachments.map(f => f.name)
      },
      viewCount: 0,
      useCount: 0
    }

    // 模拟保存到后端
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 添加到案例列表
    cases.value.unshift(newCase)

    // 保存到本地存储（模拟持久化）
    const archivedCases = JSON.parse(localStorage.getItem('archivedCases') || '[]')
    archivedCases.push(newCase)
    localStorage.setItem('archivedCases', JSON.stringify(archivedCases))

    ElMessage.success('案例保存成功')

    // 关闭对话框并重置表单
    addCaseDialogVisible.value = false
    resetCaseForm()

  } catch (error) {
    if (error !== false) { // 表单验证失败时会返回false
      console.error('保存案例失败:', error)
      ElMessage.error('保存失败，请重试')
    }
  } finally {
    saving.value = false
  }
}

// 重置案例表单
const resetCaseForm = () => {
  // 重置从项目转案例表单
  Object.assign(caseForm, {
    sourceProjectId: null,
    title: '',
    customer: '',
    industry: '',
    amount: null,
    period: null,
    location: '',
    tags: [],
    description: '',
    techHighlights: '',
    priceStrategy: '',
    successFactors: [],
    lessons: '',
    attachments: []
  })

  // 重置手动录入表单
  Object.assign(manualCaseForm, {
    title: '',
    customer: '',
    industry: '',
    amount: null,
    period: null,
    location: '',
    tags: [],
    description: '',
    techHighlights: '',
    priceStrategy: '',
    successFactors: [],
    lessons: '',
    attachments: []
  })

  addCaseTab.value = 'fromProject'
  caseFormRef.value?.clearValidate()
  manualCaseFormRef.value?.clearValidate()
}

// 行业选项
const industries = [
  { label: '政府机构', value: 'government' },
  { label: '金融银行', value: 'finance' },
  { label: '能源电力', value: 'energy' },
  { label: '交通运输', value: 'transport' },
  { label: '医疗卫生', value: 'healthcare' },
  { label: '教育科研', value: 'education' },
  { label: '制造业', value: 'manufacturing' },
  { label: '互联网', value: 'internet' }
]

// 年份选项
const currentYear = new Date().getFullYear()
const years = Array.from({ length: 10 }, (_, i) => ({
  label: `${currentYear - i}年`,
  value: currentYear - i
}))

// 金额范围
const amountRanges = [
  { label: '100万以下', value: '0-100' },
  { label: '100-500万', value: '100-500' },
  { label: '500-1000万', value: '500-1000' },
  { label: '1000万以上', value: '1000+' }
]

// 常用标签
const commonTags = [
  '智慧城市',
  '大数据',
  '云计算',
  '物联网',
  '人工智能',
  '信息安全',
  '数字化',
  '移动应用',
  '系统集成',
  '软件开发'
]

// Mock 案例
const mockCases = [
  {
    id: 1,
    title: '某市智慧城市一体化管理平台',
    customer: '杭州市人民政府',
    amount: 3850,
    year: 2024,
    location: '浙江杭州',
    period: '2023.06 - 2024.12',
    industry: 'government',
    tags: ['智慧城市', '大数据', '云计算', '物联网'],
    highlights: [
      '整合12个委办局数据，实现数据共享与业务协同',
      '构建城市运行监测中心，实时响应城市事件',
      '年均节省行政成本约2000万元',
      '获得2024年度数字化转型创新案例奖'
    ],
    description: '本项目通过构建城市大数据中心，整合公安、交通、城管、环保等多部门数据资源，打造"城市大脑"，实现城市治理智能化、服务便捷化、决策科学化。项目包含数据中台、业务中台、可视化平台三大核心模块。',
    viewCount: 1280,
    useCount: 56
  },
  {
    id: 2,
    title: '省级银行核心业务系统升级改造',
    customer: '浙江省农村信用社联合社',
    amount: 5600,
    year: 2024,
    location: '浙江杭州',
    period: '2023.01 - 2024.06',
    industry: 'finance',
    tags: ['金融', '核心系统', '高可用', '信息安全'],
    highlights: [
      '实现系统双活架构，可用性达到99.99%',
      '日均处理交易量从500万笔提升至2000万笔',
      '支持线上线下一体化业务办理',
      '通过央行等保四级认证'
    ],
    description: '基于分布式架构重构核心业务系统，采用微服务设计理念，构建高可用、高性能、可扩展的新一代银行核心系统，支撑业务快速发展和数字化转型需求。',
    viewCount: 980,
    useCount: 42
  },
  {
    id: 3,
    title: '国家电网省级电力调度自动化系统',
    customer: '国网浙江省电力有限公司',
    amount: 4200,
    year: 2023,
    location: '浙江杭州',
    period: '2022.03 - 2023.12',
    industry: 'energy',
    tags: ['能源电力', '工业软件', '实时监控', '数据分析'],
    highlights: [
      '实现电网运行实时监控与智能预警',
      '故障定位准确率提升至95%',
      '支撑新能源消纳，提升电网调峰能力',
      '获得国家电网公司科技进步一等奖'
    ],
    description: '建设新一代电网调度自动化系统，融合物联网、大数据、人工智能技术，实现电网运行状态的全面感知、智能分析和协同控制，保障电网安全稳定运行。',
    viewCount: 860,
    useCount: 38
  },
  {
    id: 4,
    title: '省交通运输厅综合交通管控平台',
    customer: '浙江省交通运输厅',
    amount: 2800,
    year: 2024,
    location: '浙江杭州',
    period: '2023.05 - 2024.10',
    industry: 'transport',
    tags: ['智慧交通', '视频分析', '大数据', 'GIS'],
    highlights: [
      '整合高速公路、普通国省道监控视频3万余路',
      '实现拥堵自动识别与预警，准确率达90%',
      '节假日拥堵指数下降15%',
      '支撑应急救援快速响应，平均响应时间缩短40%'
    ],
    description: '构建覆盖全省的综合交通管控平台，整合各类交通数据资源，实现路网运行监测、智能研判分析、应急指挥调度等功能，提升交通治理能力。',
    viewCount: 720,
    useCount: 31
  },
  {
    id: 5,
    title: '三甲医院智慧医疗信息系统',
    customer: '浙江大学医学院附属第一医院',
    amount: 1950,
    year: 2023,
    location: '浙江杭州',
    period: '2022.08 - 2023.11',
    industry: 'healthcare',
    tags: ['智慧医疗', '电子病历', '移动医疗', '互联网医院'],
    highlights: [
      '实现电子病历全覆盖，达到五级水平',
      '建设互联网医院，月均线上问诊超10万人次',
      '患者平均候诊时间缩短30%',
      '通过国家医疗健康信息互联互通标准化成熟度五级乙等测评'
    ],
    description: '按照国家智慧医院建设标准，建设以电子病历为核心的医院信息系统，集成HIS、LIS、PACS等系统，打造智慧医疗、智慧服务、智慧管理三位一体的智慧医院体系。',
    viewCount: 690,
    useCount: 28
  },
  {
    id: 6,
    title: '双一流大学智慧校园平台',
    customer: '浙江大学',
    amount: 2200,
    year: 2023,
    location: '浙江杭州',
    period: '2022.05 - 2023.09',
    industry: 'education',
    tags: ['智慧校园', '教育信息化', '大数据', '云计算'],
    highlights: [
      '构建"一网通办"师生服务平台，服务事项300余项',
      '建设智慧教室200间，支持混合式教学',
      '学生个性化学习平台，服务7万余名学生',
      '入选教育部教育信息化优秀案例'
    ],
    description: '建设以"一个中心、三大平台、N个应用"为架构的智慧校园平台，打造泛在、智能、融合的智慧教育环境，支撑学校"双一流"建设。',
    viewCount: 650,
    useCount: 25
  },
  {
    id: 7,
    title: '大型制造集团MES系统实施',
    customer: '万向集团公司',
    amount: 1680,
    year: 2024,
    location: '浙江杭州',
    period: '2023.03 - 2024.08',
    industry: 'manufacturing',
    tags: ['智能制造', 'MES', '工业互联网', '数字化转型'],
    highlights: [
      '实现生产全过程数字化管理',
      '生产效率提升20%，产品不良率下降35%',
      '实现生产设备预测性维护，设备利用率提升15%',
      '支撑柔性制造，满足小批量多品种生产需求'
    ],
    description: '基于工业互联网架构，建设覆盖生产全过程的MES系统，实现计划排程、生产执行、质量管控、设备管理的一体化管理，支撑企业智能制造转型。',
    viewCount: 580,
    useCount: 22
  },
  {
    id: 8,
    title: '头部电商平台数据中台建设',
    customer: '网易严选',
    amount: 3200,
    year: 2023,
    location: '浙江杭州',
    period: '2022.06 - 2023.08',
    industry: 'internet',
    tags: ['大数据', '数据中台', '云计算', '推荐算法'],
    highlights: [
      '整合内部20余个业务系统数据',
      '构建统一数据资产管理体系',
      '支撑智能推荐，点击转化率提升25%',
      '数据开发效率提升60%'
    ],
    description: '建设企业级数据中台，构建数据采集、存储、计算、服务的一体化能力，为业务系统提供统一的数据服务，支撑业务创新和精细化运营。',
    viewCount: 920,
    useCount: 35
  },
  {
    id: 9,
    title: '政务服务中心一网通办平台',
    customer: '宁波市政务服务办公室',
    amount: 1450,
    year: 2024,
    location: '浙江宁波',
    period: '2023.04 - 2024.06',
    industry: 'government',
    tags: ['政务服务', '一网通办', '电子证照', '人脸识别'],
    highlights: [
      '入驻事项2000余项，一网通办率达92%',
      '实现电子证照互通互认，免提交比例达70%',
      '推出"刷脸办""掌上办"，便利度大幅提升',
      '群众满意度达98.5%'
    ],
    description: '建设集网上办事大厅、移动端APP、自助终端于一体的政务服务平台，实现政务服务"一网、一门、一次"办理，打造数字政府建设标杆。',
    viewCount: 540,
    useCount: 19
  },
  {
    id: 10,
    title: '大型房地产集团ERP系统建设',
    customer: '绿城中国控股有限公司',
    amount: 2800,
    year: 2022,
    location: '浙江杭州',
    period: '2021.09 - 2022.12',
    industry: 'manufacturing',
    tags: ['ERP', '企业管理', '业财一体', '移动办公'],
    highlights: [
      '实现项目全生命周期管理',
      '业财一体化，财务结算效率提升50%',
      '集团管控能力显著增强，风险可控',
      '支持移动办公，随时随地处理业务'
    ],
    description: '建设面向大型房地产集团的ERP系统，覆盖项目开发、成本管理、财务管理、人力资源等核心业务领域，实现集团一体化管控。',
    viewCount: 480,
    useCount: 16
  },
  {
    id: 11,
    title: '省公安厅大数据情报平台',
    customer: '浙江省公安厅',
    amount: 4500,
    year: 2023,
    location: '浙江杭州',
    period: '2022.02 - 2023.10',
    industry: 'government',
    tags: ['公共安全', '大数据', '人工智能', '视频分析'],
    highlights: [
      '整合公安内部及外部数据资源50余类',
      '构建智能情报分析模型，研判效率提升80%',
      '支撑重大活动安保，实现精准防控',
      '获公安部科技创新一等奖'
    ],
    description: '建设公安大数据情报平台，整合多源数据资源，构建智能分析模型，支撑情报研判、案件侦办、治安防控等工作，提升公安机关核心战斗力。',
    viewCount: 380,
    useCount: 12
  },
  {
    id: 12,
    title: '环境监测大数据平台建设',
    customer: '浙江省生态环境厅',
    amount: 1680,
    year: 2024,
    location: '浙江杭州',
    period: '2023.02 - 2024.05',
    industry: 'government',
    tags: ['环保', '大数据', '物联网', 'GIS'],
    highlights: [
      '接入监测站点2000余个，监测数据实时采集',
      '实现污染溯源分析，准确率达85%',
      '支撑环境应急响应，响应时间缩短60%',
      '助力打好污染防治攻坚战'
    ],
    description: '建设环境监测大数据平台，整合大气、水、土壤等环境监测数据，构建环境质量分析、污染溯源、应急指挥等功能，支撑环境管理决策。',
    viewCount: 320,
    useCount: 10
  }
]

const cases = ref([])

// 过滤后的案例
const filteredCases = computed(() => {
  let result = cases.value

  if (searchForm.keyword) {
    const keyword = searchForm.keyword.toLowerCase()
    result = result.filter(item =>
      item.title.toLowerCase().includes(keyword) ||
      item.customer.toLowerCase().includes(keyword) ||
      item.highlights.some(h => h.toLowerCase().includes(keyword))
    )
  }

  if (searchForm.industry) {
    result = result.filter(item => item.industry === searchForm.industry)
  }

  if (searchForm.year) {
    result = result.filter(item => item.year === searchForm.year)
  }

  if (searchForm.amount) {
    const [min, max] = searchForm.amount.split('-').map(v => parseInt(v.replace('+', '')))
    result = result.filter(item => {
      if (max) {
        return item.amount >= min && item.amount < max
      } else {
        return item.amount >= min
      }
    })
  }

  if (selectedTags.value.length > 0) {
    result = result.filter(item =>
      selectedTags.value.some(tag => item.tags.includes(tag))
    )
  }

  pagination.total = result.length

  const start = (pagination.page - 1) * pagination.pageSize
  const end = start + pagination.pageSize

  return result.slice(start, end)
})

// 年份标签类型
const getYearTagType = (year) => {
  const currentYear = new Date().getFullYear()
  if (year === currentYear) return 'success'
  if (year === currentYear - 1) return ''
  return 'info'
}

// 标签类型
const getTagType = (tag) => {
  const types = {
    '智慧城市': 'success',
    '大数据': 'warning',
    '云计算': 'primary',
    '物联网': 'success',
    '人工智能': 'danger',
    '信息安全': 'warning',
    '数字化': 'primary',
    '移动应用': 'info',
    '系统集成': '',
    '软件开发': ''
  }
  return types[tag] || 'info'
}

// 格式化金额
const formatAmount = (amount) => {
  if (amount >= 10000) {
    return `${(amount / 10000).toFixed(1)} 亿元`
  }
  return `${amount} 万元`
}

// 切换标签
const toggleTag = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.industry = ''
  searchForm.year = ''
  searchForm.amount = ''
  selectedTags.value = []
  pagination.page = 1
}

// 新增
const handleAdd = () => {
  addCaseDialogVisible.value = true
}

// 查看
const handleView = (item) => {
  currentCase.value = item
  detailDialogVisible.value = true
}

// 分页变化
const handlePageChange = (page) => {
  pagination.page = page
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
}

onMounted(() => {
  cases.value = mockCases
  pagination.total = mockCases.length
})
</script>

<style scoped lang="scss">
.case-container {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
      margin: 0;
    }
  }

  .search-card {
    margin-bottom: 20px;

    .tags-filter {
      margin-top: 16px;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;

      .tags-label {
        font-size: 14px;
        color: #606266;
        margin-right: 4px;
      }

      .tag-item {
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          opacity: 0.8;
        }
      }
    }
  }

  .case-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
    gap: 20px;
    min-height: 400px;

    .case-card {
      background: #fff;
      border: 1px solid #e4e7ed;
      border-radius: 8px;
      padding: 20px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      }

      .case-card-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 16px;

        .case-title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          margin: 0;
          flex: 1;
          line-height: 1.5;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
      }

      .case-card-body {
        margin-bottom: 16px;

        .case-info-row {
          display: flex;
          align-items: center;
          margin-bottom: 10px;
          font-size: 14px;

          &:last-child {
            margin-bottom: 0;
          }

          .info-label {
            display: flex;
            align-items: center;
            gap: 4px;
            color: #909399;
            width: 70px;
            flex-shrink: 0;
          }

          .info-value {
            color: #606266;
            flex: 1;

            &.amount {
              color: #f56c6c;
              font-weight: 600;
            }
          }
        }
      }

      .case-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin-bottom: 16px;
      }

      .case-highlights {
        background: #f5f7fa;
        border-radius: 6px;
        padding: 12px;
        margin-bottom: 16px;

        .highlights-title {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 14px;
          font-weight: 600;
          color: #409eff;
          margin-bottom: 8px;
        }

        .highlights-list {
          margin: 0;
          padding-left: 16px;
          font-size: 13px;
          color: #606266;
          line-height: 1.8;

          li {
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }
        }
      }

      .case-card-footer {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 16px;
        padding-top: 12px;
        border-top: 1px solid #e4e7ed;
        font-size: 13px;
        color: #909399;

        span {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}

.case-detail-dialog {
  .case-detail {
    .detail-header {
      margin-bottom: 20px;
    }

    .detail-section {
      margin-bottom: 24px;

      &:last-child {
        margin-bottom: 0;
      }

      .section-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 12px;
      }

      .section-content {
        font-size: 14px;
        color: #606266;
        line-height: 1.8;
        margin: 0;
      }

      .highlight-list {
        margin: 0;
        padding-left: 0;
        list-style: none;

        li {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          font-size: 14px;
          color: #606266;
          line-height: 2;
        }

        .highlight-icon {
          color: #67c23a;
          flex-shrink: 0;
          margin-top: 6px;
        }
      }

      .detail-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .detail-files {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
      }
    }
  }

  /* 移动端响应式样式 */
  @media (max-width: 768px) {
    .case-page {
      padding: 12px;
    }

    .page-header {
      margin-bottom: 12px;
    }

    .page-title {
      font-size: 20px;
    }

    .filter-card :deep(.el-form) {
      display: block;
    }

    .filter-card :deep(.el-form-item) {
      display: block;
      margin-right: 0;
      margin-bottom: 12px;
    }

    .filter-card :deep(.el-input),
    .filter-card :deep(.el-select) {
      width: 100% !important;
    }

    /* 表格移动端优化 */
    .table-card :deep(.el-table) {
      font-size: 12px;
    }

    .table-card :deep(.el-table__body-wrapper) {
      overflow-x: auto;
    }

    /* 对话框移动端优化 */
    :deep(.el-dialog) {
      width: 95% !important;
      margin: 0 auto;
    }

    :deep(.el-dialog__body) {
      padding: 16px;
    }

    /* 案例卡片移动端优化 */
    .case-grid {
      grid-template-columns: 1fr;
    }

    .case-card {
      padding: 12px;
    }

    /* 分页移动端优化 */
    .pagination-wrapper {
      justify-content: center;
    }

    .pagination-wrapper :deep(.el-pagination) {
      flex-wrap: wrap;
      justify-content: center;
    }

    .pagination-wrapper :deep(.el-pagination__sizes),
    .pagination-wrapper :deep(.el-pagination__jump) {
      display: none;
    }
  }

  /* 触摸设备优化 */
  @media (hover: none) and (pointer: coarse) {
    .case-card {
      min-height: 100px;
    }

    .case-card:active {
      background: #f5f7fa;
    }

    .el-button {
      min-height: 44px;
    }
  }
}

.add-case-dialog {
  :deep(.el-dialog__body) {
    max-height: 70vh;
    overflow-y: auto;
  }

  :deep(.el-divider) {
    margin: 20px 0;
  }

  :deep(.el-divider__text) {
    font-weight: 600;
    color: #409eff;
  }

  :deep(.el-select-dropdown__item) {
    height: auto;
    padding: 8px 20px;
  }

  .option-extra {
    color: #909399;
    font-size: 12px;
    margin-left: 8px;
  }
}
</style>
