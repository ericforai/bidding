import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ProjectTaskBoardCard from './ProjectTaskBoardCard.vue'

describe('ProjectTaskBoardCard', () => {
  it('exposes independent tender breakdown entry separately from score draft decomposition', async () => {
    const wrapper = mount(ProjectTaskBoardCard, {
      props: {
        canManageProjectTasks: true,
        tasks: [],
        projectId: 12,
      },
      global: {
        stubs: {
          ElCard: {
            template: '<section><slot name="header" /><slot /></section>',
          },
          ElButton: {
            template: '<button v-bind="$attrs" type="button"><slot /></button>',
          },
          ElIcon: {
            template: '<span><slot /></span>',
          },
          TaskBoard: true,
        },
      },
    })

    await wrapper.find('[data-test="tender-breakdown-button"]').trigger('click')
    await wrapper.find('[data-test="score-draft-button"]').trigger('click')

    expect(wrapper.emitted('tender-breakdown')).toHaveLength(1)
    expect(wrapper.emitted('score-draft-decompose')).toHaveLength(1)
  })
})
