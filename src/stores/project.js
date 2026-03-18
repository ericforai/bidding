// Input: projectsApi (from @/api), demo project adapter (from @/api/mock-adapters)
// Output: useProjectStore - Pinia store for project management
// Pos: src/stores/ - State management layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { defineStore } from 'pinia'
import { isMockMode, projectsApi } from '@/api'
import { getDemoProjectById } from '@/api/mock-adapters/frontendDemo.js'

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
        if (result?.success) {
          this.projects = result.data || []
        } else if (!isMockMode()) {
          this.projects = []
        }
      } catch (error) {
        if (!isMockMode()) {
          console.warn('API 调用失败，返回空项目列表:', error.message)
          this.projects = []
        }
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
        if (isMockMode()) {
          console.warn('API 获取项目详情失败，使用 mock 数据:', error.message)
        } else {
          console.warn('API 获取项目详情失败，返回空结果:', error.message)
        }
      }

      if (isMockMode()) {
        const mockProject = getDemoProjectById(id)
        this.currentProject = mockProject || null
        return mockProject
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
      const project = this.projects.find(p => p.id === projectId)
      if (project) {
        const task = project.tasks.find(t => t.id === taskId)
        if (task) {
          task.status = status
          // 更新项目进度
          const doneCount = project.tasks.filter(t => t.status === 'done').length
          project.progress = Math.round((doneCount / project.tasks.length) * 100)
        }
      }
    }
  }
})
