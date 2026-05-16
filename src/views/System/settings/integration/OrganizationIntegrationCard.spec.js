// Input: mocked organization integration operations composable
// Output: organization operations card groups manual controls into stable responsive rows
// Pos: src/views/System/settings/integration/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import OrganizationIntegrationCard from './OrganizationIntegrationCard.vue'
import { useOrganizationIntegrationOperations } from '../useOrganizationIntegrationOperations.js'

vi.mock('../useOrganizationIntegrationOperations.js', () => ({
  useOrganizationIntegrationOperations: vi.fn(),
}))

describe('OrganizationIntegrationCard', () => {
  beforeEach(() => {
    useOrganizationIntegrationOperations.mockReturnValue({
      loading: ref(false),
      syncing: ref(false),
      resyncingUser: ref(false),
      resyncingDepartment: ref(false),
      replayingDeadLetter: ref(false),
      status: ref({ enabled: true, eventSdkEnabled: false, pendingRetryCount: 0, deadLetterCount: 0 }),
      loaded: ref(true),
      errorText: ref(''),
      canOperate: ref(true),
      userId: ref(''),
      deptId: ref(''),
      deadLetterEventKey: ref(''),
      load: vi.fn(),
      startSyncRun: vi.fn(),
      resyncUser: vi.fn(),
      resyncDepartment: vi.fn(),
      replayDeadLetter: vi.fn(),
    })
  })

  it('groups the three manual operations as input and button pairs', () => {
    const wrapper = mount(OrganizationIntegrationCard, { global: { stubs, directives } })

    const operations = wrapper.findAll('.resync-operation')
    expect(operations).toHaveLength(3)
    expect(operations[0].text()).toContain('重同步用户')
    expect(operations[1].text()).toContain('重同步部门')
    expect(operations[2].text()).toContain('重放死信')
  })

  it('keeps desktop grouped columns and mobile single-column controls', () => {
    const source = readFileSync(
      resolve(process.cwd(), 'src/views/System/settings/integration/OrganizationIntegrationCard.vue'),
      'utf8'
    )

    expect(source).toContain('grid-template-columns: repeat(3, minmax(0, 1fr));')
    expect(source).toContain('.resync-operation {\n    grid-template-columns: 1fr;')
    expect(source).toContain('.resync-operation :deep(.el-button) {\n    width: 100%;')
  })
})

const stubs = {
  ElAlert: true,
  ElTag: { template: '<span><slot /></span>' },
  ElInput: { props: ['placeholder'], template: '<label><input :placeholder="placeholder" /></label>' },
  ElButton: { template: '<button><slot /></button>' },
}

const directives = {
  loading: {},
}
