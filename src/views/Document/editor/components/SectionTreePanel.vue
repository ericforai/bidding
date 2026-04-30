<template>
  <div class="left-panel">
    <el-card shadow="never" class="section-tree-card">
      <template #header>
        <div class="card-header">
          <span>章节目录</span>
          <el-button :icon="Plus" size="small" text @click="$emit('add-section')">添加章节</el-button>
        </div>
      </template>
      <el-tree
        ref="sectionTreeRef"
        :data="sectionTreeData"
        :props="treeProps"
        :highlight-current="true"
        :allow-drag="checkAllowDrag"
        :allow-drop="checkAllowDrop"
        node-key="id"
        draggable
        @node-click="(data) => $emit('node-click', data)"
        @node-drop="(...args) => $emit('node-drop', ...args)"
      >
        <template #default="{ node, data }">
          <div class="tree-node-content">
            <span class="node-icon">{{ getSectionIcon(data.type) }}</span>
            <span class="node-label">{{ node.label }}</span>
            <el-dropdown trigger="click" @command="(cmd) => $emit('node-command', cmd, data)">
              <el-icon :size="14" class="node-more-icon"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="add">添加子章节</el-dropdown-item>
                  <el-dropdown-item command="rename">重命名</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="sectionForm" label-width="80px">
        <el-form-item label="章节名称">
          <el-input v-model="sectionForm.name" placeholder="请输入章节名称" />
        </el-form-item>
        <el-form-item label="章节类型">
          <el-radio-group v-model="sectionForm.type">
            <el-radio value="section">章节</el-radio>
            <el-radio value="folder">文件夹</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="$emit('confirm-section')">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Plus, MoreFilled } from '@element-plus/icons-vue'

const props = defineProps({
  sectionTreeData: { type: Array, required: true },
  showDialog: { type: Boolean, default: false },
  dialogTitle: { type: String, default: '添加章节' },
  sectionForm: { type: Object, required: true },
  getSectionIcon: { type: Function, required: true },
  checkAllowDrag: { type: Function, required: true },
  checkAllowDrop: { type: Function, required: true }
})

const emit = defineEmits([
  'add-section',
  'node-click',
  'node-drop',
  'node-command',
  'confirm-section',
  'update:showDialog'
])

const treeProps = {
  children: 'children',
  label: 'name'
}

const sectionTreeRef = ref(null)
const dialogVisible = ref(props.showDialog)

watch(() => props.showDialog, (val) => {
  dialogVisible.value = val
})

watch(dialogVisible, (val) => {
  if (val !== props.showDialog) {
    emit('update:showDialog', val)
  }
})

defineExpose({ sectionTreeRef })
</script>

<style scoped>
.left-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.section-tree-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-tree-card :deep(.el-card__body) {
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.tree-node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding-right: 8px;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 6px;
  padding: 4px 8px;
}

.tree-node-content:hover {
  background: #f1f5f9;
}

.node-icon {
  flex-shrink: 0;
}

.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-more-icon {
  flex-shrink: 0;
  opacity: 0;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 4px;
  padding: 4px;
  cursor: pointer;
}

.tree-node-content:hover .node-more-icon {
  opacity: 1;
}

.node-more-icon:hover {
  background: #e5e7eb;
  color: #0369a1;
}

:deep(.el-radio.is-bordered) {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-radio.is-bordered:hover) {
  border-color: #94a3b8;
}

:deep(.el-radio.is-bordered.is-checked) {
  border-color: #0369a1;
  background: #f0f9ff;
}

@media (max-width: 1200px) {
  .left-panel {
    width: 100%;
    height: auto;
    max-height: 300px;
  }
}
</style>
