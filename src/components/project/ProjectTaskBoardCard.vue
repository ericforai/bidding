<template>
  <el-card class="task-card">
    <template #header>
      <div class="card-title">
        <div class="title-main">
          <el-icon><List /></el-icon>
          <span>任务看板</span>
        </div>
        <div class="actions">
          <el-button
            v-if="canManageProjectTasks"
            link
            type="success"
            :icon="DocumentChecked"
            @click="$emit('score-draft-decompose')"
          >
            评分标准拆解
          </el-button>
          <el-button
            v-if="canManageProjectTasks"
            link
            type="primary"
            :icon="Plus"
            @click="$emit('add-task')"
          >
            添加任务
          </el-button>
          <el-button
            v-if="isDemoMode"
            link
            type="warning"
            @click="$emit('reset-tasks')"
          >
            重置任务
          </el-button>
        </div>
      </div>
    </template>

    <TaskBoard
      :tasks="tasks"
      :project-id="normalizedProjectId"
      :can-generate="!tasks || tasks.length === 0"
      @task-click="$emit('task-click', $event)"
      @status-change="(...args) => $emit('status-change', ...args)"
      @generate-tasks="$emit('generate-tasks')"
      @add-deliverable="$emit('add-deliverable', $event)"
      @remove-deliverable="$emit('remove-deliverable', $event)"
      @submit-to-document="$emit('submit-to-document', $event)"
    />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { DocumentChecked, List, Plus } from '@element-plus/icons-vue'
import TaskBoard from '@/components/common/TaskBoard.vue'

defineEmits([
  'add-task',
  'reset-tasks',
  'task-click',
  'status-change',
  'score-draft-decompose',
  'generate-tasks',
  'add-deliverable',
  'remove-deliverable',
  'submit-to-document',
])

const props = defineProps({
  tasks: {
    type: Array,
    default: () => [],
  },
  projectId: {
    type: [String, Number],
    default: '',
  },
  canManageProjectTasks: {
    type: Boolean,
    default: false,
  },
  isDemoMode: {
    type: Boolean,
    default: false,
  },
})

const normalizedProjectId = computed(() => String(props.projectId ?? ''))
</script>

<style scoped>
.task-card {
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 500;
}

.title-main,
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
