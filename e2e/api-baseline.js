// Input: Playwright env vars and rehearsal health endpoints
// Output: shared helpers for API-backed Playwright baseline bootstrapping
// Pos: e2e/ - Playwright end-to-end coverage
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

const DEFAULT_API_BASE_URL = 'http://127.0.0.1:18080'
const DEFAULT_WEB_BASE_URL = 'http://127.0.0.1:1314'

export function resolveApiBaseUrl() {
  return process.env.PLAYWRIGHT_API_BASE_URL || DEFAULT_API_BASE_URL
}

export function resolveWebBaseUrl() {
  return process.env.PLAYWRIGHT_BASE_URL || DEFAULT_WEB_BASE_URL
}

export async function isHttpReady(url) {
  try {
    const response = await fetch(url)
    return response.ok
  } catch {
    return false
  }
}

export async function ensureManagedStackReady() {
  const apiReady = await isHttpReady(`${resolveApiBaseUrl()}/actuator/health`)
  const webReady = await isHttpReady(resolveWebBaseUrl())
  return { apiReady, webReady, ready: apiReady && webReady }
}
