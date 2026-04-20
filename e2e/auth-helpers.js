// Input: Playwright E2E env vars and backend auth endpoints
// Output: shared helpers for authenticated API-backed Playwright sessions
// Pos: e2e/ - Playwright end-to-end coverage
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const defaultPassword = process.env.COMMERCIAL_E2E_PASSWORD || 'XiyuDemo!2026'

async function requestJson(url, options = {}) {
  const response = await fetch(url, options)
  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    throw new Error(`${options.method || 'GET'} ${url} failed with status ${response.status}: ${JSON.stringify(payload)}`)
  }

  return payload
}

export async function ensureApiSession({ username, role = 'ADMIN', fullName, password = defaultPassword }) {
  const email = `${username}@example.com`

  try {
    await requestJson(`${apiBaseUrl}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        password,
        email,
        fullName: fullName || username,
        role
      })
    })
  } catch (error) {
    const message = String(error.message)
    if (!message.includes('409') && !message.includes('already exists')) {
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

export async function injectSession(page, session) {
  await page.addInitScript(({ apiBaseUrl: browserApiBaseUrl }) => {
    const existingProcess = globalThis.process || { env: {} }
    existingProcess.env = existingProcess.env || {}
    existingProcess.env.VITE_API_BASE_URL = existingProcess.env.VITE_API_BASE_URL || browserApiBaseUrl
    globalThis.process = existingProcess
  }, { apiBaseUrl })

  await page.addInitScript(({ currentSession }) => {
    sessionStorage.setItem('token', currentSession.token)
    if (currentSession.refreshToken) {
      sessionStorage.setItem('refreshToken', currentSession.refreshToken)
    }
    sessionStorage.setItem('user', JSON.stringify(currentSession.user))
  }, { currentSession: session })
}

export { apiBaseUrl, defaultPassword }
