import { defineStore } from 'pinia'
import { isMockMode, resourcesApi } from '@/api'

function computeCapability(site) {
  const accounts = Array.isArray(site.accounts) ? site.accounts : []
  const uks = Array.isArray(site.uks) ? site.uks : []
  const hasDetailedChildData = isMockMode() || accounts.length > 0 || uks.length > 0

  const hasAccount = accounts.length > 0
  const hasAvailableUK = uks.some((uk) => uk.status === 'available')
  const hasRisk = Boolean(site.hasRisk) || uks.some((uk) => {
    if (!uk.expiryDate) return false
    const daysLeft = Math.ceil((new Date(uk.expiryDate) - new Date()) / (1000 * 60 * 60 * 24))
    return daysLeft <= 30
  })

  let status = 'available'
  if (hasDetailedChildData && (!hasAccount || (uks.length > 0 && !hasAvailableUK))) {
    status = 'unavailable'
  } else if (!hasDetailedChildData && site.status !== 'active') {
    status = 'unavailable'
  } else if (hasRisk) {
    status = 'risk'
  }

  return {
    status,
    hasAccount,
    hasAvailableUK,
    hasRisk,
    accountCount: accounts.length,
    ukCount: uks.length,
    availableUkCount: uks.filter((uk) => uk.status === 'available').length,
    primaryOwner: accounts[0]?.owner || '',
    primaryPhone: accounts[0]?.phone || '',
  }
}

function buildAuditLog(verifications = [], certificates = [], attachments = []) {
  const verifyLogs = verifications.map((item) => ({
    time: item.verifiedAt || '',
    user: item.verifiedBy || 'system',
    action: item.message || '执行了站点验证',
  }))

  const borrowLogs = certificates.flatMap((certificate) => {
    const records = Array.isArray(certificate.borrowRecords) ? certificate.borrowRecords : []
    return records.map((record) => ({
      time: record.returnedAt || record.borrowedAt || '',
      user: record.borrower || '',
      action: record.status === 'RETURNED'
        ? `归还 ${certificate.type} (${certificate.serialNo})`
        : `借用 ${certificate.type} (${certificate.serialNo})`,
    }))
  })

  const attachmentLogs = attachments.map((item) => ({
    time: item.uploadedAt || '',
    user: item.uploadedBy || 'system',
    action: `上传附件 ${item.name}`,
  }))

  return [...verifyLogs, ...borrowLogs, ...attachmentLogs]
    .filter((item) => item.time || item.action)
    .sort((left, right) => String(right.time).localeCompare(String(left.time)))
}

export const useBarStore = defineStore('bar', {
  state: () => ({
    sites: [],
    currentSite: null,
    loading: false,
  }),

  getters: {
    activeSites: (state) => state.sites.filter((site) => site.status === 'active'),
    riskSites: (state) => state.sites.filter((site) => site.hasRisk),
    getSiteByUrl: (state) => (url) => state.sites.find((site) => site.url === url),
  },

  actions: {
    async getSites(params = {}) {
      this.loading = true
      try {
        const response = await resourcesApi.barSites.getList(params)
        if (!response?.success) {
          return response
        }

        const sites = Array.isArray(response.data) ? response.data : []
        if (!isMockMode()) {
          const withCertificates = await Promise.all(sites.map(async (site) => {
            const accountsResponse = await resourcesApi.barSiteAccounts.getList(site.id)
            const certificatesResponse = await resourcesApi.certificates.getList(site.id)
            const verificationResponse = await resourcesApi.barSites.getVerificationRecords(site.id)
            const verifications = verificationResponse?.success && Array.isArray(verificationResponse.data)
              ? verificationResponse.data
              : []
            const accounts = accountsResponse?.success && Array.isArray(accountsResponse.data)
              ? accountsResponse.data
              : []
            return {
              ...site,
              accounts,
              uks: certificatesResponse?.success && Array.isArray(certificatesResponse.data)
                ? certificatesResponse.data
                : [],
              lastVerifyTime: verifications[0]?.verifiedAt || site.lastVerifyTime,
            }
          }))
          this.sites = withCertificates
        } else {
          this.sites = sites
        }
        return { success: true, data: this.sites }
      } finally {
        this.loading = false
      }
    },

    async getSiteById(id) {
      const existing = this.sites.find((site) => String(site.id) === String(id))
      if (existing) {
        this.currentSite = existing
        return existing
      }

      const response = await resourcesApi.barSites.getDetail(id)
      if (!response?.success) {
        return null
      }

      let site = response.data
      if (site && !isMockMode()) {
        const [accountsResponse, certificatesResponse, verificationResponse, sopResponse, attachmentsResponse] = await Promise.all([
          resourcesApi.barSiteAccounts.getList(id),
          resourcesApi.certificates.getList(id),
          resourcesApi.barSites.getVerificationRecords(id),
          resourcesApi.barSiteSop.get(id),
          resourcesApi.barSiteAttachments.getList(id),
        ])
        const certificates = certificatesResponse?.success && Array.isArray(certificatesResponse.data)
          ? certificatesResponse.data
          : []
        const certificatesWithRecords = await Promise.all(certificates.map(async (certificate) => {
          const borrowRecordsResponse = await resourcesApi.certificates.getBorrowRecords(id, certificate.id)
          return {
            ...certificate,
            borrowRecords: borrowRecordsResponse?.success && Array.isArray(borrowRecordsResponse.data)
              ? borrowRecordsResponse.data
              : [],
          }
        }))
        const verifications = verificationResponse?.success && Array.isArray(verificationResponse.data)
          ? verificationResponse.data
          : []
        const attachments = attachmentsResponse?.success && Array.isArray(attachmentsResponse.data)
          ? attachmentsResponse.data
          : []
        site = {
          ...site,
          accounts: accountsResponse?.success && Array.isArray(accountsResponse.data) ? accountsResponse.data : [],
          uks: certificatesWithRecords,
          sop: sopResponse?.success ? sopResponse.data : null,
          attachments,
          lastVerifyTime: verifications[0]?.verifiedAt || site.lastVerifyTime,
          auditLog: buildAuditLog(verifications, certificatesWithRecords, attachments),
        }
      }

      this.currentSite = site
      if (site) {
        const index = this.sites.findIndex((site) => String(site.id) === String(id))
        if (index === -1) {
          this.sites.push(site)
        } else {
          this.sites[index] = site
        }
      }
      return site
    },

    async checkSiteCapability(siteNameOrUrl) {
      if (!this.sites.length) {
        const response = await this.getSites()
        if (!response?.success) {
          return { found: false, siteNameOrUrl }
        }
      }

      const keyword = String(siteNameOrUrl || '').trim()
      const site = this.sites.find((item) =>
        item.name?.includes(keyword) || item.url?.includes(keyword),
      )

      if (!site) {
        return { found: false, siteNameOrUrl }
      }

      return {
        found: true,
        site,
        capability: computeCapability(site),
      }
    },

    async createSite(data) {
      const response = await resourcesApi.barSites.create(data)
      if (!response?.success) {
        return response
      }

      this.sites.unshift(response.data)
      return response
    },

    async updateSite(id, data) {
      const response = await resourcesApi.barSites.update(id, data)
      if (!response?.success) {
        return response
      }

      const index = this.sites.findIndex((site) => String(site.id) === String(id))
      if (index !== -1) {
        this.sites[index] = response.data
      }
      if (this.currentSite && String(this.currentSite.id) === String(id)) {
        this.currentSite = response.data
      }
      return response
    },

    async deleteSite(id) {
      const response = await resourcesApi.barSites.delete(id)
      if (!response?.success) {
        return response
      }

      const index = this.sites.findIndex((site) => String(site.id) === String(id))
      if (index !== -1) {
        this.sites.splice(index, 1)
      }
      if (this.currentSite && String(this.currentSite.id) === String(id)) {
        this.currentSite = null
      }
      return response
    },

    async addAccount(siteId, accountData) {
      if (!isMockMode()) {
        const response = await resourcesApi.barSiteAccounts.create(siteId, accountData)
        if (response?.success) {
          await this.getSiteById(siteId)
        }
        return response
      }

      const site = this.sites.find((item) => String(item.id) === String(siteId))
      if (!site) return { success: false, message: '站点不存在' }

      const newAccount = {
        id: `A${Date.now()}`,
        status: 'active',
        ...accountData,
      }
      site.accounts = Array.isArray(site.accounts) ? site.accounts : []
      site.accounts.push(newAccount)
      this.updateSiteRisk(siteId)
      return { success: true, data: newAccount }
    },

    async updateAccount(siteId, accountId, data) {
      if (!isMockMode()) {
        const response = await resourcesApi.barSiteAccounts.update(siteId, accountId, data)
        if (response?.success) {
          await this.getSiteById(siteId)
        }
        return response
      }

      const site = this.sites.find((item) => String(item.id) === String(siteId))
      const account = site?.accounts?.find((item) => String(item.id) === String(accountId))
      if (!account) return { success: false, message: '账号不存在' }

      Object.assign(account, data)
      this.updateSiteRisk(siteId)
      return { success: true, data: account }
    },

    async deleteAccount(siteId, accountId) {
      if (!isMockMode()) {
        const response = await resourcesApi.barSiteAccounts.delete(siteId, accountId)
        if (response?.success) {
          await this.getSiteById(siteId)
        }
        return response
      }

      const site = this.sites.find((item) => String(item.id) === String(siteId))
      if (!site?.accounts) return { success: false, message: '账号不存在' }

      const index = site.accounts.findIndex((item) => String(item.id) === String(accountId))
      if (index === -1) return { success: false, message: '账号不存在' }

      site.accounts.splice(index, 1)
      this.updateSiteRisk(siteId)
      return { success: true }
    },

    async addUk(siteId, ukData) {
      if (!isMockMode()) {
        return resourcesApi.certificates.create(siteId, ukDataToPayload(ukData))
      }

      const site = this.sites.find((item) => String(item.id) === String(siteId))
      if (!site) return { success: false, message: '站点不存在' }

      const newUk = {
        id: `UK${Date.now()}`,
        status: 'available',
        ...ukData,
      }
      site.uks = Array.isArray(site.uks) ? site.uks : []
      site.uks.push(newUk)
      this.updateSiteRisk(siteId)
      return { success: true, data: newUk }
    },

    async updateUk(siteId, ukId, data) {
      if (!isMockMode()) {
        return resourcesApi.certificates.update(siteId, ukId, ukDataToPayload(data))
      }

      const site = this.sites.find((item) => String(item.id) === String(siteId))
      const uk = site?.uks?.find((item) => String(item.id) === String(ukId))
      if (!uk) return { success: false, message: 'UK 不存在' }

      Object.assign(uk, data)
      this.updateSiteRisk(siteId)
      return { success: true, data: uk }
    },

    async deleteUk(siteId, ukId) {
      if (!isMockMode()) {
        return resourcesApi.certificates.delete(siteId, ukId)
      }

      const site = this.sites.find((item) => String(item.id) === String(siteId))
      if (!site?.uks) return { success: false, message: 'UK 不存在' }

      const index = site.uks.findIndex((item) => String(item.id) === String(ukId))
      if (index === -1) return { success: false, message: 'UK 不存在' }

      site.uks.splice(index, 1)
      this.updateSiteRisk(siteId)
      return { success: true }
    },

    async borrowUk(siteId, ukId, borrowData) {
      if (!isMockMode()) {
        return resourcesApi.certificates.borrow(siteId, ukId, {
          borrower: borrowData.borrower,
          projectId: borrowData.projectId ? Number(borrowData.projectId) : null,
          purpose: borrowData.purpose,
          remark: borrowData.remark,
          expectedReturnDate: borrowData.returnDate,
        })
      }

      const site = this.sites.find((item) => String(item.id) === String(siteId))
      const uk = site?.uks?.find((item) => String(item.id) === String(ukId))
      if (!uk) return { success: false, message: 'UK 不存在' }

      uk.status = 'borrowed'
      uk.borrower = borrowData.borrower
      uk.borrowProject = borrowData.project
      uk.borrowPurpose = borrowData.purpose
      uk.borrowTime = new Date().toISOString()
      uk.expectedReturn = borrowData.returnDate

      site.auditLog = Array.isArray(site.auditLog) ? site.auditLog : []
      site.auditLog.unshift({
        time: new Date().toLocaleString('zh-CN'),
        user: borrowData.borrower,
        action: `借用 ${uk.type} (${uk.serialNo})`,
      })

      this.updateSiteRisk(siteId)
      return { success: true, data: uk }
    },

    async returnUk(siteId, ukId) {
      if (!isMockMode()) {
        return resourcesApi.certificates.return(siteId, ukId, {})
      }

      const site = this.sites.find((item) => String(item.id) === String(siteId))
      const uk = site?.uks?.find((item) => String(item.id) === String(ukId))
      if (!uk || uk.status !== 'borrowed') {
        return { success: false, message: 'UK 当前不可归还' }
      }

      const borrower = uk.borrower
      uk.status = 'available'
      delete uk.borrower
      delete uk.borrowProject
      delete uk.borrowPurpose
      delete uk.borrowTime
      delete uk.expectedReturn

      site.auditLog = Array.isArray(site.auditLog) ? site.auditLog : []
      site.auditLog.unshift({
        time: new Date().toLocaleString('zh-CN'),
        user: borrower,
        action: `归还 ${uk.type} (${uk.serialNo})`,
      })

      this.updateSiteRisk(siteId)
      return { success: true, data: uk }
    },

    async updateSiteStatus(siteId, status) {
      const response = await resourcesApi.barSites.updateStatus(siteId, status)
      if (response?.success) {
        await this.getSiteById(siteId)
      }
      return response
    },

    async verifySite(siteId, payload = {}) {
      const response = await resourcesApi.barSites.verify(siteId, payload)
      if (response?.success) {
        await this.getSiteById(siteId)
      }
      return response
    },

    async updateSop(siteId, sop) {
      const response = await resourcesApi.barSiteSop.update(siteId, sop)
      if (response?.success) {
        await this.getSiteById(siteId)
      }
      return response
    },

    async addAttachment(siteId, attachment) {
      const response = await resourcesApi.barSiteAttachments.create(siteId, attachment)
      if (response?.success) {
        await this.getSiteById(siteId)
      }
      return response
    },

    async deleteAttachment(siteId, attachmentId) {
      const response = await resourcesApi.barSiteAttachments.delete(siteId, attachmentId)
      if (response?.success) {
        await this.getSiteById(siteId)
      }
      return response
    },

    updateSiteRisk(siteId) {
      const site = this.sites.find((item) => String(item.id) === String(siteId))
      if (!site) return

      const accounts = Array.isArray(site.accounts) ? site.accounts : []
      const uks = Array.isArray(site.uks) ? site.uks : []

      const ukRisk = uks.some((uk) => {
        if (!uk.expiryDate) return false
        const daysLeft = Math.ceil((new Date(uk.expiryDate) - new Date()) / (1000 * 60 * 60 * 24))
        return daysLeft <= 30
      })

      const accountRisk = accounts.some((account) => account.phone && !account.phone.includes('****'))
      site.hasRisk = ukRisk || accountRisk
      site.riskLevel = site.hasRisk ? 'high' : 'low'
    },
  },
})

function ukDataToPayload(data) {
  return {
    type: data.type,
    provider: data.provider,
    serialNo: data.serialNo,
    holder: data.holder,
    location: data.location,
    expiryDate: data.expiryDate,
    remark: data.remark,
  }
}
