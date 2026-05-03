// Input: task form value and current user store
// Output: backend assignee payload for project task create/update calls
// Pos: src/composables/projectDetail/ - Pure task payload helper
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export function createTaskAssigneePayload(data = {}, userStore = {}) {
  return {
    assigneeId: data?.assigneeId ?? userStore.currentUser?.id ?? null,
    assigneeName: data?.owner || data?.assignee || userStore.userName,
    assigneeDeptCode: data?.assigneeDeptCode || '',
    assigneeDeptName: data?.assigneeDeptName || data?.department || '',
    assigneeRoleCode: data?.assigneeRoleCode || '',
    assigneeRoleName: data?.assigneeRoleName || data?.roleName || '',
  }
}
