import { describe, it, expect, vi } from 'vitest'
import { getTimeGreeting } from '@/views/Dashboard/workbench-utils.js'

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn()
}))

describe('Workbench role-based visibility', () => {
  const roleConditions = [
    { role: 'staff', expectedSections: ['客户跟进'], hiddenSections: ['我的项目'] },
    { role: 'manager', expectedSections: ['我的项目'], hiddenSections: ['客户跟进'] },
    { role: 'admin', expectedSections: [], hiddenSections: ['客户跟进', '我的项目'] }
  ]

  it('uses roleCode not displayName for section visibility', () => {
    const sourceCode = `
      v-if="currentUserRole === 'staff'"
      v-if="currentUserRole === 'manager'"
      v-if="currentUserRole === 'admin'"
    `
    expect(sourceCode).not.toContain("currentUserName === '小王'")
    expect(sourceCode).not.toContain("currentUserName === '张经理'")
    expect(sourceCode).not.toContain("currentUserName === '李工'")
  })

  it.each(roleConditions)('role "$role" maps to correct section visibility', ({ role, expectedSections, hiddenSections }) => {
    const roleMap = {
      staff: ['客户跟进'],
      manager: ['我的项目'],
      admin: []
    }
    expect(roleMap[role]).toEqual(expectedSections)
    hiddenSections.forEach(section => {
      expect(roleMap[role]).not.toContain(section)
    })
  })

  it('李工 section has been removed (no backend user exists)', () => {
    const fs = require('fs')
    const source = fs.readFileSync('src/views/Dashboard/Workbench.vue', 'utf-8')
    expect(source).not.toContain("currentUserName === '李工'")
    expect(source).not.toContain("currentUserName === '小王'")
    expect(source).not.toContain("currentUserName === '张经理'")
    expect(source).toContain("currentUserRole === 'staff'")
    expect(source).toContain("currentUserRole === 'manager'")
  })
})

describe('getTimeGreeting', () => {
  it('returns "上午好" for morning hours (8)', () => {
    expect(getTimeGreeting(8)).toBe('上午好')
  })

  it('returns "下午好" for afternoon hours (14)', () => {
    expect(getTimeGreeting(14)).toBe('下午好')
  })

  it('returns "晚上好" for evening hours (20)', () => {
    expect(getTimeGreeting(20)).toBe('晚上好')
  })

  it('returns "晚上好" for late night hours (2)', () => {
    expect(getTimeGreeting(2)).toBe('晚上好')
  })

  describe('edge cases', () => {
    it('returns "上午好" for hour 5 (start of morning)', () => {
      expect(getTimeGreeting(5)).toBe('上午好')
    })

    it('returns "上午好" for hour 11 (end of morning)', () => {
      expect(getTimeGreeting(11)).toBe('上午好')
    })

    it('returns "下午好" for hour 12 (start of afternoon)', () => {
      expect(getTimeGreeting(12)).toBe('下午好')
    })

    it('returns "下午好" for hour 17 (end of afternoon)', () => {
      expect(getTimeGreeting(17)).toBe('下午好')
    })

    it('returns "晚上好" for hour 18 (start of evening)', () => {
      expect(getTimeGreeting(18)).toBe('晚上好')
    })
  })
})
