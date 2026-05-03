<template>
  <el-form class="dynamic-form-renderer" :model="localValue" label-width="110px">
    <template v-for="field in visibleFields" :key="field.key">
      <el-form-item :label="field.label" :required="field.required">
        <el-input
          v-if="['text', 'qualification', 'project', 'person'].includes(field.type)"
          v-model="localValue[field.key]"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :disabled="disabled || field.readonly"
        />
        <el-input
          v-else-if="field.type === 'textarea'"
          v-model="localValue[field.key]"
          type="textarea"
          :rows="field.rows || 3"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :disabled="disabled"
        />
        <el-date-picker
          v-else-if="field.type === 'date'"
          v-model="localValue[field.key]"
          type="date"
          value-format="YYYY-MM-DD"
          :placeholder="field.placeholder || `请选择${field.label}`"
          :disabled="disabled"
          style="width: 100%"
        />
        <el-input-number
          v-else-if="field.type === 'number'"
          v-model="localValue[field.key]"
          :min="field.min"
          :max="field.max"
          :disabled="disabled"
          style="width: 100%"
        />
        <el-select
          v-else-if="field.type === 'select'"
          v-model="localValue[field.key]"
          :disabled="disabled"
          style="width: 100%"
        >
          <el-option v-for="option in field.options || []" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-upload
          v-else-if="field.type === 'attachment'"
          :auto-upload="false"
          :file-list="getAttachmentFileList(field)"
          :http-request="(request) => uploadAttachment(field, request)"
          :limit="field.limit"
          :accept="field.accept"
          :disabled="disabled"
          @change="(file) => handleAttachmentChange(field, file)"
          @remove="(file) => handleAttachmentRemove(field, file)"
        >
          <el-button type="primary" plain :disabled="disabled">选择文件</el-button>
        </el-upload>
        <el-alert v-else-if="field.type === 'info'" type="info" :closable="false" :title="field.content || field.label" />
        <el-input
          v-else
          v-model="localValue[field.key]"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :disabled="disabled"
        />
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup>
import { computed, nextTick, reactive, watch } from 'vue'

/**
 * @callback UploadFn
 * @param {Object} field         The field descriptor
 * @param {Object} request       Element Plus http-request hook param ({ file, onSuccess, onError })
 * @returns {Promise<{ fileName:string, fileUrl:string, storagePath:string, contentType?:string, size?:number }>}
 *          Must return a normalized attachment object. The renderer keys dedupe/remove on
 *          (storagePath ?? fileUrl ?? fileName).
 */

const props = defineProps({
  fields: { type: Array, required: true },
  modelValue: { type: Object, default: () => ({}) },
  // See UploadFn typedef above
  uploadFn: { type: Function, default: null },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['submit', 'update:modelValue'])

const localValue = reactive({ ...props.modelValue })
const visibleFields = computed(() => (props.fields || []).filter((field) => !field.hidden))
let syncingFromParent = false

function hasSameEntries(left = {}, right = {}) {
  const leftKeys = Object.keys(left)
  const rightKeys = Object.keys(right)
  return leftKeys.length === rightKeys.length &&
    leftKeys.every((key) => Object.is(left[key], right[key]))
}

watch(
  () => props.modelValue,
  (value) => {
    if (hasSameEntries(localValue, value || {})) return
    syncingFromParent = true
    Object.keys(localValue).forEach((key) => delete localValue[key])
    Object.assign(localValue, value || {})
    nextTick(() => {
      syncingFromParent = false
    })
  },
  { deep: true }
)

watch(localValue, () => {
  if (syncingFromParent) return
  emit('update:modelValue', { ...localValue })
}, { deep: true })

function getAttachmentValue(field) {
  const value = localValue[field.key]
  return Array.isArray(value) ? value : []
}

function getAttachmentFileList(field) {
  return getAttachmentValue(field).map((file, index) => ({
    uid: file.storagePath || file.fileUrl || `${field.key}-${index}`,
    name: file.fileName,
    url: file.fileUrl,
    status: 'success',
    response: file
  }))
}

async function uploadAttachment(field, request) {
  if (!props.uploadFn) {
    const err = new Error('uploadFn prop is required for attachment fields')
    request?.onError?.(err)
    throw err
  }
  const file = request?.file?.raw || request?.file
  if (!file) return null
  const attachment = await props.uploadFn(field, request)
  if (attachment && !attachment.fileName && !attachment.fileUrl && !attachment.storagePath) {
    // eslint-disable-next-line no-console
    console.warn(
      '[DynamicFormRenderer] uploadFn returned an attachment without fileName/fileUrl/storagePath; ' +
      'dedupe and remove handlers key on these fields. See UploadFn typedef.'
    )
  }
  localValue[field.key] = [...getAttachmentValue(field), attachment]
  request?.onSuccess?.(attachment)
  return attachment
}

async function handleAttachmentChange(field, file) {
  const rawFile = file?.raw
  if (!rawFile || file?.status === 'success') return
  await uploadAttachment(field, { file: rawFile })
}

function handleAttachmentRemove(field, file = {}) {
  const name = file.name || file.fileName
  const url = file.url || file.fileUrl
  const storagePath = file.response?.storagePath || file.storagePath
  localValue[field.key] = getAttachmentValue(field).filter((item) => (
    (storagePath && item.storagePath !== storagePath) ||
    (!storagePath && url && item.fileUrl !== url) ||
    (!storagePath && !url && name && item.fileName !== name)
  ))
}

function isEmptyValue(value) {
  if (Array.isArray(value)) return value.length === 0
  return value === null || value === undefined || String(value).trim() === ''
}

function validate() {
  const missing = visibleFields.value.find((field) => field.required && field.type !== 'info' && isEmptyValue(localValue[field.key]))
  return missing ? `请填写${missing.label}` : ''
}

function submit() {
  const message = validate()
  if (message) return { valid: false, message }
  emit('submit', { ...localValue })
  return { valid: true, data: { ...localValue } }
}

defineExpose({ submit, validate, uploadAttachment })
</script>

<style scoped>
.dynamic-form-renderer {
  width: 100%;
}
</style>
