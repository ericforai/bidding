// Input: Settings page shell with mocked user role and settings composables
// Output: operation log tab admin visibility coverage
// Pos: src/views/System/ - page tests

import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

const state = vi.hoisted(() => ({
  role: 'admin',
  query: {},
  loadOrganizationSettings: vi.fn(),
  loadAiSettings: vi.fn(),
  loadBidMatchScoringSettings: vi.fn(),
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    get userRole() {
      return state.role
    },
  }),
}))

vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal()),
  useRoute: () => ({ query: state.query }),
}))

vi.mock('./settings/useOrganizationSettings', async () => {
  const { computed, ref } = await vi.importActual('vue')
  return {
    useOrganizationSettings: () => ({
      loading: ref(false),
      deptTree: ref([{ deptCode: 'sales', deptName: '销售部' }]),
      deptOptions: computed(() => []),
      users: ref([]),
      roles: ref([]),
      enabledRoles: computed(() => []),
      load: state.loadOrganizationSettings,
      saveDepartments: vi.fn(),
      saveUserOrganization: vi.fn(),
      saveRole: vi.fn(),
      toggleRole: vi.fn(),
      resetRole: vi.fn(),
    }),
  }
})

vi.mock('./settings/useAiModelSettings', async () => {
  const { ref } = await vi.importActual('vue')
  return {
    useAiModelSettings: () => ({
      loading: ref(false),
      saving: ref(false),
      testingProvider: ref(''),
      systemConfig: ref({}),
      aiModelConfig: ref({ providers: [] }),
      load: state.loadAiSettings,
      save: vi.fn(),
      testProvider: vi.fn(),
    }),
  }
})

vi.mock('./settings/useBidMatchScoringSettings', async () => {
  const { computed, ref } = await vi.importActual('vue')
  return {
    EVIDENCE_KEY_OPTIONS: [],
    RULE_TYPE_OPTIONS: [],
    useBidMatchScoringSettings: () => ({
      loading: ref(false),
      saving: ref(false),
      activating: ref(false),
      currentModel: ref({ dimensions: [] }),
      weightValidation: computed(() => ({ valid: true, message: '' })),
      enabledDimensionCount: computed(() => 0),
      load: state.loadBidMatchScoringSettings,
      save: vi.fn(),
      activateCurrentModel: vi.fn(),
      addDimension: vi.fn(),
      removeDimension: vi.fn(),
      addRule: vi.fn(),
      removeRule: vi.fn(),
    }),
  }
})

import Settings from './Settings.vue'

const childStub = { template: '<div />' }
const stubs = {
  ElAlert: childStub,
  ElButton: {
    props: ['loading'],
    emits: ['click'],
    template: '<button @click="$emit(\'click\')"><slot /></button>',
  },
  ElTabs: {
    props: ['modelValue'],
    template: '<div class="settings-tabs" :data-active="modelValue"><slot /></div>',
  },
  ElTabPane: {
    props: ['label', 'name'],
    template: '<section class="tab-pane" :data-name="name"><span>{{ label }}</span><slot /></section>',
  },
  DepartmentTreePanel: childStub,
  RoleManagementPanel: childStub,
  InterfacePermissionMatrixPanel: childStub,
  UserOrganizationPanel: childStub,
  AiModelSettingsPanel: childStub,
  BidMatchScoringSettingsPanel: childStub,
  SystemIntegrationPanel: childStub,
  AuditLogPanel: { template: '<div>关键操作记录</div>' },
}

function mountSettings({ role = 'admin', query = {} } = {}) {
  state.role = role
  state.query = query
  vi.clearAllMocks()

  return mount(Settings, {
    global: {
      stubs,
      directives: {
        loading: {},
      },
    },
  })
}

describe('Settings', () => {
  it('shows operation log tab only for administrators', () => {
    const adminWrapper = mountSettings({ role: 'admin' })
    expect(adminWrapper.text()).toContain('操作日志')
    expect(adminWrapper.text()).toContain('关键操作记录')

    const managerWrapper = mountSettings({ role: 'manager' })
    expect(managerWrapper.text()).not.toContain('操作日志')
    expect(managerWrapper.text()).not.toContain('关键操作记录')
  })

  it('does not keep operation log active when a non-admin requests the audit tab', () => {
    const wrapper = mountSettings({ role: 'staff', query: { tab: 'audit' } })

    expect(wrapper.find('.settings-tabs').attributes('data-active')).toBe('departments')
  })
})
