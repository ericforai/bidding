import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import source from './TenderTable.vue?raw'

const tableStyles = readFileSync(join(process.cwd(), 'src/views/Bidding/list/styles/table.css'), 'utf-8')

describe('TenderTable responsive layout contract', () => {
  it('renders all 18 tender list columns with proper responsive layout', () => {
    expect(source).toContain('项目名称')
    expect(source).toContain('prop="region"')
    expect(source).toContain('prop="projectType"')
    expect(source).toContain('prop="customerType"')
    expect(source).toContain('prop="registrationDeadline"')
    expect(source).toContain('prop="bidOpeningTime"')
    expect(source).toContain('prop="projectManagerName"')
    expect(source).toContain('prop="biddingPersonName"')
    expect(source).toContain('prop="priority"')
    expect(source).toContain('prop="creatorName"')
  })

  it('uses responsive action width with overflow handling', () => {
    expect(source).toContain('scrollbar-always-on')
    expect(source).toContain('label="操作" width="320"')
    expect(tableStyles).toContain('.table-actions')
    expect(tableStyles).toContain('flex-wrap: nowrap')
    expect(tableStyles).toContain('box-sizing: border-box')
    expect(tableStyles).toContain('overflow: visible')
    expect(tableStyles).toContain('el-table-fixed-column--right')
    expect(tableStyles).toContain('.el-scrollbar__bar.is-horizontal')
    expect(tableStyles).toContain('height: 9px')
    expect(tableStyles).toContain('opacity: 1')
  })
})
