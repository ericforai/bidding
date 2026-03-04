<template>
  <div class="bid-result-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">投标结果闭环</h2>
        <p class="page-subtitle">结果登记、通知书上传、竞争对手分析</p>
      </div>
    </div>

    <!-- 功能卡片区域 -->
    <el-row :gutter="20">
      <!-- 内部系统同步 -->
      <el-col :span="6">
        <el-card class="feature-card" shadow="hover">
          <div class="card-icon system">
            <el-icon><Connection /></el-icon>
          </div>
          <h3 class="card-title">内部系统同步</h3>
          <p class="card-desc">从ERP/CRM获取中标结果</p>
          <el-button type="primary" plain @click="handleSyncInternal">
            <el-icon><Refresh /></el-icon>
            立即同步
          </el-button>
          <div class="card-stats">
            <span>最近同步: {{ lastSyncTime }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- 自动抓取 -->
      <el-col :span="6">
        <el-card class="feature-card" shadow="hover">
          <div class="card-icon spider">
            <el-icon><Monitor /></el-icon>
          </div>
          <h3 class="card-title">公开信息抓取</h3>
          <p class="card-desc">自动抓取政府采购网等公示</p>
          <el-button type="success" plain @click="handleAutoFetch">
            <el-icon><Search /></el-icon>
            开始抓取
          </el-button>
          <div class="card-stats">
            <el-tag size="small" type="warning">{{ pendingCount }} 条待确认</el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 上传提醒 -->
      <el-col :span="6">
        <el-card class="feature-card" shadow="hover">
          <div class="card-icon upload">
            <el-icon><Bell /></el-icon>
          </div>
          <h3 class="card-title">上传提醒</h3>
          <p class="card-desc">提醒销售上传通知书/报告</p>
          <el-button type="warning" plain @click="handleSendRemind">
            <el-icon><Message /></el-icon>
            发送提醒
          </el-button>
          <div class="card-stats">
            <span>{{ uploadPending }} 人未上传</span>
          </div>
        </el-card>
      </el-col>

      <!-- 竞争对手分析 -->
      <el-col :span="6">
        <el-card class="feature-card" shadow="hover">
          <div class="card-icon analysis">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <h3 class="card-title">竞争对手分析</h3>
          <p class="card-desc">SKU/品类/折扣/账期报表</p>
          <el-button type="danger" plain @click="handleShowReport">
            <el-icon><Document /></el-icon>
            查看报表
          </el-button>
          <div class="card-stats">
            <span>{{ competitorCount }} 条记录</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待确认抓取结果 -->
    <el-card class="result-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>待确认的抓取结果</span>
          <el-button type="primary" size="small" @click="handleConfirmAll">批量确认</el-button>
        </div>
      </template>
      <el-table :data="fetchResults" stripe>
        <el-table-column type="selection" width="55" />
        <el-table-column prop="source" label="来源" width="150" />
        <el-table-column prop="projectName" label="项目名称" min-width="200" />
        <el-table-column prop="result" label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === 'won' ? 'success' : 'info'" size="small">
              {{ row.result === 'won' ? '中标' : '未中标' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column prop="fetchTime" label="抓取时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleConfirm(row)">确认</el-button>
            <el-button size="small" type="danger" link @click="handleIgnore(row)">忽略</el-button>
            <el-button size="small" link @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传提醒记录 -->
    <el-card class="reminder-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>上传提醒记录</span>
          <el-button type="primary" size="small" @click="handleSendRemindAll">全部提醒</el-button>
        </div>
      </template>
      <el-table :data="reminderRecords" stripe>
        <el-table-column prop="projectName" label="项目名称" min-width="180" />
        <el-table-column prop="owner" label="负责人" width="100" />
        <el-table-column prop="type" label="提醒类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.type === 'notice'" type="warning" size="small">中标通知书</el-tag>
            <el-tag v-else type="info" size="small">分析报告</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'pending'" type="danger" size="small">未上传</el-tag>
            <el-tag v-else-if="row.status === 'uploaded'" type="success" size="small">已上传</el-tag>
            <el-tag v-else type="info" size="small">已提醒</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remindTime" label="提醒时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" size="small" type="primary" link @click="handleRemindAgain(row)">
              再次提醒
            </el-button>
            <el-button v-else size="small" link disabled>已完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 竞争对手报表弹窗 -->
    <el-dialog v-model="reportVisible" title="竞争对手分析报表" width="900px">
      <el-table :data="competitorData" border>
        <el-table-column prop="company" label="竞争对手" width="150" fixed="left" />
        <el-table-column prop="skuCount" label="SKU数量" width="100" align="center" />
        <el-table-column prop="category" label="品类" width="120" />
        <el-table-column prop="discount" label="折扣" width="90" align="center" />
        <el-table-column prop="payment" label="账期" width="120" />
        <el-table-column prop="winRate" label="中标率" width="100" align="center" />
        <el-table-column prop="projectCount" label="中标项目数" width="120" align="center" />
        <el-table-column prop="trend" label="趋势" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.trend === 'up'" style="color: #67c23a"><CaretTop /></el-icon>
            <el-icon v-else-if="row.trend === 'down'" style="color: #f56c6c"><CaretBottom /></el-icon>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Connection, Monitor, Bell, DataAnalysis, Refresh, Search, Message,
  Document, CaretTop, CaretBottom
} from '@element-plus/icons-vue'

// Mock 数据
const lastSyncTime = ref('2025-03-02 10:30')
const pendingCount = ref(3)
const uploadPending = ref(5)
const competitorCount = ref(28)

const fetchResults = ref([
  {
    id: 1,
    source: '中国政府采购网',
    projectName: '深圳市某医院信息化项目',
    result: 'won',
    amount: '156万',
    fetchTime: '2025-03-03 09:15:00'
  },
  {
    id: 2,
    source: '广东省公共资源交易中心',
    projectName: '广州市教育局云平台采购',
    result: 'lost',
    amount: '-',
    fetchTime: '2025-03-03 08:30:00'
  },
  {
    id: 3,
    source: '军队采购网',
    projectName: '某部队后勤管理系统',
    result: 'won',
    amount: '89万',
    fetchTime: '2025-03-02 16:45:00'
  }
])

const reminderRecords = ref([
  {
    id: 1,
    projectName: '深圳地铁自动化系统',
    owner: '小王',
    type: 'notice',
    status: 'pending',
    remindTime: '2025-03-01 10:00'
  },
  {
    id: 2,
    projectName: 'XX市智慧交通项目',
    owner: '张经理',
    type: 'report',
    status: 'reminded',
    remindTime: '2025-02-28 14:30'
  },
  {
    id: 3,
    projectName: 'XX区数字政府项目',
    owner: '李总',
    type: 'notice',
    status: 'uploaded',
    remindTime: '2025-02-25 09:00'
  },
  {
    id: 4,
    projectName: 'XX县智慧社区项目',
    owner: '小王',
    type: 'report',
    status: 'pending',
    remindTime: '2025-03-02 11:00'
  },
  {
    id: 5,
    projectName: 'XX市政务云项目',
    owner: '张经理',
    type: 'notice',
    status: 'pending',
    remindTime: '2025-03-01 15:00'
  }
])

const reportVisible = ref(false)

const competitorData = ref([
  {
    company: 'A公司',
    skuCount: '1500+',
    category: '办公用品',
    discount: '85折',
    payment: '月结30天',
    winRate: '45%',
    projectCount: 12,
    trend: 'up'
  },
  {
    company: 'B公司',
    skuCount: '800-1200',
    category: '办公设备',
    discount: '88折',
    payment: '月结45天',
    winRate: '38%',
    projectCount: 8,
    trend: 'up'
  },
  {
    company: 'C公司',
    skuCount: '2000+',
    category: '劳保用品',
    discount: '82折',
    payment: '现结',
    winRate: '52%',
    projectCount: 15,
    trend: 'down'
  },
  {
    company: 'D公司',
    skuCount: '500-800',
    category: '数码产品',
    discount: '90折',
    payment: '月结60天',
    winRate: '28%',
    projectCount: 5,
    trend: 'up'
  },
  {
    company: 'E公司',
    skuCount: '1000-1500',
    category: '综合品类',
    discount: '86折',
    payment: '月结30天',
    winRate: '42%',
    projectCount: 10,
    trend: 'up'
  }
])

// 功能处理函数
const handleSyncInternal = () => {
  ElMessage.success('正在从内部系统同步数据...')
  lastSyncTime.value = new Date().toLocaleString('zh-CN')
  setTimeout(() => {
    ElMessage.success('同步完成，新增 2 条结果记录')
  }, 1500)
}

const handleAutoFetch = () => {
  ElMessage.success('正在抓取公开信息...')
  setTimeout(() => {
    ElMessage.success('抓取完成，发现 3 条新结果待确认')
    pendingCount.value = 3
  }, 1500)
}

const handleSendRemind = () => {
  ElMessage.success('已发送提醒给 {{ uploadPending }} 位负责人')
}

const handleSendRemindAll = () => {
  ElMessage.success('已发送全部提醒')
}

const handleRemindAgain = (row) => {
  ElMessage.success(`已再次提醒 ${row.owner}`)
}

const handleShowReport = () => {
  reportVisible.value = true
}

const handleConfirm = (row) => {
  const index = fetchResults.value.findIndex(r => r.id === row.id)
  if (index > -1) {
    fetchResults.value.splice(index, 1)
    pendingCount.value = fetchResults.value.length
  }
  ElMessage.success('已确认该结果')
}

const handleConfirmAll = () => {
  ElMessage.success('已批量确认所有结果')
  fetchResults.value = []
  pendingCount.value = 0
}

const handleIgnore = (row) => {
  const index = fetchResults.value.findIndex(r => r.id === row.id)
  if (index > -1) {
    fetchResults.value.splice(index, 1)
    pendingCount.value = fetchResults.value.length
  }
  ElMessage.info('已忽略该结果')
}

const handleViewDetail = (row) => {
  ElMessage.info(`查看 ${row.projectName} 详情`)
}
</script>

<style scoped>
.bid-result-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #1a1a1a;
}

.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.feature-card {
  text-align: center;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.feature-card:hover {
  transform: translateY(-4px);
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 28px;
  color: #fff;
}

.card-icon.system {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.card-icon.spider {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.card-icon.upload {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.card-icon.analysis {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #303133;
}

.card-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 16px 0;
}

.card-stats {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

.result-card,
.reminder-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
