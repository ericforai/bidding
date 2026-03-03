import { defineStore } from 'pinia'
import { mockData } from '@/api/mock'

export const useBarStore = defineStore('bar', {
  state: () => ({
    sites: [],
    currentSite: null,
    loading: false
  }),

  getters: {
    activeSites: (state) => state.sites.filter(s => s.status === 'active'),
    riskSites: (state) => state.sites.filter(s => s.hasRisk),
    getSiteById: (state) => (id) => state.sites.find(s => s.id === id),
    getSiteByUrl: (state) => (url) => state.sites.find(s => s.url === url)
  },

  actions: {
    async getSites() {
      this.loading = true
      // 从 mock 数据加载
      this.sites = mockData.barSites || []
      this.loading = false
      return this.sites
    },

    async getSiteById(id) {
      const site = this.sites.find(s => s.id === id)
      this.currentSite = site
      return site
    },

    async checkSiteCapability(siteNameOrUrl) {
      // 根据名称或URL查找站点
      const site = this.sites.find(s =>
        s.name.includes(siteNameOrUrl) ||
        s.url.includes(siteNameOrUrl)
      )
      if (!site) {
        return { found: false, siteNameOrUrl }
      }

      // 计算可投标能力状态
      const hasAccount = site.accounts && site.accounts.length > 0
      const hasAvailableUK = site.uks && site.uks.some(uk => uk.status === 'available')
      const hasRisk = site.uks && site.uks.some(uk => {
        if (!uk.expiryDate) return false
        const daysLeft = Math.ceil((new Date(uk.expiryDate) - new Date()) / (1000 * 60 * 60 * 24))
        return daysLeft <= 30
      })

      let status = 'available' // 可投标
      if (!hasAccount || !hasAvailableUK) {
        status = 'unavailable' // 不可投标
      } else if (hasRisk) {
        status = 'risk' // 有风险
      }

      return {
        found: true,
        site,
        capability: {
          status,
          hasAccount,
          hasAvailableUK,
          hasRisk,
          accountCount: site.accounts?.length || 0,
          ukCount: site.uks?.length || 0,
          availableUkCount: site.uks?.filter(uk => uk.status === 'available').length || 0,
          primaryOwner: site.accounts?.[0]?.owner || '',
          primaryPhone: site.accounts?.[0]?.phone || ''
        }
      }
    },

    async createSite(data) {
      const newSite = {
        id: `S${Date.now()}`,
        status: 'active',
        hasRisk: false,
        accounts: [],
        uks: [],
        attachments: [],
        auditLog: [],
        createTime: new Date().toISOString().split('T')[0],
        ...data
      }
      this.sites.unshift(newSite)
      return newSite
    },

    async updateSite(id, data) {
      const index = this.sites.findIndex(s => s.id === id)
      if (index !== -1) {
        this.sites[index] = { ...this.sites[index], ...data }
        return this.sites[index]
      }
      return null
    },

    async deleteSite(id) {
      const index = this.sites.findIndex(s => s.id === id)
      if (index !== -1) {
        this.sites.splice(index, 1)
        return true
      }
      return false
    },

    // 账号管理
    async addAccount(siteId, accountData) {
      const site = this.sites.find(s => s.id === siteId)
      if (site) {
        const newAccount = {
          id: `A${Date.now()}`,
          status: 'active',
          ...accountData
        }
        if (!site.accounts) site.accounts = []
        site.accounts.push(newAccount)
        return newAccount
      }
      return null
    },

    async updateAccount(siteId, accountId, data) {
      const site = this.sites.find(s => s.id === siteId)
      if (site) {
        const account = site.accounts.find(a => a.id === accountId)
        if (account) {
          Object.assign(account, data)
          return account
        }
      }
      return null
    },

    async deleteAccount(siteId, accountId) {
      const site = this.sites.find(s => s.id === siteId)
      if (site) {
        const index = site.accounts.findIndex(a => a.id === accountId)
        if (index !== -1) {
          site.accounts.splice(index, 1)
          return true
        }
      }
      return false
    },

    // UK管理
    async addUk(siteId, ukData) {
      const site = this.sites.find(s => s.id === siteId)
      if (site) {
        const newUk = {
          id: `UK${Date.now()}`,
          status: 'available',
          ...ukData
        }
        if (!site.uks) site.uks = []
        site.uks.push(newUk)
        this.updateSiteRisk(siteId)
        return newUk
      }
      return null
    },

    async updateUk(siteId, ukId, data) {
      const site = this.sites.find(s => s.id === siteId)
      if (site) {
        const uk = site.uks.find(u => u.id === ukId)
        if (uk) {
          Object.assign(uk, data)
          this.updateSiteRisk(siteId)
          return uk
        }
      }
      return null
    },

    async deleteUk(siteId, ukId) {
      const site = this.sites.find(s => s.id === siteId)
      if (site) {
        const index = site.uks.findIndex(u => u.id === ukId)
        if (index !== -1) {
          site.uks.splice(index, 1)
          this.updateSiteRisk(siteId)
          return true
        }
      }
      return false
    },

    async borrowUk(siteId, ukId, borrowData) {
      const site = this.sites.find(s => s.id === siteId)
      if (site) {
        const uk = site.uks.find(u => u.id === ukId)
        if (uk) {
          uk.status = 'borrowed'
          uk.borrower = borrowData.borrower
          uk.borrowProject = borrowData.project
          uk.borrowPurpose = borrowData.purpose
          uk.borrowTime = new Date().toISOString()
          uk.expectedReturn = borrowData.returnDate

          // 添加操作记录
          if (!site.auditLog) site.auditLog = []
          site.auditLog.unshift({
            time: new Date().toLocaleString('zh-CN'),
            user: borrowData.borrower,
            action: `借用 ${uk.type} (${uk.serialNo})`
          })

          this.updateSiteRisk(siteId)
          return uk
        }
      }
      return null
    },

    async returnUk(siteId, ukId) {
      const site = this.sites.find(s => s.id === siteId)
      if (site) {
        const uk = site.uks.find(u => u.id === ukId)
        if (uk && uk.status === 'borrowed') {
          const borrower = uk.borrower
          uk.status = 'available'
          delete uk.borrower
          delete uk.borrowProject
          delete uk.borrowPurpose
          delete uk.borrowTime
          delete uk.expectedReturn

          // 添加操作记录
          if (!site.auditLog) site.auditLog = []
          site.auditLog.unshift({
            time: new Date().toLocaleString('zh-CN'),
            user: borrower,
            action: `归还 ${uk.type} (${uk.serialNo})`
          })

          this.updateSiteRisk(siteId)
          return uk
        }
      }
      return null
    },

    // 更新站点风险状态
    updateSiteRisk(siteId) {
      const site = this.sites.find(s => s.id === siteId)
      if (!site) return

      let hasRisk = false

      // 检查UK是否即将过期
      if (site.uks) {
        for (const uk of site.uks) {
          if (uk.expiryDate) {
            const daysLeft = Math.ceil((new Date(uk.expiryDate) - new Date()) / (1000 * 60 * 60 * 24))
            if (daysLeft <= 30) {
              hasRisk = true
              break
            }
          }
        }
      }

      // 检查账号是否绑定个人手机号（简化判断：包含个人字样）
      if (site.accounts) {
        for (const account of site.accounts) {
          if (account.phone && !account.phone.includes('****')) {
            // 如果手机号没有脱敏，可能存在风险
            hasRisk = true
            break
          }
        }
      }

      site.hasRisk = hasRisk
    }
  }
})
