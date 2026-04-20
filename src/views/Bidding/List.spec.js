import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const source = readFileSync(resolve(process.cwd(), 'src/views/Bidding/List.vue'), 'utf-8')

describe('List.vue (标讯中心)', () => {
  it('保留标讯中心页面标题', () => {
    expect(source).toContain('标讯中心')
    expect(source).toContain('page-title')
  })

  it('保留搜索表单状态定义', () => {
    expect(source).toContain('searchForm')
    expect(source).toContain('keyword')
  })
})
