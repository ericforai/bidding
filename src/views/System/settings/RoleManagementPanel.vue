<template>
  <el-card shadow="never" class="org-card">
    <template #header>
      <div class="panel-header">
        <div>
          <h3>角色维护</h3>
          <p>角色决定菜单权限、默认数据范围，以及任务分配候选人的职责过滤。</p>
        </div>
        <el-button type="primary" @click="openCreate">新增角色</el-button>
      </div>
    </template>

    <el-empty v-if="roles.length === 0" description="暂无角色，请新增角色" />
    <el-table v-else :data="roles" border stripe>
      <el-table-column prop="name" label="角色名称" min-width="130" />
      <el-table-column prop="code" label="角色代码" min-width="120" />
      <el-table-column prop="dataScope" label="数据范围" width="130">
        <template #default="{ row }">{{ scopeText(row.dataScope) }}</template>
      </el-table-column>
      <el-table-column prop="userCount" label="用户数" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.enabled ? 'warning' : 'success'" @click="toggle(row)">
            {{ row.enabled ? '停用' : '启用' }}
          </el-button>
          <el-button v-if="row.isSystem" link type="info" @click="reset(row)">恢复默认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="角色代码" required>
          <el-input v-model="form.code" :disabled="Boolean(form.id)" placeholder="英文/数字/下划线" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="form.name" placeholder="如 投标经理" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="默认数据范围">
          <el-radio-group v-model="form.dataScope">
            <el-radio value="all">全部数据</el-radio>
            <el-radio value="dept">本部门</el-radio>
            <el-radio value="deptAndSub">本部门及下级</el-radio>
            <el-radio value="self">仅本人</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="部门白名单">
          <el-select v-model="form.allowedDepts" multiple clearable placeholder="可选" style="width: 100%">
            <el-option v-for="dept in deptOptions" :key="dept.value" :label="dept.label" :value="dept.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-checkbox-group v-model="form.menuPermissions">
            <el-checkbox v-for="item in menuOptions" :key="item.value" :value="item.value">{{ item.label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  roles: { type: Array, default: () => [] },
  deptOptions: { type: Array, default: () => [] },
  saveHandler: { type: Function, required: true },
  toggleHandler: { type: Function, required: true },
  resetHandler: { type: Function, required: true }
})

const dialogVisible = ref(false)
const saving = ref(false)
const form = ref(emptyForm())

const menuOptions = [
  { value: 'dashboard', label: '工作台' },
  { value: 'bidding', label: '标讯中心' },
  { value: 'project', label: '投标项目' },
  { value: 'knowledge', label: '知识库' },
  { value: 'resource', label: '资源管理' },
  { value: 'analytics', label: '数据分析' },
  { value: 'settings', label: '系统设置' }
]

function emptyForm() {
  return {
    id: null,
    code: '',
    name: '',
    description: '',
    dataScope: 'self',
    enabled: true,
    menuPermissions: [],
    allowedProjects: [],
    allowedDepts: []
  }
}

const scopeText = (scope) => ({
  all: '全部数据',
  dept: '本部门',
  deptAndSub: '本部门及下级',
  self: '仅本人'
}[scope] || scope)

const openCreate = () => {
  form.value = emptyForm()
  dialogVisible.value = true
}

const openEdit = (role) => {
  form.value = {
    ...emptyForm(),
    ...role,
    menuPermissions: Array.isArray(role.menuPermissions) ? [...role.menuPermissions] : [],
    allowedDepts: Array.isArray(role.allowedDepts) ? [...role.allowedDepts] : []
  }
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.value.code.trim()) return ElMessage.warning('请填写角色代码')
  if (!form.value.name.trim()) return ElMessage.warning('请填写角色名称')
  saving.value = true
  try {
    await props.saveHandler({ ...form.value })
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(error?.message || '保存角色失败')
  } finally {
    saving.value = false
  }
}

const toggle = (role) => props.toggleHandler(role)
const reset = (role) => props.resetHandler(role)
</script>

<style scoped>
.org-card {
  border-radius: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.panel-header h3 {
  margin: 0;
}

.panel-header p {
  margin: 6px 0 0;
  color: #667085;
}
</style>
