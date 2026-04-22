import { describe, expect, it, vi } from 'vitest'
import {
  buildPermissionFlags,
  restoreSourceConfig,
  safeTenderUrl,
  sanitizeSourceConfigForStorage,
  summarizeExternalSyncResult,
} from './helpers.js'

describe('bidding list helpers', () => {
  it('strips apiKey before source config persistence', () => {
    const result = sanitizeSourceConfigForStorage({
      platforms: ['中国政府采购网'],
      apiKey: 'secret-token',
      keywords: ['劳保'],
    })

    expect(result).not.toHaveProperty('apiKey')
    expect(result).toMatchObject({
      platforms: ['中国政府采购网'],
      keywords: ['劳保'],
    })
  })

  it('removes legacy apiKey from stored source config while restoring safe fields', () => {
    const writer = vi.fn()
    const restored = restoreSourceConfig(JSON.stringify({
      platforms: ['第三方商机服务'],
      apiKey: 'legacy-secret',
      regions: ['上海'],
    }), writer)

    expect(restored.apiKey).toBe('')
    expect(restored.platforms).toEqual(['第三方商机服务'])
    expect(writer).toHaveBeenCalledOnce()
    expect(JSON.parse(writer.mock.calls[0][0])).not.toHaveProperty('apiKey')
  })

  it('only allows http and https tender links', () => {
    expect(safeTenderUrl('https://example.com/tender')).toBe('https://example.com/tender')
    expect(safeTenderUrl('http://example.com/tender')).toBe('http://example.com/tender')
    expect(safeTenderUrl('javascript:alert(1)')).toBe('')
    expect(safeTenderUrl('/relative/path')).toBe('')
  })

  it('matches frontend permission flags to backend role boundary', () => {
    expect(buildPermissionFlags('staff')).toMatchObject({
      canManageTenders: false,
      canDeleteTenders: false,
      canSyncExternalSource: false,
    })
    expect(buildPermissionFlags('manager')).toMatchObject({
      canManageTenders: true,
      canDeleteTenders: false,
      canSyncExternalSource: false,
    })
    expect(buildPermissionFlags('ROLE_ADMIN')).toMatchObject({
      canManageTenders: true,
      canDeleteTenders: true,
      canSyncExternalSource: true,
    })
  })

  it('summarizes external sync results from the backend response shape', () => {
    expect(summarizeExternalSyncResult({
      success: true,
      message: 'Crawler executed successfully',
      savedCount: 3,
    })).toMatchObject({
      visible: true,
      saved: 3,
      skipped: 0,
      message: 'Crawler executed successfully',
    })

    expect(summarizeExternalSyncResult({
      data: { saved: 2, skipped: 1, message: 'done' },
    })).toMatchObject({
      saved: 2,
      skipped: 1,
      message: 'done',
    })
  })
})
