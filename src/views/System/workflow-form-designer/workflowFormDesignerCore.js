// Input: workflow form designer state
// Output: deterministic field/template helpers for admin configuration UI
// Pos: src/views/System/workflow-form-designer/ - Flow form designer pure helpers

export const FIELD_TYPES = [
  { label: '文本', value: 'text' },
  { label: '多行文本', value: 'textarea' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
  { label: '下拉', value: 'select' },
  { label: '人员', value: 'person' },
  { label: '项目', value: 'project' },
  { label: '附件', value: 'attachment' },
  { label: '说明文本', value: 'info' }
]

export function createField(key = 'field1', label = '字段', type = 'text') {
  const field = { key, label, type, required: type !== 'info' }
  if (type === 'select') {
    field.options = [{ label: '选项一', value: 'option_1' }]
  }
  if (type === 'info') {
    field.content = '请填写说明内容'
  }
  return field
}

export function buildDefaultTemplate() {
  return {
    templateCode: 'GENERAL_APPLY',
    name: '通用申请',
    businessType: 'GENERAL_WORKFLOW',
    enabled: true,
    schema: { fields: [createField('title', '申请标题', 'text')] }
  }
}

export function removeField(fields, key) {
  return fields.filter((field) => field.key !== key)
}

export function moveField(fields, index, direction) {
  const next = [...fields]
  const target = index + direction
  if (target < 0 || target >= next.length) return next
  const [field] = next.splice(index, 1)
  next.splice(target, 0, field)
  return next
}

export function buildMappingFromFields(workflowCode, fields) {
  return {
    workflowCode,
    mainFields: fields
      .filter((field) => field.type !== 'info')
      .map((field) => ({
        source: `formData.${field.key}`,
        target: `field_${field.key}`,
        targetName: field.label,
        type: field.type === 'date' ? 'date' : 'string',
        required: Boolean(field.required)
      }))
  }
}
