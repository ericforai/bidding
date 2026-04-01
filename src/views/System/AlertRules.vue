<template>
  <div class="alert-rules-container">
    <div class="page-header">
      <h2>告警规则</h2>
      <el-button type="primary" @click="showDialog('create')">新建规则</el-button>
    </div>

    <el-table :data="rules" v-loading="loading" stripe>
      <el-table-column prop="name" label="规则名称" />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }">
          <el-tag>{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="100">
        <template #default="{ row }">
          <el-tag :type="priorityType(row.priority)">{{ row.priority }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="toggleRule(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDialog('edit', row)">编辑</el-button>
          <el-button link type="danger" @click="deleteRule(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="规则名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option label="系统" value="SYSTEM" />
            <el-option label="项目" value="PROJECT" />
            <el-option label="投标" value="BIDDING" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { alertRulesApi } from '@/api/modules/alerts.js'

const loading = ref(false)
const rules = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新建规则')
const form = reactive({ id: null, name: '', type: 'SYSTEM', priority: 'MEDIUM', description: '' })
const isEdit = ref(false)

onMounted(() => { loadRules() })

async function loadRules() {
  loading.value = true
  try {
    const res = await alertRulesApi.getList()
    rules.value = res.data || []
  } catch (e) {
    ElMessage.error('加载告警规则失败')
  } finally {
    loading.value = false
  }
}

function showDialog(type, row = null) {
  if (type === 'create') {
    dialogTitle.value = '新建规则'
    isEdit.value = false
    Object.assign(form, { id: null, name: '', type: 'SYSTEM', priority: 'MEDIUM', description: '' })
  } else {
    dialogTitle.value = '编辑规则'
    isEdit.value = true
    Object.assign(form, { ...row })
  }
  dialogVisible.value = true
}

async function saveRule() {
  try {
    if (isEdit.value) {
      await alertRulesApi.update(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await alertRulesApi.create(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRules()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function toggleRule(row) {
  try {
    await alertRulesApi.toggle(row.id)
    ElMessage.success('状态已更新')
  } catch (e) {
    row.enabled = !row.enabled
    ElMessage.error('更新失败')
  }
}

async function deleteRule(row) {
  try {
    await ElMessageBox.confirm('确认删除该规则?', '警告', { type: 'warning' })
    await alertRulesApi.delete(row.id)
    ElMessage.success('删除成功')
    loadRules()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function priorityType(p) {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[p] || 'info'
}
</script>

<style scoped>
.alert-rules-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
</style>
