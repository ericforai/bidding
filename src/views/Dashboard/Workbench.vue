<template>
  <div class="workbench">
    <div class="page-identity">
      <span class="page-kicker">工作台</span>
    </div>
    <!-- 欢迎横幅 - 角色化 -->
    <div class="welcome-banner" :class="'banner-' + currentUserRole">
      <div class="banner-content">
        <div class="banner-greeting">
          <h1 class="banner-title">{{ bannerTitle }}</h1>
          <p class="banner-subtitle">{{ bannerSubtitle }}</p>
        </div>
        <div class="banner-actions">
          <el-button v-for="action in bannerActions" :key="action.key" :type="action.type || 'default'" :icon="action.icon" @click="action.handler">
            {{ action.label }}
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
    <div class="metrics-grid" v-loading="metricsLoading">
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
        <div class="section-card calendar-card calendar-card--hero">
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
            <el-tag size="small" type="primary">{{ visibleCalendarEvents.length }} 个节点</el-tag>
          </div>
          <div class="calendar-topbar">
            <div class="calendar-summary-strip">
              <div class="summary-pill">
                <span class="summary-label">本月节点</span>
                <strong>{{ monthCalendarSummary.total }}</strong>
              </div>
              <div class="summary-pill risk">
                <span class="summary-label">高风险</span>
                <strong>{{ monthCalendarSummary.urgent }}</strong>
              </div>
              <div class="summary-pill accent">
                <span class="summary-label">最近截止</span>
                <strong>{{ monthCalendarSummary.nextDeadlineLabel }}</strong>
              </div>
            </div>
            <div class="calendar-filter-bar">
              <div class="calendar-filter-copy">
                <span class="filter-eyebrow">节点筛选</span>
                <span class="filter-hint">按类型或风险切换主舞台视图</span>
              </div>
              <div class="calendar-filter-row">
                <button
                  v-for="filter in calendarFilters"
                  :key="filter.value"
                  type="button"
                  class="calendar-filter-chip"
                  :class="{ active: activeCalendarFilter === filter.value }"
                  @click="activeCalendarFilter = filter.value"
                >
                  <span class="chip-dot" :class="'dot-' + filter.value"></span>
                  {{ filter.label }}
                </button>
              </div>
            </div>
          </div>
          <div class="calendar-hero-grid">
            <div class="calendar-hero-main">
              <div class="calendar-wrapper">
                <el-calendar v-model="calendarDate">
                  <template #date-cell="{ data }">
                    <div
                      class="calendar-day-cell"
                      :class="calendarCellClass(data)"
                      @click="handleDateClick(data.date)"
                    >
                      <span class="calendar-day-number">{{ data.day.split('-')[2] }}</span>
                      <div class="calendar-day-marker" v-if="getEventsForDate(data.date).length > 0">
                        <div class="calendar-day-dots">
                          <span
                            v-for="event in getEventsForDate(data.date).slice(0, 3)"
                            :key="event.id"
                            class="calendar-event-dot"
                            :class="'event-' + event.type"
                          ></span>
                        </div>
                        <span class="calendar-day-count">{{ getEventsForDate(data.date).length }}</span>
                        <span v-if="getEventsForDate(data.date).some((event) => event.urgent)" class="calendar-day-alert">!</span>
                      </div>
                    </div>
                  </template>
                </el-calendar>
              </div>
              <div class="calendar-legend">
                <span class="legend-item"><span class="legend-dot event-deadline"></span>截止</span>
                <span class="legend-item"><span class="legend-dot event-bid"></span>投标</span>
                <span class="legend-item"><span class="legend-dot event-opening"></span>开标</span>
                <span class="legend-item"><span class="legend-dot event-review"></span>评审</span>
              </div>
            </div>
            <div class="calendar-hero-side">
              <div class="calendar-panel">
                <div class="calendar-panel-header">
                  <div>
                    <div class="calendar-panel-eyebrow">选中日期</div>
                    <h4 class="calendar-panel-title">{{ selectedDateLabel }}</h4>
                  </div>
                  <el-tag size="small" :type="selectedDateEvents.length > 0 ? 'danger' : 'info'">
                    {{ selectedDateEvents.length > 0 ? `${selectedDateEvents.length} 个事项` : '无事项' }}
                  </el-tag>
                </div>
                <div v-if="selectedDateEvents.length > 0" class="selected-events-list">
                  <div
                    v-for="event in selectedDateEvents"
                    :key="event.id"
                    class="selected-event-card"
                    :class="['event-' + event.type, event.priorityLevel]"
                  >
                    <div class="selected-event-main">
                      <div class="selected-event-topline">
                        <span class="selected-event-type">{{ getEventTypeTag(event.type).label }}</span>
                        <span class="selected-event-countdown">{{ event.countdownLabel }}</span>
                      </div>
                      <h5 class="selected-event-title">{{ event.shortTitle || event.title }}</h5>
                      <p class="selected-event-project">{{ event.project }}</p>
                      <div class="selected-event-meta">
                        <span>{{ event.fieldSummary.owner }}</span>
                        <span>{{ event.fieldSummary.stage }}</span>
                        <span>{{ event.fieldSummary.blocker }}</span>
                      </div>
                    </div>
                    <div class="selected-event-actions">
                      <el-tag size="small" :type="event.riskTagType" :class="['risk-tag', event.priorityLevel]">{{ event.riskLabel }}</el-tag>
                      <el-button size="small" text type="primary" @click="handleCalendarAction(event)">
                        {{ event.actionLabel }}
                      </el-button>
                    </div>
                  </div>
                </div>
                <div v-else class="calendar-empty-state">
                  当前筛选条件下，这一天没有投标节点。
                </div>
              </div>
              <div class="upcoming-panel">
                <div class="calendar-panel-header">
                  <div>
                    <div class="calendar-panel-eyebrow">未来 7 天</div>
                    <h4 class="calendar-panel-title">关键执行清单</h4>
                  </div>
                  <el-link type="primary" underline="hover" @click="activeCalendarFilter = 'all'">清除筛选</el-link>
                </div>
                <div class="upcoming-events-list">
                  <div
                    v-for="event in upcomingCalendarEvents"
                    :key="event.id"
                    class="upcoming-event-item"
                    :class="['event-' + event.type, event.priorityLevel]"
                    @click="selectCalendarEventDate(event)"
                  >
                    <div class="upcoming-event-rail">
                      <span class="upcoming-rail-countdown">{{ event.countdownLabel }}</span>
                      <span class="upcoming-rail-type">{{ getEventTypeTag(event.type).label }}</span>
                    </div>
                    <div class="upcoming-event-body">
                      <div class="upcoming-event-title-row">
                        <span class="upcoming-event-title">{{ event.project }}</span>
                        <span class="upcoming-event-date-label">{{ event.dayLabel }} {{ event.weekdayLabel }}</span>
                      </div>
                      <div class="upcoming-event-subline execution-meta">
                        <span class="execution-chip">{{ event.shortTitle || event.title }}</span>
                        <span class="execution-chip muted">{{ event.fieldSummary.stage }}</span>
                        <span class="execution-chip blocker">{{ event.fieldSummary.blocker }}</span>
                      </div>
                    </div>
                    <div class="upcoming-event-side">
                      <span class="upcoming-event-owner">{{ event.fieldSummary.owner }}</span>
                      <el-button size="small" text type="primary" @click.stop="handleCalendarAction(event)">
                        {{ event.actionLabel }}
                      </el-button>
                    </div>
                  </div>
                </div>
                <div v-if="upcomingCalendarEvents.length === 0" class="calendar-empty-state compact">
                  当前筛选下未来 7 天没有待执行节点。
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ========== 销售经理专属内容 (小王) ========== -->
        <template v-if="currentUserRole === 'staff'">
          <!-- 一站式流程发起 -->
          <div class="section-card quick-actions-card">
            <div class="section-header">
              <h3 class="section-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="8" x2="12" y2="16"/>
                  <line x1="8" y1="12" x2="16" y2="12"/>
                </svg>
                快速发起
              </h3>
            </div>
            <div class="quick-actions-grid">
              <div class="quick-action-item" @click="handleQuickAction('support')">
                <div class="action-icon" style="background: linear-gradient(135deg, #DBEAFE 0%, #BFDBFE 100%); color: #1E40AF;">
                  <el-icon :size="24"><Document /></el-icon>
                </div>
                <span class="action-title">标书支持申请</span>
                <span class="action-desc">申请技术/商务支持</span>
              </div>
              <div class="quick-action-item" @click="handleQuickAction('borrow')">
                <div class="action-icon" style="background: linear-gradient(135deg, #D1FAE5 0%, #A7F3D0 100%); color: #059669;">
                  <el-icon :size="24"><FolderOpened /></el-icon>
                </div>
                <span class="action-title">资质/合同借阅</span>
                <span class="action-desc">申请借阅相关文件</span>
              </div>
              <div class="quick-action-item" @click="handleQuickAction('expense')">
                <div class="action-icon" style="background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%); color: #D97706;">
                  <el-icon :size="24"><Wallet /></el-icon>
                </div>
                <span class="action-title">投标费用申请</span>
                <span class="action-desc">保证金/标书费</span>
              </div>
            </div>
          </div>

          <!-- 重点标讯 -->
          <div class="section-card tenders-card">
            <div class="section-header">
              <h3 class="section-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                  <path d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z"/>
                </svg>
                重点标讯
              </h3>
              <el-link type="primary" underline="hover" @click="router.push('/bidding')">
                查看全部
                <el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </el-link>
            </div>
            <div class="tenders-list">
              <div
                v-for="tender in hotTenders"
                :key="tender.id"
                class="tender-card"
                @click="handleTenderClick(tender)"
              >
                <div class="tender-score" :class="'score-' + tender.scoreLevel">
                  {{ tender.aiScore }}
                </div>
                <div class="tender-info">
                  <h4 class="tender-title">{{ tender.title }}</h4>
                  <div class="tender-meta">
                    <span class="tender-budget">{{ tender.budget }}万</span>
                    <span class="tender-region">{{ tender.region }}</span>
                    <el-tag :type="tender.probability === 'high' ? 'success' : 'warning'" size="small">
                      {{ tender.probibilityText }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 客户跟进 -->
          <div class="section-card customers-card">
            <div class="section-header">
              <h3 class="section-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                </svg>
                客户跟进
              </h3>
            </div>
            <div class="customers-list">
              <div
                v-for="customer in followUpCustomers"
                :key="customer.id"
                class="customer-item"
              >
                <div class="customer-avatar">{{ customer.name.charAt(0) }}</div>
                <div class="customer-info">
                  <span class="customer-name">{{ customer.name }}</span>
                  <span class="customer-company">{{ customer.company }}</span>
                </div>
                <div class="customer-status">
                  <el-tag :type="customer.statusType" size="small">{{ customer.status }}</el-tag>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ========== 投标经理专属内容 (张经理) ========== -->
        <template v-if="currentUserRole === 'manager'">
          <!-- 我的项目 -->
          <div class="section-card projects-card">
            <div class="section-header">
              <h3 class="section-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                  <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                  <path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/>
                </svg>
                我的项目
              </h3>
              <el-link type="primary" underline="hover" @click="router.push('/project')">
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

          <!-- 团队任务 -->
          <div class="section-card team-tasks-card">
            <div class="section-header">
              <h3 class="section-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                  <path d="M16 3.13a4 4 0 010 7.75"/>
                </svg>
                团队任务分配
              </h3>
            </div>
            <div class="team-tasks-list">
              <div
                v-for="member in teamMembers"
                :key="member.id"
                class="member-task-item"
              >
                <div class="member-avatar">{{ member.name.charAt(0) }}</div>
                <div class="member-info">
                  <span class="member-name">{{ member.name }}</span>
                  <div class="member-tasks">
                    <el-tag
                      v-for="task in member.tasks.slice(0, 2)"
                      :key="task.id"
                      size="small"
                      :type="task.priority === 'high' ? 'danger' : 'info'"
                    >
                      {{ task.title }}
                    </el-tag>
                    <span v-if="member.tasks.length > 2" class="more-tasks">+{{ member.tasks.length - 2 }}</span>
                  </div>
                </div>
                <div class="member-workload">
                  <span class="workload-label">工作量</span>
                  <span class="workload-value" :class="'workload-' + member.workloadLevel">{{ member.workload }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ========== 技术员工专属内容 (李工) ========== -->

        <!-- 进行中项目（管理层显示） -->
        <div class="section-card projects-card" v-if="currentUserRole === 'admin'">
          <div class="section-header">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                <path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/>
              </svg>
              重点项目
            </h3>
            <el-link type="primary" underline="hover" @click="router.push('/project')">
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
                    <el-icon><User /></el-icon>
                    {{ project.manager }}
                  </span>
                  <span class="meta-tag">
                    <el-icon><Calendar /></el-icon>
                    {{ project.deadline }}
                  </span>
                </div>
              </div>
              <el-tag :type="getProjectStatusType(project.status)" size="small">
                {{ project.status }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="section-card projects-card">
          <div class="section-header">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                <path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/>
              </svg>
              进行中项目
            </h3>
            <el-link type="primary" underline="hover" @click="router.push('/project')">
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
      </div>

      <!-- 右侧边栏 -->
      <div class="side-column">
        <template v-if="currentUserRole === 'admin'">
          <div class="side-summary-grid">
            <div class="section-card team-performance-card side-balance-card">
              <div class="section-header">
                <h3 class="section-title">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                    <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                    <path d="M16 3.13a4 4 0 010 7.75"/>
                  </svg>
                  团队绩效
                </h3>
              </div>
              <div class="team-performance-grid compact">
                <div v-for="team in teamPerformance" :key="team.dept" class="team-performance-item">
                  <div class="team-info">
                    <span class="team-name">{{ team.dept }}</span>
                    <span class="team-size">{{ team.size }}人</span>
                  </div>
                  <div class="team-progress">
                    <div class="progress-bar">
                      <div class="progress-fill" :style="{ width: team.progress + '%', background: team.color }"></div>
                    </div>
                    <span class="progress-label">{{ team.progress }}%</span>
                  </div>
                  <div class="team-metrics">
                    <span class="team-metric">中标: {{ team.wins }}</span>
                    <span class="team-metric">进行: {{ team.active }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="section-card approvals-card side-balance-card">
              <div class="section-header">
                <h3 class="section-title">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="section-icon">
                    <path d="M9 11l3 3L22 4"/>
                    <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
                  </svg>
                  待审批事项
                </h3>
                <el-tag size="small" type="danger">{{ pendingApprovals.length }}</el-tag>
              </div>
              <div class="approvals-list compact">
                <div
                  v-for="item in pendingApprovals"
                  :key="item.id"
                  class="approval-item"
                >
                  <div class="approval-icon" :class="'type-' + item.type">
                    <el-icon><Document /></el-icon>
                  </div>
                  <div class="approval-content">
                    <span class="approval-title">{{ item.title }}</span>
                    <span class="approval-meta">{{ item.department }} · {{ item.time }}</span>
                  </div>
                  <div class="approval-actions">
                    <el-button size="small" type="success" @click="handleApprove(item)">通过</el-button>
                    <el-button size="small" @click="handleReject(item)">驳回</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

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
              :class="['priority-' + todo.priority, todo.type === 'warning' ? 'todo-warning' : '']"
            >
              <div class="todo-checkbox" @click.stop="handleTaskComplete(todo)">
                <div class="checkbox-custom" :class="{ checked: todo.done }">
                  <el-icon v-if="todo.done"><Check /></el-icon>
                </div>
              </div>
              <div class="todo-content">
                <div class="todo-title-row">
                  <span class="todo-title" :class="{ done: todo.done }">{{ todo.title }}</span>
                  <el-tag v-if="todo.type === 'warning'" type="warning" size="small" effect="dark">系统预警</el-tag>
                </div>
                <div class="todo-meta">
                  <span class="todo-deadline">
                    <el-icon><Clock /></el-icon>
                    {{ todo.deadline }}
                  </span>
                </div>
              </div>
              <el-tag v-if="todo.type !== 'warning'" :type="getPriorityType(todo.priority)" size="small">
                {{ getPriorityLabel(todo.priority) }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ApprovalDialog
      v-model:visible="approvalDialogVisible"
      :mode="approvalMode"
      :approval-info="currentApprovalItem"
      @success="handleApprovalSuccess"
    />

    <el-dialog
      v-model="supportRequestDialogVisible"
      title="标书支持申请"
      width="640px"
      destroy-on-close
    >
      <el-form :model="supportRequestForm" label-width="110px">
        <el-form-item label="关联项目" required>
          <el-select
            v-model="supportRequestForm.projectId"
            filterable
            placeholder="请选择投标项目"
            style="width: 100%"
          >
            <el-option
              v-for="project in supportRequestProjects"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请类型" required>
          <el-select v-model="supportRequestForm.type" style="width: 100%">
            <el-option label="技术支持" value="technical_support" />
            <el-option label="商务支持" value="commercial_support" />
            <el-option label="综合支持" value="bid_support" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望完成时间">
          <el-date-picker
            v-model="supportRequestForm.dueDate"
            type="datetime"
            placeholder="请选择期望完成时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="需求说明" required>
          <el-input
            v-model="supportRequestForm.description"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请说明需要的支持内容、交付物和时间要求"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="supportRequestDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="supportRequestSubmitting" @click="submitSupportRequest">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, markRaw, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
// biddingStore removed — calendar now loads directly from calendarApi
import { approvalApi, projectsApi, dashboardApi } from '@/api'
import { tasksApi } from '@/api/modules/dashboard.js'
import { calendarApi } from '@/api/modules/collaboration.js'
import { alertHistoryApi } from '@/api/modules/alerts.js'
import {
  getTimeGreeting,
  normalizeProjectForWorkbench,
  normalizeCalendarEvent,
  normalizeAlertForTodo,
  extractCustomersFromProjects
} from '@/views/Dashboard/workbench-utils.js'
import {
  Plus, DataAnalysis, ArrowRight, Calendar, User, Clock, Check,
  Document, Briefcase, TrendCharts, Flag, FolderOpened, Wallet
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ApprovalDialog from '@/components/common/ApprovalDialog.vue'

// Wrap icon components with markRaw to prevent Vue from making them reactive
// This improves performance by avoiding unnecessary reactivity overhead
const Icons = markRaw({
  Plus, DataAnalysis, ArrowRight, Calendar, User, Clock, Check,
  Document, Briefcase, TrendCharts, Flag, FolderOpened, Wallet
})

const router = useRouter()
const userStore = useUserStore()
// biddingStore instance removed — calendar data loaded via calendarApi

// 当前日期
const currentDate = computed(() => {
  const now = new Date()
  const options = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }
  return now.toLocaleDateString('zh-CN', options)
})

// 当前用户角色
const currentUserRole = computed(() => userStore.currentUser?.role || 'staff')
const currentUserName = computed(() => userStore.currentUser?.name || '用户')

// 待处理数量
const pendingCount = computed(() => {
  return priorityTodos.value.filter(t => !t.done).length
})

// ========== 工作台真实数据 ==========
const summaryStats = ref(null)
const metricsLoading = ref(true)
const pendingApprovalsTotalCount = ref(0)
const myProjectCount = computed(() => supportRequestProjects.value?.length || 0)

async function loadWorkbenchSummary() {
  try {
    const result = await dashboardApi.getSummary()
    if (result?.success) {
      summaryStats.value = result.data
    }
  } catch (error) {
    console.error('加载工作台摘要失败:', error)
  }
}

// 格式化辅助
const formatCurrency = (val) => {
  if (val == null) return '--'
  const num = Number(val)
  if (Number.isNaN(num)) return '--'
  const wan = num / 10000
  return wan >= 1 ? `¥${wan.toFixed(0)}万` : `¥${num.toFixed(0)}`
}
const formatPercent = (val) => (val != null ? `${Number(val).toFixed(1)}%` : '--')
const formatCount = (val, suffix = '') => (val != null ? `${val}${suffix}` : '--')

// ========== 角色化视图配置 ==========

// 管理层 (admin) - 动态指标
const adminMetrics = computed(() => {
  const s = summaryStats.value
  return [
    {
      key: 'totalRevenue',
      label: '年度中标金额',
      value: s ? formatCurrency(s.totalBudget) : '--',
      icon: Icons.TrendCharts,
      iconBg: 'linear-gradient(135deg, #D1FAE5 0%, #A7F3D0 100%)',
      iconColor: '#059669',
      change: '--',
      changeClass: 'neutral',
      variant: 'green'
    },
    {
      key: 'winRate',
      label: '整体中标率',
      value: s ? formatPercent(s.successRate) : '--',
      icon: Icons.Flag,
      iconBg: 'linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%)',
      iconColor: '#D97706',
      change: '--',
      changeClass: 'neutral',
      variant: 'amber'
    },
    {
      key: 'totalTenders',
      label: '总标讯数',
      value: s ? formatCount(s.totalTenders, '条') : '--',
      icon: Icons.Document,
      iconBg: 'linear-gradient(135deg, #DBEAFE 0%, #BFDBFE 100%)',
      iconColor: '#1E40AF',
      change: '--',
      changeClass: 'neutral',
      variant: 'blue'
    },
    {
      key: 'activeProjects',
      label: '进行中项目',
      value: s ? formatCount(s.activeProjects, '个') : '--',
      icon: Icons.Briefcase,
      iconBg: 'linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%)',
      iconColor: '#DC2626',
      change: '--',
      changeClass: 'neutral',
      variant: 'red'
    }
  ]
})

// 投标经理 (manager) - 动态指标
const biddingMetrics = computed(() => [
  {
    key: 'myProjects',
    label: '负责项目',
    value: formatCount(myProjectCount.value, '个'),
    icon: Icons.Briefcase,
    iconBg: 'linear-gradient(135deg, #DBEAFE 0%, #BFDBFE 100%)',
    iconColor: '#1E40AF',
    change: '--',
    changeClass: 'neutral',
    variant: 'blue'
  },
  {
    key: 'urgentTasks',
    label: '待处理任务',
    value: formatCount(pendingCount.value, '项'),
    icon: Icons.Flag,
    iconBg: 'linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%)',
    iconColor: '#DC2626',
    change: '--',
    changeClass: 'neutral',
    variant: 'red'
  },
  {
    key: 'pendingApprovals',
    label: '待审批',
    value: formatCount(pendingApprovalsTotalCount.value, '项'),
    icon: Icons.TrendCharts,
    iconBg: 'linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%)',
    iconColor: '#D97706',
    change: '--',
    changeClass: 'neutral',
    variant: 'amber'
  },
  {
    key: 'activeProjects',
    label: '进行中项目',
    value: summaryStats.value ? formatCount(summaryStats.value.activeProjects, '个') : '--',
    icon: Icons.User,
    iconBg: 'linear-gradient(135deg, #D1FAE5 0%, #A7F3D0 100%)',
    iconColor: '#059669',
    change: '--',
    changeClass: 'neutral',
    variant: 'green'
  }
])

// 技术员工 (staff) - 动态指标
const staffMetrics = computed(() => [
  {
    key: 'myTasks',
    label: '我的任务',
    value: formatCount(pendingCount.value, '项'),
    icon: Icons.Document,
    iconBg: 'linear-gradient(135deg, #DBEAFE 0%, #BFDBFE 100%)',
    iconColor: '#1E40AF',
    change: '--',
    changeClass: 'neutral',
    variant: 'blue'
  },
  {
    key: 'completedThisWeek',
    label: '已完成',
    value: formatCount(priorityTodos.value.filter(t => t.done).length, '项'),
    icon: Icons.Check,
    iconBg: 'linear-gradient(135deg, #D1FAE5 0%, #A7F3D0 100%)',
    iconColor: '#059669',
    change: '--',
    changeClass: 'neutral',
    variant: 'green'
  },
  {
    key: 'pendingReviews',
    label: '待审批',
    value: formatCount(pendingApprovalsTotalCount.value, '项'),
    icon: Icons.Flag,
    iconBg: 'linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%)',
    iconColor: '#DC2626',
    change: '--',
    changeClass: 'neutral',
    variant: 'red'
  },
  {
    key: 'activeProjects',
    label: '参与项目',
    value: formatCount(myProjectCount.value, '个'),
    icon: Icons.Briefcase,
    iconBg: 'linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%)',
    iconColor: '#D97706',
    change: '--',
    changeClass: 'neutral',
    variant: 'amber'
  }
])

// 根据角色获取指标
const metrics = computed(() => {
  const role = currentUserRole.value
  if (role === 'admin') return adminMetrics.value
  if (role === 'manager') return biddingMetrics.value
  return staffMetrics.value
})

// 横幅标题 - 动态问候
const bannerTitle = computed(() => {
  return `${getTimeGreeting()}，${currentUserName.value}`
})

// 横幅副标题 - 真实数据
const bannerSubtitle = computed(() => {
  const today = currentDate.value
  const s = summaryStats.value
  const role = currentUserRole.value
  if (role === 'admin') {
    const projects = s ? s.activeProjects : '--'
    const approvals = pendingApprovalsTotalCount.value
    return `今天是${today}，团队有${projects}个进行中的项目，${approvals}个待审批事项`
  }
  if (role === 'manager') {
    const projects = myProjectCount.value
    const tasks = pendingCount.value
    return `今天是${today}，您负责${projects}个项目，${tasks}项待处理任务`
  }
  return `今天是${today}，您有${pendingCount.value}项待处理任务`
})

// 横幅操作按钮 - 角色化
const bannerActions = computed(() => {
  const role = currentUserRole.value
  if (role === 'admin') {
    return [
      { key: 'report', label: '业绩报表', type: 'primary', icon: Icons.DataAnalysis, handler: () => router.push('/analytics/dashboard') },
      { key: 'team', label: '团队管理', type: 'default', icon: Icons.User, handler: () => router.push('/settings') }
    ]
  }
  if (role === 'manager') {
    return [
      { key: 'projects', label: '我的项目', type: 'primary', icon: Icons.Briefcase, handler: () => router.push('/project') },
      { key: 'tenders', label: '查看标讯', type: 'default', icon: Icons.Document, handler: () => router.push('/bidding') }
    ]
  }
  return [
    { key: 'tasks', label: '我的任务', type: 'primary', icon: Icons.Document, handler: () => {} },
    { key: 'calendar', label: '日程', type: 'default', icon: Icons.Calendar, handler: () => {} }
  ]
})

// 进行中项目 - 从 API 加载
const allProjects = ref([])
const rawProjects = ref([])

// 根据角色过滤项目
const activeProjects = computed(() => {
  const role = currentUserRole.value

  if (role === 'admin') {
    return allProjects.value.filter(p => p.priority === 'high' || p.priority === 'urgent').slice(0, 5)
  }
  return allProjects.value.slice(0, 5)
})

// 待办事项 - 系统预警从 API 加载
const apiAlertItems = ref([])
const apiTodoItems = ref([])

const systemWarningTodos = computed(() => apiAlertItems.value)

const normalizeApiTodo = (task) => {
  const priority = String(task?.priority || 'MEDIUM').toLowerCase()
  const done = task?.status === 'COMPLETED'
  const deadline = task?.dueDate
    ? new Date(task.dueDate).toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false })
    : '待排期'

  return {
    id: task.id,
    title: task.title,
    priority,
    deadline,
    done,
    type: 'task',
    sourceType: 'task',
    rawStatus: task.status }
}

async function loadPriorityTodos() {
  const assigneeId = userStore.currentUser?.id
  if (!assigneeId) {
    apiTodoItems.value = []
    return
  }

  try {
    const result = await tasksApi.getMine(assigneeId)
    apiTodoItems.value = Array.isArray(result?.data)
      ? result.data.map(normalizeApiTodo)
      : []
  } catch (error) {
    console.error('加载 API 待办失败:', error)
    apiTodoItems.value = []
  }
}

// 合并：系统预警优先，然后是 API 任务
const priorityTodos = computed(() =>
  [...systemWarningTodos.value, ...apiTodoItems.value].slice(0, 8)
)

// ========== 管理层数据 ==========
const teamPerformance = ref([])

const pendingApprovals = ref([])
const approvalDialogVisible = ref(false)
const approvalMode = ref('approve')
const currentApprovalItem = ref({})
const supportRequestDialogVisible = ref(false)
const supportRequestSubmitting = ref(false)
const supportRequestProjects = ref([])
const supportRequestForm = ref({
  projectId: null,
  type: 'bid_support',
  dueDate: '',
  description: ''
})

// ========== 销售经理数据 (小王) ==========
const hotTenders = ref([])

const followUpCustomers = computed(() => extractCustomersFromProjects(rawProjects.value))

// ========== 投标经理数据 (张经理) ==========
const teamMembers = ref([])

// ========== 技术员工数据 (李工) ==========

const pendingReviews = ref([])

// ========== 处理函数 ==========
const handleApprove = (item) => {
  approvalMode.value = 'approve'
  currentApprovalItem.value = item
  approvalDialogVisible.value = true
}

const handleReject = (item) => {
  approvalMode.value = 'reject'
  currentApprovalItem.value = item
  approvalDialogVisible.value = true
}

const handleApprovalSuccess = async () => {
  await loadPendingApprovals()
  await loadMyProcesses()
}

async function loadPendingApprovals() {
  try {
    const result = await approvalApi.getPendingApprovals({ page: 0, size: 8 })
    pendingApprovalsTotalCount.value = result?.totalCount ?? 0
    pendingApprovals.value = Array.isArray(result?.data) ? result.data.map((item) => ({
      ...item,
      title: item.title || `${item.projectName} - ${item.typeName}`,
      type: item.approvalType || 'project_review',
      department: item.applicantDept || '投标管理部',
      time: item.time || item.submitTime || '' })) : []
  } catch (error) {
    console.error('加载待审批事项失败:', error)
    pendingApprovals.value = []
  }
}

const approvalStatusToProcessStatus = (status) => {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'APPROVED') return 'in-progress'
  if (normalized === 'REJECTED' || normalized === 'CANCELLED') return 'urgent'
  return 'pending'
}

async function loadMyProcesses() {
  try {
    const result = await approvalApi.getMyApprovals({ page: 0, size: 8 })
    myProcesses.value = Array.isArray(result?.data) ? result.data.map((item) => ({
      id: item.id,
      title: item.title || `${item.projectName} - ${item.typeName}`,
      status: approvalStatusToProcessStatus(item.status),
      description: item.description || '暂无说明',
      progress: item.status === 'APPROVED' ? 100 : item.status === 'PENDING' ? 55 : 0,
      time: item.submittedAt || item.submitTime || item.time || ''
    })) : []
  } catch (error) {
    console.error('加载我的流程失败:', error)
    myProcesses.value = []
  }
}

async function loadSupportRequestProjects() {
  try {
    const result = await projectsApi.getList()
    if (!result?.success || !Array.isArray(result?.data)) {
      throw new Error(result?.message || '加载项目列表失败')
    }
    supportRequestProjects.value = result.data
      .map((item) => ({
        id: Number(item.id),
        name: item.name || item.projectName || `项目#${item.id}`
      }))
      .filter((item) => Number.isFinite(item.id))
  } catch (error) {
    console.error('加载支持申请项目失败:', error)
    supportRequestProjects.value = []
  }
}

const resetSupportRequestForm = () => {
  supportRequestForm.value = {
    projectId: supportRequestProjects.value[0]?.id || null,
    type: 'bid_support',
    dueDate: '',
    description: ''
  }
}

async function submitSupportRequest() {
  if (!supportRequestForm.value.projectId) {
    ElMessage.warning('请选择关联项目')
    return
  }
  if (!supportRequestForm.value.description.trim()) {
    ElMessage.warning('请填写需求说明')
    return
  }

  const selectedProject = supportRequestProjects.value.find((item) => item.id === Number(supportRequestForm.value.projectId))
  supportRequestSubmitting.value = true
  try {
    const result = await approvalApi.submitApproval({
      projectId: Number(supportRequestForm.value.projectId),
      projectName: selectedProject?.name || `项目#${supportRequestForm.value.projectId}`,
      approvalType: supportRequestForm.value.type,
      title: `${selectedProject?.name || '当前项目'} - 标书支持申请`,
      description: supportRequestForm.value.description.trim(),
      dueDate: supportRequestForm.value.dueDate || null,
      priority: 1
    })
    if (!result?.success) {
      throw new Error(result?.message || '提交标书支持申请失败')
    }
    ElMessage.success('标书支持申请已提交')
    supportRequestDialogVisible.value = false
    await Promise.all([loadPendingApprovals(), loadMyProcesses()])
    resetSupportRequestForm()
  } catch (error) {
    ElMessage.error(error?.message || '提交标书支持申请失败')
  } finally {
    supportRequestSubmitting.value = false
  }
}

const handleTenderClick = (tender) => {
  router.push(`/bidding/${tender.id}`)
}

const handleTaskComplete = async (task) => {
  if (task.type === 'warning') {
    task.done = !task.done
    if (task.done) {
      ElMessage.success(`完成任务: ${task.title}`)
    }
    return
  }

  if (task.done) {
    return
  }

  try {
    const result = await tasksApi.complete(task.id)
    if (!result?.success) {
      throw new Error(result?.message || '更新待办状态失败')
    }
    ElMessage.success(`完成任务: ${task.title}`)
    await loadPriorityTodos()
  } catch (error) {
    ElMessage.error(error?.message || '更新待办状态失败')
  }
}

// 快速发起处理
const handleQuickAction = (type) => {
  switch (type) {
    case 'support':
      supportRequestDialogVisible.value = true
      if (supportRequestProjects.value.length === 0) {
        loadSupportRequestProjects().then(resetSupportRequestForm)
      } else {
        resetSupportRequestForm()
      }
      break
    case 'borrow':
      router.push('/knowledge/qualification')
      break
    case 'expense':
      router.push('/resource/expense')
      break
  }
}

// 我的流程
const myProcesses = ref([])

// 最新动态
const activities = ref([])

// ========== 投标日历相关 ==========
const calendarDate = ref(new Date())
const activeCalendarFilter = ref('all')
const selectedDateKey = ref('')

const calendarFilters = [
  { label: '全部', value: 'all' },
  { label: '截止', value: 'deadline' },
  { label: '投标', value: 'bid' },
  { label: '开标', value: 'opening' },
  { label: '评审', value: 'review' },
  { label: '高风险', value: 'urgent' }
]

// 从 API 获取日历数据
const calendarEventsData = ref([])
const calendarEvents = computed(() => calendarEventsData.value)

const parseDate = (dateStr) => new Date(`${dateStr}T00:00:00`)

const formatDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const startOfToday = () => parseDate(formatDateKey(new Date()))

const getDaysUntil = (dateStr) => {
  const oneDay = 24 * 60 * 60 * 1000
  return Math.round((parseDate(dateStr) - startOfToday()) / oneDay)
}

const getEventOwner = (event) => {
  const ownerMap = {
    deadline: '商务专员',
    bid: '投标经理',
    opening: '销售经理',
    review: '技术负责人'
  }
  return ownerMap[event.type] || '项目负责人'
}

const getEventStage = (event) => {
  const stageMap = {
    deadline: '资料收口',
    bid: '递交前确认',
    opening: '现场准备',
    review: '评审跟进'
  }
  return stageMap[event.type] || '节点处理中'
}

const getEventBlocker = (event) => {
  const blockerMap = {
    deadline: '待确认最终材料',
    bid: '待核对报价与盖章',
    opening: '待确认授权与签到',
    review: '待准备答疑材料'
  }
  return blockerMap[event.type] || '待补充信息'
}

const getEventActionLabel = (event) => {
  const actionMap = {
    deadline: '去补材料',
    bid: '去检查递交',
    opening: '看开标准备',
    review: '看评审清单'
  }
  return actionMap[event.type] || '查看详情'
}

const decorateCalendarEvent = (event) => {
  const diffDays = getDaysUntil(event.date)
  const isExpired = diffDays < 0
  const isCritical = event.urgent || diffDays <= 1
  const isWarning = !isCritical && diffDays <= 3

  return {
    ...event,
    diffDays,
    countdownLabel: isExpired ? '已逾期' : diffDays === 0 ? '今天' : `D-${diffDays}`,
    riskLabel: isExpired ? '已逾期' : isCritical ? '高风险' : isWarning ? '需关注' : '常规',
    riskTagType: isExpired ? 'danger' : isCritical ? 'info' : isWarning ? 'warning' : 'info',
    priorityLevel: isExpired || isCritical ? 'priority-critical' : isWarning ? 'priority-warning' : 'priority-normal',
    actionLabel: getEventActionLabel(event),
    dayLabel: event.date.slice(5),
    weekdayLabel: parseDate(event.date).toLocaleDateString('zh-CN', { weekday: 'short' }).replace('周', ''),
    fieldSummary: {
      owner: `负责人 ${getEventOwner(event)}`,
      stage: `阶段 ${getEventStage(event)}`,
      blocker: `阻塞 ${getEventBlocker(event)}`
    }
  }
}

const normalizedCalendarEvents = computed(() => calendarEvents.value.map(decorateCalendarEvent))

const eventMatchesFilter = (event, filterValue = activeCalendarFilter.value) => {
  if (filterValue === 'all') return true
  if (filterValue === 'urgent') return event.urgent || event.priorityLevel === 'priority-critical'
  return event.type === filterValue
}

const visibleCalendarEvents = computed(() => normalizedCalendarEvents.value.filter((event) => eventMatchesFilter(event)))

// 获取指定日期的事件
const getEventsForDate = (date) => {
  const dateStr = formatDateKey(date)
  return visibleCalendarEvents.value.filter(event => event.date === dateStr)
}

// 日历单元格自定义渲染
const calendarCellClass = ({ date, viewType }) => {
  if (viewType !== 'month') return ''
  const events = getEventsForDate(date)
  if (events.length === 0) return ''

  if (events.some((event) => event.priorityLevel === 'priority-critical')) return 'calendar-day-urgent'
  if (events.length >= 3) return 'calendar-day-crowded'
  return 'calendar-day-has-event'
}

// 点击日期
const handleDateClick = (date) => {
  selectedDateKey.value = formatDateKey(date)
  calendarDate.value = date
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

const selectedDateEvents = computed(() =>
  visibleCalendarEvents.value.filter((event) => event.date === selectedDateKey.value)
)

const selectedDateLabel = computed(() => {
  const date = parseDate(selectedDateKey.value || formatDateKey(new Date()))
  return date.toLocaleDateString('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

const monthCalendarSummary = computed(() => {
  const currentYear = calendarDate.value.getFullYear()
  const currentMonth = calendarDate.value.getMonth()
  const monthEvents = visibleCalendarEvents.value.filter((event) => {
    const eventDate = parseDate(event.date)
    return eventDate.getFullYear() === currentYear && eventDate.getMonth() === currentMonth
  })
  const nextDeadline = monthEvents
    .filter((event) => event.type === 'deadline' && event.diffDays >= 0)
    .sort((a, b) => a.diffDays - b.diffDays)[0]

  return {
    total: monthEvents.length,
    urgent: monthEvents.filter((event) => event.urgent || event.priorityLevel === 'priority-critical').length,
    nextDeadlineLabel: nextDeadline ? nextDeadline.countdownLabel : '暂无'
  }
})

const upcomingCalendarEvents = computed(() =>
  visibleCalendarEvents.value
    .filter((event) => event.diffDays >= 0 && event.diffDays <= 7)
    .sort((a, b) => a.diffDays - b.diffDays || a.date.localeCompare(b.date))
)

const selectCalendarEventDate = (event) => {
  selectedDateKey.value = event.date
  calendarDate.value = parseDate(event.date)
}

const handleCalendarAction = (event) => {
  ElMessage.info(`${event.actionLabel}：${event.project}`)
}

// P0: 加载真实项目列表
async function loadProjects() {
  try {
    const userId = userStore.currentUser?.id
    const role = currentUserRole.value
    const result = role === 'admin'
      ? await projectsApi.getList()
      : await projectsApi.getList({ managerId: userId })
    const items = Array.isArray(result?.data) ? result.data : []
    rawProjects.value = items
    allProjects.value = items.map(normalizeProjectForWorkbench)
  } catch {
    rawProjects.value = []
    allProjects.value = []
  }
}

// P0: 加载日历事件
async function loadCalendarEvents() {
  try {
    const date = calendarDate.value || new Date()
    const year = date.getFullYear()
    const month = date.getMonth() + 1
    const result = await calendarApi.getMonthEvents(year, month)
    const items = Array.isArray(result?.data) ? result.data : (Array.isArray(result) ? result : [])
    calendarEventsData.value = items.map(normalizeCalendarEvent)
  } catch {
    calendarEventsData.value = []
  }
}

// P1: 加载系统预警
async function loadSystemAlerts() {
  try {
    const result = await alertHistoryApi.getUnresolved({ page: 0, size: 10 })
    const items = Array.isArray(result?.data?.content) ? result.data.content : (Array.isArray(result?.data) ? result.data : [])
    apiAlertItems.value = items.map(normalizeAlertForTodo)
  } catch {
    apiAlertItems.value = []
  }
}

watch(calendarDate, (newDate, oldDate) => {
  if (!newDate || !oldDate) return
  if (newDate.getFullYear() !== oldDate.getFullYear() || newDate.getMonth() !== oldDate.getMonth()) {
    loadCalendarEvents()
  }
})

// 数据加载
onMounted(async () => {
  metricsLoading.value = true
  await Promise.allSettled([
    loadCalendarEvents(),
    loadPendingApprovals(),
    loadMyProcesses(),
    loadSupportRequestProjects(),
    loadPriorityTodos(),
    loadWorkbenchSummary(),
    loadProjects(),
    loadSystemAlerts()
  ])
  metricsLoading.value = false
  resetSupportRequestForm()
  selectedDateKey.value = formatDateKey(new Date())
  const firstUpcomingEvent = normalizedCalendarEvents.value
    .filter((event) => event.diffDays >= 0)
    .sort((a, b) => a.diffDays - b.diffDays)[0]

  if (firstUpcomingEvent) {
    selectedDateKey.value = firstUpcomingEvent.date
    calendarDate.value = parseDate(firstUpcomingEvent.date)
  }
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
    '已立项': 'info',
    '编制中': 'warning',
    '评审中': 'primary',
    '封装中': '',
    '投标中': 'danger',
    '已归档': 'success',
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

const metricDrilldownRouteMap = {
  totalRevenue: { path: '/analytics/dashboard', query: { drilldown: 'revenue' } },
  winRate: { path: '/analytics/dashboard', query: { drilldown: 'win-rate' } },
  teamSize: { path: '/analytics/dashboard', query: { drilldown: 'team' } },
  activeProjects: { path: '/analytics/dashboard', query: { drilldown: 'projects', status: 'in_progress' } },
  newTenders: '/bidding',
  myOpportunities: '/bidding',
  customerVisits: '/project',
  pendingProposals: '/project/create',
  myProjects: '/project',
  urgentTasks: '/project',
  teamWorkload: '/project',
  resourceStatus: '/settings',
  myTasks: '/project',
  completedThisWeek: '/project',
  pendingReviews: '/project',
  workHours: '/project'
}

const handleMetricClick = (metric) => {
  const targetRoute = metricDrilldownRouteMap[metric.key]

  if (targetRoute) {
    router.push(targetRoute)
    return
  }

  ElMessage.info(`${metric.label} 暂无详情页`)
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

.page-identity {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.page-kicker {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  letter-spacing: 0.08em;
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
  grid-template-columns: minmax(0, 1.28fr) minmax(340px, 0.92fr);
  gap: 20px;
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.side-summary-grid {
  display: grid;
  grid-template-columns: 1fr;
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

/* 系统预警待办样式 */
.todo-item.todo-warning {
  background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%);
  border-left-color: #F59E0B;
  border-left-width: 4px;
}

.todo-item.todo-warning .todo-title {
  color: #92400E;
  font-weight: 500;
}

.todo-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.todo-title-row .todo-title {
  flex: 1;
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
  line-height: 1.4;
}

.todo-title-row .todo-title {
  margin-bottom: 0;
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

/* ==================== 角色化横幅样式 ==================== */
.banner-admin {
  background: linear-gradient(135deg, #7C3AED 0%, #5B21B6 100%);
}

.banner-admin .banner-actions .el-button--primary {
  background: #F5F3FF;
  color: #7C3AED;
  border-color: #F5F3FF;
}

/* ==================== 团队绩效卡片 ==================== */
.team-performance-grid {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.team-performance-item {
  padding: 12px;
  background: #F9FAFB;
  border-radius: 8px;
}

.team-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.team-name {
  font-weight: 500;
  color: #111827;
}

.team-size {
  font-size: 12px;
  color: #6B7280;
}

.team-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.team-progress .progress-bar {
  flex: 1;
  height: 6px;
  background: #E5E7EB;
  border-radius: 3px;
  overflow: hidden;
}

.team-progress .progress-fill {
  height: 100%;
  transition: width 0.3s ease;
}

.team-progress .progress-label {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  min-width: 36px;
}

.team-metrics {
  display: flex;
  gap: 16px;
}

.team-metric {
  font-size: 12px;
  color: #6B7280;
}

/* ==================== 审批列表 ==================== */
.approvals-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.approval-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #F9FAFB;
  border-radius: 8px;
}

.approval-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
}

.approval-icon.type-budget { background: #3B82F6; }
.approval-icon.type-project { background: #10B981; }
.approval-icon.type-hr { background: #F59E0B; }

.approval-content {
  flex: 1;
}

.approval-title {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 2px;
}

.approval-meta {
  font-size: 12px;
  color: #6B7280;
}

.approval-actions {
  display: flex;
  gap: 6px;
}

/* ==================== 标讯卡片 ==================== */
.tenders-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tender-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #F9FAFB;
  border-radius: 8px;
  border-left: 3px solid #E5E7EB;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tender-card:hover {
  background: #F3F4F6;
  border-left-color: #3B82F6;
}

.tender-score {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-weight: 700;
  font-size: 14px;
  color: #fff;
  flex-shrink: 0;
}

.tender-score.score-high { background: linear-gradient(135deg, #10B981 0%, #059669 100%); }
.tender-score.score-medium { background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%); }

.tender-info {
  flex: 1;
}

.tender-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 6px;
}

.tender-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #6B7280;
}

/* ==================== 客户跟进 ==================== */
.customers-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.customer-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #F9FAFB;
  border-radius: 8px;
}

.customer-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.customer-info {
  flex: 1;
}

.customer-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.customer-company {
  font-size: 12px;
  color: #6B7280;
}

/* ==================== 快速发起卡片 ==================== */
.quick-actions-grid {
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  background: #F9FAFB;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
}

.quick-action-item:hover {
  background: #F3F4F6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.action-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  margin-bottom: 10px;
}

.action-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 4px;
}

.action-desc {
  font-size: 12px;
  color: #6B7280;
}

/* ==================== 团队任务 ==================== */
.team-tasks-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #F9FAFB;
  border-radius: 8px;
}

.member-avatar {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.member-info {
  flex: 1;
}

.member-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 4px;
}

.member-tasks {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.member-tasks .el-tag {
  font-size: 10px;
  padding: 1px 6px;
  height: 18px;
}

.more-tasks {
  font-size: 11px;
  color: #6B7280;
}

.member-workload {
  text-align: right;
}

.workload-label {
  display: block;
  font-size: 11px;
  color: #6B7280;
  margin-bottom: 2px;
}

.workload-value {
  font-size: 14px;
  font-weight: 600;
}

.workload-value.workload-high { color: #EF4444; }
.workload-value.workload-medium { color: #F59E0B; }
.workload-value.workload-low { color: #10B981; }

/* ==================== 我的任务 (技术员工) ==================== */
.my-tasks-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.my-task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #F9FAFB;
  border-radius: 8px;
  border-left: 3px solid #E5E7EB;
}

.my-task-item.priority-high {
  border-left-color: #EF4444;
  background: #FEF2F2;
}

.my-task-item.priority-medium {
  border-left-color: #F59E0B;
}

.task-checkbox {
  flex-shrink: 0;
}

.task-content {
  flex: 1;
}

.task-title {
  display: block;
  font-size: 14px;
  color: #111827;
  margin-bottom: 4px;
}

.task-title.done {
  text-decoration: line-through;
  color: #9CA3AF;
}

.task-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.task-project {
  color: #3B82F6;
}

.task-deadline {
  color: #6B7280;
}

/* ==================== 评审列表 ==================== */
.review-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #F9FAFB;
  border-radius: 8px;
}

.review-icon {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%);
  color: #fff;
  flex-shrink: 0;
}

.review-content {
  flex: 1;
}

.review-title {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 2px;
}

.review-meta {
  font-size: 12px;
  color: #6B7280;
}

/* ==================== 投标日历 ==================== */
.calendar-wrapper {
  padding: 16px;
}

.calendar-wrapper :deep(.el-calendar) {
  --el-calendar-cell-width: 40px;
  --el-calendar-border-color: #F3F4F6;
}

.calendar-wrapper :deep(.el-calendar__header) {
  padding: 0 0 16px 0;
  border-bottom: 1px solid #F3F4F6;
  margin-bottom: 16px;
}

.calendar-wrapper :deep(.el-calendar__title) {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.calendar-wrapper :deep(.el-calendar__button-group) {
  display: flex;
  gap: 8px;
}

.calendar-wrapper :deep(.el-calendar__button-group .el-button) {
  padding: 6px 12px;
  font-size: 13px;
}

.calendar-wrapper :deep(.el-calendar-table) {
  thead {
    th {
      padding: 8px 0;
      font-size: 12px;
      color: #6B7280;
      font-weight: 500;
      border-bottom: 1px solid #F3F4F6;
    }
  }

  td {
    border: none;
    padding: 2px;
  }

  .el-calendar-day {
    height: 60px;
    padding: 0;
    text-align: center;
    border-radius: 8px;
    transition: all 0.2s ease;
  }

  .el-calendar-day:hover {
    background: #F3F4F6;
  }

  .el-calendar-day.is-selected {
    background: linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%) !important;
    color: #fff !important;
  }

  .el-calendar-day.is-today {
    background: #DBEAFE;
    color: #1E40AF;
    font-weight: 600;
  }
}

.calendar-day-cell {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 45px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 4px 2px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.calendar-day-cell:hover {
  background: #F3F4F6;
}

.calendar-day-cell.calendar-day-has-event {
  background: #FAFBFF;
  border: 1px solid #DBEAFE;
}

.calendar-day-cell.calendar-day-urgent {
  background: #FEFAFA;
  border: 1px solid #FEE2E2;
}

.calendar-day-number {
  font-size: 13px;
  line-height: 1;
  z-index: 1;
  align-self: center;
  width: 100%;
  text-align: center;
}

.calendar-day-events {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 4px;
  width: 100%;
}

.calendar-event-item {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px 4px;
  border-radius: 3px;
  font-size: 10px;
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.calendar-event-item.event-deadline {
  background: #FEF2F2;
  color: #DC2626;
  border-left: 2px solid #EF4444;
}

.calendar-event-item.event-bid {
  background: #DBEAFE;
  color: #1D4ED8;
  border-left: 2px solid #3B82F6;
}

.calendar-event-item.event-opening {
  background: #D1FAE5;
  color: #059669;
  border-left: 2px solid #10B981;
}

.calendar-event-item.event-review {
  background: #FEF3C7;
  color: #D97706;
  border-left: 2px solid #F59E0B;
}

.event-type-icon {
  flex-shrink: 0;
  font-size: 10px;
}

.event-title-short {
  flex: 1;
  overflow: hidden;
  font-size: 10px;
  line-height: 1.3;
  word-break: break-all;
}

.event-more {
  font-size: 9px;
  color: #6B7280;
  text-align: center;
  padding: 2px 0;
}

/* 今日日程卡片 */
.today-events-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.today-event-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #F9FAFB;
  border-radius: 8px;
  border-left: 3px solid #E5E7EB;
  transition: all 0.2s ease;
  cursor: pointer;
}

.today-event-item:hover {
  background: #F3F4F6;
}

.today-event-item.event-deadline {
  border-left-color: #EF4444;
  background: #FEF2F2;
}

.today-event-item.event-bid {
  border-left-color: #3B82F6;
  background: #DBEAFE;
}

.today-event-item.event-opening {
  border-left-color: #10B981;
  background: #D1FAE5;
}

.today-event-item.event-review {
  border-left-color: #F59E0B;
  background: #FEF3C7;
}

.event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.today-event-item.event-deadline .event-dot { background: #EF4444; }
.today-event-item.event-bid .event-dot { background: #3B82F6; }
.today-event-item.event-opening .event-dot { background: #10B981; }
.today-event-item.event-review .event-dot { background: #F59E0B; }

.event-content {
  flex: 1;
  min-width: 0;
}

.event-title {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-project {
  font-size: 12px;
  color: #6B7280;
}

/* 事件详情弹窗 */
.event-dialog-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.event-dialog-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #F9FAFB;
  border-radius: 8px;
  border-left: 3px solid #E5E7EB;
}

.event-dialog-item.event-deadline {
  border-left-color: #EF4444;
  background: #FEF2F2;
}

.event-dialog-item.event-bid {
  border-left-color: #3B82F6;
  background: #DBEAFE;
}

.event-dialog-item.event-opening {
  border-left-color: #10B981;
  background: #D1FAE5;
}

.event-dialog-item.event-review {
  border-left-color: #F59E0B;
  background: #FEF3C7;
}

.event-dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.event-dialog-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
}

.event-dialog-icon.icon-deadline { background: #EF4444; }
.event-dialog-icon.icon-bid { background: #3B82F6; }
.event-dialog-icon.icon-opening { background: #10B981; }
.event-dialog-icon.icon-review { background: #F59E0B; }

.event-dialog-info {
  flex: 1;
}

.event-dialog-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin: 0 0 2px 0;
}

.event-dialog-project {
  font-size: 12px;
  color: #6B7280;
  margin: 0;
}

/* ==================== 投标日历重构覆盖 ==================== */
.calendar-card--hero {
  border-color: #D7E3F8;
  box-shadow: 0 16px 40px rgba(30, 64, 175, 0.08);
}

.calendar-topbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 16px 16px;
}

.calendar-summary-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 0;
}

.summary-pill {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: linear-gradient(180deg, #FBFDFF 0%, #F3F7FF 100%);
  border: 1px solid #E2E8F0;
  box-shadow: none;
}

.calendar-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.95fr);
  gap: 16px;
  padding: 0 16px 16px;
  align-items: start;
}

.calendar-hero-main,
.calendar-hero-side {
  min-width: 0;
}

.calendar-hero-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-pill.risk {
  background: linear-gradient(180deg, #FDF4FF 0%, #FAE8FF 100%);
  border-color: #E9D5FF;
}

.summary-pill.accent {
  background: linear-gradient(180deg, #EEF2FF 0%, #E0E7FF 100%);
  border-color: #C7D2FE;
}

.summary-label {
  font-size: 10px;
  color: #64748B;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.summary-pill strong {
  font-size: 16px;
  line-height: 1;
  color: #0F172A;
  white-space: nowrap;
}

.calendar-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 14px;
  background: linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%);
  border: 1px solid #E5E7EB;
}

.calendar-filter-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 120px;
}

.filter-eyebrow {
  font-size: 11px;
  color: #64748B;
  letter-spacing: 0.05em;
}

.filter-hint {
  font-size: 12px;
  color: #94A3B8;
}

.calendar-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  padding: 0;
}

.calendar-filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 11px;
  border: 1px solid #E5E7EB;
  border-radius: 999px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.calendar-filter-chip:hover,
.calendar-filter-chip.active {
  color: #1D4ED8;
  border-color: #BFDBFE;
  background: #EFF6FF;
}

.dot-all {
  background: linear-gradient(135deg, #3B82F6 0%, #8B5CF6 100%);
}

.dot-urgent {
  background: #C026D3;
  box-shadow: 0 0 0 2px rgba(192, 38, 211, 0.16);
}

.calendar-wrapper {
  padding: 0;
}

.calendar-wrapper :deep(.el-calendar__header) {
  margin-bottom: 12px;
}

.calendar-wrapper :deep(.el-calendar-table .el-calendar-day) {
  height: 54px;
  border-radius: 10px;
}

.calendar-wrapper :deep(.el-calendar-table .el-calendar-day:hover) {
  background: #F8FAFC;
}

.calendar-wrapper :deep(.el-calendar-table .el-calendar-day.is-selected) {
  background: linear-gradient(135deg, #E0ECFF 0%, #DBEAFE 100%) !important;
  color: #1D4ED8 !important;
}

.calendar-wrapper :deep(.el-calendar-table .el-calendar-day.is-today) {
  background: #F8FAFC;
  color: #0F172A;
}

.calendar-day-cell {
  min-height: 44px;
  align-items: center;
  padding: 5px 4px;
  border-radius: 10px;
}

.calendar-day-cell.calendar-day-urgent {
  background: #FDF4FF;
  border-color: #E9D5FF;
}

.calendar-day-cell.calendar-day-crowded {
  background: #FFFDF5;
  border: 1px solid #FDE68A;
}

.calendar-day-number {
  color: #0F172A;
}

.calendar-day-marker {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 7px;
}

.calendar-day-dots {
  display: flex;
  align-items: center;
  gap: 3px;
}

.calendar-event-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.calendar-event-dot.event-deadline,
.legend-dot.event-deadline,
.chip-dot.dot-deadline {
  background: #EF4444;
}

.calendar-event-dot.event-bid,
.legend-dot.event-bid,
.chip-dot.dot-bid {
  background: #3B82F6;
}

.calendar-event-dot.event-opening,
.legend-dot.event-opening,
.chip-dot.dot-opening {
  background: #10B981;
}

.calendar-event-dot.event-review,
.legend-dot.event-review,
.chip-dot.dot-review {
  background: #F59E0B;
}

.chip-dot,
.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
}

.calendar-day-count {
  min-width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  border-radius: 999px;
  background: #E2E8F0;
  color: #334155;
  font-size: 10px;
  font-weight: 600;
}

.calendar-day-alert {
  font-size: 10px;
  font-weight: 700;
  color: #A21CAF;
}

.calendar-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 0 0;
  color: #64748B;
  font-size: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.calendar-panel,
.upcoming-panel {
  margin: 0;
  padding: 14px;
  border-radius: 16px;
  background: linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%);
  border: 1px solid #E5E7EB;
}

.calendar-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.calendar-panel-eyebrow {
  font-size: 11px;
  color: #94A3B8;
  margin-bottom: 2px;
}

.calendar-panel-title {
  margin: 0;
  font-size: 15px;
  color: #0F172A;
}

.selected-events-list,
.upcoming-events-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.selected-event-card,
.upcoming-event-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
  background: #FFFFFF;
  transition: all 0.2s ease;
}

.upcoming-event-item {
  cursor: pointer;
}

.selected-event-card.priority-critical,
.upcoming-event-item.priority-critical {
  border-color: #E9D5FF;
  background: linear-gradient(180deg, #FDF7FF 0%, #FAE8FF 100%);
}

.selected-event-card.priority-warning,
.upcoming-event-item.priority-warning {
  border-color: #FDE68A;
  background: linear-gradient(180deg, #FFFDF5 0%, #FFFBEB 100%);
}

.selected-event-card.priority-normal,
.upcoming-event-item.priority-normal {
  border-color: #DBEAFE;
  background: linear-gradient(180deg, #FFFFFF 0%, #F8FBFF 100%);
}

.selected-event-main,
.upcoming-event-body {
  flex: 1;
  min-width: 0;
}

.selected-event-topline,
.upcoming-event-title-row,
.selected-event-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.selected-event-type,
.upcoming-event-date-label,
.upcoming-event-owner {
  font-size: 11px;
  color: #64748B;
}

.selected-event-countdown,
.upcoming-rail-countdown {
  font-size: 12px;
  font-weight: 700;
  color: #0F172A;
}

.selected-event-title,
.upcoming-event-title {
  margin: 6px 0 4px;
  font-size: 14px;
  font-weight: 500;
  color: #0F172A;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-event-project,
.upcoming-event-subline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: #64748B;
}

.selected-event-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.selected-event-meta span {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.12);
  color: #475569;
  font-size: 11px;
}

.selected-event-actions {
  flex-direction: column;
  align-items: flex-end;
}

.selected-event-actions :deep(.el-button),
.upcoming-event-side :deep(.el-button) {
  margin: 0;
  border: none;
  background: linear-gradient(135deg, #1d4ed8 0%, #0f766e 100%);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(29, 78, 216, 0.18);
}

.selected-event-actions :deep(.el-button:hover),
.upcoming-event-side :deep(.el-button:hover) {
  background: linear-gradient(135deg, #1e40af 0%, #115e59 100%);
  color: #ffffff;
}

.selected-event-card.priority-critical .selected-event-actions :deep(.el-button),
.upcoming-event-item.priority-critical .upcoming-event-side :deep(.el-button) {
  background: linear-gradient(135deg, #a21caf 0%, #7e22ce 100%);
  box-shadow: 0 8px 18px rgba(162, 28, 175, 0.2);
}

.selected-event-card.priority-critical .selected-event-actions :deep(.el-button:hover),
.upcoming-event-item.priority-critical .upcoming-event-side :deep(.el-button:hover) {
  background: linear-gradient(135deg, #86198f 0%, #6b21a8 100%);
}

.selected-event-card.priority-warning .selected-event-actions :deep(.el-button),
.upcoming-event-item.priority-warning .upcoming-event-side :deep(.el-button) {
  background: linear-gradient(135deg, #c2410c 0%, #ea580c 100%);
  box-shadow: 0 8px 18px rgba(234, 88, 12, 0.18);
}

.selected-event-card.priority-warning .selected-event-actions :deep(.el-button:hover),
.upcoming-event-item.priority-warning .upcoming-event-side :deep(.el-button:hover) {
  background: linear-gradient(135deg, #9a3412 0%, #c2410c 100%);
}

.risk-tag.priority-critical {
  --el-tag-bg-color: #FAE8FF;
  --el-tag-border-color: #E9D5FF;
  --el-tag-text-color: #A21CAF;
}

.upcoming-event-rail {
  width: 58px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 10px 6px;
  border-radius: 10px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  flex-shrink: 0;
}

.upcoming-rail-type {
  padding: 3px 7px;
  border-radius: 999px;
  background: #E2E8F0;
  font-size: 10px;
  color: #475569;
}

.execution-meta {
  margin-top: 6px;
}

.execution-chip {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 999px;
  background: #EEF2FF;
  color: #4338CA;
  font-size: 11px;
}

.execution-chip.muted {
  background: #F1F5F9;
  color: #475569;
}

.execution-chip.blocker {
  background: #FFF7ED;
  color: #C2410C;
}

.upcoming-event-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  min-width: 88px;
  gap: 8px;
}

.calendar-empty-state {
  padding: 24px 12px;
  text-align: center;
  border-radius: 12px;
  background: #F8FAFC;
  color: #94A3B8;
  font-size: 13px;
}

.calendar-empty-state.compact {
  padding: 14px 12px;
  font-size: 12px;
}

.team-performance-grid.compact,
.approvals-list.compact {
  padding: 14px;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .calendar-filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .calendar-filter-row {
    justify-content: flex-start;
  }

  .calendar-hero-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
  }

  .side-summary-grid {
    grid-column: 1 / -1;
    grid-template-columns: 1fr 1fr;
  }

  /* 快速发起平板响应式 */
  .quick-actions-grid {
    grid-template-columns: 1fr;
    gap: 8px;
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

  .side-summary-grid {
    grid-template-columns: 1fr;
  }

  .calendar-summary-strip {
    grid-template-columns: 1fr;
  }

  /* 快速发起响应式 */
  .quick-actions-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .quick-action-item {
    padding: 12px 8px;
  }

  .action-icon {
    width: 40px;
    height: 40px;
  }

  .action-title {
    font-size: 12px;
  }

  .action-desc {
    font-size: 10px;
  }

  /* 日历响应式 */
  .calendar-wrapper :deep(.el-calendar-table .el-calendar-day) {
    height: 40px;
  }

  .calendar-day-number {
    font-size: 11px;
  }

  .calendar-day-marker {
    gap: 4px;
  }

  .calendar-day-count {
    min-width: 16px;
    height: 16px;
    font-size: 9px;
  }

  .calendar-day-cell {
    min-height: 40px;
    padding: 4px 2px;
  }

  .selected-event-card,
  .upcoming-event-item,
  .calendar-panel-header,
  .selected-event-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .upcoming-event-rail {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
  }

  .upcoming-event-side {
    width: 100%;
    min-width: 0;
    flex-direction: row;
    align-items: center;
  }

  .calendar-filter-row {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 14px;
  }

  .calendar-filter-copy {
    min-width: 0;
  }
}
</style>
