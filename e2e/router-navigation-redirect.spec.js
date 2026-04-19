import { test, expect } from '@playwright/test'

/**
 * TDD Test: Router Navigation Redirect
 *
 * This test verifies that 401 errors trigger proper Vue Router navigation
 * instead of window.location.href, ensuring navigation guards work correctly.
 */

test.describe('router navigation redirect', () => {
  const seedExpiredSession = async (page, user = {}) => {
    await page.addInitScript((nextUser) => {
      sessionStorage.setItem('token', 'expired-access-token')
      sessionStorage.setItem('refreshToken', 'expired-refresh-token')
      sessionStorage.setItem('user', JSON.stringify({
        id: nextUser.id ?? 1,
        name: nextUser.name ?? 'Test User',
        username: nextUser.username ?? 'testuser',
        email: nextUser.email ?? 'test@example.com',
        role: nextUser.role ?? 'admin',
        allowedProjectIds: Array.isArray(nextUser.allowedProjectIds) ? nextUser.allowedProjectIds : [],
        allowedDepts: Array.isArray(nextUser.allowedDepts) ? nextUser.allowedDepts : []
      }))
    }, user)
  }

  const installRouterPushSpy = async (page) => {
    await page.addInitScript(() => {
      window.routerPushCalled = false

      const checkAndPatch = () => {
        const app = window.__VUE_APP__
        if (app && app.config && app.config.globalProperties && app.config.globalProperties.$router) {
          const router = app.config.globalProperties.$router
          if (router._patched) return true

          const originalPush = router.push
          router.push = function (...args) {
            window.routerPushCalled = true
            return originalPush.apply(this, args)
          }
          router._patched = true
          return true
        }
        return false
      }

      const start = Date.now()
      const timer = setInterval(() => {
        if (checkAndPatch() || Date.now() - start > 5000) clearInterval(timer)
      }, 100)
    })
  }

  const mockExpiredSessionApis = async (page, onRequest) => {
    await page.route('**/api/auth/refresh', async (route) => {
      await onRequest?.(route)
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Refresh token expired' })
      })
    })

    await page.route('**/api/**', async (route) => {
      await onRequest?.(route)
      const url = route.request().url()
      if (url.includes('/api/auth/refresh')) {
        await route.fallback()
        return
      }

      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Unauthorized' })
      })
    })
  }

  // Clear all storage before each test to ensure clean state
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
      sessionStorage.clear()
    })
  })

  test('should use router.push for login redirect on 401, not window.location.href', async ({ page }) => {
    await seedExpiredSession(page)
    await mockExpiredSessionApis(page)
    await installRouterPushSpy(page)

    // Navigate to a protected route
    await page.goto('/dashboard')

    // Wait for redirect with generous timeout
    await expect(page).toHaveURL(/\/login$/, { timeout: 15000 })

    // Verify router.push was used
    const pushCalled = await page.evaluate(() => window.routerPushCalled)
    expect(pushCalled).toBe(true)

    // Verify session was cleared
    const storageState = await page.evaluate(() => ({
      token: sessionStorage.getItem('token'),
      user: sessionStorage.getItem('user')
    }))

    expect(storageState.token).toBeNull()
    expect(storageState.user).toBeNull()

    // Verify error message was shown
    await expect(page.getByText('登录已过期，请重新登录')).toBeVisible()
  })

  test('should not trigger redirect when already on login page', async ({ page }) => {
    // Go directly to login page
    await page.goto('/login')

    // Mock a 401 response
    await page.route('**/api/**', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Unauthorized' })
      })
    })

    // Trigger an API call that would return 401
    await page.evaluate(() => {
      fetch('/api/auth/me').catch(() => {})
    })

    // After the 401 response is processed, verify we're still on login page
    await expect(page).toHaveURL(/\/login$/)
  })

  test('should preserve router navigation guards during redirect', async ({ page }) => {
    await seedExpiredSession(page, {
      id: 2,
      name: 'Another User',
      username: 'another',
      email: 'another@example.com',
      role: 'user'
    })
    await mockExpiredSessionApis(page)
    await installRouterPushSpy(page)

    // Navigate to protected route
    await page.goto('/project')

    // Should end up on login
    await expect(page).toHaveURL(/\/login$/, { timeout: 15000 })

    // Verify router.push was used
    const pushCalled = await page.evaluate(() => window.routerPushCalled)
    expect(pushCalled).toBe(true)
  })

  test('should handle multiple 401s gracefully without redirect loops', async ({ page }) => {
    let requestCount = 0

    await seedExpiredSession(page)
    await installRouterPushSpy(page)
    await mockExpiredSessionApis(page, async (route) => {
      const url = route.request().url()
      if (url.includes('/api/auth/refresh') || url.includes('/api/')) {
        requestCount += 1
      }
    })

    // Navigate
    await page.goto('/dashboard')

    // Wait for redirect
    await expect(page).toHaveURL(/\/login$/, { timeout: 15000 })

    // Verify no redirect loop
    expect(requestCount).toBeLessThan(20)

    // Verify router.push used
    const pushCalled = await page.evaluate(() => window.routerPushCalled)
    expect(pushCalled).toBe(true)
  })
})
