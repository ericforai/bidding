<template>
  <div class="workbench-page">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="stat in stats" :key="stat.key">
        <div class="b2b-stat-card">
          <div class="b2b-stat-icon" :style="{ background: stat.color }">
            <el-icon :size="24">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <div class="b2b-stat-content">
            <div class="b2b-stat-value">{{ stat.value }}</div>
            <div class="b2b-stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="quick-actions-card b2b-section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">快捷操作</span>
        </div>
      </template>
      <div class="quick-actions">
        <el-button
          v-for="action in quickActions"
          :key="action.key"
          :type="action.type"
          :icon="action.icon"
          @click="action.handler"
        >
          {{ action.label }}
        </el-button>
      </div>
    </el-card>

    <!-- 主要内容区域 -->
    <el-row :gutter="20" class="content-row">
      <!-- 左侧：流程状态跟踪 + 待办优先级 -->
      <el-col :xs="24" :lg="16">
        <!-- 流程状态跟踪 -->
        <el-card class="b2b-section-card section-card process-tracking-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><List /></el-icon>
                我的流程
              </span>
              <el-tag size="small" type="info">{{ myProcesses.length }} 个</el-tag>
            </div>
          </template>

          <el-timeline>
            <el-timeline-item
              v-for="process in myProcesses"
              :key="process.id"
              :timestamp="process.time"
              :type="getProcessType(process.status)"
            >
              <el-card class="process-item-card" shadow="hover">
                <div class="process-header">
                  <h4>{{ process.title }}</h4>
                  <div class="process-meta">
                    <el-tag size="small">{{ process.category }}</el-tag>
                    <el-tag size="small" :type="getProcessStatusType(process.status)">
                      {{ getProcessStatusLabel(process.status) }}
                    </el-tag>
                  </div>
                </div>
                <p class="process-desc">{{ process.description }}</p>

                <!-- 流程进度 -->
                <div v-if="process.progress" class="process-progress">
                  <el-progress
                    :percentage="process.progress"
                    :status="process.progress === 100 ? 'success' : undefined"
                  />
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 待办优先级管理 -->
        <el-card class="b2b-section-card section-card todos-priority-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Warning /></el-icon>
                待办事项
              </span>
              <el-tag size="small" type="danger">{{ priorityTodos.length }} 项</el-tag>
            </div>
          </template>

          <div class="todo-list">
            <div
              v-for="todo in priorityTodos"
              :key="todo.id"
              class="todo-item"
              :class="'priority-' + todo.priority"
            >
              <div class="todo-check">
                <el-checkbox v-model="todo.done" @change="handleTodoCheck(todo)" />
              </div>
              <div class="todo-content">
                <div class="todo-title" :class="{ done: todo.done }">
                  {{ todo.title }}
                </div>
                <div class="todo-meta">
                  <el-tag :type="getPriorityType(todo.priority)" size="small">
                    {{ getPriorityLabel(todo.priority) }}
                  </el-tag>
                  <span class="todo-deadline">
                    <el-icon><Clock /></el-icon>
                    {{ todo.deadline }}
                  </span>
                </div>
              </div>
              <div class="todo-actions">
                <el-button
                  :icon="View"
                  circle
                  size="small"
                  @click="handleViewTodo(todo)"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：进行中项目 -->
      <el-col :xs="24" :lg="8">
        <el-card class="b2b-section-card section-card project-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Briefcase /></el-icon>
                进行中项目
              </span>
              <el-tag size="small" type="primary">{{ activeProjects.length }} 个</el-tag>
            </div>
          </template>

          <div class="project-list">
            <div
              v-for="project in activeProjects"
              :key="project.id"
              class="project-item"
              @click="handleProjectClick(project)"
            >
              <div class="project-header">
                <h4 class="project-name">{{ project.name }}</h4>
                <el-tag :type="getProjectStatusType(project.status)" size="small">
                  {{ project.status }}
                </el-tag>
              </div>
              <div class="project-info">
                <div class="project-meta">
                  <span class="meta-item">
                    <el-icon><Calendar /></el-icon>
                    {{ project.deadline }}
                  </span>
                  <span class="meta-item">
                    <el-icon><User /></el-icon>
                    {{ project.manager }}
                  </span>
                </div>
                <el-progress
                  :percentage="project.progress"
                  :show-text="false"
                  class="project-progress"
                />
              </div>
            </div>
          </div>
        </el-card>

        <!-- 最新动态 -->
        <el-card class="b2b-section-card section-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Bell /></el-icon>
                最新动态
              </span>
            </div>
          </template>

          <div class="activity-list">
            <div v-for="activity in activities" :key="activity.id" class="activity-item">
              <div class="activity-icon" :class="'activity-' + activity.type">
                <el-icon>
                  <component :is="activity.icon" />
                </el-icon>
              </div>
              <div class="activity-content">
                <div class="activity-text">{{ activity.text }}</div>
                <div class="activity-time">{{ activity.time }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  List, Warning, Briefcase, Bell, View, Clock, Calendar, User,
  Document, Plus, TrendCharts, DataAnalysis, Message, Check,
  CircleCheck, CircleClose, Loading
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 统计数据
const stats = ref([
  { key: 'tenders', label: '标讯数量', value: '128', icon: Document, color: '#E6F7FF' },
  { key: 'projects', label: '进行中项目', value: '12', icon: Briefcase, color: '#F6FFED' },
  { key: 'winRate', label: '中标率', value: '68%', icon: TrendCharts, color: '#FFF7E6' },
  { key: 'tasks', label: '待处理任务', value: '23', icon: Check, color: '#FFF1F0' }
])

// 快捷操作处理函数（必须在 quickActions 之前定义）
const handleCreateProject = () => {
  router.push('/project/create')
}

const handleAnalysis = () => {
  router.push('/analytics/dashboard')
}

const handleMessage = () => {
  ElMessage.info('消息中心功能开发中...')
}

// 快捷操作
const quickActions = ref([
  { key: 'create', label: '新建项目', type: 'primary', icon: Plus, handler: handleCreateProject },
  { key: 'analysis', label: '数据分析', type: 'success', icon: DataAnalysis, handler: handleAnalysis },
  { key: 'message', label: '消息中心', type: 'warning', icon: Message, handler: handleMessage }
])

// 我的流程
const myProcesses = ref([
  {
    id: 1,
    title: 'XX市智慧交通项目 - 标书编制',
    category: '标书编制',
    status: 'in-progress',
    description: '技术方案编写中，预计2天内完成',
    progress: 65,
    time: '2025-02-26 14:30'
  },
  {
    id: 2,
    title: 'XX区数字政府项目 - 资质准备',
    category: '资格预审',
    status: 'pending',
    description: '需要补充CMMI 5级认证文件',
    progress: 30,
    time: '2025-02-26 10:15'
  },
  {
    id: 3,
    title: 'XX县智慧社区项目 - 开标前准备',
    category: '开标准备',
    status: 'urgent',
    description: '投标保证金已缴纳，确认密封要求',
    progress: 90,
    time: '2025-02-25 16:45'
  }
])

// 待办事项
const priorityTodos = ref([
  { id: 1, title: 'XX项目技术方案终审', priority: 'high', deadline: '今天 18:00', done: false },
  { id: 2, title: 'XX项目商务报价确认', priority: 'high', deadline: '明天 10:00', done: false },
  { id: 3, title: '资质文件更新（ISO9001）', priority: 'medium', deadline: '2025-02-28', done: false },
  { id: 4, title: '新员工投标系统培训', priority: 'low', deadline: '2025-03-01', done: false }
])

// 进行中项目
const activeProjects = ref([
  {
    id: 'P001',
    name: 'XX市智慧交通管理系统',
    status: '编制中',
    progress: 45,
    deadline: '2025-03-05',
    manager: '张三'
  },
  {
    id: 'P002',
    name: 'XX区数字政府平台',
    status: '评审中',
    progress: 70,
    deadline: '2025-03-10',
    manager: '李四'
  },
  {
    id: 'P003',
    name: 'XX县智慧社区建设',
    status: '即将开标',
    progress: 95,
    deadline: '2025-02-28',
    manager: '王五'
  }
])

// 最新动态
const activities = ref([
  { id: 1, type: 'success', text: 'XX项目技术方案评审通过', time: '10分钟前', icon: CircleCheck },
  { id: 2, type: 'warning', text: 'XX项目需要补充业绩材料', time: '1小时前', icon: Warning },
  { id: 3, type: 'info', text: '新标讯：XX市大数据平台采购', time: '2小时前', icon: Bell },
  { id: 4, type: 'success', text: 'XX县项目成功中标！', time: '昨天', icon: CircleClose }
])

// 获取流程类型
const getProcessType = (status) => {
  const map = {
    'in-progress': 'primary',
    'pending': 'info',
    'urgent': 'danger',
    'completed': 'success'
  }
  return map[status] || 'info'
}

const getProcessStatusType = (status) => {
  const map = {
    'in-progress': '',
    'pending': 'info',
    'urgent': 'danger',
    'completed': 'success'
  }
  return map[status] || ''
}

const getProcessStatusLabel = (status) => {
  const map = {
    'in-progress': '进行中',
    'pending': '待处理',
    'urgent': '紧急',
    'completed': '已完成'
  }
  return map[status] || status
}

// 获取优先级类型
const getPriorityType = (priority) => {
  const map = {
    'high': 'danger',
    'medium': 'warning',
    'low': 'info'
  }
  return map[priority] || ''
}

const getPriorityLabel = (priority) => {
  const map = {
    'high': '高优先级',
    'medium': '中优先级',
    'low': '低优先级'
  }
  return map[priority] || priority
}

// 获取项目状态类型
const getProjectStatusType = (status) => {
  const map = {
    '编制中': 'warning',
    '评审中': 'primary',
    '即将开标': 'danger'
  }
  return map[status] || ''
}

// 处理待办勾选
const handleTodoCheck = (todo) => {
  if (todo.done) {
    ElMessage.success('已完成: ' + todo.title)
  }
}

// 查看待办
const handleViewTodo = (todo) => {
  ElMessage.info('查看详情: ' + todo.title)
}

// 点击项目
const handleProjectClick = (project) => {
  router.push(`/project/${project.id}`)
}
</script>

<script>
export default {
  name: 'DashboardWorkbench'
}
</script>

<style scoped>
.workbench-page {
  padding: var(--space-md);
}

/* 统计卡片行 */
.stats-row {
  margin-bottom: var(--space-md);
}

.b2b-stat-card {
  display: flex;
  align-items: center;
  padding: var(--space-md);
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--gray-100);
  transition: all 0.25s ease;
}

.b2b-stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.b2b-stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  color: var(--brand-primary);
  margin-right: var(--space-md);
}

.b2b-stat-content {
  flex: 1;
}

.b2b-stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.b2b-stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 快捷操作卡片 */
.quick-actions-card {
  margin-bottom: var(--space-md);
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

/* 内容区域 */
.content-row {
  margin-bottom: var(--space-md);
}

/* 卡片通用样式 */
.b2b-section-card {
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--gray-100);
  margin-bottom: var(--space-md);
}

.section-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 流程卡片 */
.process-item-card {
  margin-bottom: var(--space-sm);
}

.process-item-card :deep(.el-card__body) {
  padding: var(--space-md);
}

.process-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-sm);
}

.process-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.process-meta {
  display: flex;
  gap: var(--space-xs);
}

.process-desc {
  margin: var(--space-sm) 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.process-progress {
  margin-top: var(--space-sm);
}

/* 待办列表 */
.todo-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.todo-item {
  display: flex;
  align-items: center;
  padding: var(--space-sm);
  background: var(--gray-50);
  border-radius: var(--radius-sm);
  border-left: 3px solid transparent;
  transition: all 0.2s;
}

.todo-item.priority-high {
  border-left-color: var(--color-danger);
  background: #FFF7F0;
}

.todo-item.priority-medium {
  border-left-color: var(--color-warning);
}

.todo-item.priority-low {
  border-left-color: var(--color-success);
}

.todo-check {
  margin-right: var(--space-sm);
}

.todo-content {
  flex: 1;
  min-width: 0;
}

.todo-title {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.todo-title.done {
  text-decoration: line-through;
  color: var(--text-placeholder);
}

.todo-meta {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 12px;
}

.todo-deadline {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
}

.todo-actions {
  margin-left: var(--space-sm);
}

/* 项目列表 */
.project-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.project-item {
  padding: var(--space-md);
  background: var(--gray-50);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;
}

.project-item:hover {
  background: var(--gray-100);
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-sm);
}

.project-name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.project-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.project-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 动态列表 */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.activity-item {
  display: flex;
  gap: var(--space-sm);
}

.activity-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.activity-success {
  background: #F6FFED;
  color: var(--color-success);
}

.activity-warning {
  background: #FFFBE6;
  color: var(--color-warning);
}

.activity-info {
  background: #E6F7FF;
  color: var(--brand-primary);
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-text {
  font-size: 13px;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.activity-time {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 响应式 */
@media (max-width: 768px) {
  .workbench-page {
    padding: var(--space-sm);
  }

  .stats-row {
    margin-bottom: var(--space-sm);
  }

  .quick-actions {
    flex-direction: column;
  }

  .quick-actions .el-button {
    width: 100%;
  }
}
</style>
