import { describe, expect, it } from 'vitest'
import source from './TenderSearchCard.vue?raw'

describe('TenderSearchCard focus styles', () => {
  it('removes the inner Element Plus select input focus ring', () => {
    expect(source).toContain('.tender-search-card :deep(.el-select__input:focus-visible)')
    expect(source).toContain('.tender-search-card :deep(.el-select__wrapper:focus-visible)')
    expect(source).toContain('outline: none !important')
    expect(source).toContain('box-shadow: none !important')
  })

  it('keeps Element Plus select active color gray inside the search card', () => {
    expect(source).toContain('.filter-select {')
    expect(source).toContain('--el-color-primary: var(--gray-200, #D0D0D0)')
  })
})
