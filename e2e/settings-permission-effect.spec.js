import { test, expect } from '@playwright/test'

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const password = process.env.COMMERCIAL_E2E_PASSWORD || 'XiyuDemo!2026'

async function requestJson(url, options = {}) {
  const response = await fetch(url, options)
  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    throw new Error(`${options.method || 'GET'} ${url} failed with status ${response.status}: ${JSON.stringify(payload)}`)
  }

  return payload
}

async function ensureSession({ username, role, fullName }) {
  const email = `${username}@example.com`

  try {
    await requestJson(`${apiBaseUrl}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        password,
        email,
        fullName,
        role
      })
    })
  } catch (error) {
    if (!String(error.message).includes('409') && !String(error.message).includes('already exists')) {
      throw error
    }
  }

  const payload = await requestJson(`${apiBaseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })

  if (!payload?.success || !payload?.data?.token || !payload?.data?.id) {
    throw new Error('Backend login response missing token or user identity')
  }

  return {
    token: payload.data.token,
    refreshToken: payload.data.refreshToken || null,
    user: {
      id: payload.data.id,
      name: payload.data.fullName || payload.data.username,
      username: payload.data.username,
      email: payload.data.email,
      role: String(payload.data.role || '').toLowerCase()
    }
  }
}

async function setSession(page, session) {
  await page.addInitScript(({ currentSession }) => {
    sessionStorage.setItem('token', currentSession.token)
    if (currentSession.refreshToken) {
      sessionStorage.setItem('refreshToken', currentSession.refreshToken)
    }
    sessionStorage.setItem('user', JSON.stringify(currentSession.user))
  }, { currentSession: session })
}

test('saved settings permissions affect manager runtime route access', async ({ page, context }) => {
  const suffix = Date.now()
  const adminSession = await ensureSession({
    username: `settings_admin_${suffix}`,
    role: 'ADMIN',
    fullName: 'Settings Admin'
  })
  const managerSession = await ensureSession({
    username: `settings_manager_${suffix}`,
    role: 'MANAGER',
    fullName: 'Settings Manager'
  })

  await setSession(page, adminSession)
  await page.goto('/settings')

  await expect(page).toHaveURL(/\/settings$/)
  await page.getByRole('tab', { name: '角色权限' }).click()

  await page.getByRole('button', { name: '权限配置' }).nth(1).click()

  const permissionDialog = page.locator('.permission-dialog')
  await expect(permissionDialog).toBeVisible()

  const analyticsNode = permissionDialog.locator('.el-tree-node').filter({ hasText: '数据分析' }).first()
  await analyticsNode.locator('.el-checkbox').click()

  await permissionDialog.getByRole('button', { name: '保存配置' }).click()
  await expect(page.getByText('权限配置已保存')).toBeVisible()

  const managerPage = await context.newPage()
  await setSession(managerPage, managerSession)

  await managerPage.goto('/analytics/dashboard')

  await expect(managerPage).toHaveURL(/\/dashboard$/)
  await expect(managerPage.getByText('工作台').first()).toBeVisible()
  await expect(managerPage.locator('.sidebar-menu').getByText('数据分析')).toHaveCount(0)
})
