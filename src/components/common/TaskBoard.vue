<template>
  <div class="task-board">
    <div class="board-column" v-for="column in columns" :key="column.key">
      <div class="column-header" :class="`column-${column.key}`">
        <span class="column-title">{{ column.title }}</span>
        <el-badge :value="getTaskCount(column.key)" class="badge" />
      </div>
      <div class="column-content">
        <div
          v-for="task in getTasksByStatus(column.key)"
          :key="task.id"
          class="task-card"
          :class="{ 'task-high': task.priority === 'high' }"
          @click="handleTaskClick(task)"
        >
          <div class="task-header">
            <el-tag
              :type="getPriorityType(task.priority)"
              size="small"
              v-if="task.priority"
            >
              {{ getPriorityText(task.priority) }}
            </el-tag>
            <el-dropdown trigger="click" @click.stop>
              <el-icon class="more-icon"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleStatusChange(task, 'todo')">
                    移至待办
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleStatusChange(task, 'doing')">
                    移至进行中
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleStatusChange(task, 'done')">
                    移至已完成
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="task-name">{{ task.name }}</div>
          <div class="task-meta">
            <div class="task-owner">
              <el-icon><User /></el-icon>
              <span>{{ task.owner }}</span>
            </div>
            <div class="task-deadline" :class="{ 'deadline-urgent': isUrgent(task.deadline) }">
              <el-icon><Calendar /></el-icon>
              <span>{{ task.deadline }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="getTasksByStatus(column.key).length === 0" description="暂无任务" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { MoreFilled, User, Calendar } from '@element-plus/icons-vue'

const props = defineProps({
  tasks: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['task-click', 'status-change'])

const columns = [
  { key: 'todo', title: '待办' },
  { key: 'doing', title: '进行中' },
  { key: 'done', title: '已完成' }
]

const getTasksByStatus = (status) => {
  return props.tasks.filter(t => t.status === status)
}

const getTaskCount = (status) => {
  return getTasksByStatus(status).length
}

const getPriorityType = (priority) => {
  const typeMap = {
    high: 'danger',
    medium: 'warning',
    low: 'info'
  }
  return typeMap[priority] || 'info'
}

const getPriorityText = (priority) => {
  const textMap = {
    high: '高',
    medium: '中',
    low: '低'
  }
  return textMap[priority] || priority
}

const isUrgent = (deadline) => {
  if (!deadline) return false
  const deadlineDate = new Date(deadline)
  const today = new Date()
  const diffDays = Math.ceil((deadlineDate - today) / (1000 * 60 * 60 * 24))
  return diffDays <= 3
}

const handleTaskClick = (task) => {
  emit('task-click', task)
}

const handleStatusChange = (task, newStatus) => {
  emit('status-change', task.id, newStatus)
}
</script>

<style scoped>
.task-board {
  display: flex;
  gap: 16px;
  min-height: 400px;
}

.board-column {
  flex: 1;
  min-width: 280px;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.column-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.column-todo .column-header {
  background: #e8e9eb;
  color: #606266;
}

.column-doing .column-header {
  background: #e1f3ff;
  color: #409eff;
}

.column-done .column-header {
  background: #e1f9e8;
  color: #67c23a;
}

.column-title {
  font-size: 14px;
}

.column-content {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  max-height: 500px;
}

.task-card {
  background: white;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.task-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.task-card.task-high {
  border-left: 3px solid #f56c6c;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.more-icon {
  color: #909399;
  cursor: pointer;
  font-size: 16px;
}

.more-icon:hover {
  color: #409eff;
}

.task-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 12px;
  font-weight: 500;
  line-height: 1.4;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.task-owner,
.task-deadline {
  display: flex;
  align-items: center;
  gap: 4px;
}

.task-deadline.deadline-urgent {
  color: #f56c6c;
}

.badge :deep(.el-badge__content) {
  background-color: transparent;
  color: inherit;
  border: none;
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .board-container {
    flex-direction: column;
    gap: 12px;
  }

  .board-column {
    min-width: 100%;
    width: 100%;
  }

  .column-header {
    padding: 12px;
  }

  .column-title {
    font-size: 14px;
  }

  .task-card {
    padding: 12px;
    margin-bottom: 10px;
  }

  .task-name {
    font-size: 13px;
  }

  .task-meta {
    font-size: 11px;
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
  .task-card {
    min-height: 80px;
  }

  .task-card:active {
    background: #f5f7fa;
  }

  .el-button {
    min-height: 44px;
  }
}
</style>
