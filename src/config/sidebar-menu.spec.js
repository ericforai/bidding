import { describe, expect, it } from 'vitest'
import { roleMenuOptions, sidebarMenuConfig } from './sidebar-menu'

describe('sidebar-menu config', () => {
  it('keeps role menu options aligned with visible top-level sidebar menus', () => {
    expect(roleMenuOptions.map((item) => item.label)).toEqual([
      '工作台',
      '标讯中心',
      '投标项目',
      '知识库',
      '资源管理',
      'AI 智能中心',
      '数据分析',
      '系统设置'
    ])
    expect(roleMenuOptions.map((item) => item.value)).not.toContain('dashboard.quickStart')
  })

  it('uses the same primary permission key as the top-level sidebar menu', () => {
    const visibleMenus = sidebarMenuConfig.filter((menu) => menu.name !== 'CustomerOpportunityCenter')

    expect(roleMenuOptions).toEqual(
      visibleMenus.map((menu) => ({
        value: menu.meta.permissionKeys[0],
        label: menu.meta.title
      }))
    )
  })
})
