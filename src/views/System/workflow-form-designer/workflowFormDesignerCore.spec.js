import { describe, expect, it } from 'vitest'
import { buildDefaultTemplate, buildMappingFromFields, createField, moveField, removeField } from './workflowFormDesignerCore.js'

describe('workflowFormDesignerCore', () => {
  it('creates productized default template draft', () => {
    const draft = buildDefaultTemplate()

    expect(draft.businessType).toBe('GENERAL_WORKFLOW')
    expect(draft.enabled).toBe(true)
    expect(draft.schema.fields[0]).toMatchObject({ type: 'text', required: true })
  })

  it('supports field add remove and deterministic ordering', () => {
    const fields = [createField('title', '标题', 'text'), createField('amount', '金额', 'number')]

    expect(moveField(fields, 1, -1).map((field) => field.key)).toEqual(['amount', 'title'])
    expect(removeField(fields, 'title').map((field) => field.key)).toEqual(['amount'])
  })

  it('builds safe OA mapping from configured fields', () => {
    const mapping = buildMappingFromFields('WF_SEAL', [
      createField('title', '标题', 'text'),
      createField('projectId', '项目', 'project')
    ])

    expect(mapping.workflowCode).toBe('WF_SEAL')
    expect(mapping.mainFields).toContainEqual(expect.objectContaining({
      source: 'formData.title',
      target: 'field_title'
    }))
  })
})
