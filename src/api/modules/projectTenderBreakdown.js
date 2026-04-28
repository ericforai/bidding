// Input: project ID and tender document file
// Output: independent project tender breakdown API request
// Pos: src/api/modules/ - Project tender breakdown API boundary
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'
import { apiModeFailure, demoReadonlyFailure, isDemoEntityId, isNumericId } from './projectApiGuards.js'

export async function parseTenderBreakdown(projectId, file) {
  if (!isNumericId(projectId)) {
    return apiModeFailure('project')
  }

  if (isDemoEntityId(projectId)) {
    return demoReadonlyFailure()
  }

  const formData = new FormData()
  formData.set('file', file, file?.name || '招标文件')
  return httpClient.post(`/api/projects/${projectId}/tender-breakdown`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
    silentError: true,
  })
}
