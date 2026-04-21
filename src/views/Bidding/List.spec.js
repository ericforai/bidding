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

  it('搜索动作应刷新服务端列表而不是只触发本地过滤', () => {
    expect(source).toContain('const handleSearch = async () =>')
    expect(source).toContain('await refreshTenderList()')
    expect(source).not.toContain('result = result.filter(t => t.region === searchForm.value.region)')
    expect(source).not.toContain('result = result.filter(t => t.industry === searchForm.value.industry)')
    expect(source).not.toContain('result = result.filter(t => t.source === searchForm.value.source)')
  })

  it('人工录入应通过 tendersApi.create 入库而不是插入本地假数据', () => {
    expect(source).toContain('await tendersApi.create')
    expect(source).toContain('purchaserName')
    expect(source).toContain('contactName')
    expect(source).toContain('contactPhone')
    expect(source).not.toContain('id: `manual_${Date.now()}`')
    expect(source).not.toContain("aiReason: '人工录入标讯'")
  })
})
