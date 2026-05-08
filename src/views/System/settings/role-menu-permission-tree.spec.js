import { roleMenuGroups } from '@/config/sidebar-menu'
import {
  isRolePermissionGroupIndeterminate,
  normalizeRoleMenuPermissions,
  setRolePermissionChild,
  setRolePermissionGroup
} from './role-menu-permission-tree'

const dashboardGroup = roleMenuGroups.find((group) => group.value === 'dashboard')
const quickStart = dashboardGroup.children.find((child) => child.value === 'dashboard.quickStart')

describe('role-menu-permission-tree', () => {
  it('adds the top-level menu when a secondary menu is selected', () => {
    expect(normalizeRoleMenuPermissions(['dashboard.quickStart'])).toEqual([
      'dashboard',
      'dashboard.quickStart'
    ])

    expect(setRolePermissionChild([], dashboardGroup, quickStart, true)).toEqual([
      'dashboard',
      'dashboard.quickStart'
    ])
  })

  it('lets the top-level menu select or clear its secondary menus', () => {
    expect(setRolePermissionGroup([], dashboardGroup, true)).toEqual([
      'dashboard',
      'dashboard.quickStart',
      'dashboard:view_tender_list',
      'dashboard:view_project_list',
      'dashboard:view_technical_task',
      'dashboard:view_review_list',
      'dashboard:view_team_task',
      'dashboard:view_global_projects'
    ])

    expect(setRolePermissionGroup(['dashboard', 'dashboard.quickStart'], dashboardGroup, false)).toEqual([])
  })

  it('shows partial state when only some secondary menus are selected', () => {
    expect(isRolePermissionGroupIndeterminate(['dashboard', 'dashboard.quickStart'], dashboardGroup)).toBe(true)
    expect(isRolePermissionGroupIndeterminate(setRolePermissionGroup([], dashboardGroup, true), dashboardGroup))
      .toBe(false)
  })
})
