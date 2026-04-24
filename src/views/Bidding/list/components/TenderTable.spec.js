import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import source from './TenderTable.vue?raw'

const tableStyles = readFileSync(join(process.cwd(), 'src/views/Bidding/list/styles/table.css'), 'utf-8')

describe('TenderTable responsive layout contract', () => {
  it('keeps secondary tender attributes inside the primary tender column', () => {
    expect(source).toContain('label="标讯"')
    expect(source).toContain('class="tender-meta-line"')
    expect(source).not.toContain('prop="region" label="地区"')
    expect(source).not.toContain('prop="industry" label="行业"')
    expect(source).not.toContain('prop="deadline" label="截止日期"')
  })

  it('uses compact action width with an obvious horizontal-scroll fallback', () => {
    expect(source).toContain('scrollbar-always-on')
    expect(source).toContain('label="操作" width="224"')
    expect(tableStyles).toContain('.table-actions')
    expect(tableStyles).toContain('flex-wrap: nowrap')
    expect(tableStyles).toContain('.el-scrollbar__bar.is-horizontal')
    expect(tableStyles).toContain('height: 9px')
    expect(tableStyles).toContain('opacity: 1')
  })
})
