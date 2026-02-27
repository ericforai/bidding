import { defineStore } from 'pinia'
import { mockData } from '@/api/mock'

export const useProjectStore = defineStore('project', {
  state: () => ({
    projects: mockData.projects,
    currentProject: null
  }),

  getters: {
    inProgressProjects: (state) => state.projects.filter(p => p.status !== 'won' && p.status !== 'lost'),
    wonProjects: (state) => state.projects.filter(p => p.status === 'won'),
    getProjectById: (state) => (id) => state.projects.find(p => p.id === id)
  },

  actions: {
    async getProjects() {
      return this.projects
    },

    async getProjectById(id) {
      const project = this.projects.find(p => p.id === id)
      this.currentProject = project
      return project
    },

    async createProject(data) {
      const newProject = {
        id: `P${Date.now()}`,
        status: 'pending',
        progress: 0,
        tasks: [],
        documents: [],
        createTime: new Date().toISOString().split('T')[0],
        ...data
      }
      this.projects.unshift(newProject)
      return newProject
    },

    async updateProject(id, data) {
      const index = this.projects.findIndex(p => p.id === id)
      if (index !== -1) {
        this.projects[index] = { ...this.projects[index], ...data }
      }
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
