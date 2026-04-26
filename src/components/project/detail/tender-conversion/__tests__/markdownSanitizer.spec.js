import { describe, expect, it } from 'vitest'
import { renderSafeMarkdown } from '../markdownSanitizer.js'

describe('renderSafeMarkdown', () => {
  it('strips onerror and src=x from injected img tags', () => {
    const input = '# Hello\n<img src=x onerror=alert(1)>'
    const output = renderSafeMarkdown(input)
    expect(output).not.toContain('onerror')
    expect(output).not.toContain('src=x')
    expect(output).not.toContain('src="x"')
  })

  it('strips script tags entirely', () => {
    const input = '<script>alert(1)</script>'
    const output = renderSafeMarkdown(input)
    expect(output).not.toContain('<script')
    expect(output).not.toContain('alert(1)')
  })

  it('strips iframe tags', () => {
    const output = renderSafeMarkdown('<iframe src="https://evil.com"></iframe>')
    expect(output).not.toContain('<iframe')
  })

  it('strips style tags', () => {
    const output = renderSafeMarkdown('<style>body { display:none }</style>')
    expect(output).not.toContain('<style')
  })

  it('strips on* event handlers from allowed elements', () => {
    const output = renderSafeMarkdown('<p onclick="alert(1)">text</p>')
    expect(output).not.toContain('onclick')
  })

  it('renders bold markdown to <strong>', () => {
    const output = renderSafeMarkdown('**bold text**')
    expect(output).toContain('<strong>')
    expect(output).toContain('bold text')
  })

  it('renders h1 heading markdown', () => {
    const output = renderSafeMarkdown('# Heading One')
    expect(output).toContain('<h1')
    expect(output).toContain('Heading One')
  })

  it('renders unordered list items', () => {
    const output = renderSafeMarkdown('- item one\n- item two')
    expect(output).toContain('<ul')
    expect(output).toContain('<li')
    expect(output).toContain('item one')
  })

  it('renders links but without target=_blank that could be dangerous without rel', () => {
    const output = renderSafeMarkdown('[link](https://example.com)')
    expect(output).toContain('<a')
    expect(output).toContain('https://example.com')
    // rel=noopener must be present when target=_blank is allowed
    if (output.includes('target="_blank"') || output.includes("target='_blank'")) {
      expect(output).toContain('rel=')
    }
  })

  it('renders inline code', () => {
    const output = renderSafeMarkdown('use `code` here')
    expect(output).toContain('<code>')
    expect(output).toContain('code')
  })

  it('returns a string for empty input', () => {
    expect(typeof renderSafeMarkdown('')).toBe('string')
    expect(renderSafeMarkdown('')).toBe('')
  })

  it('returns a string for non-string input (defensive)', () => {
    expect(typeof renderSafeMarkdown(null)).toBe('string')
    expect(typeof renderSafeMarkdown(undefined)).toBe('string')
  })
})
