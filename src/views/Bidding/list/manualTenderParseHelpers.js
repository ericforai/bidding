// Input: doc-insight parse result for manual tender intake
// Output: normalized editable manual tender form fields
// Pos: src/views/Bidding/list/ - Pure helpers for manual tender document recognition
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

function cleanText(value) {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function firstText(...values) {
  for (const value of values) {
    const text = cleanText(value)
    if (text) return text
  }
  return ''
}

function normalizeTags(value) {
  if (Array.isArray(value)) {
    return value.map(cleanText).filter(Boolean)
  }
  const text = cleanText(value)
  if (!text) return []
  return text.split(/[,，、;\s]+/).map(cleanText).filter(Boolean)
}

export function normalizeBudgetYuan(value) {
  if (value === null || value === undefined || value === '') return null
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null
  }

  const text = cleanText(value).replace(/,/g, '')
  const matched = text.match(/-?\d+(?:\.\d+)?/)
  if (!matched) return null

  const amount = Number(matched[0])
  if (!Number.isFinite(amount)) return null
  if (text.includes('亿元') || text.includes('亿')) return amount * 100000000
  if (text.includes('万元') || text.includes('万')) return amount * 10000
  return amount
}

function normalizeDeadline(value) {
  const text = cleanText(value)
  if (!text) return null
  const date = new Date(text)
  return Number.isNaN(date.getTime()) ? null : date
}

export function normalizeManualTenderParseResult(result = {}) {
  const data = result?.extractedData || {}
  return {
    title: firstText(data.tenderTitle, data.title, data.projectName),
    budget: normalizeBudgetYuan(data.budget),
    region: firstText(data.region),
    industry: firstText(data.industry),
    deadline: normalizeDeadline(data.deadline),
    purchaser: firstText(data.purchaserName, data.purchaser),
    contact: firstText(data.contactName, data.contactPerson, data.contact),
    description: firstText(data.description, data.tenderScope),
    tags: normalizeTags(data.tags),
  }
}
