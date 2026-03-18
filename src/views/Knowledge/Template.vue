<template>
  <div class="template-container">
    <div class="page-header">
      <h2 class="page-title">模板库</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="handleAdd">
          新建模板
        </el-button>
      </div>
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs">
      <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
        <el-tab-pane label="全部" name="all">
          <template #label>
            <span class="tab-label">
              <el-icon><Grid /></el-icon>
              全部
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="技术方案" name="technical">
          <template #label>
            <span class="tab-label">
              <el-icon><Document /></el-icon>
              技术方案
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="商务文件" name="commercial">
          <template #label>
            <span class="tab-label">
              <el-icon><DocumentCopy /></el-icon>
              商务文件
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="实施方案" name="implementation">
          <template #label>
            <span class="tab-label">
              <el-icon><Operation /></el-icon>
              实施方案
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="报价清单" name="quotation">
          <template #label>
            <span class="tab-label">
              <el-icon><Tickets /></el-icon>
              报价清单
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="资质文件" name="qualification">
          <template #label>
            <span class="tab-label">
              <el-icon><Medal /></el-icon>
              资质文件
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="合同范本" name="contract">
          <template #label>
            <span class="tab-label">
              <el-icon><Notebook /></el-icon>
              合同范本
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="模板名称">
          <el-input
            v-model="searchForm.name"
            placeholder="搜索模板名称"
            clearable
            :prefix-icon="Search"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="searchForm.tags"
            placeholder="选择标签"
            multiple
            collapse-tags
            clearable
            style="width: 300px"
          >
            <el-option
              v-for="tag in allTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="searchForm.sort" placeholder="默认排序">
            <el-option label="默认排序" value="default" />
            <el-option label="下载量" value="downloads" />
            <el-option label="更新时间" value="updateTime" />
            <el-option label="名称" value="name" />
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

    <!-- 模板列表 -->
    <el-card class="table-card">
      <FeaturePlaceholder
        v-if="featurePlaceholder"
        :title="featurePlaceholder.title"
        :message="featurePlaceholder.message"
        :hint="featurePlaceholder.hint"
      />
      <el-table
        v-else
        v-loading="loading"
        :data="filteredTemplates"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="name" label="模板名称" min-width="200">
          <template #default="{ row }">
            <div class="name-cell">
              <el-icon class="category-icon" :color="getCategoryColor(row.category)">
                <component :is="getCategoryIcon(row.category)" />
              </el-icon>
              <div class="name-content">
                <span class="name-text">{{ row.name }}</span>
                <span class="name-desc">{{ row.description }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.category)" size="small">
              {{ getCategoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="tags" label="标签" min-width="200">
          <template #default="{ row }">
            <div class="tags-cell">
              <el-tag
                v-for="tag in row.tags.slice(0, 3)"
                :key="tag"
                size="small"
                effect="plain"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
              <el-tooltip
                v-if="row.tags.length > 3"
                :content="row.tags.slice(3).join(', ')"
                placement="top"
              >
                <el-tag size="small" effect="plain" class="tag-more">
                  +{{ row.tags.length - 3 }}
                </el-tag>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="downloads" label="下载量" width="150" sortable>
          <template #default="{ row }">
            <span class="download-count">
              <el-icon><Download /></el-icon>
              {{ formatNumber(row.downloads) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="updateTime" label="更新时间" width="120">
          <template #default="{ row }">
            {{ formatDate(row.updateTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="version" label="版本" width="100">
          <template #default="{ row }">
            <el-tag type="info" size="small">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              :icon="View"
              size="small"
              @click="handlePreview(row)"
            >
              预览
            </el-button>
            <el-button
              type="success"
              link
              :icon="DocumentAdd"
              size="small"
              @click="handleUseTemplate(row)"
            >
              一键使用
            </el-button>
            <el-dropdown @command="(cmd) => handleMoreAction(cmd, row)">
              <el-button type="info" link :icon="MoreFilled" size="small">
                更多
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" :icon="Edit">
                    编辑模板
                  </el-dropdown-item>
                  <el-dropdown-item command="copy" :icon="CopyDocument">
                    复制模板
                  </el-dropdown-item>
                  <el-dropdown-item command="version" :icon="Clock">
                    版本历史
                  </el-dropdown-item>
                  <el-dropdown-item command="download" :icon="Download">
                    下载
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" divided>
                    删除模板
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 模板预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="`预览: ${previewTemplate?.name}`"
      width="900px"
      class="preview-dialog"
    >
      <div class="template-preview" v-if="previewTemplate">
        <!-- 模板元信息 -->
        <div class="template-meta">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="模板名称">
              {{ previewTemplate.name }}
            </el-descriptions-item>
            <el-descriptions-item label="分类">
              <el-tag :type="getCategoryTagType(previewTemplate.category)" size="small">
                {{ getCategoryLabel(previewTemplate.category) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="标签">
              <el-tag
                v-for="tag in previewTemplate.tags"
                :key="tag"
                size="small"
                style="margin-right: 4px"
              >
                {{ tag }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="版本">v{{ previewTemplate.version }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatDate(previewTemplate.updateTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="下载量">
              {{ formatNumber(previewTemplate.downloads) }} 次
            </el-descriptions-item>
            <el-descriptions-item label="文件大小" :span="3">
              {{ previewTemplate.fileSize }}
            </el-descriptions-item>
            <el-descriptions-item label="描述" :span="3">
              {{ previewTemplate.description }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 模板内容预览 -->
        <div class="template-content-preview">
          <el-alert
            title="模板内容预览"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
          >
            <p>这是模板的完整内容预览，点击"使用此模板"可将其内容应用到您的文档中。</p>
          </el-alert>

          <el-tabs v-model="activePreviewTab" class="preview-tabs">
            <el-tab-pane label="内容预览" name="content">
              <div class="content-frame">
                <pre class="template-content-text">{{ previewTemplate.content || '暂无内容' }}</pre>
              </div>
            </el-tab-pane>
            <el-tab-pane label="文件结构" name="structure">
              <el-tree
                :data="previewTemplate.structure"
                :props="{ label: 'name', children: 'children' }"
                default-expand-all
                class="template-tree"
              >
                <template #default="{ node, data }">
                  <span class="tree-node">
                    <el-icon>
                      <component :is="data.type === 'folder' ? Folder : Document" />
                    </el-icon>
                    {{ node.label }}
                  </span>
                </template>
              </el-tree>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="preview-actions">
          <el-button @click="previewDialogVisible = false">关闭</el-button>
          <el-button type="primary" :icon="DocumentAdd" @click="useTemplateFromPreview">
            使用此模板
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 使用模板对话框 -->
    <el-dialog
      v-model="useTemplateDialogVisible"
      title="使用模板"
      width="600px"
      class="use-template-dialog"
    >
      <div class="use-template-content" v-if="selectedTemplate">
        <el-alert
          :title="`正在使用模板: ${selectedTemplate.name}`"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />

        <el-form :model="useTemplateForm" label-width="120px">
          <el-form-item label="创建文档类型">
            <el-radio-group v-model="useTemplateForm.docType">
              <el-radio value="tech">
                <div class="radio-option">
                  <el-icon><Document /></el-icon>
                  <span>技术方案</span>
                </div>
              </el-radio>
              <el-radio value="business">
                <div class="radio-option">
                  <el-icon><DocumentCopy /></el-icon>
                  <span>商务应答</span>
                </div>
              </el-radio>
              <el-radio value="contract">
                <div class="radio-option">
                  <el-icon><Notebook /></el-icon>
                  <span>合同文档</span>
                </div>
              </el-radio>
              <el-radio value="standalone">
                <div class="radio-option">
                  <el-icon><Folder /></el-icon>
                  <span>独立文档</span>
                </div>
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="关联项目" v-if="useTemplateForm.docType !== 'standalone'">
            <el-select
              v-model="useTemplateForm.projectId"
              placeholder="选择关联项目（可选）"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="project in inProgressProjects"
                :key="project.id"
                :label="`${project.name} (${project.customer})`"
                :value="project.id"
              >
                <div class="project-option">
                  <span class="project-name">{{ project.name }}</span>
                  <span class="project-customer">{{ project.customer }}</span>
                  <el-tag size="small" :type="getProjectStatusType(project.status)">
                    {{ getProjectStatusLabel(project.status) }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              如未选择项目，将创建独立文档，后续可手动关联
            </div>
          </el-form-item>

          <el-form-item label="文档名称" required>
            <el-input
              v-model="useTemplateForm.docName"
              :placeholder="`请输入文档名称，如：${selectedTemplate.name}应用`"
            />
          </el-form-item>

          <el-form-item label="应用方式">
            <el-checkbox-group v-model="useTemplateForm.applyOptions">
              <el-checkbox value="content">应用模板内容</el-checkbox>
              <el-checkbox value="format">保留格式设置</el-checkbox>
              <el-checkbox value="styles">应用样式风格</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="useTemplateDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" @click="confirmUseTemplate">
          确认使用
        </el-button>
      </template>
    </el-dialog>

    <!-- 版本历史对话框 -->
    <el-dialog
      v-model="versionDialogVisible"
      title="版本历史"
      width="700px"
    >
      <FeaturePlaceholder
        v-if="versionPlaceholder"
        compact
        :title="versionPlaceholder.title"
        :message="versionPlaceholder.message"
        :hint="versionPlaceholder.hint"
      />
      <el-timeline v-else-if="versionHistory.length > 0">
        <el-timeline-item
          v-for="version in versionHistory"
          :key="version.id"
          :timestamp="version.date"
          :type="version.isCurrent ? 'primary' : 'info'"
        >
          <div class="version-item">
            <div class="version-header">
              <span class="version-number">v{{ version.version }}</span>
              <el-tag v-if="version.isCurrent" type="success" size="small">当前版本</el-tag>
              <el-tag v-else type="info" size="small">历史版本</el-tag>
            </div>
            <div class="version-description">{{ version.description }}</div>
            <div class="version-actions" v-if="!version.isCurrent">
              <el-button link type="primary" size="small">查看此版本</el-button>
              <el-button link type="primary" size="small">恢复此版本</el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无版本历史" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import {
  Plus,
  Search,
  View,
  Download,
  Delete,
  Edit,
  CopyDocument,
  Check,
  MoreFilled,
  Grid,
  Document,
  DocumentCopy,
  Operation,
  Tickets,
  Medal,
  Notebook,
  Folder,
  DocumentAdd,
  Clock,
  InfoFilled
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FeaturePlaceholder from '@/components/common/FeaturePlaceholder.vue'
import { getFeaturePlaceholder, isFeatureUnavailableResponse, knowledgeApi, isMockMode } from '@/api'
import { getTemplateDemoState, saveTemplateDemoState } from '@/api/mock-adapters/frontendDemo.js'
import { notifyFeatureUnavailable } from '@/utils/featureFeedback'
import { triggerDownload } from '@/api/modules/export'

const router = useRouter()
const projectStore = useProjectStore()
const userStore = useUserStore()
const TEMPLATE_STORAGE_KEY = 'knowledge-template-overrides'

// 进行中的项目
const inProgressProjects = computed(() => projectStore.inProgressProjects)

// 当前分类
const activeCategory = ref('all')

// 搜索表单
const searchForm = reactive({
  name: '',
  tags: [],
  sort: 'default'
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 加载状态
const loading = ref(false)

// 预览对话框
const previewDialogVisible = ref(false)
const previewTemplate = ref(null)
const activePreviewTab = ref('content')

// 使用模板对话框
const useTemplateDialogVisible = ref(false)
const selectedTemplate = ref(null)
const useTemplateForm = reactive({
  docType: 'standalone',
  projectId: '',
  docName: '',
  applyOptions: ['content', 'format', 'styles']
})

// 版本历史对话框
const versionDialogVisible = ref(false)
const versionHistory = ref([])
const featurePlaceholder = ref(null)
const versionPlaceholder = ref(null)

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

const isPersistentTemplateId = (templateId) => /^\d+$/.test(String(templateId))

const getCurrentUserId = () => {
  const rawId = userStore.currentUser?.id
  if (rawId === undefined || rawId === null || rawId === '') return null
  const numericId = Number(rawId)
  return Number.isFinite(numericId) ? numericId : null
}

const upsertTemplate = (template) => {
  const index = templates.value.findIndex((item) => String(item.id) === String(template.id))
  if (index > -1) {
    templates.value.splice(index, 1, template)
  } else {
    templates.value.unshift(template)
  }
}

// 分类配置
const categories = {
  technical: { label: '技术方案', icon: Document, color: '#409eff', tagType: '' },
  commercial: { label: '商务文件', icon: DocumentCopy, color: '#67c23a', tagType: 'success' },
  implementation: { label: '实施方案', icon: Operation, color: '#e6a23c', tagType: 'warning' },
  quotation: { label: '报价清单', icon: Tickets, color: '#f56c6c', tagType: 'danger' },
  qualification: { label: '资质文件', icon: Medal, color: '#909399', tagType: 'info' },
  contract: { label: '合同范本', icon: Notebook, color: '#409eff', tagType: 'primary' }
}

// 项目状态配置
const projectStatusConfig = {
  pending: { label: '待启动', type: 'info' },
  reviewing: { label: '评审中', type: 'warning' },
  bidding: { label: '投标中', type: 'primary' },
  won: { label: '已中标', type: 'success' },
  lost: { label: '未中标', type: 'danger' }
}

// Mock 模板详细内容数据
const templateContents = {
  1: `# 智慧城市项目技术方案

## 一、项目背景与需求分析

### 1.1 项目背景
随着城市化进程的加快和信息技术的飞速发展，智慧城市建设已成为推动城市可持续发展的重要抓手。本项目旨在通过信息化手段，提升城市管理水平和公共服务质量。

### 1.2 需求分析
- 城市管理精细化需求
- 公共服务便捷化需求
- 决策支持科学化需求
- 基础设施智能化需求

## 二、总体架构设计

### 2.1 设计原则
- 先进性原则
- 可靠性原则
- 可扩展性原则
- 安全性原则
- 标准化原则

### 2.2 总体架构
本方案采用"云-管-端"三层架构设计：
- 云平台层：IaaS、PaaS、SaaS服务
- 网络层：5G、物联网、专网
- 终端层：感知设备、应用终端

### 2.3 技术路线
- 前端技术：Vue3 + TypeScript
- 后端技术：Spring Cloud微服务架构
- 数据库：MySQL + MongoDB + Redis
- 大数据：Hadoop + Spark + Flink
- AI算法：TensorFlow + PyTorch

## 三、系统功能设计

### 3.1 城市运行管理中心
- 城市体征监测
- 应急指挥调度
- 决策支持分析

### 3.2 智慧交通系统
- 交通信号优化
- 智能停车管理
- 公共交通调度

### 3.3 智慧环保系统
- 环境质量监测
- 污染源追溯
- 环境预警

## 四、实施计划

### 4.1 项目阶段划分
- 第一阶段：需求调研与方案设计（3个月）
- 第二阶段：平台开发与部署（6个月）
- 第三阶段：系统试运行（2个月）
- 第四阶段：验收与运维（1个月）

### 4.2 项目组织架构
- 项目经理：1人
- 技术负责人：1人
- 架构师：2人
- 开发工程师：10人
- 测试工程师：3人
- 运维工程师：2人

## 五、质量保障措施

### 5.1 质量管理体系
- 建立完善的质量管理体系
- 执行严格的测试流程
- 实施代码审查机制

### 5.2 安全保障
- 网络安全：防火墙、入侵检测
- 数据安全：加密存储、访问控制
- 应用安全：身份认证、权限管理`,

  2: `# 软件开发项目技术方案

## 一、项目概述

### 1.1 项目背景
本项目旨在为客户开发一套高效、稳定的[系统名称]，满足[业务场景]的核心需求。

### 1.2 项目目标
- 实现核心业务流程数字化
- 提升工作效率XX%
- 降低运营成本XX%
- 优化用户体验

## 二、需求分析

### 2.1 业务需求
- 需求1：[具体描述]
- 需求2：[具体描述]
- 需求3：[具体描述]

### 2.2 功能需求
| 模块 | 功能项 | 优先级 |
|------|--------|--------|
| 用户管理 | 登录/注册 | 高 |
| 用户管理 | 权限管理 | 高 |
| 业务模块A | 功能A1 | 高 |
| 业务模块A | 功能A2 | 中 |
| 业务模块B | 功能B1 | 中 |

### 2.3 非功能需求
- 性能要求：响应时间 < 2秒，并发用户数 > 1000
- 可用性：系统可用性 > 99.9%
- 安全性：符合等保2.0标准
- 可维护性：代码规范，文档完整

## 三、系统设计

### 3.1 架构设计
采用前后端分离的微服务架构：
- 前端：Vue3 + Element Plus + TypeScript
- 后端：Spring Boot + Spring Cloud
- 数据库：MySQL 8.0
- 缓存：Redis 6.0
- 消息队列：RabbitMQ

### 3.2 数据库设计
核心表结构：
- 用户表(t_user)
- 角色表(t_role)
- 权限表(t_permission)
- 业务表A(t_business_a)
- 业务表B(t_business_b)

### 3.3 接口设计
遵循RESTful API设计规范，主要接口包括：
- GET /api/users - 获取用户列表
- POST /api/users - 创建用户
- PUT /api/users/{id} - 更新用户
- DELETE /api/users/{id} - 删除用户

## 四、开发计划

### 4.1 开发模式
采用敏捷开发模式，2周为一个迭代周期。

### 4.2 迭代计划
- Sprint 1 (第1-2周)：需求确认、原型设计
- Sprint 2 (第3-4周)：数据库设计、基础框架搭建
- Sprint 3 (第5-6周)：用户模块开发
- Sprint 4 (第7-8周)：业务模块A开发
- Sprint 5 (第9-10周)：业务模块B开发
- Sprint 6 (第11-12周)：联调测试、系统优化

## 五、测试方案

### 5.1 测试策略
- 单元测试：覆盖率 > 80%
- 集成测试：核心业务流程全覆盖
- 性能测试：模拟高并发场景
- 安全测试：漏洞扫描、渗透测试

### 5.2 验收标准
- 所有功能正常运行
- 性能指标达标
- 无重大安全漏洞
- 用户验收通过`,

  4: `# 投标函及投标函附录

## 投标函

致：[招标人名称]

1. 根据贵方[项目名称]招标文件（项目编号：[项目编号]），投标人[投标人名称]（以下简称"投标人"）提交投标文件正本1份，副本[份数]份。

2. 投标总价：
| 序号 | 内容 | 金额（元） |
|------|------|-----------|
| 1 | 设备费 | [金额] |
| 2 | 安装调试费 | [金额] |
| 3 | 培训费 | [金额] |
| 4 | 其他费用 | [金额] |
| 5 | 合计 | [总金额] |

3. 投标人承诺：
- 投标有效期为开标后[天数]天
- 在投标有效期内不撤销投标文件
- 中标后按要求签订合同
- 履约保证金为中标金额的[X]%

4. 投标人联系方式：
- 投标人（盖章）：[投标人名称]
- 法定代表人或授权代表（签字）：_________
- 地址：[地址]
- 电话：[电话]
- 传真：[传真]
- 日期：[日期]

## 投标函附录

| 序号 | 项目 | 内容 |
|------|------|------|
| 1 | 投标人名称 | [填写] |
| 2 | 投标人地址 | [填写] |
| 3 | 注册资本 | [填写] |
| 4 | 统一社会信用代码 | [填写] |
| 5 | 法定代表人 | [填写] |
| 6 | 项目经理 | [填写] |
| 7 | 投标有效期 | [填写] |
| 8 | 履约保证金比例 | [填写] |
| 9 | 质量保证期 | [填写] |
| 10 | 交货期/工期 | [填写] |
| 11 | 质量标准 | [填写] |
| 12 | 售后服务承诺 | [填写] |

## 法定代表人身份证明

### 单位基本信息
单位名称：[单位名称]
单位性质：[性质]
成立时间：[时间]
营业期限：[期限]
注册资本：[金额]

### 法定代表人信息
姓名：[姓名]
身份证号：[身份证号]
职务：[职务]

特此证明。

投标人（盖章）：_________
日期：_________年_________月_________日

## 授权委托书

本人[法定代表人姓名]系[投标人名称]的法定代表人，现授权委托[被授权人姓名]为我单位的代理人，参加贵方组织的[项目名称]（项目编号：[项目编号]）的投标活动。

代理人在投标、开标、评标、合同谈判过程中所签署的一切文件和处理与之有关的一切事务，我均予以承认。

代理人无转委托权。

特此授权。

法定代表人（签字）：_________
代理人（签字）：_________
身份证号：[代理人身份证号]

投标人（盖章）：_________
日期：_________年_________月_________日`,

  6: `# 项目实施方案

## 一、项目概述

### 1.1 项目背景
[项目名称]是[客户名称]为解决[业务痛点]而启动的建设项目，我司经过认真调研和分析，制定了本实施方案。

### 1.2 项目目标
- 总体目标：[描述项目总体目标]
- 阶段目标：[分阶段列出目标]
- 验收标准：[明确验收标准]

## 二、项目组织架构

### 2.1 组织结构

项目经理
├── 技术负责人
│   ├── 架构师
│   ├── 开发组长
│   └── 开发工程师
├── 实施负责人
│   ├── 实施工程师
│   └── 培训讲师
└── 质量负责人
    ├── 测试工程师
    └── 配置管理员

### 2.2 人员配置
| 角色 | 人数 | 职责 |
|------|------|------|
| 项目经理 | 1 | 项目整体协调 |
| 技术负责人 | 1 | 技术方案设计 |
| 架构师 | 1 | 系统架构设计 |
| 开发工程师 | 8 | 功能开发 |
| 实施工程师 | 3 | 现场实施 |
| 测试工程师 | 2 | 质量保障 |

### 2.3 项目管理制度
- 例会制度：每周项目例会
- 汇报制度：双周进度汇报
- 变更管理：变更申请审批流程
- 风险管理：风险识别与应对

## 三、实施进度计划

### 3.1 总体进度
项目总工期：[X]个月

### 3.2 详细进度

| 阶段 | 工作内容 | 工期 | 里程碑 |
|------|----------|------|--------|
| 第一阶段 | 需求调研 | 第1-2周 | 需求规格说明书 |
| 第二阶段 | 方案设计 | 第3-4周 | 详细设计方案 |
| 第三阶段 | 系统开发 | 第5-12周 | 系统上线 |
| 第四阶段 | 测试验收 | 第13-14周 | 验收报告 |
| 第五阶段 | 培训交付 | 第15-16周 | 项目交付 |

### 3.3 关键路径
[列出关键路径活动和依赖关系]

## 四、资源保障计划

### 4.1 人员保障
- 项目团队：配备经验丰富的专业人员
- 外部支持：专家顾问团队随时待命

### 4.2 设备保障
- 开发设备：高性能开发工作站
- 测试设备：专业测试环境和设备
- 网络环境：稳定的网络支持

### 4.3 资金保障
- 项目资金专款专用
- 建立资金使用审批制度

## 五、质量管理

### 5.1 质量目标
- 交付合格率：100%
- 用户满意度：≥90%
- 系统可用性：≥99%

### 5.2 质量控制措施
- 建立质量管理体系
- 执行严格的测试流程
- 实施代码审查机制
- 定期质量评审会议

### 5.3 验收标准
- 功能验收：按需求规格说明书逐项验收
- 性能验收：响应时间、并发用户数达标
- 安全验收：通过安全测试
- 文档验收：文档齐全、规范

## 六、风险管理

### 6.1 风险识别
| 风险类别 | 风险描述 | 可能性 | 影响程度 |
|----------|----------|--------|----------|
| 技术风险 | 新技术应用风险 | 中 | 高 |
| 进度风险 | 需求变更导致延期 | 高 | 中 |
| 人员风险 | 关键人员离职 | 低 | 高 |

### 6.2 风险应对
- 技术风险：提前技术预研，准备备选方案
- 进度风险：加强需求管理，预留缓冲时间
- 人员风险：建立知识库，避免单点依赖

## 七、沟通管理

### 7.1 沟通计划
| 沟通对象 | 沟通内容 | 沟通频率 | 沟通方式 |
|----------|----------|----------|----------|
| 项目干系人 | 项目进展 | 双周 | 邮件/会议 |
| 项目团队 | 任务安排 | 每周 | 站会 |
| 客户 | 需求确认 | 按需 | 会议/电话 |

### 7.2 报告机制
- 日报：项目团队成员提交工作日报
- 周报：项目经理提交项目周报
- 月报：提交项目月度总结报告`,

  9: `# 工程量清单报价

## 报价说明

1. 本报价根据招标文件、设计图纸及现场踏勘情况编制
2. 货币单位：人民币
3. 报价有效期：90天
4. 本报价包含完成全部工作内容所需的人工、材料、设备、管理、利润、税金等一切费用

## 分部分项工程量清单

| 序号 | 项目编码 | 项目名称 | 项目特征 | 计量单位 | 工程量 | 综合单价 | 合价 |
|------|----------|----------|----------|----------|--------|----------|------|
| 一 | | | 土建工程 | | | | |
| 1 | 010101001 | 平整场地 | 1.土壤类别:三类土 | m² | 5000 | 8.50 | 42,500 |
| 2 | 010101002 | 挖土方 | 1.挖掘深度:2m | m³ | 3000 | 35.00 | 105,000 |
| 3 | 010103001 | 回填方 | 1.填方材料:原土 | m³ | 2000 | 25.00 | 50,000 |
| 二 | | | 主体结构 | | | | |
| 4 | 010501001 | 基础混凝土 | 1.C30混凝土 | m³ | 800 | 520.00 | 416,000 |
| 5 | 010502001 | 柱混凝土 | 1.C35混凝土 | m³ | 600 | 550.00 | 330,000 |
| 6 | 010503001 | 梁混凝土 | 1.C35混凝土 | m³ | 1200 | 540.00 | 648,000 |
| 三 | | | 装饰装修 | | | | |
| 7 | 011102001 | 石材楼地面 | 1.20mm厚大理石 | m² | 3000 | 280.00 | 840,000 |
| 8 | 011204001 | 石材墙面 | 1.25mm厚花岗岩 | m² | 2000 | 320.00 | 640,000 |

## 措施项目清单

| 序号 | 项目名称 | 计算基础 | 费率(%) | 金额(元) |
|------|----------|----------|---------|----------|
| 1 | 安全文明施工费 | 分部分项工程费 | 3.5 | 85,000 |
| 2 | 夜间施工增加费 | 分部分项工程费 | 0.5 | 12,000 |
| 3 | 二次搬运费 | 分部分项工程费 | 0.8 | 19,000 |
| 4 | 冬雨季施工增加费 | 分部分项工程费 | 1.0 | 24,000 |

## 其他项目清单

| 序号 | 项目名称 | 金额(元) | 备注 |
|------|----------|----------|------|
| 1 | 暂列金额 | 500,000 | 工程变更预留 |
| 2 | 暂估价 | 200,000 | 专业工程暂估价 |
| 3 | 计日工 | 50,000 | 零星工作 |
| 4 | 总承包服务费 | 80,000 | 协调配合费用 |

## 规费与税金

| 序号 | 项目名称 | 计算基础 | 费率(%) | 金额(元) |
|------|----------|----------|---------|----------|
| 1 | 社会保险费 | 人工费 | 26.0 | 180,000 |
| 2 | 住房公积金 | 人工费 | 8.0 | 55,000 |
| 3 | 工程排污费 | 分部分项工程费 | 0.1 | 2,400 |
| 4 | 增值税 | 不含税工程造价 | 9.0 | 280,000 |

## 报价汇总

| 序号 | 汇总内容 | 金额(元) |
|------|----------|----------|
| 1 | 分部分项工程费 | 3,371,500 |
| 2 | 措施项目费 | 140,000 |
| 3 | 其他项目费 | 830,000 |
| 4 | 规费 | 237,400 |
| 5 | 税金 | 418,000 |
| 6 | 投标总价 | 4,996,900 |

## 价格说明

1. 本报价为固定总价合同，除工程变更外不予调整
2. 主要材料价格波动超过5%时可调整
3. 付款方式：
   - 预付款：合同价的30%
   - 进度款：按月支付完成工程量的80%
   - 竣工验收后支付至95%
   - 质保金5%期满后退还`
}

// Mock 模板数据
const mockTemplates = [
  {
    id: 1,
    name: '智慧城市项目技术方案模板',
    category: 'technical',
    tags: ['智慧城市', '大数据', '云计算', '系统集成'],
    description: '适用于智慧城市类投标项目的技术方案模板，包含总体架构、技术方案、实施方案等内容',
    downloads: 1280,
    updateTime: '2024-02-15',
    version: '2.1',
    fileSize: '2.8 MB',
    content: templateContents[1],
    structure: [
      {
        name: '总体方案',
        type: 'folder',
        children: [
          { name: '项目背景与需求分析.doc', type: 'file' },
          { name: '总体架构设计.doc', type: 'file' },
          { name: '技术路线.doc', type: 'file' }
        ]
      },
      {
        name: '技术方案',
        type: 'folder',
        children: [
          { name: '系统功能设计.doc', type: 'file' },
          { name: '数据库设计.doc', type: 'file' },
          { name: '接口设计.doc', type: 'file' }
        ]
      },
      {
        name: '实施方案',
        type: 'folder',
        children: [
          { name: '项目组织架构.doc', type: 'file' },
          { name: '实施计划.doc', type: 'file' },
          { name: '质量保障措施.doc', type: 'file' }
        ]
      }
    ]
  },
  {
    id: 2,
    name: '软件开发项目技术方案模板',
    category: 'technical',
    tags: ['软件开发', 'Web应用', '微服务', 'Java'],
    description: '适用于软件开发类投标项目的技术方案模板，支持瀑布和敏捷开发模式',
    downloads: 2150,
    updateTime: '2024-01-20',
    version: '3.0',
    fileSize: '3.5 MB',
    content: templateContents[2],
    structure: [
      {
        name: '需求分析',
        type: 'folder',
        children: [
          { name: '业务需求.doc', type: 'file' },
          { name: '功能需求.doc', type: 'file' },
          { name: '非功能需求.doc', type: 'file' }
        ]
      },
      {
        name: '架构设计',
        type: 'folder',
        children: [
          { name: '应用架构.doc', type: 'file' },
          { name: '技术架构.doc', type: 'file' },
          { name: '部署架构.doc', type: 'file' }
        ]
      }
    ]
  },
  {
    id: 3,
    name: '系统集成项目技术方案模板',
    category: 'technical',
    tags: ['系统集成', '硬件', '网络', '安全'],
    description: '适用于系统集成类投标项目的技术方案模板，包含硬件配置、网络设计等内容',
    downloads: 890,
    updateTime: '2024-02-10',
    version: '1.8',
    fileSize: '2.2 MB',
    content: `# 系统集成项目技术方案

## 一、需求分析
- 硬件配置需求
- 网络架构需求
- 安全防护需求

## 二、系统设计
### 2.1 硬件配置方案
| 设备类型 | 型号配置 | 数量 | 单价 |
|----------|----------|------|------|
| 服务器 | Dell R740 | 5台 | XX万 |
| 存储设备 | EMC VNX | 1套 | XX万 |

### 2.2 网络设计方案
- 核心交换机配置
- 接入交换机配置
- 防火墙配置

## 三、实施方案
- 设备到货验收
- 系统安装调试
- 联调测试`,
    structure: [
      { name: '系统需求.doc', type: 'file' },
      { name: '硬件配置方案.doc', type: 'file' },
      { name: '网络设计方案.doc', type: 'file' }
    ]
  },
  {
    id: 4,
    name: '投标函及投标函附录模板',
    category: 'commercial',
    tags: ['投标函', '标准模板', '通用'],
    description: '投标函及投标函附录标准模板，适用于各类投标项目',
    downloads: 3580,
    updateTime: '2024-01-05',
    version: '4.2',
    fileSize: '580 KB',
    content: templateContents[4],
    structure: [
      { name: '投标函.doc', type: 'file' },
      { name: '投标函附录.doc', type: 'file' },
      { name: '法定代表人身份证明.doc', type: 'file' },
      { name: '授权委托书.doc', type: 'file' }
    ]
  },
  {
    id: 5,
    name: '商务偏差表模板',
    category: 'commercial',
    tags: ['商务偏差表', '响应表'],
    description: '商务条款偏差表标准模板',
    downloads: 1920,
    updateTime: '2023-12-15',
    version: '2.0',
    fileSize: '320 KB',
    content: `# 商务条款偏差表

| 序号 | 招标文件条款号 | 招标文件条款内容 | 投标响应 | 偏差说明 |
|------|----------------|------------------|----------|----------|
| 1 | 12.1 | 付款方式：3-6-1 | 完全响应 | 无偏差 |
| 2 | 15.2 | 质保期：2年 | 完全响应 | 无偏差 |
| 3 | 18.1 | 交货期：30天 | 部分响应 | 承诺35天交货 |

说明：
1. "完全响应"表示完全接受招标文件条款
2. "部分响应"表示有条件接受，请在偏差说明中详细说明
3. "不响应"表示不能接受，请在偏差说明中详细说明原因`,
    structure: [
      { name: '商务条款偏差表.doc', type: 'file' },
      { name: '填写说明.doc', type: 'file' }
    ]
  },
  {
    id: 6,
    name: '项目实施方案模板',
    category: 'implementation',
    tags: ['实施', '进度', '质量', '风险'],
    description: '项目实施全过程方案模板，覆盖项目启动到验收全流程',
    downloads: 1680,
    updateTime: '2024-02-01',
    version: '3.5',
    fileSize: '4.2 MB',
    content: templateContents[6],
    structure: [
      {
        name: '项目启动',
        type: 'folder',
        children: [
          { name: '项目组织架构.doc', type: 'file' },
          { name: '项目管理制度.doc', type: 'file' }
        ]
      },
      {
        name: '项目实施',
        type: 'folder',
        children: [
          { name: '实施进度计划.doc', type: 'file' },
          { name: '里程碑计划.doc', type: 'file' },
          { name: '资源保障.doc', type: 'file' }
        ]
      },
      {
        name: '项目收尾',
        type: 'folder',
        children: [
          { name: '验收方案.doc', type: 'file' },
          { name: '移交方案.doc', type: 'file' }
        ]
      }
    ]
  },
  {
    id: 7,
    name: '项目培训方案模板',
    category: 'implementation',
    tags: ['培训', '用户培训', '技术培训'],
    description: '项目培训方案模板，包含用户培训、技术培训、运维培训等',
    downloads: 1120,
    updateTime: '2024-01-25',
    version: '2.2',
    fileSize: '1.5 MB',
    content: `# 项目培训方案

## 一、培训目标
- 使用户熟练掌握系统操作
- 使管理员掌握系统维护
- 使技术人员掌握技术原理

## 二、培训对象
| 类别 | 人数 | 培训时长 | 培训内容 |
|------|------|----------|----------|
| 普通用户 | 50 | 4学时 | 基础操作 |
| 系统管理员 | 5 | 8学时 | 系统管理 |
| 技术人员 | 3 | 16学时 | 技术原理 |

## 三、培训方式
- 现场集中培训
- 在线视频培训
- 操作手册自学

## 四、考核方式
- 理论考试
- 实操考核
- 发放培训证书`,
    structure: [
      { name: '培训计划.doc', type: 'file' },
      { name: '培训课程大纲.doc', type: 'file' },
      { name: '培训考核方案.doc', type: 'file' }
    ]
  },
  {
    id: 8,
    name: '售后服务方案模板',
    category: 'implementation',
    tags: ['售后', '运维', '服务承诺'],
    description: '售后服务承诺及运维方案模板',
    downloads: 1450,
    updateTime: '2024-01-10',
    version: '2.8',
    fileSize: '1.8 MB',
    content: `# 售后服务方案

## 一、服务承诺
1. 质保期：X年免费质保
2. 响应时间：
   - 电话响应：即时
   - 现场响应：4小时内
   - 问题解决：24小时内

## 二、服务内容
### 2.1 日常维护
- 系统巡检：每月1次
- 数据备份：每日1次
- 性能优化：每季度1次

### 2.2 故障处理
- 故障受理：7×24小时热线
- 故障诊断：远程或现场
- 故障修复：按SLA承诺

### 2.3 升级服务
- 免费小版本升级
- 优惠大版本升级

## 三、应急响应
建立应急响应机制，确保关键业务不中断`,
    structure: [
      { name: '服务体系.doc', type: 'file' },
      { name: '服务承诺.doc', type: 'file' },
      { name: '应急响应预案.doc', type: 'file' }
    ]
  },
  {
    id: 9,
    name: '工程量清单报价模板',
    category: 'quotation',
    tags: ['工程量清单', '报价', 'Excel'],
    description: '标准工程量清单报价Excel模板，支持分部分项报价',
    downloads: 2680,
    updateTime: '2024-02-18',
    version: '5.0',
    fileSize: '1.2 MB',
    content: templateContents[9],
    structure: [
      { name: '工程量清单.xlsx', type: 'file' },
      { name: '报价汇总表.xlsx', type: 'file' },
      { name: '填写说明.doc', type: 'file' }
    ]
  },
  {
    id: 10,
    name: '软件服务费报价模板',
    category: 'quotation',
    tags: ['软件', '人天', '服务费'],
    description: '软件开发项目服务费报价模板，支持人天单价和总价计算',
    downloads: 1890,
    updateTime: '2024-01-30',
    version: '3.1',
    fileSize: '890 KB',
    content: `# 软件服务费报价

## 一、人员报价
| 角色 | 级别 | 人天单价(元) | 人天数 | 小计(元) |
|------|------|--------------|--------|----------|
| 项目经理 | 高级 | 3000 | 60 | 180,000 |
| 架构师 | 高级 | 3500 | 40 | 140,000 |
| 开发工程师 | 中级 | 2000 | 300 | 600,000 |
| 测试工程师 | 中级 | 1800 | 80 | 144,000 |
| 合计 | | | | 1,064,000 |

## 二、其他费用
| 费用项目 | 金额(元) | 说明 |
|----------|----------|------|
| 差旅费 | 50,000 | 按实结算 |
| 培训费 | 30,000 | 含培训师费 |
| 合计 | 80,000 | |

## 三、报价汇总
| 序号 | 项目 | 金额(元) |
|------|------|----------|
| 1 | 人员服务费 | 1,064,000 |
| 2 | 其他费用 | 80,000 |
| 3 | 税金(6%) | 68,640 |
| 4 | 合计 | 1,212,640 |`,
    structure: [
      { name: '人员报价表.xlsx', type: 'file' },
      { name: '服务费汇总表.xlsx', type: 'file' },
      { name: '价格说明.doc', type: 'file' }
    ]
  },
  {
    id: 11,
    name: '企业资质汇编模板',
    category: 'qualification',
    tags: ['营业执照', '资质证书', '认证'],
    description: '企业资质文件汇编模板，用于投标文件资质部分',
    downloads: 980,
    updateTime: '2023-11-20',
    version: '1.5',
    fileSize: '650 KB',
    content: `# 企业资质汇编

## 一、营业执照
- 统一社会信用代码：XXXXXXXX
- 注册资本：XXX万元
- 成立日期：XXXX年XX月XX日
- 营业期限：长期

## 二、资质证书
### 2.1 企业资质
- ISO9001质量管理体系认证
- ISO27001信息安全管理体系
- CMMI5级认证
- 信息系统建设和服务能力CS4级

### 2.2 安全资质
- 涉密信息系统集成资质
- 信息安全服务资质

### 2.3 行业资质
- [相关行业资质]

## 三、认证证书
- 高新技术企业证书
- 软件企业证书`,
    structure: [
      { name: '营业执照.doc', type: 'file' },
      { name: '资质证书汇编.doc', type: 'file' },
      { name: '认证证书汇编.doc', type: 'file' },
      { name: '荣誉证书汇编.doc', type: 'file' }
    ]
  },
  {
    id: 12,
    name: '项目人员简历模板',
    category: 'qualification',
    tags: ['人员', '简历', '证书'],
    description: '项目团队成员简历模板，包含项目经理、技术负责人等核心人员',
    downloads: 1350,
    updateTime: '2024-01-15',
    version: '2.0',
    fileSize: '420 KB',
    content: `# 项目人员简历

## 一、项目经理

### 基本信息
- 姓名：XXX
- 学历：本科
- 专业：计算机科学与技术
- 工作年限：15年
- 职称：高级工程师

### 项目经验
1. XXX项目（2021-2023）担任项目经理
2. XXX项目（2019-2021）担任项目经理
3. XXX项目（2017-2019）担任技术负责人

### 证书
- PMP项目管理专业人士
- 信息系统项目管理师（高级）

## 二、技术负责人

### 基本信息
- 姓名：XXX
- 学历：硕士
- 专业：软件工程
- 工作年限：12年
- 职称：高级工程师

### 项目经验
1. XXX项目（2022-2023）担任架构师
2. XXX项目（2020-2022）担任技术负责人

### 证书
- 系统架构设计师
- AWS认证架构师`,
    structure: [
      { name: '项目经理简历.doc', type: 'file' },
      { name: '技术负责人简历.doc', type: 'file' },
      { name: '其他人员简历.doc', type: 'file' },
      { name: '人员证书汇编.doc', type: 'file' }
    ]
  },
  {
    id: 13,
    name: '设备采购合同模板',
    category: 'contract',
    tags: ['设备采购', '合同', '范本'],
    description: '设备采购合同标准范本',
    downloads: 780,
    updateTime: '2023-12-01',
    version: '1.2',
    fileSize: '560 KB',
    content: `# 设备采购合同

**合同编号：** XXXX-2024-XXX

**签订日期：** 2024年XX月XX日

**甲方（需方）：** XXX公司
**乙方（供方）：** XXX公司

## 一、产品名称、型号、数量、金额
| 序号 | 产品名称 | 型号规格 | 单位 | 数量 | 单价(元) | 金额(元) |
|------|----------|----------|------|------|----------|----------|
| 1 | | | | | | |

## 二、质量标准
产品符合国家标准及行业标准，并提供质量证明文件。

## 三、交货时间及地点
- 交货时间：合同签订后XX日内
- 交货地点：XXXXXXXX

## 四、付款方式
1. 预付款：合同签订后支付30%
2. 到货款：验收合格后支付60%
3. 质保金：10%质保期满后支付

## 五、售后服务
- 质保期：XX年
- 响应时间：XX小时内

## 六、违约责任
[详细条款]`,
    structure: [
      { name: '设备采购合同.doc', type: 'file' },
      { name: '合同附件.doc', type: 'file' }
    ]
  },
  {
    id: 14,
    name: '软件开发服务合同模板',
    category: 'contract',
    tags: ['软件开发', '服务合同', '范本'],
    description: '软件开发服务合同标准范本',
    downloads: 1120,
    updateTime: '2024-01-08',
    version: '2.5',
    fileSize: '780 KB',
    content: `# 软件开发服务合同

**合同编号：** XXXX-2024-XXX

**甲方（委托方）：** XXX公司
**乙方（开发方）：** XXX公司

## 一、服务内容
乙方为甲方开发[系统名称]，具体功能需求详见附件。

## 二、开发周期
项目总工期XX个月，自XX年XX月XX日至XX年XX月XX日。

## 三、服务费用及支付
1. 合同总金额：人民币XX万元
2. 支付方式：
   - 首付款：30%（合同签订后）
   - 进度款：40%（系统上线后）
   - 验收款：25%（验收合格后）
   - 质保金：5%（质保期满后）

## 四、验收标准
1. 功能验收：按需求规格说明书
2. 性能验收：响应时间<2秒
3. 安全验收：通过安全测试

## 五、知识产权
1. 甲方拥有定制开发部分的著作权
2. 乙方保留通用组件的知识产权
3. 源代码交付

## 六、售后服务
- 免费质保期：1年
- 维护响应：7×24小时`,
    structure: [
      { name: '软件开发服务合同.doc', type: 'file' },
      { name: '服务级别协议(SLA).doc', type: 'file' },
      { name: '保密协议.doc', type: 'file' }
    ]
  },
  {
    id: 15,
    name: '技术方案-数据中心建设',
    category: 'technical',
    tags: ['数据中心', '机房', '服务器', '存储'],
    description: '数据中心建设技术方案模板',
    downloads: 680,
    updateTime: '2023-11-25',
    version: '1.6',
    fileSize: '3.8 MB',
    content: `# 数据中心建设技术方案

## 一、需求分析
### 1.1 机房环境要求
- 面积：XXX平方米
- 机柜数量：XX个
- 供电容量：XX kW
- 制冷量：XX冷吨

### 1.2 建设标准
- GB50174-2017《数据中心设计规范》
- TIA-942《数据中心电信基础设施标准》

## 二、系统设计方案
### 2.1 机房布局
- 主机房区
- 配电区
- 空调区
- 监控区

### 2.2 供配电系统
- 双路市电接入
- UPS不间断电源
- 柴油发电机组

### 2.3 空调系统
- 精密空调N+1冗余
- 冷通道封闭
- 氟泵节能技术

### 2.4 综合布线
- 光纤骨干网络
- 六类铜缆系统
- 走线架上走线

## 三、设备配置
| 设备类型 | 品牌 | 型号 | 数量 |
|----------|------|------|------|
| 服务器 | | | |
| 存储 | | | |
| 网络设备 | | | |

## 四、实施方案
- 阶段一：机房装修（X周）
- 阶段二：设备安装（X周）
- 阶段三：系统调试（X周）`,
    structure: [
      { name: '需求分析.doc', type: 'file' },
      { name: '架构设计.doc', type: 'file' },
      { name: '设备配置.doc', type: 'file' },
      { name: '实施方案.doc', type: 'file' }
    ]
  }
]

const templates = ref([])

const loadTemplatePersistence = () => {
  if (!isMockMode()) {
    return {
      patches: {},
      copies: [],
    }
  }
  return getTemplateDemoState()
}

const saveTemplatePersistence = (state) => {
  if (!isMockMode()) {
    return
  }
  saveTemplateDemoState(state)
}

const applyTemplatePersistence = (list) => {
  if (!isMockMode()) {
    return list
  }
  const state = loadTemplatePersistence()
  const patchedTemplates = list.map((item) => ({
    ...item,
    ...(state.patches?.[String(item.id)] || {}),
  }))
  const copyIds = new Set(patchedTemplates.map((item) => String(item.id)))
  const copies = Array.isArray(state.copies)
    ? state.copies.filter((item) => !copyIds.has(String(item.id)))
    : []
  return [...copies, ...patchedTemplates]
}

const persistTemplatePatch = (templateId, patch) => {
  if (!isMockMode()) {
    return
  }
  const state = loadTemplatePersistence()
  state.patches = state.patches || {}
  state.patches[String(templateId)] = {
    ...(state.patches[String(templateId)] || {}),
    ...patch,
  }
  saveTemplatePersistence(state)
}

const persistTemplateCopy = (template) => {
  if (!isMockMode()) {
    return
  }
  const state = loadTemplatePersistence()
  state.copies = Array.isArray(state.copies) ? state.copies : []
  state.copies = [
    template,
    ...state.copies.filter((item) => String(item.id) !== String(template.id)),
  ]
  saveTemplatePersistence(state)
}

const removeTemplatePersistence = (templateId) => {
  if (!isMockMode()) {
    return
  }
  const state = loadTemplatePersistence()
  if (state.patches) {
    delete state.patches[String(templateId)]
  }
  if (Array.isArray(state.copies)) {
    state.copies = state.copies.filter((item) => String(item.id) !== String(templateId))
  }
  saveTemplatePersistence(state)
}

const loadTemplates = async () => {
  loading.value = true
  try {
    const result = await knowledgeApi.templates.getList()
    if (result?.success) {
      templates.value = isMockMode() ? applyTemplatePersistence(result.data || []) : (result.data || [])
      pagination.total = templates.value.length
      featurePlaceholder.value = null
    } else {
      templates.value = []
      pagination.total = 0
      featurePlaceholder.value = notifyFeatureUnavailable(result, {
        fallback: {
          title: '模板库暂未接入',
          hint: '模板相关页面已切换为统一占位态，不再把未接入能力当成空数据。',
        },
      })
      if (!featurePlaceholder.value && result?.message) {
        ElMessage.error(result.message)
      }
    }
  } finally {
    loading.value = false
  }
}

// 所有标签
const allTags = computed(() => {
  const tagSet = new Set()
  templates.value.forEach(t => t.tags.forEach(tag => tagSet.add(tag)))
  return Array.from(tagSet).sort()
})

// 过滤后的模板
const filteredTemplates = computed(() => {
  let result = templates.value

  // 分类过滤
  if (activeCategory.value !== 'all') {
    result = result.filter(item => item.category === activeCategory.value)
  }

  // 名称搜索
  if (searchForm.name) {
    const keyword = searchForm.name.toLowerCase()
    result = result.filter(item =>
      item.name.toLowerCase().includes(keyword) ||
      item.description.toLowerCase().includes(keyword)
    )
  }

  // 标签过滤
  if (searchForm.tags.length > 0) {
    result = result.filter(item =>
      searchForm.tags.some(tag => item.tags.includes(tag))
    )
  }

  // 排序
  if (searchForm.sort === 'downloads') {
    result.sort((a, b) => b.downloads - a.downloads)
  } else if (searchForm.sort === 'updateTime') {
    result.sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime))
  } else if (searchForm.sort === 'name') {
    result.sort((a, b) => a.name.localeCompare(b.name, 'zh'))
  }

  pagination.total = result.length

  const start = (pagination.page - 1) * pagination.pageSize
  const end = start + pagination.pageSize

  return result.slice(start, end)
})

// 获取分类标签
const getCategoryLabel = (category) => {
  return categories[category]?.label || category
}

// 获取分类图标
const getCategoryIcon = (category) => {
  return categories[category]?.icon || Document
}

// 获取分类颜色
const getCategoryColor = (category) => {
  return categories[category]?.color || '#409eff'
}

// 获取分类标签类型
const getCategoryTagType = (category) => {
  return categories[category]?.tagType || ''
}

// 获取项目状态标签
const getProjectStatusLabel = (status) => {
  return projectStatusConfig[status]?.label || status
}

// 获取项目状态类型
const getProjectStatusType = (status) => {
  return projectStatusConfig[status]?.type || 'info'
}

// 格式化数字
const formatNumber = (num) => {
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return date
}

// 分类切换
const handleCategoryChange = () => {
  pagination.page = 1
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
}

// 重置
const handleReset = () => {
  searchForm.name = ''
  searchForm.tags = []
  searchForm.sort = 'default'
  pagination.page = 1
}

// 新增
const handleAdd = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入模板名称', '新建模板', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：智慧办公技术方案模板'
    })

    const category = activeCategory.value === 'all' ? 'technical' : activeCategory.value
    const result = await knowledgeApi.templates.create({
      name: value,
      category,
      tags: [],
      description: '新建模板',
    })

    if (!result?.success) {
      ElMessage.error(result?.message || '创建失败')
      return
    }

    templates.value.unshift(result.data)
    ElMessage.success('模板创建成功')
  } catch {
    // 用户取消
  }
}

// 预览
const handlePreview = async (row) => {
  const result = await knowledgeApi.templates.getDetail(row.id)
  previewTemplate.value = result?.success && result.data ? result.data : row
  previewDialogVisible.value = true
  activePreviewTab.value = 'content'
}

// 一键使用
const handleUseTemplate = (row) => {
  selectedTemplate.value = row
  useTemplateForm.docType = 'standalone'
  useTemplateForm.projectId = ''
  useTemplateForm.docName = `${row.name}应用`
  useTemplateForm.applyOptions = ['content', 'format', 'styles']
  useTemplateDialogVisible.value = true
}

// 从预览使用模板
const useTemplateFromPreview = () => {
  previewDialogVisible.value = false
  handleUseTemplate(previewTemplate.value)
}

// 确认使用模板
const confirmUseTemplate = async () => {
  if (!useTemplateForm.docName.trim()) {
    ElMessage.warning('请输入文档名称')
    return
  }

  // 构建应用结果消息
  const docTypeMap = {
    tech: '技术方案',
    business: '商务应答',
    contract: '合同文档',
    standalone: '独立文档'
  }

  let message = `已使用模板「${selectedTemplate.value.name}」创建${docTypeMap[useTemplateForm.docType]}`
  if (useTemplateForm.projectId) {
    const project = inProgressProjects.value.find(p => p.id === useTemplateForm.projectId)
    message += `，关联项目：${project?.name}`
  }

  if (isPersistentTemplateId(selectedTemplate.value.id)) {
    const useResult = await knowledgeApi.templates.recordUse(selectedTemplate.value.id, {
      documentName: useTemplateForm.docName,
      docType: useTemplateForm.docType,
      projectId: useTemplateForm.projectId || null,
      applyOptions: useTemplateForm.applyOptions,
      usedBy: getCurrentUserId(),
    })
    if (!useResult?.success) {
      ElMessage.error(useResult?.message || '模板使用记录失败')
      return
    }
  } else {
    const templateIndex = templates.value.findIndex(t => t.id === selectedTemplate.value.id)
    if (templateIndex > -1) {
      const nextUseCount = (templates.value[templateIndex].useCount || 0) + 1
      templates.value[templateIndex].useCount = nextUseCount
      persistTemplatePatch(templates.value[templateIndex].id, {
        useCount: nextUseCount,
      })
    }
  }

  ElMessage.success(message)
  useTemplateDialogVisible.value = false

  // 实际创建文档并跳转到编辑页面
  await createDocumentFromTemplate()
}

// 根据模板创建文档
const createDocumentFromTemplate = async () => {
  try {
    // 构造新文档数据
    const newDocument = {
      id: `doc_${Date.now()}`, // 生成唯一ID
      name: useTemplateForm.docName,
      templateId: selectedTemplate.value.id,
      templateName: selectedTemplate.value.name,
      docType: useTemplateForm.docType,
      projectId: useTemplateForm.projectId || null,
      content: selectedTemplate.value.content || generateTemplateContent(),
      createdAt: new Date().toISOString(),
      createdBy: userStore.currentUser?.name || '当前用户',
      status: 'draft'
    }

    if (isMockMode()) {
      // 仅在 mock 模式下保留本地草稿，避免污染 API 模式的真实展示
      const savedDocs = JSON.parse(localStorage.getItem('draftDocuments') || '[]')
      savedDocs.push(newDocument)
      localStorage.setItem('draftDocuments', JSON.stringify(savedDocs))
    }

    // 跳转到文档编辑页面
    await router.push({
      name: 'DocumentEditor',
      params: { id: newDocument.id }
    })

    ElMessage.success(`文档「${useTemplateForm.docName}」创建成功`)
  } catch (error) {
    console.error('创建文档失败:', error)
    ElMessage.error(`创建文档失败: ${error.message}`)
  }
}

// 根据模板类型生成默认内容
const generateTemplateContent = () => {
  const docType = useTemplateForm.docType
  const template = selectedTemplate.value

  const baseContent = {
    sections: [],
    metadata: {
      templateId: template.id,
      templateName: template.name,
      createdAt: new Date().toISOString()
    }
  }

  // 根据文档类型生成不同的章节结构
  switch (docType) {
    case 'tech':
      baseContent.sections = [
        { id: '1', type: 'chapter', title: '1. 项目概述', content: '', level: 1 },
        { id: '2', type: 'chapter', title: '2. 技术方案', content: '', level: 1 },
        { id: '2-1', type: 'section', title: '2.1 系统架构', content: '', level: 2, parentId: '2' },
        { id: '2-2', type: 'section', title: '2.2 功能设计', content: '', level: 2, parentId: '2' },
        { id: '2-3', type: 'section', title: '2.3 技术优势', content: '', level: 2, parentId: '2' },
        { id: '3', type: 'chapter', title: '3. 实施方案', content: '', level: 1 },
        { id: '4', type: 'chapter', title: '4. 售后服务', content: '', level: 1 }
      ]
      break
    case 'business':
      baseContent.sections = [
        { id: '1', type: 'chapter', title: '1. 应答说明', content: '', level: 1 },
        { id: '2', type: 'chapter', title: '2. 商务条款响应', content: '', level: 1 },
        { id: '3', type: 'chapter', title: '3. 报价明细', content: '', level: 1 },
        { id: '4', type: 'chapter', title: '4. 付款方式', content: '', level: 1 },
        { id: '5', type: 'chapter', title: '5. 交付周期', content: '', level: 1 }
      ]
      break
    case 'contract':
      baseContent.sections = [
        { id: '1', type: 'chapter', title: '1. 合同主体', content: '', level: 1 },
        { id: '2', type: 'chapter', title: '2. 合同标的', content: '', level: 1 },
        { id: '3', type: 'chapter', title: '3. 合同金额', content: '', level: 1 },
        { id: '4', type: 'chapter', title: '4. 履约条款', content: '', level: 1 },
        { id: '5', type: 'chapter', title: '5. 违约责任', content: '', level: 1 }
      ]
      break
    default:
      baseContent.sections = [
        { id: '1', type: 'chapter', title: '第一章', content: '', level: 1 },
        { id: '2', type: 'chapter', title: '第二章', content: '', level: 1 },
        { id: '3', type: 'chapter', title: '第三章', content: '', level: 1 }
      ]
  }

  return JSON.stringify(baseContent)
}

// 下载模板
const handleDownloadTemplate = () => {
  if (previewTemplate.value) {
    downloadTextFile(
      `${previewTemplate.value.name}.md`,
      previewTemplate.value.content || previewTemplate.value.description || previewTemplate.value.name,
      'text/markdown;charset=utf-8'
    )
    ElMessage.success(`开始下载：${previewTemplate.value.name}`)
    if (isPersistentTemplateId(previewTemplate.value.id)) {
      knowledgeApi.templates.recordDownload(previewTemplate.value.id, {
        downloadedBy: getCurrentUserId(),
      }).then((result) => {
        if (result?.success && result.data) {
          upsertTemplate(result.data)
          previewTemplate.value = result.data
        }
      }).catch(() => {})
    } else {
      const templateIndex = templates.value.findIndex(t => t.id === previewTemplate.value.id)
      if (templateIndex > -1) {
        templates.value[templateIndex].downloads++
        persistTemplatePatch(templates.value[templateIndex].id, {
          downloads: templates.value[templateIndex].downloads,
        })
        previewTemplate.value = { ...previewTemplate.value, downloads: templates.value[templateIndex].downloads }
      }
    }
  }
}

// 更多操作
const handleMoreAction = async (command, row) => {
  switch (command) {
    case 'edit':
      ElMessageBox.prompt('请输入新的模板名称', '编辑模板', {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputValue: row.name
      }).then(async ({ value }) => {
        const result = await knowledgeApi.templates.update(row.id, {
          ...row,
          name: value,
        })

        if (!result?.success) {
          ElMessage.error(result?.message || '更新失败')
          return
        }

        const index = templates.value.findIndex(t => t.id === row.id)
        if (index > -1) {
          templates.value.splice(index, 1, result.data)
        }
        persistTemplatePatch(row.id, result.data)
        ElMessage.success('模板更新成功')
      }).catch(() => {})
      break
    case 'copy':
      if (isPersistentTemplateId(row.id)) {
        const copyResult = await knowledgeApi.templates.copy(row.id, {
          name: `${row.name}（副本）`,
          createdBy: getCurrentUserId(),
        })
        if (!copyResult?.success) {
          ElMessage.error(copyResult?.message || '复制失败')
          break
        }
        templates.value.unshift(copyResult.data)
      } else {
        const copiedTemplate = {
          ...row,
          id: `TPL_COPY_${Date.now()}`,
          name: `${row.name}（副本）`,
          version: row.version || '1.0',
          downloads: 0,
          useCount: 0,
          updateTime: new Date().toISOString().split('T')[0],
        }
        templates.value.unshift(copiedTemplate)
        persistTemplateCopy(copiedTemplate)
      }
      pagination.total = templates.value.length
      ElMessage.success(`已复制模板：${row.name}`)
      break
    case 'version':
      versionPlaceholder.value = null
      if (isPersistentTemplateId(row.id)) {
        const versionResult = await knowledgeApi.templates.getVersions(row.id)
        if (isFeatureUnavailableResponse(versionResult)) {
          versionHistory.value = []
          versionPlaceholder.value = notifyFeatureUnavailable(versionResult, {
            fallback: {
              title: '版本历史暂未接入',
              hint: '后端补齐模板版本接口后，这里会展示真实版本轨迹。',
            },
          })
          versionDialogVisible.value = true
          break
        }
        if (!versionResult?.success) {
          ElMessage.error(versionResult?.message || '获取版本历史失败')
          break
        }
        versionHistory.value = (versionResult.data || []).map((version, index) => ({
          id: version.id,
          version: version.version,
          date: String(version.createdAt || '').slice(0, 10) || row.updateTime,
          description: version.description || '版本变更',
          isCurrent: index === 0,
        }))
      } else {
        versionPlaceholder.value = null
        versionHistory.value = [
          {
            id: 1,
            version: row.version,
            date: row.updateTime,
            description: '当前版本，优化了文档结构，增加了新功能',
            isCurrent: true
          },
          {
            id: 2,
            version: (parseFloat(row.version) - 0.1).toFixed(1),
            date: '2023-12-01',
            description: '上一版本，修复了若干问题',
            isCurrent: false
          },
          {
            id: 3,
            version: (parseFloat(row.version) - 0.2).toFixed(1),
            date: '2023-10-15',
            description: '初始版本',
            isCurrent: false
          }
        ]
      }
      versionDialogVisible.value = true
      break
    case 'download':
      downloadTextFile(
        `${row.name}.md`,
        row.content || row.description || row.name,
        'text/markdown;charset=utf-8'
      )
      if (isPersistentTemplateId(row.id)) {
        const downloadResult = await knowledgeApi.templates.recordDownload(row.id, {
          downloadedBy: getCurrentUserId(),
        })
        if (downloadResult?.success && downloadResult.data) {
          upsertTemplate(downloadResult.data)
        }
      } else {
        row.downloads = (row.downloads || 0) + 1
        persistTemplatePatch(row.id, {
          downloads: row.downloads,
        })
      }
      ElMessage.success(`开始下载：${row.name}`)
      break
    case 'delete':
      ElMessageBox.confirm(
        `确定要删除模板「${row.name}」吗？删除后不可恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        const result = await knowledgeApi.templates.delete(row.id)
        if (!result?.success && result !== undefined) {
          ElMessage.error(result?.message || '删除失败')
          return
        }
        const index = templates.value.findIndex(t => t.id === row.id)
        if (index > -1) {
          templates.value.splice(index, 1)
        }
        removeTemplatePersistence(row.id)
        ElMessage.success('删除成功')
      }).catch(() => {})
      break
  }
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
  loadTemplates()
})
</script>

<style scoped lang="scss">
.template-container {
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

  .category-tabs {
    background: #fff;
    border-radius: 4px;
    padding: 0 20px;
    margin-bottom: 20px;

    :deep(.el-tabs__header) {
      margin: 0;
    }

    .tab-label {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .search-card {
    margin-bottom: 20px;
  }

  .table-card {
    .name-cell {
      display: flex;
      align-items: flex-start;
      gap: 12px;

      .category-icon {
        font-size: 20px;
        flex-shrink: 0;
        margin-top: 2px;
      }

      .name-content {
        flex: 1;

        .name-text {
          display: block;
          font-size: 14px;
          color: #303133;
          margin-bottom: 4px;
        }

        .name-desc {
          display: block;
          font-size: 12px;
          color: #909399;
          line-height: 1.4;
        }
      }
    }

    .tags-cell {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;

      .tag-item {
        margin: 0;
      }

      .tag-more {
        margin: 0;
      }
    }

    .download-count {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #606266;
    }

    .pagination-wrapper {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }
}

// 预览对话框样式
.preview-dialog {
  .template-preview {
    .template-meta {
      margin-bottom: 20px;
    }

    .template-content-preview {
      margin-bottom: 20px;

      .preview-tabs {
        margin-top: 16px;

        .content-frame {
          max-height: 400px;
          overflow-y: auto;
          background: #f5f7fa;
          border-radius: 8px;
          padding: 16px;

          .template-content-text {
            margin: 0;
            white-space: pre-wrap;
            word-wrap: break-word;
            font-size: 13px;
            line-height: 1.8;
            color: #303133;
            font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
          }
        }

        .template-tree {
          .tree-node {
            display: flex;
            align-items: center;
            gap: 6px;
          }
        }
      }
    }

    .preview-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding-top: 16px;
      border-top: 1px solid #ebeef5;
    }
  }
}

// 使用模板对话框样式
.use-template-dialog {
  .use-template-content {
    .radio-option {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .project-option {
      display: flex;
      align-items: center;
      gap: 12px;
      width: 100%;

      .project-name {
        flex: 1;
        font-weight: 500;
      }

      .project-customer {
        color: #909399;
        font-size: 12px;
      }
    }

    .form-tip {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-top: 8px;
      font-size: 12px;
      color: #909399;
    }
  }
}

// 版本历史样式
.version-item {
  .version-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;

    .version-number {
      font-weight: 600;
      font-size: 16px;
      color: #303133;
    }
  }

  .version-description {
    color: #606266;
    margin-bottom: 8px;
  }

  .version-actions {
    display: flex;
    gap: 16px;
  }
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .template-container {
    padding: 12px;

    .page-header {
      flex-direction: column;
      gap: 12px;
      align-items: flex-start;

      .header-actions {
        width: 100%;

        .el-button {
          width: 100%;
        }
      }
    }

    .category-tabs {
      padding: 0 12px;

      :deep(.el-tabs__nav-wrap) {
        overflow-x: auto;
      }
    }

    .search-card {
      :deep(.el-form) {
        display: block;
      }

      :deep(.el-form-item) {
        display: block;
        margin-right: 0;
        margin-bottom: 12px;

        .el-input,
        .el-select {
          width: 100% !important;
        }
      }
    }

    .table-card {
      :deep(.el-table) {
        font-size: 12px;

        .el-table__body-wrapper {
          overflow-x: auto;
        }
      }

      .pagination-wrapper {
        justify-content: center;

        :deep(.el-pagination) {
          flex-wrap: wrap;
          justify-content: center;
        }

        :deep(.el-pagination__sizes),
        :deep(.el-pagination__jump) {
          display: none;
        }
      }
    }
  }

  .preview-dialog {
    :deep(.el-dialog) {
      width: 95% !important;
      margin: 0 auto;
    }

    :deep(.el-dialog__body) {
      padding: 16px;
    }

    .template-preview {
      .template-meta {
        :deep(.el-descriptions) {
          --el-descriptions-table-column-border: 1px solid #ebeef5;

          .el-descriptions__cell {
            padding: 8px;
          }
        }
      }

      .content-frame {
        max-height: 300px;
      }
    }
  }

  .use-template-dialog {
    :deep(.el-dialog) {
      width: 95% !important;
    }

    .use-template-content {
      :deep(.el-radio-group) {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
    }
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .el-button {
    min-height: 44px;
  }

  .table-card {
    :deep(.el-table__row) {
      .el-button {
        padding: 8px 12px;
      }
    }
  }
}

/* ==================== Button Enhancements ==================== */

.header-actions .el-button {
  min-width: 110px;
  height: 38px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.header-actions .el-button--primary {
  background: linear-gradient(135deg, #0369a1, #0284c7);
  border: none;
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.2);
}

.header-actions .el-button--primary:hover {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.3);
}

.header-actions .el-button--primary:active {
  transform: translateY(0);
}

/* ==================== Tab Enhancements ==================== */

.category-tabs :deep(.el-tabs__item) {
  height: 42px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.category-tabs :deep(.el-tabs__item:hover) {
  color: #0369a1;
}

.category-tabs :deep(.el-tabs__item.is-active) {
  color: #0369a1;
  font-weight: 600;
}

.category-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #0369a1, #0ea5e9);
  height: 3px;
  border-radius: 2px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-label .el-icon {
  font-size: 16px;
}

/* ==================== Search Card Enhancements ==================== */

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

/* ==================== Template Card Enhancements ==================== */

.template-card {
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  border: 1.5px solid #e5e7eb;
}

.template-card:hover {
  border-color: #0369a1;
  box-shadow: 0 8px 24px rgba(3, 105, 161, 0.12);
  transform: translateY(-2px);
}

.template-card:active {
  transform: translateY(0);
}

.template-card .card-actions {
  gap: 8px;
}

.template-card .card-actions .el-button {
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.template-card .card-actions .el-button:hover {
  transform: translateY(-1px);
}

/* ==================== Tag Enhancements ==================== */

:deep(.el-tag) {
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border: none;
}

:deep(.el-tag--primary) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #ffffff;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #ffffff;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #ffffff;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #ffffff;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #64748b, #475569);
  color: #ffffff;
}

/* ==================== Table Action Buttons ==================== */

.table-card :deep(.el-button--link) {
  font-size: 13px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.table-card :deep(.el-button--link:hover) {
  transform: translateX(2px);
}

/* ==================== Pagination Enhancement ==================== */

.pagination-wrapper :deep(.el-pagination) {
  gap: 8px;
}

.pagination-wrapper :deep(.el-pager li) {
  border-radius: 8px;
  min-width: 36px;
  height: 36px;
  font-size: 14px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.pagination-wrapper :deep(.el-pager li:hover) {
  background: #f1f5f9;
}

.pagination-wrapper :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #0369a1, #0284c7);
  color: #ffffff;
}

/* 修复表格排序箭头位置 - 与文字同行 */
:deep(.el-table th .cell) {
  display: inline-flex;
  align-items: center;
}

:deep(.el-table .caret-wrapper) {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  height: auto;
  margin-left: 4px;
  vertical-align: middle;
}

:deep(.el-table .sort-caret) {
  width: 0;
  height: 0;
  border: 0;
  margin: 0 2px;
}

:deep(.el-table .sort-caret.ascending) {
  border-bottom: 4px solid #c0c4cc;
}

:deep(.el-table .sort-caret.descending) {
  border-top: 4px solid #c0c4cc;
}

/* 修复版本列 el-tag 省略号问题 */
:deep(.el-table .el-tag) {
  max-width: none !important;
  overflow: visible !important;
  text-overflow: clip !important;
  white-space: nowrap !important;
}

:deep(.el-table .cell) {
  overflow: visible !important;
  text-overflow: clip !important;
}
</style>
