<template>
  <div class="qualification-container">
    <div class="page-header">
      <h2 class="page-title">资质库</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Upload" @click="handleUpload">
          上传资质
        </el-button>
      </div>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="资质名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入资质名称"
            clearable
            :prefix-icon="Search"
          />
        </el-form-item>
        <el-form-item label="资质类型">
          <el-select v-model="searchForm.type" placeholder="全部类型" clearable>
            <el-option label="企业资质" value="enterprise" />
            <el-option label="人员资质" value="personnel" />
            <el-option label="产品资质" value="product" />
            <el-option label="行业认证" value="industry" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
            <el-option label="有效" value="valid" />
            <el-option label="即将到期" value="expiring" />
            <el-option label="已过期" value="expired" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="filteredQualifications"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="name" label="资质名称" min-width="200">
          <template #default="{ row }">
            <div class="name-cell">
              <el-icon class="type-icon" :color="getTypeColor(row.type)">
                <component :is="getTypeIcon(row.type)" />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="certificateNo" label="证书编号" min-width="180" />

        <el-table-column prop="issueDate" label="发证日期" width="120">
          <template #default="{ row }">
            {{ formatDate(row.issueDate) }}
          </template>
        </el-table-column>

        <el-table-column prop="expiryDate" label="有效期至" width="120">
          <template #default="{ row }">
            <span :class="getDateClass(row.expiryDate, row.status)">
              {{ formatDate(row.expiryDate) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="issuer" label="发证机关" min-width="150" />

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getStatusTagType(row.status)"
              :icon="getStatusIcon(row.status)"
              size="small"
            >
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              :icon="View"
              size="small"
              @click="handleView(row)"
            >
              查看
            </el-button>
            <el-button
              type="success"
              link
              :icon="Share"
              size="small"
              @click="handleBorrow(row)"
            >
              借阅
            </el-button>
            <el-button
              type="primary"
              link
              :icon="Download"
              size="small"
              @click="handleDownload(row)"
            >
              下载
            </el-button>
            <el-button
              v-if="isAdmin"
              type="danger"
              link
              :icon="Delete"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传资质文件"
      width="600px"
    >
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="资质名称" required>
          <el-input v-model="uploadForm.name" placeholder="请输入资质名称" />
        </el-form-item>
        <el-form-item label="资质类型" required>
          <el-select v-model="uploadForm.type" placeholder="请选择类型">
            <el-option label="企业资质" value="enterprise" />
            <el-option label="人员资质" value="personnel" />
            <el-option label="产品资质" value="product" />
            <el-option label="行业认证" value="industry" />
          </el-select>
        </el-form-item>
        <el-form-item label="证书编号">
          <el-input v-model="uploadForm.certificateNo" placeholder="请输入证书编号" />
        </el-form-item>
        <el-form-item label="发证机关">
          <el-input v-model="uploadForm.issuer" placeholder="请输入发证机关" />
        </el-form-item>
        <el-form-item label="发证日期" required>
          <el-date-picker
            v-model="uploadForm.issueDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="有效期至" required>
          <el-date-picker
            v-model="uploadForm.expiryDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="上传文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            accept=".pdf,.jpg,.jpeg,.png"
          >
            <el-button :icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持上传 PDF、JPG、PNG 格式文件，文件大小不超过 10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmUpload">确认上传</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="资质详情"
      width="700px"
    >
      <el-descriptions :column="2" border v-if="currentQualification">
        <el-descriptions-item label="资质名称">
          {{ currentQualification.name }}
        </el-descriptions-item>
        <el-descriptions-item label="资质类型">
          <el-tag :type="getTypeTagType(currentQualification.type)" size="small">
            {{ getTypeLabel(currentQualification.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="证书编号">
          {{ currentQualification.certificateNo }}
        </el-descriptions-item>
        <el-descriptions-item label="发证机关">
          {{ currentQualification.issuer }}
        </el-descriptions-item>
        <el-descriptions-item label="发证日期">
          {{ formatDate(currentQualification.issueDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="有效期至">
          <span :class="getDateClass(currentQualification.expiryDate, currentQualification.status)">
            {{ formatDate(currentQualification.expiryDate) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag
            :type="getStatusTagType(currentQualification.status)"
            :icon="getStatusIcon(currentQualification.status)"
          >
            {{ getStatusLabel(currentQualification.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="剩余天数">
          <span :class="getDaysClass(currentQualification.remainingDays)">
            {{ currentQualification.remainingDays }} 天
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="附件" :span="2">
          <el-button type="primary" link :icon="Download">下载附件</el-button>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 借阅对话框 -->
    <el-dialog v-model="borrowDialogVisible" title="资质借阅申请" width="500px">
      <el-form :model="borrowForm" label-width="100px">
        <el-form-item label="资质名称">
          <el-input :value="currentQualification?.name" disabled />
        </el-form-item>
        <el-form-item label="借用人" required>
          <el-input v-model="borrowForm.borrower" placeholder="请输入借用人姓名" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-input v-model="borrowForm.department" placeholder="请输入所属部门" />
        </el-form-item>
        <el-form-item label="借阅用途" required>
          <el-select v-model="borrowForm.purpose" placeholder="请选择用途" style="width: 100%">
            <el-option label="投标使用" value="bidding" />
            <el-option label="资质审核" value="audit" />
            <el-option label="客户展示" value="presentation" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="预计归还">
          <el-date-picker
            v-model="borrowForm.returnDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="borrowForm.remark" type="textarea" :rows="2" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmBorrow">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 借阅记录 -->
    <el-card class="borrow-history-card">
      <template #header>
        <div class="card-header">
          <span>借阅记录</span>
          <el-button type="primary" size="small" @click="borrowDialogVisible = true; currentQualification = null">
            新增借阅
          </el-button>
        </div>
      </template>
      <el-table :data="borrowRecords" stripe>
        <el-table-column prop="qualificationName" label="资质名称" min-width="180" />
        <el-table-column prop="borrower" label="借用人" width="100" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="purpose" label="用途" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getPurposeLabel(row.purpose) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="borrowDate" label="借阅日期" width="120" />
        <el-table-column prop="returnDate" label="应归还日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getBorrowStatusType(row.status)" size="small">
              {{ getBorrowStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 'borrowed'" size="small" type="primary" link @click="handleReturn(row)">
              归还
            </el-button>
            <el-button v-else size="small" link disabled>已归还</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Upload,
  Search,
  View,
  Download,
  Delete,
  Warning,
  CircleCheck,
  CircleClose,
  OfficeBuilding,
  User,
  Box,
  Medal,
  Share
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.currentUser?.role === 'admin')

// 搜索表单
const searchForm = reactive({
  name: '',
  type: '',
  status: ''
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 加载状态
const loading = ref(false)

// 对话框
const uploadDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const borrowDialogVisible = ref(false)

// 当前查看的资质
const currentQualification = ref(null)

// 借阅表单
const borrowForm = reactive({
  borrower: '',
  department: '',
  purpose: '',
  returnDate: '',
  remark: ''
})

// 借阅记录 Mock 数据
const borrowRecords = ref([
  {
    id: 1,
    qualificationName: '高新技术企业证书',
    borrower: '小王',
    department: '销售部',
    purpose: 'bidding',
    borrowDate: '2025-02-20',
    returnDate: '2025-03-05',
    status: 'borrowed'
  },
  {
    id: 2,
    qualificationName: 'ISO9001质量管理体系认证',
    borrower: '张经理',
    department: '商务部',
    purpose: 'audit',
    borrowDate: '2025-02-15',
    returnDate: '2025-02-28',
    status: 'returned'
  },
  {
    id: 3,
    qualificationName: '涉密信息系统集成资质',
    borrower: '李工',
    department: '技术部',
    purpose: 'bidding',
    borrowDate: '2025-02-25',
    returnDate: '2025-03-10',
    status: 'borrowed'
  },
  {
    id: 4,
    qualificationName: 'CMMI5级认证证书',
    borrower: '小王',
    department: '销售部',
    purpose: 'presentation',
    borrowDate: '2025-01-20',
    returnDate: '2025-02-05',
    status: 'returned'
  }
])

// 上传表单
const uploadForm = reactive({
  name: '',
  type: '',
  certificateNo: '',
  issuer: '',
  issueDate: '',
  expiryDate: '',
  file: null
})

// Mock 数据
const mockQualifications = [
  {
    id: 1,
    name: '高新技术企业证书',
    type: 'enterprise',
    certificateNo: 'GR202311000123',
    issueDate: '2023-06-15',
    expiryDate: '2026-06-14',
    issuer: '科学技术部',
    status: 'valid',
    remainingDays: 890
  },
  {
    id: 2,
    name: 'ISO9001质量管理体系认证',
    type: 'industry',
    certificateNo: 'QMS2023-01-001',
    issueDate: '2023-03-20',
    expiryDate: '2026-03-19',
    issuer: '中国质量认证中心',
    status: 'valid',
    remainingDays: 750
  },
  {
    id: 3,
    name: '建筑工程施工总承包壹级',
    type: 'enterprise',
    certificateNo: 'D144007891',
    issueDate: '2021-08-10',
    expiryDate: '2026-08-09',
    issuer: '住房和城乡建设部',
    status: 'valid',
    remainingDays: 890
  },
  {
    id: 4,
    name: '软件能力成熟度CMMI5级',
    type: 'industry',
    certificateNo: 'CMMI-5-2023-001',
    issueDate: '2023-05-01',
    expiryDate: '2025-05-01',
    issuer: 'CMMI Institute',
    status: 'expiring',
    remainingDays: 430
  },
  {
    id: 5,
    name: '安全生产许可证',
    type: 'enterprise',
    certificateNo: '（浙）JZ安许证字〔2023〕000123',
    issueDate: '2023-01-15',
    expiryDate: '2026-01-14',
    issuer: '浙江省应急管理厅',
    status: 'valid',
    remainingDays: 690
  },
  {
    id: 6,
    name: 'PMP项目管理专业人士认证',
    type: 'personnel',
    certificateNo: '3889123',
    issueDate: '2023-09-01',
    expiryDate: '2026-09-01',
    issuer: 'PMI',
    status: 'valid',
    remainingDays: 920
  },
  {
    id: 7,
    name: '信息安全等级保护三级认证',
    type: 'industry',
    certificateNo: 'DJCP2023-330100-001',
    issueDate: '2023-07-20',
    expiryDate: '2026-07-19',
    issuer: '公安部网络安全保卫局',
    status: 'valid',
    remainingDays: 870
  },
  {
    id: 8,
    name: '环保工程专项设计资质乙级',
    type: 'enterprise',
    certificateNo: 'A233001234',
    issueDate: '2020-03-15',
    expiryDate: '2025-03-14',
    issuer: '环境保护部',
    status: 'expiring',
    remainingDays: 22
  },
  {
    id: 9,
    name: '系统集成及服务资质二级',
    type: 'enterprise',
    certificateNo: 'XZ2023-001-0056',
    issueDate: '2023-02-10',
    expiryDate: '2025-02-09',
    issuer: '中国电子信息行业联合会',
    status: 'expiring',
    remainingDays: 18
  },
  {
    id: 10,
    name: '注册建造师一级证书',
    type: 'personnel',
    certificateNo: '浙133202301234',
    issueDate: '2020-05-20',
    expiryDate: '2024-05-19',
    issuer: '人力资源和社会保障部',
    status: 'expired',
    remainingDays: -280
  },
  {
    id: 11,
    name: '3C强制性产品认证',
    type: 'product',
    certificateNo: '2023010101234567',
    issueDate: '2023-01-10',
    expiryDate: '2028-01-09',
    issuer: '中国强制性产品认证中心',
    status: 'valid',
    remainingDays: 2150
  },
  {
    id: 12,
    name: '环境管理体系认证ISO14001',
    type: 'industry',
    certificateNo: 'EMS2023-01-002',
    issueDate: '2023-04-15',
    expiryDate: '2026-04-14',
    issuer: '中国质量认证中心',
    status: 'valid',
    remainingDays: 780
  }
]

const qualifications = ref([])

// 过滤后的数据
const filteredQualifications = computed(() => {
  let result = qualifications.value

  if (searchForm.name) {
    result = result.filter(item =>
      item.name.toLowerCase().includes(searchForm.name.toLowerCase())
    )
  }

  if (searchForm.type) {
    result = result.filter(item => item.type === searchForm.type)
  }

  if (searchForm.status) {
    result = result.filter(item => item.status === searchForm.status)
  }

  // 到期预警排序（即将到期的排在前面）
  result.sort((a, b) => a.remainingDays - b.remainingDays)

  pagination.total = result.length

  const start = (pagination.page - 1) * pagination.pageSize
  const end = start + pagination.pageSize

  return result.slice(start, end)
})

// 类型标签类型
const getTypeTagType = (type) => {
  const types = {
    enterprise: '',
    personnel: 'success',
    product: 'warning',
    industry: 'info'
  }
  return types[type] || ''
}

// 类型标签文本
const getTypeLabel = (type) => {
  const labels = {
    enterprise: '企业资质',
    personnel: '人员资质',
    product: '产品资质',
    industry: '行业认证'
  }
  return labels[type] || type
}

// 类型图标
const getTypeIcon = (type) => {
  const icons = {
    enterprise: OfficeBuilding,
    personnel: User,
    product: Box,
    industry: Medal
  }
  return icons[type] || OfficeBuilding
}

// 类型颜色
const getTypeColor = (type) => {
  const colors = {
    enterprise: '#409eff',
    personnel: '#67c23a',
    product: '#e6a23c',
    industry: '#909399'
  }
  return colors[type] || '#409eff'
}

// 状态标签类型
const getStatusTagType = (status) => {
  const types = {
    valid: 'success',
    expiring: 'warning',
    expired: 'danger'
  }
  return types[status] || ''
}

// 状态标签文本
const getStatusLabel = (status) => {
  const labels = {
    valid: '有效',
    expiring: '即将到期',
    expired: '已过期'
  }
  return labels[status] || status
}

// 状态图标
const getStatusIcon = (status) => {
  const icons = {
    valid: CircleCheck,
    expiring: Warning,
    expired: CircleClose
  }
  return icons[status] || ''
}

// 日期样式类
const getDateClass = (date, status) => {
  if (status === 'expiring') return 'date-warning'
  if (status === 'expired') return 'date-expired'
  return ''
}

// 天数样式类
const getDaysClass = (days) => {
  if (days < 0) return 'days-expired'
  if (days <= 30) return 'days-warning'
  if (days <= 90) return 'days-notice'
  return 'days-normal'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return date
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
}

// 重置
const handleReset = () => {
  searchForm.name = ''
  searchForm.type = ''
  searchForm.status = ''
  pagination.page = 1
}

// 上传
const handleUpload = () => {
  Object.assign(uploadForm, {
    name: '',
    type: '',
    certificateNo: '',
    issuer: '',
    issueDate: '',
    expiryDate: '',
    file: null
  })
  uploadDialogVisible.value = true
}

// 文件变化
const handleFileChange = (file) => {
  uploadForm.file = file.raw
}

// 确认上传
const handleConfirmUpload = () => {
  if (!uploadForm.name || !uploadForm.type || !uploadForm.expiryDate) {
    ElMessage.warning('请填写必填项')
    return
  }

  ElMessage.success('上传成功')
  uploadDialogVisible.value = false

  // 模拟添加数据
  const newQualification = {
    id: Date.now(),
    name: uploadForm.name,
    type: uploadForm.type,
    certificateNo: uploadForm.certificateNo || '-',
    issueDate: uploadForm.issueDate || '-',
    expiryDate: uploadForm.expiryDate,
    issuer: uploadForm.issuer || '-',
    status: 'valid',
    remainingDays: Math.ceil((new Date(uploadForm.expiryDate) - new Date()) / (1000 * 60 * 60 * 24))
  }
  qualifications.value.unshift(newQualification)
}

// 查看
const handleView = (row) => {
  currentQualification.value = row
  detailDialogVisible.value = true
}

// 下载
const handleDownload = (row) => {
  ElMessage.success(`开始下载：${row.name}`)
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除「${row.name}」吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    const index = qualifications.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      qualifications.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  }).catch(() => {})
}

// 借阅
const handleBorrow = (row) => {
  currentQualification.value = row
  // 重置表单
  borrowForm.borrower = ''
  borrowForm.department = ''
  borrowForm.purpose = ''
  borrowForm.returnDate = ''
  borrowForm.remark = ''
  borrowDialogVisible.value = true
}

// 确认借阅
const handleConfirmBorrow = () => {
  if (!borrowForm.borrower || !borrowForm.purpose) {
    ElMessage.warning('请填写必填项')
    return
  }

  // 添加借阅记录
  const newRecord = {
    id: Date.now(),
    qualificationName: currentQualification.value?.name || '资质文件',
    borrower: borrowForm.borrower,
    department: borrowForm.department || '-',
    purpose: borrowForm.purpose,
    borrowDate: new Date().toISOString().split('T')[0],
    returnDate: borrowForm.returnDate || '-',
    status: 'borrowed'
  }

  borrowRecords.value.unshift(newRecord)
  ElMessage.success('借阅申请已提交')
  borrowDialogVisible.value = false
}

// 归还
const handleReturn = (row) => {
  ElMessageBox.confirm(
    `确认「${row.qualificationName}」已归还吗？`,
    '归还确认',
    {
      confirmButtonText: '确认归还',
      cancelButtonText: '取消',
      type: 'success'
    }
  ).then(() => {
    const record = borrowRecords.value.find(r => r.id === row.id)
    if (record) {
      record.status = 'returned'
      ElMessage.success('归还成功')
    }
  }).catch(() => {})
}

// 用途标签
const getPurposeLabel = (purpose) => {
  const map = {
    'bidding': '投标使用',
    'audit': '资质审核',
    'presentation': '客户展示',
    'other': '其他'
  }
  return map[purpose] || purpose
}

// 借阅状态类型
const getBorrowStatusType = (status) => {
  const map = {
    'borrowed': 'warning',
    'returned': 'success',
    'overdue': 'danger'
  }
  return map[status] || ''
}

// 借阅状态标签
const getBorrowStatusLabel = (status) => {
  const map = {
    'borrowed': '借阅中',
    'returned': '已归还',
    'overdue': '逾期'
  }
  return map[status] || status
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
  qualifications.value = mockQualifications
  pagination.total = mockQualifications.length
})
</script>

<style scoped lang="scss">
.qualification-container {
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
  }

  .table-card {
    .name-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .type-icon {
        font-size: 18px;
      }
    }

    .date-warning {
      color: #e6a23c;
      font-weight: 500;
    }

    .date-expired {
      color: #f56c6c;
      font-weight: 500;
    }

    .days-normal {
      color: #67c23a;
    }

    .days-notice {
      color: #e6a23c;
    }

    .days-warning {
      color: #e6a23c;
      font-weight: 600;
    }

    .days-expired {
      color: #f56c6c;
      font-weight: 600;
    }

    .pagination-wrapper {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .el-upload__tip {
    color: #909399;
    font-size: 12px;
    margin-top: 8px;
  }

  /* 移动端响应式样式 */
  @media (max-width: 768px) {
    .qualification-page {
      padding: 12px;
    }

    .page-header {
      margin-bottom: 12px;
    }

    .page-title {
      font-size: 20px;
    }

    .filter-row {
      margin-bottom: 12px;
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

    .table-card :deep(.el-table__cell) {
      padding: 8px 4px;
    }

    /* 对话框移动端优化 */
    :deep(.el-dialog) {
      width: 95% !important;
      margin: 0 auto;
    }

    :deep(.el-dialog__body) {
      padding: 16px;
    }

    :deep(.el-descriptions) {
      font-size: 12px;
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
    .el-button {
      min-height: 44px;
    }
  }

  .borrow-history-card {
    margin-top: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}
</style>
