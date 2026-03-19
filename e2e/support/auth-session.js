const frontendBaseUrl = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:1314'

export const extractRefreshToken = (response) => {
  const setCookieHeader = response.headers.get('set-cookie') || ''
  const match = setCookieHeader.match(/refresh_token=([^;]+)/)

  if (!match) {
    throw new Error('Login response missing refresh token cookie')
  }

  return match[1]
}

export const attachRefreshSession = async (page, refreshToken) => {
  await page.context().addCookies([
    {
      name: 'refresh_token',
      value: refreshToken,
      url: frontendBaseUrl,
      httpOnly: true,
      sameSite: 'Lax'
    }
  ])
}
