<template>
  <el-dialog :model-value="modelValue" title="资质借阅申请" width="500px" @close="$emit('update:modelValue', false)">
    <el-alert
      v-if="featurePlaceholder"
      type="warning"
      :closable="false"
      show-icon
      :title="featurePlaceholder.title"
      :description="featurePlaceholder.message"
      class="borrow-alert"
    />
    <el-form :model="form" label-width="100px">
      <el-form-item label="资质名称">
        <el-input :value="qualification?.name || '请先从列表中选择资质'" disabled />
      </el-form-item>
      <el-form-item label="借用人" required>
        <el-input v-model="form.borrower" placeholder="请输入借用人姓名" />
      </el-form-item>
      <el-form-item label="所属部门">
        <el-input v-model="form.department" placeholder="请输入所属部门" />
      </el-form-item>
      <el-form-item label="借阅用途" required>
        <el-select v-model="form.purpose" placeholder="请选择用途" style="width: 100%">
          <el-option label="投标使用" value="bidding" />
          <el-option label="资质审核" value="audit" />
          <el-option label="客户展示" value="presentation" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      <el-form-item label="预计归还">
        <el-date-picker
          v-model="form.returnDate"
          type="date"
          placeholder="选择日期"
          style="width: 100%"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注信息" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="$emit('confirm')">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({
  featurePlaceholder: {
    type: Object,
    default: null
  },
  form: {
    type: Object,
    required: true
  },
  modelValue: {
    type: Boolean,
    default: false
  },
  qualification: {
    type: Object,
    default: null
  }
})

defineEmits(['confirm', 'update:modelValue'])
</script>

<style scoped lang="scss">
.borrow-alert {
  margin-bottom: 16px;
}
</style>
