// Input: projectsApi (from @/api)
// Output: useProjectStore - Pinia store for project management
// Pos: src/stores/ - State management layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { defineStore } from 'pinia'
import { projectsApi } from '@/api'
import { normalizeTaskStatusForApi } from '@/views/Project/project-utils.js'

export const useProjectStore = defineStore('project', {
  state: () => ({
    projects: [],
    currentProject: null
  }),

  getters: {
    inProgressProjects: (state) => state.projects.filter(p => p.status !== 'won' && p.status !== 'lost'),
    wonProjects: (state) => state.projects.filter(p => p.status === 'won'),
    findProjectById: (state) => (id) => state.projects.find(p => p.id === id)
  },

  actions: {
    async getProjects(filters = {}) {
      try {
        const result = await projectsApi.getList(filters)
        this.projects = result?.success ? (result.data || []) : []
      } catch (error) {
        console.warn('API 调用失败，返回空项目列表:', error.message)
        this.projects = []
      }
      return this.projects
    },

    async getProjectById(id) {
      const existingProject = this.projects.find(p => String(p.id) === String(id))
      if (existingProject) {
        this.currentProject = existingProject
        return existingProject
      }

      try {
        const result = await projectsApi.getDetail(id)
        const project = result?.success ? result.data : null
        if (project) {
          this.currentProject = project
          return project
        }
      } catch (error) {
        console.warn('API 获取项目详情失败，返回空结果:', error.message)
      }

      this.currentProject = null
      return null
    },

    async createProject(data) {
      const result = await projectsApi.create(data)
      const newProject = result?.data
      if (newProject) {
        this.projects.unshift(newProject)
      }
      return newProject
    },

    async updateProject(id, data) {
      const result = await projectsApi.update(id, data)
      const updatedProject = result?.data
      const index = this.projects.findIndex(p => String(p.id) === String(id))
      if (index !== -1 && updatedProject) {
        this.projects[index] = { ...this.projects[index], ...updatedProject }
      }
      if (this.currentProject && String(this.currentProject.id) === String(id) && updatedProject) {
        this.currentProject = { ...this.currentProject, ...updatedProject }
      }
      return updatedProject
    },

    async updateTaskStatus(projectId, taskId, status) {
      const apiStatus = normalizeTaskStatusForApi(status) ?? status
      try {
        const result = await projectsApi.updateTaskStatus(projectId, taskId, apiStatus)
        if (!result?.success) {
          console.warn('Task status update returned non-success:', result)
          return result
        }
        // Immutably update local state (using string-coerced id comparison for route-param safety)
        this.projects = this.projects.map(p => {
          if (String(p.id) !== String(projectId)) return p
          const updatedTasks = (p.tasks || []).map(t =>
            String(t.id) === String(taskId) ? { ...t, status } : t
          )
          const doneCount = updatedTasks.filter(t => t.status === 'done').length
          const total = updatedTasks.length
          const progress = total > 0 ? Math.round((doneCount / total) * 100) : 0
          return { ...p, tasks: updatedTasks, progress }
        })
        if (String(this.currentProject?.id) === String(projectId)) {
          const updated = this.projects.find(p => String(p.id) === String(projectId))
          if (updated) this.currentProject = updated
        }
        return result
      } catch (error) {
        console.warn('Failed to update task status:', error)
        throw error
      }
    }
  }
})
