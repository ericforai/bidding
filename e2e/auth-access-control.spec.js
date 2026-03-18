import { test, expect } from '@playwright/test'

const mockUsers = {
  admin: { id: 'U003', name: '李总', role: 'admin', dept: '管理层', avatar: '' },
  manager: { id: 'U001', name: '小王', role: 'manager', dept: '华南销售部', avatar: '' },
  staff: { id: 'U004', name: '李工', role: 'staff', dept: '技术部', avatar: '' },
}

async function setSession(page, user) {
  await page.addInitScript(({ sessionUser }) => {
    sessionStorage.setItem('token', `playwright-token-${sessionUser.id}`)
    sessionStorage.setItem('user', JSON.stringify(sessionUser))
  }, { sessionUser: user })
}

test.describe('auth access control', () => {
  test('redirects unauthenticated visitors to login', async ({ page }) => {
    await page.goto('/settings')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible()
  })

  test('blocks manager from admin-only settings route', async ({ page }) => {
    await setSession(page, mockUsers.manager)

    await page.goto('/settings')

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByText('工作台').first()).toBeVisible()
    await page.locator('.user-info').click()
    await expect(page.getByRole('menuitem', { name: /系统设置/ })).toHaveCount(0)
  })

  test('blocks staff from analytics dashboard route', async ({ page }) => {
    await setSession(page, mockUsers.staff)

    await page.goto('/analytics/dashboard')

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByText('工作台').first()).toBeVisible()
  })

  test('allows admin to reach settings and keeps settings entry visible', async ({ page }) => {
    await setSession(page, mockUsers.admin)

    await page.goto('/settings')

    await expect(page).toHaveURL(/\/settings$/)
    await expect(page.getByText('系统设置').first()).toBeVisible()

    await page.locator('.user-info').click()
    await expect(page.getByRole('menuitem', { name: /系统设置/ })).toBeVisible()
  })
})
