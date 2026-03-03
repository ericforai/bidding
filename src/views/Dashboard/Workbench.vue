<template>
  <div class="workbench">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-content">
        <div class="banner-greeting">
          <h1 class="banner-title">欢迎回来，{{ userStore.currentUser?.name || '用户' }}</h1>
          <p class="banner-subtitle">今天是 {{ currentDate }}，您有 {{ pendingCount }} 项待处理任务</p>
        </div>
        <div class="banner-actions">
          <el-button type="primary" :icon="Plus" @click="handleCreateProject">
            新建项目
          </el-button>
          <el-button :icon="DataAnalysis" @click="handleAnalysis">
            数据分析
          </el-button>
        </div>
      </div>
      <div class="banner-decoration">
        <div class="decoration-circle circle-1"></div>
        <div class="decoration-circle circle-2"></div>
        <div class="decoration-circle circle-3"></div>
      </div>
    </div>

    <!-- 统计指标卡片 -->
    <div class="metrics-grid">
      <div
        v-for="metric in metrics"
        :key="metric.key"
        class="metric-card"
        :class="'metric-' + metric.variant"
        @click="handleMetricClick(metric)"
      >
        <div class="metric-header">
          <span class="metric-label">{{ metric.label }}</span>
          <div class="metric-icon" :style="{ background: metric.iconBg }">
            <el-icon :size="20">
              <component :is="metric.icon" />
            </el-icon>
          </div>
        </div>
        <div class="metric-value">{{ metric.value }}</div>
        <div class="metric-footer">
          <span class="metric-change" :class="metric.changeClass">
            {{ metric.change }}
          </span>
          <span class="metric-compare">较上月</span>
        </div>
      </div>
    </div>

    <!-- 主内容网格 -->
    <div class="content-grid">
      <!-- 左侧主栏 -->
      <div class="main-column">
        <!-- 进行中项目 -->
        <div class="section-card projects-card">
          <div class="section-header">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                <path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/>
              </svg>
              进行中项目
            </h3>
            <el-link type="primary" :underline="false" @click="router.push('/project')">
              查看全部
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-link>
          </div>
          <div class="projects-list">
            <div
              v-for="project in activeProjects"
              :key="project.id"
              class="project-card"
              @click="handleProjectClick(project)"
            >
              <div class="project-progress-ring">
                <svg viewBox="0 0 36 36" class="progress-ring">
                  <path
                    class="progress-ring-bg"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    fill="none"
                    stroke="#E5E7EB"
                    stroke-width="3"
                  />
                  <path
                    class="progress-ring-fill"
                    :stroke-dasharray="project.progress + ', 100'"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    fill="none"
                    :stroke="getProgressColor(project.progress)"
                    stroke-width="3"
                    stroke-linecap="round"
                  />
                </svg>
                <span class="progress-text">{{ project.progress }}%</span>
              </div>
              <div class="project-info">
                <h4 class="project-name">{{ project.name }}</h4>
                <div class="project-meta">
                  <span class="meta-tag">
                    <el-icon><Calendar /></el-icon>
                    {{ project.deadline }}
                  </span>
                  <span class="meta-tag">
                    <el-icon><User /></el-icon>
                    {{ project.manager }}
                  </span>
                </div>
              </div>
              <el-tag :type="getProjectStatusType(project.status)" size="small">
                {{ project.status }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 待办事项 -->
        <div class="section-card todos-card">
          <div class="section-header">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                <path d="M9 11l3 3L22 4"/>
                <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
              </svg>
              待办事项
            </h3>
            <el-tag size="small" type="danger">{{ priorityTodos.length }}</el-tag>
          </div>
          <div class="todos-list">
            <div
              v-for="todo in priorityTodos"
              :key="todo.id"
              class="todo-item"
              :class="'priority-' + todo.priority"
            >
              <div class="todo-checkbox" @click.stop="todo.done = !todo.done">
                <div class="checkbox-custom" :class="{ checked: todo.done }">
                  <el-icon v-if="todo.done"><Check /></el-icon>
                </div>
              </div>
              <div class="todo-content">
                <span class="todo-title" :class="{ done: todo.done }">{{ todo.title }}</span>
                <div class="todo-meta">
                  <span class="todo-deadline">
                    <el-icon><Clock /></el-icon>
                    {{ todo.deadline }}
                  </span>
                </div>
              </div>
              <el-tag :type="getPriorityType(todo.priority)" size="small">
                {{ getPriorityLabel(todo.priority) }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧边栏 -->
      <div class="side-column">
        <!-- 投标日历 -->
        <div class="section-card calendar-card">
          <div class="section-header">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              投标日历
            </h3>
            <el-tag size="small" type="primary">{{ calendarEvents.length }} 个日程</el-tag>
          </div>
          <div class="calendar-wrapper">
            <el-calendar v-model="calendarDate">
              <template #date-cell="{ data }">
                <div
                  class="calendar-day-cell"
                  :class="calendarCellClass(data)"
                  @click="handleDateClick(data.date)"
                >
                  <span class="calendar-day-number">{{ data.day.split('-')[2] }}</span>
                  <div class="calendar-day-dots" v-if="getEventsForDate(data.date).length > 0">
                    <span
                      v-for="event in getEventsForDate(data.date).slice(0, 3)"
                      :key="event.id"
                                              class="calendar-dot"
                                              :class="'type-' + event.type"
                                            ></span>
                  </div>
                </div>
              </template>
            </el-calendar>
          </div>
        </div>

        <!-- 今日日程 -->
        <div class="section-card today-events-card" v-if="todayCalendarEvents.length > 0">
          <div class="section-header">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                <path d="M13.73 21a2 2 0 01-3.46 0"/>
              </svg>
              今日日程
            </h3>
          </div>
          <div class="today-events-list">
            <div
              v-for="event in todayCalendarEvents"
              :key="event.id"
              class="today-event-item"
              :class="'event-' + event.type"
            >
              <div class="event-dot"></div>
              <div class="event-content">
                <span class="event-title">{{ event.title }}</span>
                <span class="event-project">{{ event.project }}</span>
              </div>
              <el-tag :type="getEventTypeTag(event.type).type" size="small">
                {{ getEventTypeTag(event.type).label }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 流程跟踪 -->
        <div class="section-card process-card">
          <div class="section-header">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
              我的流程
            </h3>
          </div>
          <div class="process-timeline">
            <div
              v-for="(process, index) in myProcesses"
              :key="process.id"
              class="process-item"
            >
              <div class="process-dot" :class="'status-' + process.status"></div>
              <div class="process-content">
                <div class="process-header">
                  <span class="process-title">{{ process.title }}</span>
                  <span class="process-time">{{ formatTime(process.time) }}</span>
                </div>
                <p class="process-desc">{{ process.description }}</p>
                <div v-if="process.progress" class="process-progress">
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: process.progress + '%' }"></div>
                  </div>
                  <span class="progress-label">{{ process.progress }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 最新动态 -->
        <div class="section-card activity-card">
          <div class="section-header">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                <path d="M13.73 21a2 2 0 01-3.46 0"/>
              </svg>
              最新动态
            </h3>
          </div>
          <div class="activity-list">
            <div
              v-for="activity in activities"
              :key="activity.id"
              class="activity-item"
            >
              <div class="activity-dot" :class="'type-' + activity.type"></div>
              <div class="activity-content">
                <p class="activity-text">{{ activity.text }}</p>
                <span class="activity-time">{{ activity.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 日历事件详情弹窗 -->
    <el-dialog
      v-model="calendarEventDialogVisible"
      :title="`${selectedDateEvents[0]?.date || ''} 的日程`"
      width="500px"
      class="calendar-event-dialog"
    >
      <div class="event-dialog-list">
        <div
          v-for="event in selectedDateEvents"
          :key="event.id"
          class="event-dialog-item"
          :class="'event-' + event.type"
        >
          <div class="event-dialog-header">
            <div class="event-dialog-icon" :class="'icon-' + event.type">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="event-dialog-info">
              <h4 class="event-dialog-title">{{ event.title }}</h4>
              <p class="event-dialog-project">{{ event.project }}</p>
            </div>
          </div>
          <el-tag :type="getEventTypeTag(event.type).type" size="small">
            {{ getEventTypeTag(event.type).label }}
          </el-tag>
        </div>
      </div>
      <template #footer>
        <el-button @click="calendarEventDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useBiddingStore } from '@/stores/bidding'
import {
  Plus, DataAnalysis, ArrowRight, Calendar, User, Clock, Check,
  Document, Briefcase, TrendCharts, Flag
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const biddingStore = useBiddingStore()

// 当前日期
const currentDate = computed(() => {
  const now = new Date()
  const options = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }
  return now.toLocaleDateString('zh-CN', options)
})

// 待处理数量
const pendingCount = computed(() => {
  return priorityTodos.value.filter(t => !t.done).length
})

// 统计指标
const metrics = ref([
  {
    key: 'tenders',
    label: '标讯数量',
    value: '128',
    icon: Document,
    iconBg: 'linear-gradient(135deg, #DBEAFE 0%, #BFDBFE 100%)',
    iconColor: '#1E40AF',
    change: '+12.5%',
    changeClass: 'up',
    variant: 'blue'
  },
  {
    key: 'projects',
    label: '进行中项目',
    value: '12',
    icon: Briefcase,
    iconBg: 'linear-gradient(135deg, #D1FAE5 0%, #A7F3D0 100%)',
    iconColor: '#059669',
    change: '+2',
    changeClass: 'up',
    variant: 'green'
  },
  {
    key: 'winRate',
    label: '中标率',
    value: '68%',
    icon: TrendCharts,
    iconBg: 'linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%)',
    iconColor: '#D97706',
    change: '-3.2%',
    changeClass: 'down',
    variant: 'amber'
  },
  {
    key: 'tasks',
    label: '待处理任务',
    value: '23',
    icon: Flag,
    iconBg: 'linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%)',
    iconColor: '#DC2626',
    change: '0',
    changeClass: 'neutral',
    variant: 'red'
  }
])

// 进行中项目
const activeProjects = ref([
  {
    id: 'P001',
    name: 'XX市智慧交通管理系统',
    status: '编制中',
    progress: 45,
    deadline: '03-05',
    manager: '张三'
  },
  {
    id: 'P002',
    name: 'XX区数字政府平台',
    status: '评审中',
    progress: 70,
    deadline: '03-10',
    manager: '李四'
  },
  {
    id: 'P003',
    name: 'XX县智慧社区建设',
    status: '即将开标',
    progress: 95,
    deadline: '02-28',
    manager: '王五'
  }
])

// 待办事项
const priorityTodos = ref([
  { id: 1, title: 'XX项目技术方案终审', priority: 'high', deadline: '今天 18:00', done: false },
  { id: 2, title: 'XX项目商务报价确认', priority: 'high', deadline: '明天 10:00', done: false },
  { id: 3, title: '资质文件更新（ISO9001）', priority: 'medium', deadline: '02-28', done: false },
  { id: 4, title: '新员工投标系统培训', priority: 'low', deadline: '03-01', done: false }
])

// 我的流程
const myProcesses = ref([
  {
    id: 1,
    title: 'XX市智慧交通项目 - 标书编制',
    status: 'in-progress',
    description: '技术方案编写中，预计2天内完成',
    progress: 65,
    time: '2025-02-26 14:30'
  },
  {
    id: 2,
    title: 'XX区数字政府项目 - 资质准备',
    status: 'pending',
    description: '需要补充CMMI 5级认证文件',
    progress: 30,
    time: '2025-02-26 10:15'
  },
  {
    id: 3,
    title: 'XX县智慧社区项目 - 开标前准备',
    status: 'urgent',
    description: '投标保证金已缴纳，确认密封要求',
    progress: 90,
    time: '2025-02-25 16:45'
  }
])

// 最新动态
const activities = ref([
  { id: 1, type: 'success', text: 'XX项目技术方案评审通过', time: '10分钟前' },
  { id: 2, type: 'warning', text: 'XX项目需要补充业绩材料', time: '1小时前' },
  { id: 3, type: 'info', text: '新标讯：XX市大数据平台采购', time: '2小时前' },
  { id: 4, type: 'success', text: 'XX县项目成功中标！', time: '昨天' }
])

// ========== 投标日历相关 ==========
const calendarDate = ref(new Date())
const selectedDateEvents = ref([])
const calendarEventDialogVisible = ref(false)

// 从 store 获取日历数据
const calendarEvents = computed(() => biddingStore.calendar || [])

// 获取指定日期的事件
const getEventsForDate = (date) => {
  const dateStr = formatDateKey(date)
  return calendarEvents.value.filter(event => event.date === dateStr)
}

// 格式化日期为 YYYY-MM-DD
const formatDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 日历单元格自定义渲染
const calendarCellClass = ({ date, viewType }) => {
  if (viewType !== 'month') return ''
  const dateStr = formatDateKey(date)
  const events = getEventsForDate(date)
  if (events.length === 0) return ''

  const hasUrgent = events.some(e => e.urgent)
  return hasUrgent ? 'calendar-day-urgent' : 'calendar-day-has-event'
}

// 日历日期单元格内容
const calendarDayContent = ({ date, viewType }) => {
  if (viewType !== 'month') return null

  const events = getEventsForDate(date)
  if (events.length === 0) {
    return date.getDate()
  }

  return {
    children: [
      h('div', { class: 'calendar-day-number' }, date.getDate()),
      h('div', { class: 'calendar-day-dots' },
        events.slice(0, 3).map(event =>
          h('span', {
            class: `calendar-dot type-${event.type}`,
            key: event.id
          })
        )
      )
    ]
  }
}

// 点击日期
const handleDateClick = (date) => {
  const events = getEventsForDate(date)
  if (events.length > 0) {
    selectedDateEvents.value = events
    calendarEventDialogVisible.value = true
  }
}

// 获取事件类型标签
const getEventTypeTag = (type) => {
  const map = {
    'deadline': { type: 'danger', label: '截止' },
    'bid': { type: 'primary', label: '投标' },
    'opening': { type: 'success', label: '开标' },
    'review': { type: 'warning', label: '评审' }
  }
  return map[type] || { type: 'info', label: '其他' }
}

// 获取事件图标
const getEventIcon = (type) => {
  const map = {
    'deadline': 'Clock',
    'bid': 'Document',
    'opening': 'Check',
    'review': 'View'
  }
  return map[type] || 'Calendar'
}

// 日历加载
onMounted(async () => {
  await biddingStore.getCalendar()
})

// 今日日程事件
const todayCalendarEvents = computed(() => {
  const today = formatDateKey(new Date())
  return calendarEvents.value.filter(e => e.date === today)
})

// 获取进度条颜色
const getProgressColor = (progress) => {
  if (progress >= 80) return '#059669'
  if (progress >= 50) return '#3B82F6'
  if (progress >= 20) return '#F59E0B'
  return '#EF4444'
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
    'high': '高',
    'medium': '中',
    'low': '低'
  }
  return map[priority] || priority
}

// 格式化时间
const formatTime = (time) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return Math.floor(diff / 86400000) + '天前'
}

// 事件处理
const handleCreateProject = () => {
  router.push('/project/create')
}

const handleAnalysis = () => {
  router.push('/analytics/dashboard')
}

const handleMetricClick = (metric) => {
  ElMessage.info(`查看 ${metric.label} 详情`)
}

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
.workbench {
  padding: 24px;
  background: #F8FAFC;
  min-height: 100%;
}

/* ==================== 欢迎横幅 ==================== */
.welcome-banner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32px 40px;
  background: linear-gradient(135deg, #1E40AF 0%, #3B82F6 100%);
  border-radius: 16px;
  margin-bottom: 24px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(30, 64, 175, 0.2);
}

.banner-content {
  position: relative;
  z-index: 2;
}

.banner-greeting {
  margin-bottom: 20px;
}

.banner-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.banner-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.banner-actions {
  display: flex;
  gap: 12px;
}

.banner-actions .el-button {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.banner-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.25);
}

.banner-actions .el-button--primary {
  background: #fff;
  color: #1E40AF;
  border-color: #fff;
}

.banner-actions .el-button--primary:hover {
  background: #F8FAFC;
}

.banner-decoration {
  position: absolute;
  right: 40px;
  top: 50%;
  transform: translateY(-50%);
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 120px;
  height: 120px;
  right: 0;
  top: -60px;
}

.circle-2 {
  width: 80px;
  height: 80px;
  right: 100px;
  top: 20px;
}

.circle-3 {
  width: 60px;
  height: 60px;
  right: 40px;
  top: -40px;
}

/* ==================== 统计指标网格 ==================== */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.metric-card {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}

.metric-card.metric-blue::before { background: linear-gradient(180deg, #1E40AF 0%, #3B82F6 100%); }
.metric-card.metric-green::before { background: linear-gradient(180deg, #059669 0%, #10B981 100%); }
.metric-card.metric-amber::before { background: linear-gradient(180deg, #D97706 0%, #F59E0B 100%); }
.metric-card.metric-red::before { background: linear-gradient(180deg, #DC2626 0%, #EF4444 100%); }

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.metric-label {
  font-size: 13px;
  font-weight: 500;
  color: #6B7280;
}

.metric-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1E40AF;
}

.metric-value {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  line-height: 1;
  margin-bottom: 12px;
}

.metric-footer {
  display: flex;
  align-items: center;
  gap: 6px;
}

.metric-change {
  font-size: 13px;
  font-weight: 600;
}

.metric-change.up { color: #059669; }
.metric-change.down { color: #DC2626; }
.metric-change.neutral { color: #6B7280; }

.metric-compare {
  font-size: 12px;
  color: #9CA3AF;
}

/* ==================== 内容网格 ==================== */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ==================== 卡片通用样式 ==================== */
.section-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #F3F4F6;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.section-icon {
  width: 20px;
  height: 20px;
  color: #1E40AF;
}

/* ==================== 项目列表 ==================== */
.projects-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.project-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #F9FAFB;
  border-radius: 10px;
  border: 1px solid #E5E7EB;
  cursor: pointer;
  transition: all 0.2s ease;
}

.project-card:hover {
  background: #F3F4F6;
  border-color: #D1D5DB;
}

.project-progress-ring {
  position: relative;
  width: 52px;
  height: 52px;
  flex-shrink: 0;
}

.progress-ring {
  transform: rotate(-90deg);
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  font-weight: 600;
  color: #374151;
}

.project-info {
  flex: 1;
  min-width: 0;
}

.project-name {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-meta {
  display: flex;
  gap: 16px;
}

.meta-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #6B7280;
}

.meta-tag .el-icon {
  font-size: 14px;
}

/* ==================== 待办列表 ==================== */
.todos-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #F9FAFB;
  border-radius: 8px;
  border-left: 3px solid transparent;
  transition: all 0.2s ease;
}

.todo-item.priority-high {
  border-left-color: #EF4444;
  background: #FEF2F2;
}

.todo-item.priority-medium {
  border-left-color: #F59E0B;
}

.todo-item.priority-low {
  border-left-color: #10B981;
}

.todo-checkbox {
  cursor: pointer;
}

.checkbox-custom {
  width: 20px;
  height: 20px;
  border: 2px solid #D1D5DB;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #fff;
}

.checkbox-custom.checked {
  background: #10B981;
  border-color: #10B981;
}

.checkbox-custom .el-icon {
  font-size: 14px;
}

.todo-content {
  flex: 1;
  min-width: 0;
}

.todo-title {
  display: block;
  font-size: 14px;
  color: #111827;
  margin-bottom: 4px;
}

.todo-title.done {
  text-decoration: line-through;
  color: #9CA3AF;
}

.todo-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-deadline {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #6B7280;
}

/* ==================== 流程时间线 ==================== */
.process-timeline {
  padding: 16px;
}

.process-item {
  display: flex;
  gap: 12px;
  padding-bottom: 20px;
  position: relative;
}

.process-item:last-child {
  padding-bottom: 0;
}

.process-item::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 24px;
  bottom: 0;
  width: 2px;
  background: #E5E7EB;
}

.process-item:last-child::before {
  display: none;
}

.process-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 0 0 1px #E5E7EB;
  flex-shrink: 0;
  margin-top: 2px;
}

.process-dot.status-in-progress {
  background: #3B82F6;
  box-shadow: 0 0 0 1px #3B82F6;
}

.process-dot.status-pending {
  background: #9CA3AF;
}

.process-dot.status-urgent {
  background: #EF4444;
  box-shadow: 0 0 0 1px #EF4444;
}

.process-content {
  flex: 1;
}

.process-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.process-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.process-time {
  font-size: 12px;
  color: #9CA3AF;
}

.process-desc {
  font-size: 13px;
  color: #6B7280;
  margin-bottom: 8px;
  line-height: 1.5;
}

.process-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: #E5E7EB;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3B82F6 0%, #1E40AF 100%);
  transition: width 0.3s ease;
}

.progress-label {
  font-size: 12px;
  color: #6B7280;
  min-width: 36px;
}

/* ==================== 动态列表 ==================== */
.activity-list {
  padding: 16px;
}

.activity-item {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
}

.activity-item:last-child {
  padding-bottom: 0;
}

.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.activity-dot.type-success {
  background: #10B981;
}

.activity-dot.type-warning {
  background: #F59E0B;
}

.activity-dot.type-info {
  background: #3B82F6;
}

.activity-content {
  flex: 1;
}

.activity-text {
  font-size: 13px;
  color: #374151;
  margin-bottom: 2px;
  line-height: 1.5;
}

.activity-time {
  font-size: 12px;
  color: #9CA3AF;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .workbench {
    padding: 16px;
  }

  .welcome-banner {
    flex-direction: column;
    padding: 24px;
    text-align: center;
  }

  .banner-actions {
    justify-content: center;
  }

  .banner-decoration {
    display: none;
  }

  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .metric-card {
    padding: 16px;
  }

  .metric-value {
    font-size: 24px;
  }

  .side-column {
    grid-template-columns: 1fr;
  }
}
</style>
