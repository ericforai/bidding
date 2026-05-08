import { describe, expect, it } from 'vitest'
import projectListSource from './List.vue?raw'

describe('Project/List.vue layout', () => {
  it('uses scoped table layout styles instead of stretching the list card by viewport height', () => {
    expect(projectListSource).toContain('class="project-table"')
    expect(projectListSource).toContain('.project-table')
    expect(projectListSource).not.toContain('min-height: calc(100vh - 280px)')
  })

  it('keeps row actions compact without text buttons', () => {
    expect(projectListSource).toContain('content="查看详情"')
    expect(projectListSource).toContain('aria-label="查看详情"')
    expect(projectListSource).not.toContain('>查看详情</el-button>')
    expect(projectListSource).not.toContain('>编辑</el-button>')
  })

  it('uses consolidated desktop list columns instead of a horizontally scrolled wide table', () => {
    expect(projectListSource).toContain('label="项目信息"')
    expect(projectListSource).toContain('class="project-name-cell"')
    expect(projectListSource).toContain('class="project-meta"')
    expect(projectListSource).toContain('class="meta-label">客户</span>')
    expect(projectListSource).toContain('label="状态/进度"')
    expect(projectListSource).toContain('label="负责人/截止"')
    expect(projectListSource).not.toContain(':fit="false"')
    expect(projectListSource).not.toContain('overflow-x: auto')
  })

  it('left aligns the project title instead of centering the link inside the cell', () => {
    expect(projectListSource).toContain('.project-title-link { align-self: flex-start; justify-content: flex-start; }')
  })

  it('does not fix the action column over table content', () => {
    expect(projectListSource).toContain('label="操作" width="104" align="center"')
    expect(projectListSource).not.toContain('fixed="right"')
    expect(projectListSource).not.toContain('.el-table__fixed-right')
  })
})
