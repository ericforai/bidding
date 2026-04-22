<template>
  <el-dialog
    :model-value="modelValue"
    title="指派标讯"
    width="520px"
    @update:model-value="$emit('update:modelValue', $event)"
    @close="$emit('reset')"
  >
    <el-form :model="form" label-width="100px">
      <el-form-item label="标讯标题">
        <el-text>{{ form.tenderTitle }}</el-text>
      </el-form-item>
      <el-form-item label="指派给" required>
        <el-select v-model="form.assignee" filterable placeholder="选择人员" class="full-width" :loading="loadingCandidates">
          <el-option
            v-for="candidate in candidates"
            :key="candidate.id"
            :label="candidate.name"
            :value="candidate.id"
          >
            {{ candidate.name }} · {{ candidate.departmentName }}
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="优先级">
        <el-radio-group v-model="form.priority">
          <el-radio value="high">高</el-radio>
          <el-radio value="medium">中</el-radio>
          <el-radio value="low">低</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="填写指派说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="$emit('submit')">确认指派</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  form: { type: Object, required: true },
  candidates: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  loadingCandidates: { type: Boolean, default: false },
})

defineEmits(['update:modelValue', 'reset', 'submit'])
</script>
