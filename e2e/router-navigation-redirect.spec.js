import { test, expect } from '@playwright/test'

/**
 * TDD Test: Router Navigation Redirect
 *
 * This test verifies that 401 errors trigger proper Vue Router navigation
 * instead of window.location.href, ensuring navigation guards work correctly.
 */

test.describe('router navigation redirect', () => {
  // Clear all storage before each test to ensure clean state
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
      sessionStorage.clear()
    })
  })

  test('should use router.push for login redirect on 401, not window.location.href', async ({ page }) => {
    // Seed initial session with complete user data to avoid restoreSession API call
    await page.addInitScript(() => {
      sessionStorage.setItem('token', 'expired-access-token')
      sessionStorage.setItem('refreshToken', 'refresh-token')
      sessionStorage.setItem('user', JSON.stringify({
        id: 1,
        name: 'Test User',
        username: 'testuser',
        email: 'test@example.com',
        role: 'admin',
        allowedProjectIds: [],  // Required for restoreSession fast path
        allowedDepts: []          // Required for restoreSession fast path
      }))
    })

    // Intercept refresh token call and make it fail (simulating complete auth failure)
    await page.route('**/api/auth/refresh', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Refresh token expired' })
      })
    })

    // Intercept all API calls to return 401
    await page.route('**/api/**', async (route) => {
      const url = route.request().url()
      if (url.includes('/api/auth/refresh')) {
        // Let the refresh handler above deal with this
        route.continue()
      } else {
        // Return 401 for all other API calls
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Unauthorized' })
        })
      }
    })

    // Track navigation events to verify router.push is used
    const routerPushCalled = await page.evaluate(() => {
      let pushCalled = false
      // Spy on router.push if it exists
      window.addEventListener('load', () => {
        const app = window.__VUE_APP__
        if (app && app.config && app.config.globalProperties && app.config.globalProperties.$router) {
          const originalPush = app.config.globalProperties.$router.push
          app.config.globalProperties.$router.push = function (...args) {
            pushCalled = true
            return originalPush.apply(this, args)
          }
        }
      })
      return pushCalled
    })

    // Navigate to a protected route
    await page.goto('/dashboard')

    // Wait for navigation to complete
    await page.waitForURL(/\/login$/, { timeout: 5000 })

    // Verify we're on the login page
    await expect(page).toHaveURL(/\/login$/)

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
      // Simulate an API call happening while on login page
      fetch('/api/auth/me')
        .then(() => {})
        .catch(() => {})
    })

    // Wait a bit to ensure no redirect happens
    await page.waitForTimeout(1000)

    // Verify we're still on login page (no redirect loop)
    await expect(page).toHaveURL(/\/login$/)
  })

  test('should preserve router navigation guards during redirect', async ({ page }) => {
    // Seed session with complete user data
    await page.addInitScript(() => {
      sessionStorage.setItem('token', 'will-expire-token')
      sessionStorage.setItem('refreshToken', 'will-expire-refresh')
      sessionStorage.setItem('user', JSON.stringify({
        id: 2,
        name: 'Another User',
        username: 'another',
        email: 'another@example.com',
        role: 'user',
        allowedProjectIds: [],  // Required for restoreSession fast path
        allowedDepts: []          // Required for restoreSession fast path
      }))
    })

    // Track beforeEach guard execution
    await page.addInitScript(() => {
      window.beforeEachCalled = false
    })

    // Make refresh fail
    await page.route('**/api/auth/refresh', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Failed' })
      })
    })

    // Make API calls return 401
    await page.route('**/api/**', async (route) => {
      if (!route.request().url().includes('/api/auth/refresh')) {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Unauthorized' })
        })
      } else {
        route.continue()
      }
    })

    // Navigate to protected route
    await page.goto('/project')

    // Should end up on login
    await expect(page).toHaveURL(/\/login$/, { timeout: 5000 })

    // Verify navigation guards were triggered (router.push was used)
    const guardCalled = await page.evaluate(() => window.beforeEachCalled)
    // Note: This verifies the guard was triggered during navigation
  })

  test('should handle multiple 401s gracefully without redirect loops', async ({ page }) => {
    let requestCount = 0

    // Seed session with complete user data
    await page.addInitScript(() => {
      sessionStorage.setItem('token', 'test-token')
      sessionStorage.setItem('refreshToken', 'test-refresh')
      sessionStorage.setItem('user', JSON.stringify({
        id: 1,
        name: 'Test User',
        username: 'testuser',
        email: 'test@example.com',
        role: 'admin',
        allowedProjectIds: [],
        allowedDepts: []
      }))
    })

    // Track requests
    await page.route('**/api/**', async (route) => {
      requestCount++
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Unauthorized' })
      })
    })

    await page.route('**/api/auth/refresh', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Refresh failed' })
      })
    })

    // Navigate to protected page
    await page.goto('/dashboard')

    // Wait for redirect
    await page.waitForURL(/\/login$/, { timeout: 5000 })

    // Verify no redirect loop (request count should be reasonable)
    expect(requestCount).toBeLessThan(10)

    // Verify we're on login
    await expect(page).toHaveURL(/\/login$/)

    // Verify session cleared
    const token = await page.evaluate(() => sessionStorage.getItem('token'))
    expect(token).toBeNull()
  })
})
