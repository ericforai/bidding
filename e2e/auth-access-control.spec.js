import { test, expect } from '@playwright/test'
import { attachRefreshSession, extractRefreshToken } from './support/auth-session.js'

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const demoPassword = process.env.E2E_DEMO_PASSWORD || '123456'

const users = {
  admin: { username: 'lizong', password: demoPassword },
  manager: { username: 'zhangjingli', password: demoPassword },
  staff: { username: 'xiaowang', password: demoPassword },
}

const readStoredUserHint = () => {
  const rawUser =
    window.localStorage.getItem('user') ||
    window.sessionStorage.getItem('user') ||
    'null'

  return JSON.parse(rawUser)
}

async function loginViaApi(page, user) {
  const response = await fetch(`${apiBaseUrl}/api/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      username: user.username,
      password: user.password,
      rememberMe: true
    })
  })

  if (!response.ok) {
    throw new Error(`Login failed with status ${response.status}`)
  }

  const responseBody = await response.json()
  await attachRefreshSession(page, extractRefreshToken(response))
  await page.addInitScript((authData) => {
    const normalizedUser = {
      id: authData?.id,
      name: authData?.fullName || authData?.name || authData?.username,
      username: authData?.username,
      email: authData?.email,
      role: String(authData?.role || '').toLowerCase(),
      allowedProjectIds: Array.isArray(authData?.allowedProjectIds) ? authData.allowedProjectIds : [],
      allowedDepts: Array.isArray(authData?.allowedDepts) ? authData.allowedDepts : []
    }

    sessionStorage.setItem('token', authData?.token || '')
    sessionStorage.setItem('user', JSON.stringify(normalizedUser))
  }, responseBody.data)
  return responseBody.data
}

test.describe('auth access control', () => {
  test('redirects unauthenticated visitors to login', async ({ page }) => {
    await page.goto('/settings')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible()
  })

  test('blocks manager from admin-only settings route', async ({ page }) => {
    await loginViaApi(page, users.manager)

    await page.goto('/settings')

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByText('工作台').first()).toBeVisible()
    await page.locator('.user-info').click()
    await expect(page.getByRole('menuitem', { name: /系统设置/ })).toHaveCount(0)
  })

  test('blocks staff from analytics dashboard route', async ({ page }) => {
    await loginViaApi(page, users.staff)

    await page.goto('/analytics/dashboard')

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByText('工作台').first()).toBeVisible()
  })

  test('restores allowed project scope from refresh when stored user hint is stale', async ({ page }) => {
    const authPayload = {
      id: 1,
      username: 'lizong',
      email: 'lizong@example.com',
      fullName: '李总',
      role: 'ADMIN',
      token: 'restored-access-token',
      type: 'Bearer',
      allowedProjectIds: [101, 202, 303]
    }

    const staleUserHint = {
      id: authPayload.id,
      name: authPayload.fullName || authPayload.name || authPayload.username,
      username: authPayload.username,
      email: authPayload.email,
      role: String(authPayload.role || '').toLowerCase()
    }

    await page.route('**/api/auth/refresh', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'Token refreshed successfully',
          data: authPayload
        })
      })
    })

    await page.addInitScript((userHint) => {
      const existingUser = JSON.parse(window.localStorage.getItem('user') || 'null')
      if (Array.isArray(existingUser?.allowedProjectIds)) {
        return
      }

      window.localStorage.setItem('user', JSON.stringify(userHint))
    }, staleUserHint)

    await page.goto('/login')
    await expect(page).toHaveURL(/\/dashboard$/)
    await page.waitForFunction((expectedScope) => {
      const restoredUser = (window.localStorage.getItem('user') || window.sessionStorage.getItem('user'))
        ? JSON.parse(window.localStorage.getItem('user') || window.sessionStorage.getItem('user') || 'null')
        : null
      return JSON.stringify(restoredUser?.allowedProjectIds || []) === JSON.stringify(expectedScope)
    }, authPayload.allowedProjectIds)

    const restoredUserHint = await page.evaluate(readStoredUserHint)
    expect(restoredUserHint?.allowedProjectIds).toEqual(authPayload.allowedProjectIds)
    expect(restoredUserHint?.username).toBe(authPayload.username)
  })
})
