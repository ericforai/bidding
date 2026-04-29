<template>
  <el-form class="dynamic-workflow-form" :model="localValue" label-width="110px">
    <template v-for="field in visibleFields" :key="field.key">
      <el-form-item :label="field.label" :required="field.required">
        <el-input
          v-if="field.type === 'text' || field.type === 'qualification' || field.type === 'project'"
          v-model="localValue[field.key]"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :disabled="field.readonly"
        />
        <el-input
          v-else-if="field.type === 'textarea'"
          v-model="localValue[field.key]"
          type="textarea"
          :rows="field.rows || 3"
          :placeholder="field.placeholder || `请输入${field.label}`"
        />
        <el-date-picker
          v-else-if="field.type === 'date'"
          v-model="localValue[field.key]"
          type="date"
          value-format="YYYY-MM-DD"
          :placeholder="field.placeholder || `请选择${field.label}`"
          style="width: 100%"
        />
        <el-input-number
          v-else-if="field.type === 'number'"
          v-model="localValue[field.key]"
          :min="field.min"
          :max="field.max"
          style="width: 100%"
        />
        <el-select v-else-if="field.type === 'select'" v-model="localValue[field.key]" style="width: 100%">
          <el-option v-for="option in field.options || []" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-input v-else v-model="localValue[field.key]" :placeholder="field.placeholder || `请输入${field.label}`" />
      </el-form-item>
    </template>
    <el-alert v-if="validationMessage" type="warning" :closable="false" :title="validationMessage" />
  </el-form>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'

const props = defineProps({
  schema: {
    type: Object,
    required: true
  },
  modelValue: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['submit', 'update:modelValue'])

const localValue = reactive({ ...props.modelValue })
const validationMessage = computed(() => '')

const visibleFields = computed(() => (props.schema?.fields || []).filter((field) => !field.hidden))

watch(
  () => props.modelValue,
  (value) => {
    Object.keys(localValue).forEach((key) => delete localValue[key])
    Object.assign(localValue, value || {})
  },
  { deep: true }
)

watch(localValue, () => emit('update:modelValue', { ...localValue }), { deep: true })

function validate() {
  const missing = visibleFields.value.find((field) => field.required && !String(localValue[field.key] ?? '').trim())
  return missing ? `请填写${missing.label}` : ''
}

function submit() {
  const message = validate()
  if (message) {
    return { valid: false, message }
  }
  emit('submit', { ...localValue })
  return { valid: true, data: { ...localValue } }
}

defineExpose({ submit, validate })
</script>

<style scoped>
.dynamic-workflow-form {
  width: 100%;
}
</style>
