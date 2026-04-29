import { describe, expect, it } from 'vitest'
import { roleMenuOptions, sidebarMenuConfig } from './sidebar-menu'

describe('sidebar-menu config', () => {
  it('keeps role menu options aligned with visible top-level sidebar menus', () => {
    const visibleMenus = sidebarMenuConfig.filter((menu) => menu.name !== 'CustomerOpportunityCenter')
    const topLevelOptions = roleMenuOptions.slice(0, visibleMenus.length)
    const workbenchOptions = roleMenuOptions.slice(visibleMenus.length)

    expect(topLevelOptions.map((item) => item.label)).toEqual([
      '工作台',
      '标讯中心',
      '投标项目',
      '知识库',
      '资源管理',
      'AI 智能中心',
      '数据分析',
      '系统设置'
    ])
    expect(workbenchOptions.map((item) => item.value)).toEqual([
      'dashboard.quickStart',
      'dashboard:view_tender_list',
      'dashboard:view_project_list',
      'dashboard:view_technical_task',
      'dashboard:view_review_list',
      'dashboard:view_team_task',
      'dashboard:view_global_projects'
    ])
  })

  it('uses the same primary permission key as the top-level sidebar menu', () => {
    const visibleMenus = sidebarMenuConfig.filter((menu) => menu.name !== 'CustomerOpportunityCenter')
    const topLevelOptions = roleMenuOptions.slice(0, visibleMenus.length)

    expect(topLevelOptions).toEqual(
      visibleMenus.map((menu) => ({
        value: menu.meta.permissionKeys[0],
        label: menu.meta.title
      }))
    )
  })
})
