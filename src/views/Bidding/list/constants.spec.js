import { describe, expect, it } from 'vitest'
import { DEFAULT_SOURCE_CONFIG, REGION_OPTIONS } from './constants.js'

describe('bidding list constants', () => {
  it('uses province-level nationwide region options', () => {
    expect(REGION_OPTIONS).toHaveLength(34)
    expect(REGION_OPTIONS).toEqual(expect.arrayContaining([
      '北京',
      '上海',
      '广东',
      '四川',
      '新疆',
      '台湾',
      '香港',
      '澳门',
    ]))
    for (const legacyOption of ['广州', '深圳', '成都', '其他']) {
      expect(REGION_OPTIONS).not.toContain(legacyOption)
    }
  })

  it('does not limit external source sync to sample cities by default', () => {
    expect(DEFAULT_SOURCE_CONFIG.regions).toEqual([])
  })
})
