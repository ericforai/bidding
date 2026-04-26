/**
 * markdownSanitizer.js
 *
 * Input:  raw markdown string (possibly untrusted — from uploaded documents)
 * Output: sanitized HTML string safe for use with v-html
 * Pos:    Components/Project Detail/tender-conversion (pure helper, no Vue dependencies)
 * 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
 *
 * Uses `marked` to parse markdown and `DOMPurify` to strip any dangerous HTML.
 * This module is the single place where markdown→HTML conversion happens for
 * TenderConversionWorkbench. It must never import Vue or Element Plus.
 */

import { marked } from 'marked'
import DOMPurify from 'dompurify'

/**
 * Allowed HTML tags after sanitization.
 * Intentionally excludes: script, iframe, object, embed, style, form, input, button.
 */
const ALLOWED_TAGS = [
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'p', 'ul', 'ol', 'li',
  'strong', 'em', 'code', 'pre', 'blockquote',
  'a', 'hr', 'br',
  'table', 'thead', 'tbody', 'tr', 'th', 'td',
]

/**
 * Allowed HTML attributes after sanitization.
 * `href` is allowed on <a> only; no on* event handlers.
 */
const ALLOWED_ATTR = ['href', 'rel', 'title']

/**
 * DOMPurify config: strict allowlist, no dangerous protocols.
 */
const PURIFY_CONFIG = {
  ALLOWED_TAGS,
  ALLOWED_ATTR,
  ALLOW_DATA_ATTR: false,
  FORBID_TAGS: ['script', 'iframe', 'object', 'embed', 'style', 'form'],
  FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'onfocus', 'onblur'],
}

/**
 * Renders a raw markdown string to sanitized HTML.
 *
 * @param {string|null|undefined} raw - Raw markdown content from an uploaded document.
 * @returns {string} Sanitized HTML string. Returns empty string for falsy input.
 */
export function renderSafeMarkdown(raw) {
  if (!raw || typeof raw !== 'string') return ''

  // marked.parse returns a string of HTML
  const rawHtml = marked.parse(raw)

  // DOMPurify.sanitize needs a window/document context.
  // In jsdom (Vitest) and real browsers, globalThis.window is available.
  // We call sanitize with the strict allowlist config.
  return DOMPurify.sanitize(rawHtml, PURIFY_CONFIG)
}
