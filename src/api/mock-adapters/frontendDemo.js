// Input: none
// Output: frontend demo adapter functions returning API-only empty fallbacks
// Pos: src/api/mock-adapters/ - Demo data adapter layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export function getDemoUsers() {
  return []
}

export function getDemoTodos() {
  return []
}

export function getDemoCalendar() {
  return []
}

export function getDemoProjects() {
  return []
}

export function getDemoProjectById(projectId) {
  void projectId
  return null
}

export function getDemoAutoTasks() {
  return []
}

export function getDemoMobileCard(projectId) {
  void projectId
  return null
}

export function getDemoAutomationPanelData() {
  return {
    rules: [],
    pendingReminders: [],
    executionHistory: []
  }
}

export function getDemoDashboardProjects() {
  return []
}

export function getCaseDemoOverride(caseId) {
  void caseId
  return null
}

export function saveCaseDemoPatch(caseId, patch) {
  void caseId
  void patch
}

export function getTemplateDemoState() {
  return {
    patches: {},
    copies: []
  }
}

export function saveTemplateDemoState(state) {
  void state
}

export function getBarSiteDemoOverride(siteId) {
  void siteId
  return null
}

export function saveBarSiteDemoPatch(siteId, patch) {
  void siteId
  void patch
}
