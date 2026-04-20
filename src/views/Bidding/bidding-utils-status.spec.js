import { describe, expect, it } from 'vitest'
import {
  getTenderStatusBadgeClass,
  getTenderStatusTagType,
  getTenderStatusText,
  matchesTenderStatus,
  normalizeTenderCollection,
  normalizeTenderStatusCode,
  TENDER_STATUSES,
  toBackendTenderStatus,
} from './bidding-utils-status.js'

describe('bidding-utils-status', () => {
  it.each([
    ['new', TENDER_STATUSES.PENDING],
    ['contacted', TENDER_STATUSES.TRACKING],
    ['following', TENDER_STATUSES.TRACKING],
    ['quoting', TENDER_STATUSES.TRACKING],
    ['bidding', TENDER_STATUSES.BIDDED],
    ['abandoned', TENDER_STATUSES.ABANDONED],
    ['已投标', TENDER_STATUSES.BIDDED],
    ['TRACKING', TENDER_STATUSES.TRACKING],
  ])('normalizes %s to %s', (input, expected) => {
    expect(normalizeTenderStatusCode(input)).toBe(expected)
  })

  it('normalizes tender collections in one place', () => {
    expect(normalizeTenderCollection([
      { id: 1, status: 'new' },
      { id: 2, status: 'quoting' },
    ])).toEqual([
      { id: 1, status: 'PENDING' },
      { id: 2, status: 'TRACKING' },
    ])
  })

  it('matches canonical and legacy status filters', () => {
    expect(matchesTenderStatus('TRACKING', 'following')).toBe(true)
    expect(matchesTenderStatus('bidding', 'BIDDED')).toBe(true)
    expect(matchesTenderStatus('PENDING', 'ABANDONED')).toBe(false)
  })

  it('exposes display helpers from one source of truth', () => {
    expect(getTenderStatusText('quoting')).toBe('跟踪中')
    expect(getTenderStatusTagType('TRACKING')).toBe('warning')
    expect(getTenderStatusBadgeClass('bidding')).toBe('bidded')
  })

  it('re-exports backend status conversion through canonical mapping', () => {
    expect(toBackendTenderStatus('contacted')).toBe('TRACKING')
  })
})
