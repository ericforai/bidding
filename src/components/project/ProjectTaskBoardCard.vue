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
            class="header-action header-action--tender"
            :icon="DocumentChecked"
            data-test="tender-breakdown-button"
            @click="$emit('tender-breakdown')"
          >
            解析招标文件
          </el-button>
          <el-button
            v-if="canManageProjectTasks"
            link
            type="warning"
            class="header-action header-action--score"
            :icon="DocumentChecked"
            data-test="score-draft-button"
            @click="$emit('score-draft-decompose')"
          >
            评分标准拆解
          </el-button>
          <el-button
            v-if="canManageProjectTasks"
            link
            type="primary"
            class="header-action header-action--add"
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
  'tender-breakdown',
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

.actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-action.el-button.is-link {
  min-height: 32px;
  padding: 0 10px;
  border-radius: 7px;
  font-weight: 600;
  transition: background-color 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;
}

.header-action--tender.el-button.is-link {
  --el-button-text-color: #23785d;
  --el-button-hover-link-text-color: #14684d;
  --el-button-active-color: #10563f;
}

.header-action--score.el-button.is-link {
  --el-button-text-color: #98620a;
  --el-button-hover-link-text-color: #805005;
  --el-button-active-color: #6c4200;
}

.header-action--add.el-button.is-link {
  --el-button-text-color: #2f6fba;
  --el-button-hover-link-text-color: #225b9b;
  --el-button-active-color: #1d4c82;
}

.header-action.el-button.is-link:hover,
.header-action.el-button.is-link:focus {
  background: #f4f8f6;
}

.header-action--score.el-button.is-link:hover,
.header-action--score.el-button.is-link:focus {
  background: #fff7e8;
}

.header-action--add.el-button.is-link:hover,
.header-action--add.el-button.is-link:focus {
  background: #eef5ff;
}

.header-action.el-button.is-link:focus-visible {
  box-shadow: 0 0 0 3px rgba(35, 120, 93, 0.16);
  outline: none;
}

.header-action--score.el-button.is-link:focus-visible {
  box-shadow: 0 0 0 3px rgba(152, 98, 10, 0.16);
}

.header-action--add.el-button.is-link:focus-visible {
  box-shadow: 0 0 0 3px rgba(47, 111, 186, 0.16);
}
</style>
